# Adversarial swarm for the trade-capture service

A parent Devin session fans out one child per attacker persona (authorization, injection, race/replay, business-logic abuse,
third-party components). Each child must reproduce a finding with a failing test before it opens a fix PR. The parent
deduplicates what the personas report and routes third-party findings to an advisory instead of an in-place patch.

| File | Purpose |
|---|---|
| `personas.json` | Attacker personas, goals, techniques, attack surface |
| `playbooks/fs-swarm-persona-child.md` | Rules every child follows (test first, one finding per PR, third-party is report-only) |
| `swarm.py plan [--live]` | Writes one prompt per persona to `swarm/out/prompts/`; `--live` launches them via the Devin API (`DEVIN_API_KEY`) |
| `swarm.py confirm` | Runs `mvn -Pswarm test` and maps each confirmation test to its finding id |
| `swarm.py report` | Dedupes persona findings (`fixtures/persona-findings.json`) by CWE + sink, writes `swarm/out/swarm-report.{md,html}` |
| `run-swarm.sh` | All four steps in order, about 30 seconds with a warm Maven cache |
| `reset.sh` | Clears `swarm/out` and returns to the clean harness branch |
| `advisories/` | Third-party findings, reported to the owner, never patched in place |

Confirmation tests live in `src/test/java/.../swarm/`, are tagged `@Tag("swarm")`, and are excluded from the normal build.
Each test asserts the safe behavior, so on this branch it fails ("exploit reproduces"); on the matching fix branch it passes.

Requirements: JDK 21, Maven 3.6+, Python 3. No Kafka, Postgres, or network access is needed for the confirmation tests.
