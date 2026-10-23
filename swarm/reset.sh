#!/usr/bin/env bash
# Return this local checkout to the seeded BEFORE state. Local only: never touches remote branches or PRs.
set -euo pipefail
trap 'echo "reset: FAILED at line $LINENO" >&2' ERR
cd "$(dirname "$0")/.."
git fetch -q origin demo/swarm-live-before
git checkout -q -B demo/swarm-live-before origin/demo/swarm-live-before
git reset -q --hard origin/demo/swarm-live-before
git clean -qfdx -e .idea
echo "READY: local checkout at origin/demo/swarm-live-before, swarm/out and target cleared"
