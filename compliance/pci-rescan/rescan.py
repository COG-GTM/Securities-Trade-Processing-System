#!/usr/bin/env python3
"""Quarterly PCI rescan: scan, diff against last quarter's ledger, prioritise by risk, route, and write an evidence pack.

    python3 compliance/pci-rescan/rescan.py --workspace ~/pci-workspace            # live: Maven + OSV.dev
    python3 compliance/pci-rescan/rescan.py --snapshot fixtures/2026-Q4-scan.json  # replay a recorded scan

Steps
  1. collect   Resolve each in-scope repo's Maven dependencies and query OSV.dev (https://osv.dev) for known
               vulnerabilities. Scan Spring config files for literal credentials.
  2. diff      Compare with the prior quarter's ledger (baseline/<YYYY-Qn>.json):
                 new       first seen this quarter
                 carried   already open last quarter (keeps its owner, first_seen and due date)
                 closed    open last quarter, not reported now (the rescan confirms the fix)
                 accepted  reported, but covered by an unexpired risk acceptance in the ledger
               Findings the ledger records as fixed are never reopened as new work; if one is reported
               again it is flagged as a regression for a human, not auto-fixed.
  3. rank      priority = severity weight x scope weight (cardholder data environment > connected).
  4. route     devin-autofix: dependency finding with a fixed version on the same major line.
               human-owner:   needs a decision (credentials, major upgrades, regressions of fixed findings).
               watch-list:    no fixed version published yet; re-checked next quarter.
  5. evidence  out/<quarter>/evidence.json, evidence.md, digest.md, autofix-plan.json, MANIFEST.sha256
               and ledger/<quarter>.json, which becomes next quarter's baseline.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import pathlib
import re
import subprocess
import sys
import urllib.request

HERE = pathlib.Path(__file__).resolve().parent
OSV = "https://api.osv.dev/v1"
SEVERITY_WEIGHT = {"critical": 4, "high": 3, "medium": 2, "low": 1}
SCOPE_WEIGHT = {"cde": 1.0, "connected": 0.7, "out-of-scope": 0.3}
PRIORITY_BANDS = [(3.0, "P1"), (2.0, "P2"), (0.0, "P3")]
SECRET_KEY = re.compile(r"^\s*([\w.\-]*(password|secret|token|api[-_.]?key)[\w.\-]*)\s*[=:]\s*(.+?)\s*$", re.I)


# ---------------------------------------------------------------- dates

def quarter_of(day: dt.date) -> str:
    return f"{day.year}-Q{(day.month - 1) // 3 + 1}"


def prior_quarter(q: str) -> str:
    year, n = int(q[:4]), int(q[-1])
    return f"{year - 1}-Q4" if n == 1 else f"{year}-Q{n - 1}"


# ---------------------------------------------------------------- collect

def maven_deps(repo_dir: pathlib.Path) -> list[tuple[str, str]]:
    out = repo_dir / "target" / "pci-rescan-deps.txt"
    out.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["mvn", "-q", "-B", "dependency:list", f"-DoutputFile={out}"], cwd=repo_dir, check=True,
                   stdout=subprocess.DEVNULL)
    deps = set()
    for line in out.read_text().splitlines():
        parts = line.strip().split(" ")[0].split(":")
        if len(parts) >= 5:
            deps.add((f"{parts[0]}:{parts[1]}", parts[3]))
    return sorted(deps)


def _post(url: str, body: dict) -> dict:
    req = urllib.request.Request(url, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def _get(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=60) as r:
        return json.load(r)


def _vkey(v: str) -> tuple:
    return tuple(int(x) if x.isdigit() else 0 for x in re.split(r"[.\-]", v)[:4])


def osv_findings(repo: str, deps: list[tuple[str, str]]) -> list[dict]:
    queries = [{"package": {"name": n, "ecosystem": "Maven"}, "version": v} for n, v in deps]
    results = []
    for i in range(0, len(queries), 500):
        results += _post(f"{OSV}/querybatch", {"queries": queries[i:i + 500]})["results"]
    details: dict[str, dict] = {}
    findings = []
    for (name, version), res in zip(deps, results):
        for v in res.get("vulns", []):
            vid = v["id"]
            if vid not in details:
                details[vid] = _get(f"{OSV}/vulns/{vid}")
            d = details[vid]
            fixed = sorted({e["fixed"] for a in d.get("affected", []) if a.get("package", {}).get("name") == name
                            for r in a.get("ranges", []) for e in r.get("events", []) if "fixed" in e
                            and _vkey(e["fixed"]) > _vkey(version)}, key=_vkey)
            sev = (d.get("database_specific", {}).get("severity") or "MODERATE").lower()
            findings.append({
                "id": f"{repo}:{name}:{vid}",
                "repo": repo, "kind": "dependency", "source": "osv.dev",
                "advisory": vid, "aliases": sorted(a for a in d.get("aliases", []) if a.startswith("CVE-")),
                "severity": {"moderate": "medium"}.get(sev, sev),
                "title": d.get("summary") or vid,
                "package": name, "version": version, "fixed_in": fixed[:1],
                "location": "pom.xml",
                "published": (d.get("published") or "")[:10],
                "reference": f"https://osv.dev/vulnerability/{vid}",
            })
    return findings


def secret_findings(repo: str, repo_dir: pathlib.Path) -> list[dict]:
    findings = []
    for f in sorted(repo_dir.glob("src/main/resources/application*.*")):
        for n, line in enumerate(f.read_text().splitlines(), 1):
            m = SECRET_KEY.match(line)
            if not m or m.group(3).startswith("${") or line.lstrip().startswith("#"):
                continue
            rel = f.relative_to(repo_dir)
            findings.append({
                "id": f"{repo}:secret:{rel}:{m.group(1)}",
                "repo": repo, "kind": "credential", "source": "pci-rescan secret check",
                "advisory": "CWE-798", "aliases": [], "severity": "high",
                "title": f"Literal credential in {rel} ({m.group(1)})",
                "package": None, "version": None, "fixed_in": [],
                "location": f"{rel}:{n}",
                "published": None,
                "reference": "https://cwe.mitre.org/data/definitions/798.html",
            })
    return findings


def git_sha(repo_dir: pathlib.Path) -> str:
    try:
        return subprocess.run(["git", "rev-parse", "HEAD"], cwd=repo_dir, capture_output=True, text=True,
                              check=True).stdout.strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        return "unknown"


def collect(scope: dict, workspace: pathlib.Path) -> dict:
    repos, findings = {}, []
    for name, cfg in scope["repos"].items():
        d = workspace / name
        repos[name] = {"commit": git_sha(d), "path": str(d)}
        findings += osv_findings(name, maven_deps(d)) + secret_findings(name, d)
    return {"scanned_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
            "scanner": "Maven dependency:list + OSV.dev querybatch; pci-rescan secret check",
            "repos": repos, "findings": findings}


# ---------------------------------------------------------------- diff, rank, route

def rank(f: dict, scope: dict) -> dict:
    cfg = scope["repos"][f["repo"]]
    score = SEVERITY_WEIGHT[f["severity"]] * SCOPE_WEIGHT[cfg["pci_scope"]]
    f["pci_scope"] = cfg["pci_scope"]
    f["risk_score"] = round(score, 2)
    f["priority"] = next(p for floor, p in PRIORITY_BANDS if score >= floor)
    return f


def route(f: dict, scope: dict) -> dict:
    cfg = scope["repos"][f["repo"]]
    same_major = bool(f["fixed_in"]) and f["version"] and f["fixed_in"][0].split(".")[0] == f["version"].split(".")[0]
    if f.get("regression"):
        f["route"], f["route_reason"] = "human-owner", "previously fixed finding reported again"
    elif f["kind"] == "dependency" and same_major:
        f["route"], f["route_reason"] = "devin-autofix", f"fixed in {f['fixed_in'][0]} on the same major line"
    elif f["kind"] == "credential":
        f["route"], f["route_reason"] = "human-owner", "credential must be rotated by its owner before code changes"
    elif f["kind"] == "dependency" and f["fixed_in"]:
        f["route"], f["route_reason"] = "human-owner", f"fix needs a major upgrade to {f['fixed_in'][0]}"
    else:
        f["route"], f["route_reason"] = "watch-list", "no fixed version published; re-checked every quarter"
    f["owner"] = cfg["owner"] if f["route"] == "devin-autofix" else cfg["security_owner"]
    return f


def diff(scan: dict, baseline: dict, scope: dict, today: dt.date) -> dict:
    prior = {e["id"]: e for e in baseline.get("entries", [])}
    days = scope["remediation_days"]
    out = {"new": [], "carried": [], "accepted": [], "closed": []}
    seen = set()
    for f in scan["findings"]:
        f = dict(f)
        seen.add(f["id"])
        p = prior.get(f["id"])
        if p and p["status"] == "risk-accepted" and dt.date.fromisoformat(p["accepted_until"]) >= today:
            f.update(status="accepted", first_seen=p["first_seen"], accepted_until=p["accepted_until"],
                     acceptance_ref=p.get("acceptance_ref"))
            out["accepted"].append(route(rank(f, scope), scope))
            continue
        if p and p["status"] == "open":
            f.update(status="carried", first_seen=p["first_seen"], due=p["due"])
            f = route(rank(f, scope), scope)
            f["owner"] = p.get("owner", f["owner"])
            out["carried"].append(f)
            continue
        f["regression"] = bool(p and p["status"] == "fixed")
        f.update(status="new", first_seen=today.isoformat())
        f = route(rank(f, scope), scope)
        f["due"] = (today + dt.timedelta(days=days[f["severity"]])).isoformat()
        out["new"].append(f)
    for fid, p in prior.items():
        if fid not in seen and p["status"] == "open":
            out["closed"].append({**p, "status": "closed", "closed_on": today.isoformat(),
                                  "closure_evidence": "not reported by this quarter's rescan"})
    for k in out:
        out[k].sort(key=lambda f: (-f.get("risk_score", 0), f["id"]))
    return out


def autofix_plan(new: list[dict]) -> list[dict]:
    plan: dict[str, dict] = {}
    for f in new:
        if f["route"] != "devin-autofix":
            continue
        p = plan.setdefault(f["repo"], {"repo": f["repo"], "findings": [], "packages": {}})
        p["findings"].append(f["id"])
        cur = p["packages"].get(f["package"])
        if not cur or _vkey(f["fixed_in"][0]) > _vkey(cur["to"]):
            p["packages"][f["package"]] = {"from": f["version"], "to": f["fixed_in"][0]}
    return list(plan.values())


def next_ledger(quarter: str, d: dict, baseline: dict) -> dict:
    keep = [e for e in baseline.get("entries", []) if e["status"] in ("fixed", "risk-accepted")
            and e["id"] not in {f["id"] for f in d["accepted"]}]
    entries = keep + [{"id": c["id"], "status": "fixed", "first_seen": c["first_seen"], "fixed_on": c["closed_on"],
                       "evidence": c["closure_evidence"]} for c in d["closed"]]
    for f in d["carried"] + d["new"]:
        entries.append({"id": f["id"], "status": "open", "first_seen": f["first_seen"], "due": f["due"],
                        "severity": f["severity"], "route": f["route"], "owner": f["owner"]})
    for f in d["accepted"]:
        entries.append({"id": f["id"], "status": "risk-accepted", "first_seen": f["first_seen"],
                        "accepted_until": f["accepted_until"], "acceptance_ref": f.get("acceptance_ref")})
    return {"quarter": quarter, "entries": sorted(entries, key=lambda e: e["id"])}


# ---------------------------------------------------------------- evidence

def _line(f: dict) -> str:
    what = f"{f['package']} {f['version']}" if f["package"] else f["location"]
    ids = ", ".join(f["aliases"]) or f["advisory"]
    fix = f" -> {f['fixed_in'][0]}" if f["fixed_in"] else ""
    return f"| {f['priority']} | {f['severity']} | {f['repo']} | {what}{fix} | {ids} | {f['route']} | {f['owner']} | {f['due']} |"


def evidence_md(meta: dict, d: dict, plan: list[dict]) -> str:
    c = {k: len(v) for k, v in d.items()}
    rows = lambda fs: "\n".join(_line(f) for f in fs) or "| | | | none | | | | |"  # noqa: E731
    hdr = "| Priority | Severity | Repo | Component | Identifier | Route | Owner | Due |\n|---|---|---|---|---|---|---|---|"
    hard = [f for f in d["new"] if f["route"] == "human-owner"]
    return f"""# PCI quarterly rescan evidence pack: {meta['quarter']}

