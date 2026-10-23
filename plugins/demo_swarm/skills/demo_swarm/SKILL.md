---
name: "demo_swarm"
description: "Seed, run, and reset the FS Golden Demo Bank adversarial security swarm on COG-GTM/Securities-Trade-Processing-System (STPS). Use when asked to prepare, rehearse, run, or reset the STPS security swarm demo, or when a parent session is told to run an adversarial test swarm against the STPS trade APIs."
---

# demo_swarm: live adversarial swarm on STPS

The demo is a situation, not a replay. A seeded branch holds real flaws; the presenter pastes one prompt into a new
Devin session; Devin fans out five attacker personas, each proves findings with a failing test, and Devin opens one fix
PR per confirmed finding. Kit files live on the kit branch under `demo_swarm/` in the STPS repo.

## Seed the BEFORE state
1. Clone STPS, check out the kit branch, run `bash demo_swarm/seed-situation.sh`.
2. Only continue if it prints `READY:`. It proves the three planted flaws with temporary probe tests (Surefire counts,
   not BUILD SUCCESS), checks the seed has no answer tests, fixtures, or bytecode, and clears prior live-run PRs and
   `demo/swarm-live-*` branches. It never touches PRs #36 and below.

## Run live
Paste the live prompt from `demo_swarm/PROMPTS.md` into a NEW session on STPS. The parent follows
`swarm/playbooks/fs-swarm-parent.md` on the seeded branch; children follow `swarm/playbooks/fs-swarm-persona-child.md`.
Measured wall time and the presenter script are in `demo_swarm/TALK-TRACK.md`.

## Rules that keep the demo honest
- A finding counts only if `python3 swarm/swarm.py verify` prints `VERIFIED` (red at the test commit, green at head,
  regression suite green). The `-Pswarm` profile ignores test failures, so `BUILD SUCCESS` proves nothing.
- Third-party findings are reported with evidence and never patched in place, forked, or bumped.
- Never merge. Every PR body ends with `Devin-Org: engineering`.
- Maven: use the GCS mirror in `swarm/maven-settings.xml`; Maven Central rate-limits Devin VMs.

## Reset
Re-run `bash demo_swarm/seed-situation.sh`. Locally, `bash swarm/reset.sh` on the seeded branch.
