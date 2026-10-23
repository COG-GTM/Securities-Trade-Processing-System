# demo_swarm: adversarial security swarm on STPS

**Goal:** Five attackers hit a trade API at once. A finding counts only if a failing test proves it. You end with one fix PR per finding, and vendor bugs are reported to their owners, not patched. About 20 min.

**Say first:** "Devin planted three flaws in this trade API. Anything else the swarm finds is real."

## Tabs
1. [BEFORE branch](https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before)
2. [The 5 attackers](https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before/swarm/personas)
3. [Live session](CHILD_SESSION_URL): the setup session starts it automatically and posts the link here.
4. [Fix PRs](https://github.com/COG-GTM/Securities-Trade-Processing-System/pulls?q=is%3Apr+is%3Aopen+base%3Ademo%2Fswarm-live-before)

## Beats
1. **0:00 The ask (tab 1).** "This is a broker-dealer's trade service. The pen test is next quarter; we run five attackers today."
2. **0:30 Hand it to Devin (tab 3).** Open the live session; the prompt below is already in it. "Nothing counts without a failing test."
3. **1:00 The swarm (tabs 2 and 3).** Devin starts one child session per attacker; open two side by side.
4. **~10:00 The planted flaws.** [Settlement booked twice](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/SettlementService.java#L25-L38), [negative amendment](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/TradeAmendmentService.java#L27), [another desk's trade readable](https://github.com/COG-GTM/Securities-Trade-Processing-System/blob/demo/swarm-live-before/src/main/java/com/Himanshu/TradesPostProcessingSystem/service/TradeQueryService.java#L28-L35).
5. **~14:00 Trust.** Devin re-runs every test itself, red then green, and drops anything it can't prove.
6. **~15:00 Hard case.** A known vulnerability in Tomcat or Kafka gets an advisory for its owner, not a patch.
7. **~18:00 The PRs (tab 4).** Open one PR and show the test, then the fix.
- **Close.** "Five attackers in 20 minutes, every finding proven, one reviewable PR each."

## Prompt (already sent to the live session, word for word)
```
Repo COG-GTM/Securities-Trade-Processing-System, branch demo/swarm-live-before. Follow swarm/playbooks/fs-swarm-parent.md.

Run an adversarial test swarm against the STPS trade APIs on this branch. Launch one child session per attacker persona in `swarm/personas/` (authorization, injection, race and replay, business-logic abuse, supply chain). Each child must prove every finding with a failing test before reporting it; unproven findings are not findings. Dedupe across children, rank by severity, and open one fix PR per confirmed finding with the test turning green. Report anything exploitable in a third-party component rather than patching it in place.
```

## Check
- [ ] The 3 planted flaws are at the top of the ranking.
- [ ] Every finding says `VERIFIED`.
- [ ] Tomcat or Kafka is reported as an advisory, not patched.
- [ ] One PR per finding (7 in past runs; don't promise a number).

**Fallback:** "Here's the run we did while preparing the demo." [Session](https://devin-gtm.devinenterprise.com/sessions/bf9e8dcaa335442981b213a7cea60e93), PRs [#54](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/54) to [#60](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/60).
