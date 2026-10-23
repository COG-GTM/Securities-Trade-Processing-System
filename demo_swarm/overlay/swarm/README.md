# Adversarial swarm for the STPS trade APIs

Parent rules: `playbooks/fs-swarm-parent.md`. Child rules: `playbooks/fs-swarm-persona-child.md`.

A parent Devin session launches one child per attacker persona in `personas/`. Each child must prove a finding with a
failing test before it counts. The parent re-verifies every branch red then green, deduplicates, ranks, checks the fix
branches do not conflict, and opens one fix PR per confirmed finding. Third-party findings are reported, never patched in place.

| Command | What it does |
|---|---|
| `python3 swarm/swarm.py plan --run-id <id>` | Writes one child prompt per persona to `swarm/out/prompts/` |
| `python3 swarm/swarm.py confirm [--test X] [--expect red\|green]` | Runs `mvn -Pswarm test` and reports Surefire counts (Tests run / Failures / Errors). `BUILD SUCCESS` is ignored on purpose: the profile sets `testFailureIgnore` |
| `python3 swarm/swarm.py verify <branch>` | Red at the test-only commit, green at the branch head, regression suite green |
| `python3 swarm/swarm.py conflicts <branch>...` | Pairwise trial merges of fix branches onto the base |
| `python3 swarm/swarm.py report --findings swarm/out/findings.json` | Dedupe, rank, join verification, write `swarm/out/swarm-report.{md,html}` |
| `bash swarm/setup-child.sh` | JDK 21, Maven, GCS mirror, warm cache on a fresh VM |
| `bash swarm/reset.sh` | Local checkout back to `origin/demo/swarm-live-before` |

Swarm tests live in `src/test/java/.../swarm/`, are tagged `@Tag("swarm")`, and are excluded from the normal build.
`-Pswarm` runs them together with the `@Tag("regression")` suite. No Kafka, Postgres, or network is needed.
