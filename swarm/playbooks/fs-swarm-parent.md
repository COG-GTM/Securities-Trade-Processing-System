# STPS adversarial swarm: parent playbook (v2)

You are the parent of the swarm. You never write findings yourself; you launch, verify, dedupe, rank, and open PRs.

1. Setup: `git fetch origin && git checkout -B swarm-parent origin/demo/swarm-live-before && bash swarm/setup-child.sh`.
   Run id = current unix time. Record the start time; you will report wall time at the end.
2. Fan-out: `python3 swarm/swarm.py plan --run-id <run-id>` writes one prompt per persona to `swarm/out/prompts/`.
   Launch exactly one child session per prompt file (5 total, in parallel), repo `COG-GTM/Securities-Trade-Processing-System`,
   title `swarm <run-id> <PERSONA>`, with `notify_on_response=true` and a structured output schema of
   `{"findings": [<the playbook's finding object>]}`. Do not pass tags.
3. Collect: as each child settles, read its structured output (fall back to the fenced json in its last message) and
   append its findings to `swarm/out/findings.json`. Do not message children for status.
4. Verify every branch yourself: `git fetch origin <branch> && python3 swarm/swarm.py verify origin/<branch>`.
   A child's claim is not evidence. Anything that does not print `VERIFIED` is dropped and listed as unproven.
5. Dedupe and rank: `python3 swarm/swarm.py report --findings swarm/out/findings.json`. Merge duplicates that share a
   root cause and sink (keep the lowest-friction fix branch). Lead with business-logic and money-movement findings
   (settlement, amendment, cross-account data), then authorization, then injection. Zero filler: theoretical or
   unprovable items go in the unproven list, not the table.
6. Conflicts: `python3 swarm/swarm.py conflicts origin/<fix-branch> ...` across every confirmed fix branch. If two
   conflict, rebase the later one onto the base, resolve, push, and re-run verify for it.
7. PRs: one PR per confirmed fix finding, head = the child branch, base = `demo/swarm-live-before`, never merge.
   Title `swarm(<FINDING-ID>): <title>`. Body: impact in one sentence, the red Surefire counts at the test commit,
   the green counts at the head, files changed, and the final line `Devin-Org: engineering`.
   Third-party (report-only) findings get NO PR and no patch: put the advisory in your final message and the report.
8. Final message: wall time, the ranked table (swarm id, severity, finding, test, PR), the third-party advisory, the
   unproven/dropped list with one reason each, and the 5 child session links. Attach `swarm/out/swarm-report.md`
   and `swarm/out/swarm-report.html`.
