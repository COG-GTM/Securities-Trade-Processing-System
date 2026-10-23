#!/usr/bin/env python3
"""Adversarial swarm runner for the STPS trade APIs.

  plan      build one child prompt per attacker persona (--live launches them, needs DEVIN_API_KEY)
  confirm   run the swarm tests and report Surefire counts, not the Maven exit code
  verify    re-verify one child branch: red at the test commit, green at the head, regression green
  conflicts pairwise trial merges of the fix branches onto the base branch
  report    dedupe and rank findings, write swarm/out/swarm-report.{md,html}

The -Pswarm profile sets testFailureIgnore, so `BUILD SUCCESS` says nothing about the tests. Everything
in here reads the Surefire result files (Tests run / Failures / Errors) instead.
"""
import argparse, glob, html, json, os, re, shutil, subprocess, sys, time, urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
OUT = HERE / "out"
REPORTS = ROOT / "target/surefire-reports"
JDK21 = "/usr/lib/jvm/java-21-openjdk-amd64"
CONFLICT_MARKER = re.compile(r"^[+ ]?<{7} ", re.M)
PERSONA_RANK = {"LOGIC": 0, "RACE": 1, "AUTHZ": 2, "INJECT": 3, "SUPPLY": 4}
SEV_RANK = {"critical": 0, "high": 1, "medium": 2, "low": 3}
G, R, Y, B, X = "\033[32m", "\033[31m", "\033[33m", "\033[1m", "\033[0m"


def load(p):
    return json.loads(Path(p).read_text())


def target():
    return load(HERE / "target.json")


def personas():
    return sorted((load(p) for p in HERE.glob("personas/*.json")), key=lambda p: p.get("order", 99))


def git(*args, check=True, cwd=ROOT):
    p = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    if check and p.returncode != 0:
        sys.exit(f"git {' '.join(args)} failed: {p.stderr.strip()}")
    return p.stdout.strip()


# ---------------------------------------------------------------- test results

def parse_reports(d=REPORTS):
    """Surefire XML -> {Class#method: 'pass'|'fail'} plus totals. The only source of truth for red/green."""
    cases, totals = {}, {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for x in sorted(glob.glob(str(Path(d) / "TEST-*.xml"))):
        for tc in ET.parse(x).getroot().iter("testcase"):
            name = tc.get("name").split("(")[0]
            cls = (tc.get("classname") or "").split(".")[-1]
            if tc.find("skipped") is not None:
                totals["skipped"] += 1
                continue
            failed = tc.find("failure") is not None
            errored = tc.find("error") is not None
            totals["tests"] += 1
            totals["failures"] += 1 if failed else 0
            totals["errors"] += 1 if errored else 0
            cases[f"{cls}#{name}"] = "fail" if failed or errored else "pass"
    return cases, totals


def run_tests(selector=None, fail_fast_on_build_error=True):
    """mvn -Pswarm test; returns (cases, totals). Dies only if the build itself broke (no results at all)."""
    env = dict(os.environ)
    if "21" not in env.get("JAVA_HOME", "") and os.path.isdir(JDK21):
        env["JAVA_HOME"] = JDK21
    shutil.rmtree(REPORTS, ignore_errors=True)
    cmd = ["mvn", "-B", "-Pswarm", "test"]
    if (HERE / "maven-settings.xml").exists():
        cmd[1:1] = ["-s", str(HERE / "maven-settings.xml")]
    if selector:
        cmd.append(f"-Dtest={selector}")
        cmd.append("-DfailIfNoSpecifiedTests=false")
    proc = subprocess.run(cmd, cwd=ROOT, env=env, capture_output=True, text=True)
    cases, totals = parse_reports()
    if totals["tests"] == 0 and fail_fast_on_build_error:
        sys.stderr.write(proc.stdout[-6000:] + proc.stderr[-3000:])
        sys.exit(f"{R}confirm: no test results produced (compile or build error, mvn exit {proc.returncode}){X}")
    return cases, totals


def summarize(cases, totals, label=""):
    state = "RED" if totals["failures"] or totals["errors"] else "GREEN"
    c = R if state == "RED" else G
    print(f"{B}{label}{X}Tests run: {totals['tests']}, Failures: {totals['failures']}, "
          f"Errors: {totals['errors']}, Skipped: {totals['skipped']} -> {c}{state}{X}")
    for name, st in sorted(cases.items()):
        if st == "fail":
            print(f"  {R}FAIL{X} {name}")
    return state


def cmd_confirm(a):
    cases, totals = run_tests(a.test)
    state = summarize(cases, totals)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "confirm.json").write_text(json.dumps({"cases": cases, "totals": totals, "state": state}, indent=2))
    if a.expect and a.expect.upper() != state:
        sys.exit(f"{R}confirm: expected {a.expect.upper()}, got {state}{X}")


