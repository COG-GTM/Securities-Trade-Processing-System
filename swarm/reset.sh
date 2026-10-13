#!/usr/bin/env bash
# Reset the swarm demo to its pre-run state (under 10 seconds). Never touches remote branches or PRs.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf swarm/out target/surefire-reports
git checkout -q devin/fs-swarm-1791848400-adversarial-swarm
git checkout -q -- .
echo "reset: swarm/out cleared, harness branch checked out clean"