Run: {meta['scanned_at']} | prior quarter ledger: {meta['baseline']} | scanner: {meta['scanner']}

Scope (repo, PCI scope, commit):
{chr(10).join(f"- {r}: {meta['scope'][r]['pci_scope']}, `{v['commit'][:12]}`" for r, v in meta['repos'].items())}

## Summary

| New | Carried open | Closed by rescan | Risk-accepted |
|---|---|---|---|
| {c['new']} | {c['carried']} | {c['closed']} | {c['accepted']} |

Remediation targets (organisation policy in `scope.json`): {', '.join(f"{k} {v} days" for k, v in meta['remediation_days'].items())}.

## New this quarter
{hdr}
{rows(d['new'])}

## Routed to a human owner (not auto-fixed)
{chr(10).join(f"- `{f['id']}`: {f['route_reason']}. Owner: {f['owner']}. Due {f['due']}." for f in hard) or "- none"}

## Watch list (no fixed version published yet)
{chr(10).join(f"- `{f['id']}` ({f['severity']}). Owner: {f['owner']}." for f in d['new'] if f['route'] == 'watch-list') or "- none"}

## Remediation PRs Devin opens (new, auto-fixable findings only)
{chr(10).join(f"- {p['repo']}: {len(p['findings'])} findings, " + ", ".join(f"{k} {v['from']} -> {v['to']}" for k, v in p['packages'].items()) for p in plan) or "- none"}

