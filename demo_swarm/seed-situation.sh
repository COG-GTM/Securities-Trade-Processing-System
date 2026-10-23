#!/usr/bin/env bash
# Seed the demo_swarm BEFORE situation: branch demo/swarm-live-before = STPS main + trade lifecycle API + swarm harness.
# Proves the three planted flaws with private probe tests (never committed), clears prior live-run branches and PRs
# (never PRs #36 or below, which are fallback evidence), pushes, and prints READY.
#   demo_swarm/seed-situation.sh            seed, verify, push, clean up prior runs
#   demo_swarm/seed-situation.sh --no-push  seed and verify locally only
set -euo pipefail
trap 'echo "seed-situation: FAILED at line $LINENO: $BASH_COMMAND" >&2' ERR

KIT="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$KIT/.." && git rev-parse --show-toplevel)"
SEED=demo/swarm-live-before
GH_REPO=COG-GTM/Securities-Trade-Processing-System
FALLBACK_MAX_PR=36
PUSH=1; [ "${1:-}" = "--no-push" ] && PUSH=0
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
step() { printf '\n== %s ==\n' "$1"; }

step "1/6 machine"
bash "$KIT/setup-machine.sh"

step "2/6 build seed from origin/main"
cd "$REPO"
git fetch -q origin main
WT="$(mktemp -d)/stps-seed"
cleanup() { git -C "$REPO" worktree remove --force "$WT" >/dev/null 2>&1 || true; }
trap cleanup EXIT
git worktree add -q --detach "$WT" origin/main
cd "$WT"
cp -r "$KIT/overlay/src" .
git add src
git commit -q -m "feat: trade lifecycle API (account-scoped reads, amendments, settlements)"
git apply "$KIT/pom-and-gitignore.patch"
cp -r "$KIT/overlay/swarm" "$KIT/overlay/.github" .
find swarm -name '__pycache__' -prune -exec rm -rf {} +
git add -A
git commit -q -m "swarm: adversarial persona harness, -Pswarm Maven profile, CI confirm job"
SEED_SHA="$(git rev-parse HEAD)"

step "3/6 invariants: no answers in the seed"
fail() { echo "INVARIANT FAILED: $*" >&2; exit 1; }
[ -z "$(git ls-files 'swarm/fixtures/*')" ] || fail "findings fixture present"
[ -z "$(git ls-files '*SwarmTest.java')" ] || fail "swarm answer tests present"
[ -z "$(git ls-files '*__pycache__*' '*.pyc')" ] || fail "python bytecode present"
[ -z "$(git ls-files 'demo_swarm/*' 'plugins/*')" ] || fail "kit files leaked into seed"
[ "$(ls swarm/personas/*.json | wc -l)" -eq 5 ] || fail "expected 5 personas"
git log --format=%s origin/main..HEAD | grep -qiE 'plant|flaw|vuln|answer' && fail "commit messages give the game away"
echo "ok: no fixtures, no *SwarmTest, no bytecode, 5 personas, neutral history"

step "4/6 harness self-test and baseline (seed as shipped)"
python3 -m unittest discover -s swarm -p 'test_*.py' 2>&1 | tail -1
rm -rf swarm/__pycache__
python3 swarm/swarm.py confirm --expect green

step "5/6 probe: the three flaws reproduce (probe tests are temporary)"
mkdir -p src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm
cp "$KIT"/verify/swarm/*.java src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm/
python3 swarm/swarm.py confirm --expect red
python3 - <<'PY'
import json, sys
c = json.load(open("swarm/out/confirm.json"))
probes = {k: v for k, v in c["cases"].items() if k.startswith("SeedProbeSwarmTest#")}
reg = {k: v for k, v in c["cases"].items() if k.startswith("TradeLifecycleRegressionTest#")}
want = ["probeIdor_otherAccountCannotReadTrade", "probeNegativeAmend_negativeQuantityRejected",
        "probeDuplicateSettlement_sameInstructionSettlesOnce"]
missing = [w for w in want if probes.get(f"SeedProbeSwarmTest#{w}") != "fail"]
if missing or len(probes) != 3:
    sys.exit(f"flaw not present: {missing}")
if not reg or any(v != "pass" for v in reg.values()):
    sys.exit("regression suite not green on seed")
t = c["totals"]
print(f"ok: 3/3 flaws reproduce (IDOR read, negative amendment, duplicate settlement); "
      f"{len(reg)}/{len(reg)} regression green; Surefire Tests run {t['tests']}, Failures {t['failures']}, Errors {t['errors']}")
PY
rm -rf src/test/java/com/Himanshu/TradesPostProcessingSystem/swarm swarm/out swarm/__pycache__ target
[ -z "$(git status --porcelain)" ] || fail "probe left residue in the seed tree"

step "6/6 publish"
if [ "$PUSH" -eq 1 ]; then
  for n in $(gh pr list -R "$GH_REPO" --state open --base "$SEED" --json number -q '.[].number'); do
    [ "$n" -gt "$FALLBACK_MAX_PR" ] || fail "refusing to touch fallback PR #$n"
    gh pr close -R "$GH_REPO" "$n" -c "Closed by demo_swarm seed reset (prior live run)." >/dev/null
    echo "closed prior live-run PR #$n"
  done
  for b in $(git ls-remote --heads origin 'demo/swarm-live-*' | awk '{print $2}' | sed 's#refs/heads/##'); do
    [ "$b" = "$SEED" ] && continue
    git push -q origin --delete "$b"
    echo "deleted prior live-run branch $b"
  done
  git push -q -f origin "HEAD:refs/heads/$SEED"
  REMOTE_SHA="$(git ls-remote origin "refs/heads/$SEED" | awk '{print $1}')"
  [ "$REMOTE_SHA" = "$SEED_SHA" ] || fail "remote $SEED is $REMOTE_SHA, expected $SEED_SHA"
  echo "READY: https://github.com/$GH_REPO/tree/$SEED @ ${SEED_SHA:0:8}"
else
  echo "READY (local only, not pushed): $SEED_SHA"
fi
