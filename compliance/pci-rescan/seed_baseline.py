#!/usr/bin/env python3
"""Seed the prior-quarter ledger for the demo from a recorded scan.

A real deployment starts from last quarter's ledger-next.json. For the demo there is no prior run, so this
reconstructs what a 2026-07-01 scan would have recorded: every finding whose advisory was published before the
cut-off (and every config-file credential) is 'open' with its policy due date, and one finding is recorded as
risk-accepted to show suppression. Advisories published after the cut-off are what this quarter reports as new.

    python3 compliance/pci-rescan/seed_baseline.py --scan fixtures/2026-Q4-scan.json --cutoff 2026-07-01
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
ACCEPTED = {
    "id_prefix": "Securities-Trade-Processing-System:org.apache.zookeeper:zookeeper:",
    "accepted_until": "2027-01-31",
    "acceptance_ref": "RA-2026-031 (demo seed): ZooKeeper is test-scope only via spring-kafka-test",
}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--scan", required=True)
    ap.add_argument("--cutoff", default="2026-07-01")
    ap.add_argument("--quarter", default="2026-Q3")
    ap.add_argument("--scope", default=str(HERE / "scope.json"))
    a = ap.parse_args()
    scan = json.loads(pathlib.Path(a.scan).read_text())
    days = json.loads(pathlib.Path(a.scope).read_text())["remediation_days"]
    first = dt.date.fromisoformat(a.cutoff)
    entries = []
    for f in scan["findings"]:
        if (f.get("published") or "") >= a.cutoff:
            continue
        if f["id"].startswith(ACCEPTED["id_prefix"]):
            entries.append({"id": f["id"], "status": "risk-accepted", "first_seen": a.cutoff,
                            "accepted_until": ACCEPTED["accepted_until"], "acceptance_ref": ACCEPTED["acceptance_ref"]})
            continue
        entries.append({"id": f["id"], "status": "open", "first_seen": a.cutoff,
                        "due": (first + dt.timedelta(days=days[f["severity"]])).isoformat(),
                        "severity": f["severity"], "owner": "appsec-oncall"})
    out = HERE / "baseline" / f"{a.quarter}.json"
    out.write_text(json.dumps({"quarter": a.quarter, "seeded": True, "cutoff": a.cutoff,
                               "entries": sorted(entries, key=lambda e: e["id"])}, indent=2) + "\n")
    print(f"{out}: {len(entries)} entries")


if __name__ == "__main__":
    main()