# ---------------------------------------------------------------- verification

def swarm_tests_changed(base, ref):
    files = git("diff", "--name-only", f"{base}...{ref}").splitlines()
    return [f for f in files if "/swarm/" in f and f.endswith("SwarmTest.java")]


def cmd_verify(a):
    """A child branch is only confirmed if its own test fails at the test commit and passes at the head."""
    base = a.base or target()["base_branch"]
    ref = a.branch
    git("fetch", "-q", "origin", base, check=False)
    head = git("rev-parse", ref)
    start = git("rev-parse", f"origin/{base}" if git("rev-parse", "--verify", "-q", f"origin/{base}", check=False) else base)
    commits = git("rev-list", "--reverse", f"{start}..{head}").splitlines()
    if not commits:
        sys.exit(f"{R}verify: {ref} has no commits on top of {base}{X}")
    tests = swarm_tests_changed(start, head)
    if not tests:
        sys.exit(f"{R}verify: {ref} adds no @Tag(\"swarm\") confirmation test{X}")
    selectors = ",".join(sorted({Path(t).stem for t in tests}))
    test_commit = next((c for c in commits if any(f in swarm_tests_changed(start, c) for f in tests)), None)
    if test_commit is None:
        sys.exit(f"{R}verify: cannot find the commit that adds {selectors}{X}")
    touches_main = [f for f in git("diff", "--name-only", f"{start}...{head}").splitlines() if f.startswith("src/main/")]
    stash = git("rev-parse", "--abbrev-ref", "HEAD")
    stash = git("rev-parse", "HEAD") if stash == "HEAD" else stash
    results = {}
    try:
        for stage, rev in (("red", test_commit), ("green", head)):
            git("checkout", "-q", "--detach", rev)
            cases, totals = run_tests(selectors)
            results[stage] = (cases, totals, summarize(cases, totals, f"{stage} @ {rev[:8]}  "))
        git("checkout", "-q", "--detach", head)
        rc, rt = run_tests("TradeLifecycleRegressionTest")
        results["regression"] = (rc, rt, summarize(rc, rt, "regression @ head  "))
    finally:
        git("checkout", "-q", stash, check=False)
    ok = (results["red"][2] == "RED" and results["regression"][2] == "GREEN"
          and (results["green"][2] == "GREEN" if touches_main else True))
    report_only = not touches_main
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"verify-{ref.replace('/', '_')}.json").write_text(json.dumps(
        {"branch": ref, "tests": selectors, "test_commit": test_commit, "head": head, "report_only": report_only,
         "stages": {k: v[1] for k, v in results.items()}, "verified": bool(ok)}, indent=2))
    if not ok:
        sys.exit(f"{R}NOT VERIFIED{X} {ref}: red={results['red'][2]} green={results['green'][2]} "
                 f"regression={results['regression'][2]}")
    suffix = " (report-only: test proves the exposure, no in-place fix)" if report_only else ""
    print(f"{G}VERIFIED{X} {ref}: {selectors} red at {test_commit[:8]}, "
          f"{'green' if touches_main else 'red (no fix expected)'} at {head[:8]}, regression green{suffix}")


def cmd_conflicts(a):
    base = a.base or target()["base_branch"]
    git("fetch", "-q", "origin", base, check=False)
    branches = a.branches
    bad = []
    for i, x in enumerate(branches):
        for y in branches[i + 1:]:
            merged = git("merge-tree", f"origin/{base}", x, y, check=False)
            if CONFLICT_MARKER.search(merged):
                bad.append((x, y))
                print(f"  {R}CONFLICT{X} {x} <> {y}")
            else:
                print(f"  {G}ok{X}       {x} <> {y}")
    if bad:
        sys.exit(f"{R}conflicts: {len(bad)} conflicting pair(s); rebase or combine before opening PRs{X}")
    print(f"{G}no conflicts{X} across {len(branches)} fix branches")


