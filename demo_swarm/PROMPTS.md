# demo_swarm: prompts

## Before the demo (presenter machine or any Devin VM, about 15 seconds warm)

```bash
git clone https://github.com/COG-GTM/Securities-Trade-Processing-System.git && cd Securities-Trade-Processing-System
git fetch origin <kit-branch> && git checkout <kit-branch>
bash demo_swarm/seed-situation.sh
```

It must end with `READY: https://github.com/COG-GTM/Securities-Trade-Processing-System/tree/demo/swarm-live-before`.
The seed closes prior live-run PRs into `demo/swarm-live-before` and deletes prior `demo/swarm-live-*` child branches.
It refuses to touch PRs #36 and below (fallback evidence).

## Live prompt (paste into a NEW Devin session, repo COG-GTM/Securities-Trade-Processing-System)

```
Repo COG-GTM/Securities-Trade-Processing-System, branch demo/swarm-live-before. Follow swarm/playbooks/fs-swarm-parent.md.

Run an adversarial test swarm against the STPS trade APIs on this branch. Launch one child session per attacker persona in `swarm/personas/` (authorization, injection, race and replay, business-logic abuse, supply chain). Each child must prove every finding with a failing test before reporting it; unproven findings are not findings. Dedupe across children, rank by severity, and open one fix PR per confirmed finding with the test turning green. Report anything exploitable in a third-party component rather than patching it in place.
```

## Follow-ups the presenter can type while children run

- `Show me the red test for the settlement finding and the Surefire counts before and after the fix.`
- `Why did you drop <finding>? Show me what you tried.`
- `Which of these PRs touch the same file? Prove they merge cleanly together.`

## Fallback (only if the live run fails on stage)

Say "this is a recording of an earlier run", then show PRs #15 to #36 and the pre-run session linked in TALK-TRACK.md.