## Carried from last quarter (no new PR; existing owner keeps it)
{hdr}
{rows(d['carried'])}

## Closed: open last quarter, not reported by this rescan
{chr(10).join(f"- `{x['id']}` (first seen {x['first_seen']})" for x in d['closed']) or "- none"}

## Risk-accepted (suppressed until expiry)
{chr(10).join(f"- `{x['id']}` until {x['accepted_until']} ({x.get('acceptance_ref')})" for x in d['accepted']) or "- none"}

## Control mapping
- PCI DSS v4.0.1 Requirement 11.3.1: internal vulnerability scans at least once every three months, high-risk and critical vulnerabilities resolved, and rescans confirming it. This pack is the application-dependency part of that record; the Closed section is the rescan confirmation. It does not replace infrastructure scans or the qualified, independent reviewer the requirement calls for.
- PCI DSS v4.0.1 Requirement 6.3.3: patches for critical vulnerabilities installed within one month of release; all other applicable patches within a time frame set by the entity's assessment of risk. The organisation's policy in `scope.json` also applies 30 days to high. Due dates above follow that policy.
"""


def decisions(new: list[dict]) -> list[dict]:
    """Group new human-owner findings into decisions: one per package (or credential location)."""
    groups: dict[str, dict] = {}
    for f in new:
        if f["route"] != "human-owner":
            continue
        g = groups.setdefault(f["package"] or f["location"], {"subject": f["package"] or f["location"], "findings": []})
        g["findings"].append(f)
    out = []
    for g in groups.values():
        fs = g["findings"]
        out.append({"subject": g["subject"], "repos": sorted({f["repo"] for f in fs}),
                    "advisories": sorted({a for f in fs for a in (f["aliases"] or [f["advisory"]])}),
                    "severity": max((f["severity"] for f in fs), key=SEVERITY_WEIGHT.get),
                    "reason": fs[0]["route_reason"], "owner": fs[0]["owner"], "due": min(f["due"] for f in fs),
                    "findings": [f["id"] for f in fs]})
    return sorted(out, key=lambda g: -SEVERITY_WEIGHT[g["severity"]])


def digest_md(meta: dict, d: dict, plan: list[dict]) -> str:
    c = {k: len(v) for k, v in d.items()}
    sev = lambda fs, s: sum(1 for f in fs if f["severity"] == s)  # noqa: E731
    today = meta["run_date"]
    overdue = sum(1 for f in d["carried"] if f["due"] < today)
    watch = [f for f in d["new"] if f["route"] == "watch-list"]
    lines = [f"*PCI quarterly rescan {meta['quarter']}* (PCI DSS v4.0.1 Req 11.3.1 / 6.3.3)",
             f"New since {meta['baseline']}: *{c['new']}* (critical {sev(d['new'], 'critical')}, high {sev(d['new'], 'high')}) | "
             f"carried open: {c['carried']} ({overdue} past due) | closed by rescan: {c['closed']} | "
             f"risk-accepted: {c['accepted']}"]
    for p in plan:
        lines.append(f"Devin remediation PR, {p['repo']}: {len(p['findings'])} new findings via "
                     + ", ".join(f"`{k.split(':')[1]}` {v['from']} -> {v['to']}" for k, v in p["packages"].items()))
    for g in decisions(d["new"]):
        lines.append(f"Needs a human decision: `{g['subject']}` in {', '.join(g['repos'])} "
                     f"({', '.join(g['advisories'])}, {g['severity']}). {g['reason']}. Owner {g['owner']}, due {g['due']}.")
    if watch:
        lines.append(f"Watch list: {len(watch)} new findings with no fixed version yet "
                     f"({', '.join(sorted({f['package'].split(':')[1] for f in watch}))}); re-checked next quarter.")
    lines.append("Carried findings keep their existing owner and PR; nothing already fixed or accepted was reopened.")
    return "\n".join(lines) + "\n"


def write_pack(outdir: pathlib.Path, meta: dict, d: dict, plan: list[dict], ledger: dict) -> None:
    outdir.mkdir(parents=True, exist_ok=True)
    files = {
        "evidence.json": json.dumps({"meta": meta, **d}, indent=2) + "\n",
        "evidence.md": evidence_md(meta, d, plan),
        "digest.md": digest_md(meta, d, plan),
        "autofix-plan.json": json.dumps(plan, indent=2) + "\n",
        "decisions.json": json.dumps(decisions(d["new"]), indent=2) + "\n",
        "ledger-next.json": json.dumps(ledger, indent=2) + "\n",
    }
    for name, text in files.items():
        (outdir / name).write_text(text)
    (outdir / "MANIFEST.sha256").write_text(
        "".join(f"{hashlib.sha256(t.encode()).hexdigest()}  {n}\n" for n, t in sorted(files.items())))


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--scope", default=str(HERE / "scope.json"))
    ap.add_argument("--workspace", help="directory holding a clone of each in-scope repo (live mode)")
    ap.add_argument("--snapshot", help="replay a recorded scan instead of collecting")
    ap.add_argument("--save-snapshot", help="write the collected scan here")
    ap.add_argument("--date", help="run date (YYYY-MM-DD); default today (UTC)")
    ap.add_argument("--baseline-dir", default=str(HERE / "baseline"))
    ap.add_argument("--out", default=str(HERE / "out"))
    a = ap.parse_args(argv)

    scope = json.loads(pathlib.Path(a.scope).read_text())
    today = dt.date.fromisoformat(a.date) if a.date else dt.datetime.now(dt.timezone.utc).date()
    quarter, prev = quarter_of(today), prior_quarter(quarter_of(today))
    if a.snapshot:
        scan = json.loads(pathlib.Path(a.snapshot).read_text())
    elif a.workspace:
        scan = collect(scope, pathlib.Path(a.workspace).expanduser())
    else:
        ap.error("pass --workspace (live) or --snapshot (replay)")
    if a.save_snapshot:
        pathlib.Path(a.save_snapshot).write_text(json.dumps(scan, indent=2) + "\n")

    base_path = pathlib.Path(a.baseline_dir) / f"{prev}.json"
    baseline = json.loads(base_path.read_text()) if base_path.exists() else {"quarter": prev, "entries": []}
    d = diff(scan, baseline, scope, today)
    plan = autofix_plan(d["new"])
    meta = {"quarter": quarter, "run_date": today.isoformat(), "baseline": prev if base_path.exists() else "none (first run)",
            "scanned_at": scan["scanned_at"], "scanner": scan["scanner"], "repos": scan["repos"],
            "scope": {r: {"pci_scope": c["pci_scope"]} for r, c in scope["repos"].items()},
            "remediation_days": scope["remediation_days"]}
    outdir = pathlib.Path(a.out) / quarter
    write_pack(outdir, meta, d, plan, next_ledger(quarter, d, baseline))
    sys.stdout.write(digest_md(meta, d, plan))
    print(f"evidence pack: {outdir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
