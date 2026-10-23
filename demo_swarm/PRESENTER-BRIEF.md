# demo_swarm presenter brief: adversarial security swarm on STPS

## Goal
Five attacker personas hit a broker-dealer's trade API at the same time, and a finding counts only if it comes with a failing test. You end with one reviewable fix PR per proven finding, plus third-party issues routed to the vendor's owner, not patched. Budget 20 minutes (the two measured runs took 19m40s and 18m30s).

**Say at the start:** STPS is a real open-source Spring Boot trade service. Devin added a small trade-lifecycle API on top and planted three flaws in it: a cross-desk trade read, a negative amendment, and a settlement that can be booked twice. Nothing in the branch says where they are. Anything else the swarm finds is really there.

## Pull up before you start
| Tab | Link |
|---|---|
| 1. BEFORE branch (no swarm tests yet) | https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before |
| 2. The five attackers | https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before/swarm/personas |
| 3. Live Devin session | [live session](CHILD_SESSION_URL). The setup session starts it with the prompt below, word for word, when you say go, and posts the link. |
| 4. Fix PRs as they open | https://github.com/COG-GTM/Securities-Trade-Processing-System/pulls?q=is%3Apr+is%3Aopen+base%3Ademo%2Fswarm-live-before |

## Beats
| Time | Beat | Show |
|---|---|---|
| 0:00 | Disclose the three planted flaws. "A broker-dealer's post-trade service. The pen test is next quarter; we run five attackers now." | Tab 1 |
| 0:30 | Say go in the setup session and open the live session. "Nothing counts unless it comes with a failing test." | [live session](CHILD_SESSION_URL) |
| 1:00 | Devin starts five child sessions, one per attacker. Open two of them side by side. | [personas](https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before/swarm/personas), child links in the live session |
| 2:00 to ~10:00 | While the children run, show the rule: a finding is a test that fails on this branch. `BUILD SUCCESS` proves nothing here; Devin reads the test counts. | [child rules, lines 14-25](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/swarm/playbooks/fs-swarm-persona-child.md#L14-L25) |
| ~10:00 | **Duplicate settlement.** "The same settlement instruction sent twice moves the cash twice." | [SettlementService#settle](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/SettlementService.java#L25-L38) |
| ~11:00 | **Negative amendment.** "Booking rejects quantity 0 or less; amendment only rejects 0, so a buy of 100 becomes minus 500." | [booking check](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/TradeService.java#L23) vs [amendment check](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/TradeAmendmentService.java#L27) |
| ~12:00 | **Cross-desk read.** "Amend and settle check the desk; the read doesn't, so desk B reads desk A's trade." | [TradeQueryService#getTrade](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/TradeQueryService.java#L28-L35) |
| ~14:00 | Devin doesn't trust its children: it reruns every branch itself, red then green. Duplicates are merged and anything unproven is dropped with a reason. | Live session (`VERIFIED` lines) |
| ~15:00 | **Hard case.** The supply-chain attacker proves a known vulnerability in a shipped component (Tomcat or Kafka) and reports it to the owner instead of patching vendor code. | Live session (advisory in the final report) |
| ~18:00 | PRs open, one per finding, each with its test turning green. Open one PR and show the test, then the fix. | Tab 4 |
| ~19:00 | Optional: Devin Review comments on Devin's own fixes. "The fixes get reviewed too, and the reviewer can disagree with Devin." | Review comments on a PR in tab 4 |
| Close | "Five attackers, every finding proven by a test, one reviewable PR each, and the vendor issue sent to its owner." | Tab 4 |

## Prompt (the setup session sends this to the live session word for word)
```
Repo COG-GTM/Securities-Trade-Processing-System, branch demo/swarm-live-before. Follow swarm/playbooks/fs-swarm-parent.md.

Run an adversarial test swarm against the STPS trade APIs on this branch. Launch one child session per attacker persona in `swarm/personas/` (authorization, injection, race and replay, business-logic abuse, supply chain). Each child must prove every finding with a failing test before reporting it; unproven findings are not findings. Dedupe across children, rank by severity, and open one fix PR per confirmed finding with the test turning green. Report anything exploitable in a third-party component rather than patching it in place.
```

## Follow-ups you can type into the live session
```
Show me the red test for the settlement finding and the Surefire counts before and after the fix.
```
```
Which of these PRs touch the same file? Prove they merge cleanly together.
```

## What you should see
- [ ] Five child sessions, one per persona.
- [ ] The three planted flaws at the top of the ranking: duplicate settlement, negative amendment (or infinite price), cross-desk read.
- [ ] Every finding marked `VERIFIED`: red at the test commit, green at the fix, regression tests green.
- [ ] Duplicates merged (two attackers usually find the settlement bug).
- [ ] Tomcat and/or kafka-clients reported as advisories, not patched.
- [ ] One fix PR per finding into `demo/swarm-live-before`. The count varies between runs (7 both times so far); don't promise a number.

## If the live run stalls
Say "this is the run we did when we prepared the demo", then show [the earlier parent session](https://devin-gtm.devinenterprise.com/sessions/bf9e8dcaa335442981b213a7cea60e93) and its PRs [#54](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/54) to [#60](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/60) (closed by the reseed, still readable). Older swarm runs: PRs #15 to #36.
