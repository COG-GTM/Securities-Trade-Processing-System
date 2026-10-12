#!/usr/bin/env python3
"""FS adversarial swarm runner for the STPS trade-capture service.

  plan     build one child prompt per attacker persona (dry-run) or launch them (--live, needs DEVIN_API_KEY)
  confirm  run the swarm confirmation tests (mvn -Pswarm test) and map each test to a finding
  collect  (live only) pull each child's structured findings into swarm/out/persona-findings.json
  report   deduplicate persona findings, join test results, write swarm/out/swarm-report.{md,html}

Fan-out mechanics follow Demo 1 (traderx arden/finserv-estate/demo/scanner/fanout.py): one child per unit of work,
prompts written to disk first, live mode via POST /v1/sessions (https://docs.devin.ai/api-reference).
"""
import argparse, glob, html, json, os, re, subprocess, sys, time, urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = HERE / "out"
SEV_RANK = {"critical": 0, "high": 1, "medium": 2, "low": 3}
G, R, Y, B, X = "\033[32m", "\033[31m", "\033[33m", "\033[1m", "\033[0m"


def load(p):
    return json.loads(Path(p).read_text())


def build_prompt(persona, cfg, playbook):
    t = cfg["target"]
    lines = [
        f"Adversarial swarm child: persona {persona['id']} ({persona['name']}).",
        f"Repo: https://github.com/{t['repo']} (base branch {t['base_branch']}).",
        f"Goal: {persona['goal']}",
        "Techniques to try: " + "; ".join(persona["techniques"]) + ".",
        "Attack surface: " + ", ".join(t["surface"]) + ".",
    ]
    if persona.get("report_only"):
        lines.append("This persona is report-only: third-party findings get an advisory, never an in-place patch.")
    return "\n".join(lines) + "\n\nFollow this playbook exactly:\n\n" + playbook


def create_session(prompt, title, tags):
    key = os.environ["DEVIN_API_KEY"]
    body = json.dumps({"prompt": prompt, "title": title, "tags": tags, "unlisted": False, "idempotent": True}).encode()
    req = urllib.request.Request(os.environ.get("DEVIN_API", "https://api.devin.ai") + "/v1/sessions", data=body,
                                 headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())


