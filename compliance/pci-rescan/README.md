# Quarterly PCI rescan

A scheduled Devin Automation runs this every quarter across the in-scope repos in `scope.json`
(Springboot-BankApp: cardholder data environment; Securities-Trade-Processing-System: connected system).

What one run does:

1. **Scan.** `rescan.py` resolves each repo's Maven dependencies, queries [OSV.dev](https://osv.dev) for known
   vulnerabilities, and checks Spring config files for literal credentials.
2. **Diff against last quarter.** The prior quarter's ledger (`baseline/<YYYY-Qn>.json`) decides what is
   new, carried (still open, keeps its owner and due date), closed (the rescan no longer reports it), or
   risk-accepted (suppressed until the acceptance expires). Fixed findings are never reopened as new work; if one
   comes back it goes to a human as a regression.
3. **Prioritise by risk.** Severity weight x PCI scope weight gives P1/P2/P3; due dates follow
   `remediation_days` in `scope.json` (the organisation's policy under PCI DSS v4.0.1 Req 6.3.3).
4. **Route.** New dependency findings with a fix on the same major line go to Devin, which opens one remediation PR
   per repo. Findings that need a decision (major upgrades, credentials, regressions) go to a human owner with a
   decision brief. Findings with no published fix go on a watch list.
5. **Evidence.** `out/<quarter>/`: `evidence.md` / `evidence.json`, `digest.md` (the Slack post),
   `autofix-plan.json`, `decisions.json`, `ledger-next.json` (next quarter's baseline) and `MANIFEST.sha256`.
   The automation files these under `evidence/<quarter>/` in a PR.

```bash
# live scan (needs Maven + network to api.osv.dev); workspace holds one clone per repo in scope.json
python3 compliance/pci-rescan/rescan.py --workspace ~/pci-workspace
# replay the recorded 2026-10-12 scan (no network)
python3 compliance/pci-rescan/rescan.py --snapshot compliance/pci-rescan/fixtures/2026-Q4-scan.json --date 2026-10-12
# tests
python3 -m pytest -q compliance/pci-rescan/tests
# reset the demo (re-seed the Q3 ledger, reset the workspace clones, replay check); about 5 seconds
compliance/pci-rescan/reset.sh
```

`baseline/2026-Q3.json` is seeded by `seed_baseline.py`: there was no real Q3 run, so it records every finding whose
advisory was published before 2026-07-01 as open, plus one seeded risk acceptance. Advisories published after that
date are what the Q4 run reports as new.

The automation definition is in `automation.json` and its session prompt in `automation-prompt.md`.