# ---------------------------------------------------------------- fan-out

def build_prompt(persona, cfg, playbook, run_id):
    lines = [
        f"Adversarial swarm child: persona {persona['id']} ({persona['name']}). Run id: {run_id}.",
        f"Repo: https://github.com/{cfg['repo']} (base branch `{cfg['base_branch']}`, already cloned on your machine).",
        f"Goal: {persona['goal']}",
        "Techniques to try: " + "; ".join(persona["techniques"]) + ".",
        "Attack surface: " + ", ".join(cfg["surface"]) + ".",
        f"Your branch prefix: `{cfg['child_branch_prefix']}-{run_id}-<finding-id-lower>`.",
    ]
    if persona.get("report_only"):
        lines.append("You are report-only: third-party findings get an advisory with evidence, never an in-place patch, "
                     "never a dependency or parent bump.")
    return "\n".join(lines) + "\n\nFollow this playbook exactly:\n\n" + playbook


def create_session(prompt, title, tags):
    body = json.dumps({"prompt": prompt, "title": title, "tags": tags, "idempotent": True}).encode()
    req = urllib.request.Request(os.environ.get("DEVIN_API", "https://api.devin.ai") + "/v1/sessions", data=body,
                                 headers={"Authorization": f"Bearer {os.environ['DEVIN_API_KEY']}",
                                          "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())


def cmd_plan(a):
    cfg, playbook = target(), (HERE / "playbooks" / "fs-swarm-persona-child.md").read_text()
    run_id = a.run_id or time.strftime("%Y%m%d-%H%M%S", time.gmtime())
    pdir = OUT / "prompts"
    pdir.mkdir(parents=True, exist_ok=True)
    plan = {"run_id": run_id, "mode": "live" if a.live else "dry-run", "target": cfg["repo"], "children": []}
    print(f"{B}Swarm fan-out ({plan['mode']}, run {run_id}): {len(personas())} personas -> {cfg['repo']}{X}")
    for i, p in enumerate(personas(), 1):
        prompt = build_prompt(p, cfg, playbook, run_id)
        f = pdir / f"{i:02d}-{p['id'].lower()}.md"
        f.write_text(prompt)
        child = {"persona": p["id"], "prompt_file": str(f.relative_to(ROOT))}
        if a.live:
            s = create_session(prompt, f"swarm {run_id} {p['id']}: {p['name']}", ["fs-swarm", f"run-{run_id}"])
            child["session_url"] = s.get("url")
            child["session_id"] = s.get("session_id")
        print(f"  {p['id']:<7} {child.get('session_url') or child['prompt_file']}")
        plan["children"].append(child)
    (OUT / "plan.json").write_text(json.dumps(plan, indent=2))
    print(f"plan -> swarm/out/plan.json")


# ---------------------------------------------------------------- report

def pr_link(u):
    return f"<a href='{html.escape(u)}'>{html.escape(u.rsplit('/', 1)[-1])}</a>" if u else "-"


def dedupe(raw):
    groups = {}
    for f in raw:
        groups.setdefault((f["cwe"], f["sink"], f.get("root_cause") or f["id"]), []).append(f)
    rows = []
    for fs in groups.values():
        fs.sort(key=lambda f: SEV_RANK[f["severity"]])
        lead = dict(fs[0])
        lead["duplicates"] = [f["id"] for f in fs[1:]]
        lead["reported_by"] = sorted({f["persona"] for f in fs})
        rows.append(lead)
    rows.sort(key=lambda r: (0 if r.get("disposition") == "fix" else 1, SEV_RANK[r["severity"]],
                             PERSONA_RANK.get(r["persona"], 9), r["id"]))
    for i, r in enumerate(rows, 1):
        r["swarm_id"] = f"SW-{i}"
    return rows


def cmd_report(a):
    raw = load(a.findings)
    rows = dedupe(raw)
    for r in rows:
        v = OUT / f"verify-{r.get('branch', '').replace('/', '_')}.json"
        r["verified"] = bool(r.get("branch")) and v.exists() and load(v)["verified"]
    print(f"{B}Dedupe: {len(raw)} raw persona findings -> {len(rows)} unique{X}")
    for r in rows:
        dup = f" (dupes: {', '.join(r['duplicates'])})" if r["duplicates"] else ""
        mark = f"{G}verified{X}" if r["verified"] else f"{Y}unverified{X}"
        print(f"  {r['swarm_id']:<5} {r['severity']:<8} {r['disposition']:<11} {mark:<20} {r['title']}{dup}")
    hdr = "| Swarm id | Severity | Finding | Impact | CWE | Sink | Reported by | Confirmation test | Red then green | Disposition | PR |"
    md = ["# Adversarial swarm report: STPS trade APIs", "",
          f"{len(raw)} raw findings from {len({f['persona'] for f in raw})} personas, {len(rows)} after dedupe. "
          "Every fix PR carries a test that failed on the base branch before the fix and passes after it.", "",
          hdr, "|---" * 11 + "|"]
    for r in rows:
        md.append(f"| {r['swarm_id']} | {r['severity']} | {r['title']} | {r.get('impact', '')} | {r['cwe']} | `{r['sink']}` | "
                  f"{', '.join(r['reported_by'])} | `{r.get('test') or 'none'}` | {'yes' if r['verified'] else 'no'} | "
                  f"{r['disposition']} | {r.get('pr') or 'none'} |")
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "swarm-report.md").write_text("\n".join(md) + "\n")
    color = {"critical": "#b00020", "high": "#d9480f", "medium": "#e8a400", "low": "#2b8a3e"}
    trs = "".join(
        f"<tr><td>{r['swarm_id']}</td><td style='color:{color[r['severity']]};font-weight:600'>{r['severity']}</td>"
        f"<td>{html.escape(r['title'])}</td><td>{html.escape(r.get('impact', ''))}</td><td>{r['cwe']}</td>"
        f"<td><code>{html.escape(r['sink'])}</code></td><td>{', '.join(r['reported_by'])}</td>"
        f"<td><code>{html.escape(r.get('test') or '-')}</code></td>"
        f"<td>{'yes' if r['verified'] else 'no'}</td><td>{r['disposition']}</td><td>{pr_link(r.get('pr'))}</td></tr>"
        for r in rows)
    (OUT / "swarm-report.html").write_text(
        "<!doctype html><meta charset=utf-8><title>STPS adversarial swarm</title><style>body{font-family:system-ui;margin:32px}"
        "table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:4px 8px;text-align:left;font-size:13px}"
        "code{font-size:11px}th{background:#f3f3f3}</style><h1>Adversarial swarm: STPS trade APIs</h1>"
        f"<p>{len(raw)} raw persona findings, {len(rows)} unique after dedupe.</p><table><tr>"
        + "".join(f"<th>{h.strip()}</th>" for h in hdr.strip('|').split('|')) + "</tr>" + trs + "</table>")
    print("report -> swarm/out/swarm-report.md, swarm/out/swarm-report.html")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    sp = ap.add_subparsers(dest="cmd", required=True)
    p = sp.add_parser("plan"); p.add_argument("--live", action="store_true"); p.add_argument("--run-id")
    p.set_defaults(fn=cmd_plan)
    p = sp.add_parser("confirm"); p.add_argument("--test"); p.add_argument("--expect", choices=["red", "green", "RED", "GREEN"])
    p.set_defaults(fn=cmd_confirm)
    p = sp.add_parser("verify"); p.add_argument("branch"); p.add_argument("--base"); p.set_defaults(fn=cmd_verify)
    p = sp.add_parser("conflicts"); p.add_argument("branches", nargs="+"); p.add_argument("--base")
    p.set_defaults(fn=cmd_conflicts)
    p = sp.add_parser("report"); p.add_argument("--findings", default=str(OUT / "findings.json"))
    p.set_defaults(fn=cmd_report)
    a = ap.parse_args()
    a.fn(a)
