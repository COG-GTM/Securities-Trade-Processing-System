# demo_swarm: talk track (Security Swarm on STPS)

## Disclosure (say this first, every time)
STPS is a real open-source Spring Boot trade processing service. For this demo, Devin added a small trade-lifecycle API
on top of it (account-scoped trade reads, amendments, settlements) and planted three flaws in that API: a cross-account
trade read, a negative-quantity amendment, and a settlement instruction that can be booked twice. Nothing in the branch
tells the swarm where they are. Everything else the swarm finds is whatever is really there.

## Setup before the call
`bash demo_swarm/seed-situation.sh` until it prints `READY:` (about 15 seconds warm, about 2 minutes on a fresh VM).
Open https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before in one tab and a new
Devin session on STPS in another.

## Minute by minute (live run measured: see "Measured" below)
- 0:00 Situation. "A broker-dealer's post-trade service. Desks book, amend, and settle trades through these APIs. The
  security team has a pen test scheduled next quarter. We are going to run five attackers at it now, and nothing counts
  unless it comes with a failing test."
- 0:30 Presenter types: paste the live prompt from PROMPTS.md into the new session.
- 1:00 Fan-out. Devin reads `swarm/personas/`, writes one prompt per attacker, and launches five child sessions:
  authorization, injection, race and replay, business-logic abuse, supply chain. Open two of them side by side.
- While children run: show `swarm/playbooks/fs-swarm-persona-child.md`. The rule that matters: a finding is a test that
  fails on this branch. Show `swarm.py confirm`: the swarm profile ignores test failures, so `BUILD SUCCESS` means
  nothing; Devin reads the Surefire counts (Tests run / Failures / Errors).
- Business logic first (lead here, not with injection):
  - Duplicate settlement. The same settlement instruction posted twice moves cash twice. Show the red test, then the fix
    PR with the test green.
  - Negative amendment. Booking rejects quantity 0 or below, amendment only rejects 0, so a desk can amend a buy of 100
    into a sell of 500 and the system books it. Red, then green.
  - Cross-account read. Desk B reads desk A's trade by id. Amend and settle check ownership, the read does not.
- Parent verification. Devin does not trust the children: it re-runs every branch itself, red at the test commit and
  green at the head, with the regression suite green. Anything that does not verify is dropped and listed with a reason.
- Dedupe and rank. Two personas often report the same settlement root cause; Devin merges them into one finding.
- Non-conflicting PRs. `swarm.py conflicts` trial-merges every pair of fix branches. One PR per finding, each one
  reviewable on its own, all merge together.
- Hard case: supply chain. The supply-chain child finds a published CVE in a shipped component, proves the shipped version
  with a red test, says whether the vulnerable path is reachable in this configuration, and does NOT patch, fork, or bump
  the vendor code. That goes to the component owner as an advisory.
- Close. "Five attackers, every finding proven by a test, one reviewable PR each, and a third-party issue routed to its
  owner instead of hacked around."

## Live versus fallback (say which one you are showing)
- Live: the session you started a minute ago. Say "this is running now".
- Pre-run of this exact prompt: see "Measured". Say "this is the run we did when we prepared the demo".
- Fallback evidence from earlier swarm runs: PRs #15 to #36. Say "this is a recording of an earlier run".

## Measured
Filled in from the pre-run of the PROMPTS.md prompt in a fresh session.