def cmd_plan(a):
    cfg = load(HERE / "personas.json")
    playbook = (HERE / "playbooks" / "fs-swarm-persona-child.md").read_text()
    pdir = OUT / "prompts"
    pdir.mkdir(parents=True, exist_ok=True)
    plan = {"mode": "live" if a.live else "dry-run", "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "target": cfg["target"]["repo"], "children": []}
    print(f"{B}Swarm fan-out ({plan['mode']}): {len(cfg['personas'])} attacker personas -> {cfg['target']['repo']}{X}")
    for i, p in enumerate(cfg["personas"], 1):
        prompt = build_prompt(p, cfg, playbook)
        f = pdir / f"{i:02d}-{p['id'].lower()}.md"
        f.write_text(prompt)
        child = {"persona": p["id"], "title": f"[swarm] STPS {p['id']}: {p['name']}", "prompt_file": str(f.relative_to(ROOT))}
        if a.live:
            child["session"] = create_session(prompt, child["title"], ["fs-swarm", "stps", p["id"].lower()])
        plan["children"].append(child)
        mark = "report-only" if p.get("report_only") else "fix-pr"
        print(f"  child {i}  {p['id']:<7} {p['name']:<45} disposition={mark}")
    (OUT / "fanout-plan.json").write_text(json.dumps(plan, indent=2))
    print(f"plan -> {(OUT / 'fanout-plan.json').relative_to(ROOT)}")


def run_confirm():
    env = dict(os.environ)
    jdk21 = "/usr/lib/jvm/java-21-openjdk-amd64"
    if "21" not in env.get("JAVA_HOME", "") and os.path.isdir(jdk21):
        env["JAVA_HOME"] = jdk21
    for f in glob.glob(str(ROOT / "target/surefire-reports/*")):
        os.remove(f)
    proc = subprocess.run(["mvn", "-q", "-B", "-o", "test", "-Pswarm"], cwd=ROOT, env=env,
                          capture_output=True, text=True)
    reports = glob.glob(str(ROOT / "target/surefire-reports/TEST-*.xml"))
    if proc.returncode != 0 or (not reports and "Tests run" not in proc.stdout and "No tests" not in proc.stdout
                                and glob.glob(str(ROOT / "src/test/java/**/swarm/*.java"), recursive=True)):
        sys.stderr.write(proc.stdout[-4000:] + proc.stderr[-4000:])
        sys.exit(f"confirm: Maven build failed (exit {proc.returncode}); no confirmation results recorded")
    results = {}
    for x in reports:
        for tc in ET.parse(x).getroot().iter("testcase"):
            name = tc.get("name").split("(")[0]
            m = re.match(r"([a-z]+)(\d+)_", name)
            if not m:
                continue
            fid = f"{m.group(1).upper()}-{m.group(2)}"
            failed = tc.find("failure") is not None or tc.find("error") is not None
            results[fid] = {"test": f"{tc.get('classname').split('.')[-1]}#{name}", "exploit_reproduces": failed}
    return results


def cmd_confirm(a):
    print(f"{B}Confirming findings: mvn -Pswarm test (each test asserts the SAFE behavior){X}")
    res = run_confirm()
    if not res:
        print(f"{Y}no swarm confirmation tests found{X}")
    for fid, r in sorted(res.items()):
        state = f"{R}EXPLOIT REPRODUCES{X}" if r["exploit_reproduces"] else f"{G}DEFENDED{X}"
        print(f"  {fid:<9} {state:<30} {r['test']}")
    OUT.mkdir(exist_ok=True)
    (OUT / "confirm.json").write_text(json.dumps(res, indent=2))


def pr_link(url):
    if not url or not re.fullmatch(r"https://github\.com/[\w.-]+/[\w.-]+/pull/\d+", url):
        return "-"
    return f"<a href='{html.escape(url, quote=True)}'>PR</a>"


def get_session(sid):
    req = urllib.request.Request(os.environ.get("DEVIN_API", "https://api.devin.ai") + f"/v1/session/{sid}",
                                 headers={"Authorization": f"Bearer {os.environ['DEVIN_API_KEY']}"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())


def cmd_collect(a):
    plan = load(OUT / "fanout-plan.json")
    findings, pending = [], []
    for c in plan["children"]:
        sid = (c.get("session") or {}).get("session_id")
        if not sid:
            sys.exit("collect: fanout-plan.json has no live sessions; run `plan --live` first")
        s = get_session(sid)
        so = s.get("structured_output") or {}
        if not so.get("findings"):
            pending.append(f"{c['persona']} ({s.get('status_enum') or s.get('status')})")
            continue
        findings += so["findings"]
    (OUT / "persona-findings.json").write_text(json.dumps(findings, indent=2))
    print(f"collected {len(findings)} findings" + (f"; still running: {', '.join(pending)}" if pending else ""))
    if pending:
        sys.exit(1)


def default_findings():
    live = OUT / "persona-findings.json"
    return str(live if live.exists() else HERE / "fixtures" / "persona-findings.json")


def cmd_report(a):
    raw = load(a.findings or default_findings())
    confirm = load(OUT / "confirm.json") if (OUT / "confirm.json").exists() else {}
    groups = {}
    for f in raw:
        # Only merge findings a persona marked as the same root cause; distinct bugs in one method stay separate.
        groups.setdefault((f["cwe"], f["sink"], f.get("root_cause") or f["id"]), []).append(f)
    rows = []
    for fs in groups.values():
        fs.sort(key=lambda f: SEV_RANK[f["severity"]])
        lead = dict(fs[0])
        lead["duplicates"] = [f["id"] for f in fs[1:]]
        lead["reported_by"] = sorted({f["persona"] for f in fs})
        c = confirm.get(lead["id"]) or next((confirm[d] for d in lead["duplicates"] if d in confirm), None)
        lead["test_state"] = ("reproduces" if c["exploit_reproduces"] else "defended") if c else (
            "n/a (third-party)" if lead["disposition"] == "report-only" else "no test")
        rows.append(lead)
    rows.sort(key=lambda r: (SEV_RANK[r["severity"]], r["id"]))
    for i, r in enumerate(rows, 1):
        r["swarm_id"] = f"SW-{i}"
    print(f"{B}Dedupe: {len(raw)} raw persona findings -> {len(rows)} unique{X}")
    for r in rows:
        dup = f" (dupes: {', '.join(r['duplicates'])})" if r["duplicates"] else ""
        print(f"  {r['swarm_id']:<5} {r['severity']:<8} {r['disposition']:<12} {r['test_state']:<18} {r['title']}{dup}")
    md = ["# Adversarial swarm report: STPS trade capture", "",
          f"{len(raw)} raw findings from {len({f['persona'] for f in raw})} personas, {len(rows)} after dedupe.", "",
          "| Swarm id | Severity | Finding | CWE | Sink | Reported by | Confirmation test | Test on main | Disposition | PR |",
          "|---|---|---|---|---|---|---|---|---|---|"]
    for r in rows:
        md.append(f"| {r['swarm_id']} | {r['severity']} | {r['title']} | {r['cwe']} | `{r['sink']}` | {', '.join(r['reported_by'])} | "
                  f"{r.get('confirmed_by_test') or 'none'} | {r['test_state']} | {r['disposition']} | {r.get('pr') or 'none'} |")
    (OUT / "swarm-report.md").write_text("\n".join(md) + "\n")
    color = {"critical": "#b00020", "high": "#d9480f", "medium": "#e8a400", "low": "#2b8a3e"}
    trs = "".join(
        f"<tr><td>{r['swarm_id']}</td><td style='color:{color[r['severity']]};font-weight:600'>{r['severity']}</td>"
        f"<td>{html.escape(r['title'])}</td><td>{r['cwe']}</td><td><code>{html.escape(r['sink'])}</code></td>"
        f"<td>{', '.join(r['reported_by'])}</td><td>{html.escape(r['test_state'])}</td><td>{r['disposition']}</td>"
        f"<td>{pr_link(r.get('pr'))}</td></tr>" for r in rows)
    (OUT / "swarm-report.html").write_text(
        "<!doctype html><meta charset=utf-8><title>STPS adversarial swarm</title><style>body{font-family:system-ui;margin:32px}"
        "table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:6px 10px;text-align:left}th{background:#f3f3f3}</style>"
        f"<h1>Adversarial swarm: STPS trade capture</h1><p>{len(raw)} raw persona findings, {len(rows)} unique after dedupe. "
        "Every fix PR carries a test that failed on main before the fix.</p><table><tr><th>Swarm id</th><th>Severity</th>"
        "<th>Finding</th><th>CWE</th><th>Sink</th><th>Reported by</th><th>Test on main</th><th>Disposition</th><th>PR</th></tr>"
        + trs + "</table>")
    print(f"report -> swarm/out/swarm-report.md, swarm/out/swarm-report.html")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    sp = ap.add_subparsers(dest="cmd", required=True)
    p = sp.add_parser("plan"); p.add_argument("--live", action="store_true"); p.set_defaults(fn=cmd_plan)
    sp.add_parser("confirm").set_defaults(fn=cmd_confirm)
    sp.add_parser("collect").set_defaults(fn=cmd_collect)
    p = sp.add_parser("report"); p.add_argument("--findings", default=None)
    p.set_defaults(fn=cmd_report)
    a = ap.parse_args()
    a.fn(a)
