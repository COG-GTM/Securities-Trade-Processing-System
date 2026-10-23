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

## Minute by minute (budget 20 minutes on stage; both measured runs finished in under 20, see "Measured")
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
- Optional beat: Devin Review caught edge cases in Devin's own fixes. In run 2, Devin Review commented on three of the
  fix PRs and Devin left them for human review instead of changing the PRs: a send that times out can still be delivered
  later, so a retry could book twice (#56); the new symbol pattern rejects `BRK/B` (#59); the log sanitizer strips CR/LF
  and tab but not ESC/ANSI escapes (#60). Say "the fixes get reviewed too, and the reviewer is allowed to disagree with
  Devin".
- Close. "Five attackers, every finding proven by a test, one reviewable PR each, and a third-party issue routed to its
  owner instead of hacked around."

## Live versus fallback (say which one you are showing)
- Live: the session you started a minute ago. Say "this is running now".
- Pre-run of this exact prompt: see "Measured". Say "this is the run we did when we prepared the demo".
- Fallback evidence from earlier swarm runs: PRs #15 to #36. Say "this is a recording of an earlier run".

## Measured
Two pre-runs on 2026-10-23 of the exact PROMPTS.md prompt, each in a fresh Devin session. Budget 20 minutes on stage.

| | Run 1 | Run 2 |
|---|---|---|
| Seed | `fa84abb2` | `53a590f6` (current LOGIC persona text and conflict check) |
| Parent session | [b43c3b42](https://devin-gtm.devinenterprise.com/sessions/b43c3b4292e0422e8bee82eca19efd58) | [bf9e8dca](https://devin-gtm.devinenterprise.com/sessions/bf9e8dcaa335442981b213a7cea60e93) |
| Prompt to Devin's final report | 19m40s | 18m30s |
| Devin's own count (setup to all PRs open) | about 13m | 11m20s |
| Raw findings, unique after dedupe | 10, 9 | 10, 9 |
| Fix PRs, each red then green | 7 (#43-#47, #49, #50) | 7 (#54-#60) |
| Third-party advisories, no patch | 2 (Tomcat) | 2 (Tomcat, kafka-clients) |
| All three planted flaws found | yes | yes |
| All fixes merged together | 21/21 green | 16/16 green |

Run 1 findings:
1. High. A settled trade can be settled again, so cash moves twice (LOGIC-2, #44).
2. High. A replayed settlement instruction ID books a second settlement (RACE-1, #45). Planted. RACE-2 merged into LOGIC-2.
3. High. An amendment accepts a negative quantity or an infinite price (LOGIC-1, #43). Planted.
4. High. Any desk can read another desk's trade by ID (AUTHZ-1, #46). Planted.
5. High. Trades can be booked with a plain GET with no caller identity, and they default to HOUSE (AUTHZ-2, #47).
6. High. The symbol echoed by sendTrade is served as text/html (INJECT-1, #49).
7. Medium. CR/LF in a trade ID forges audit log lines (INJECT-2, #50).
- Advisories: Tomcat 10.1.34 CVE-2025-48988 (multipart part count, reachable) and CVE-2025-24813 (partial PUT, not reachable as shipped).

Run 2 findings:
1. Critical. A replayed or duplicate settlement instruction settles the same trade twice (RACE-1, #54). Planted. LOGIC-2 (settle an already-settled trade) merged into it.
2. High. An amendment accepts a negative quantity or a price that overflows to Infinity (LOGIC-1, #55). Planted.
3. High. sendTrade reports success before Kafka acknowledges, so failed sends are silently lost (RACE-2, #56).
4. High. Any desk can read another desk's trade by ID (AUTHZ-1, #57). Planted.
5. High. An anonymous GET on sendTrade books trades on the HOUSE account (AUTHZ-2, #58).
6. High. Script in the symbol parameter is echoed back as HTML and published to Kafka (INJECT-1, #59).
7. Medium. CR/LF in the tradeId path variable forges audit log lines (INJECT-2, #60). Merge after #57; both edit `TradeQueryService#getTrade`.
- Advisories: Tomcat 10.1.34 CVE-2025-48988 (reachable) and CVE-2025-24813 (on CISA KEV, not reachable as shipped);
  kafka-clients 3.8.1 CVE-2026-35554 / GHSA-5qcv-4rpc-jp93 (producer buffer-pool race can misroute messages; NVD lists
  2.8.0 to 3.9.1 as affected, fixed in 3.9.2), on the trade publish path.

Every finding in both runs was re-verified red then green outside the run, from Surefire counts. Expect the count and
order to vary between runs. Do not promise a number on stage. The planted three were found both times; the rest are
real gaps in the upstream code or its dependencies, not planted ones.
