#!/usr/bin/env bash
# One-command adversarial swarm replay for the STPS trade-capture service.
#   swarm/run-swarm.sh            dry-run fan-out (no credentials), confirm, dedupe report
#   swarm/run-swarm.sh --live     same, but launches real Devin child sessions (needs DEVIN_API_KEY)
set -euo pipefail
cd "$(dirname "$0")/.."
[[ "${JAVA_HOME:-}" == *21* ]] || export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
step() { printf '\n\033[1;36m== %s ==\033[0m\n' "$1"; }
step "1/4 Baseline: existing unit build compiles and swarm tests are excluded from the normal build"
mvn -q -B -o test-compile && echo "compile OK (swarm tests are opt-in via -Pswarm)"
step "2/4 Fan-out: one Devin child per attacker persona"
python3 swarm/swarm.py plan ${1:+$1}
step "3/4 Confirm: every finding must be reproduced by a test before a fix PR opens"
python3 swarm/swarm.py confirm
step "4/4 Dedupe and route: fix PRs for our code, advisory for third-party code"
if [ "${1:-}" = "--live" ]; then
  until python3 swarm/swarm.py collect; do echo "waiting 60s for persona children..."; sleep 60; done
fi
python3 swarm/swarm.py report
