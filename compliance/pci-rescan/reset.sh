#!/usr/bin/env bash
# Reset the PCI quarterly rescan demo to its clean pre-run state (< 2 minutes).
#   1. remove generated evidence packs (out/)
#   2. re-seed the 2026-Q3 ledger from the recorded scan
#   3. clone or hard-reset the in-scope repos into $PCI_WORKSPACE at the branches in scope.json
#   4. replay the recorded scan and check the expected counts
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WS="${PCI_WORKSPACE:-$HOME/pci-workspace}"
start=$(date +%s)

rm -rf "$HERE/out"
python3 "$HERE/seed_baseline.py" --scan "$HERE/fixtures/2026-Q4-scan.json" --cutoff 2026-07-01

mkdir -p "$WS"
python3 - "$HERE/scope.json" <<'PY' | while read -r name url branch; do
import json, sys
for n, c in json.load(open(sys.argv[1]))["repos"].items():
    print(n, c["git"], c["branch"])
PY
  if [[ -L "$WS/$name" ]]; then
    echo "workspace: $name is a symlink to a local checkout, leaving it"
  elif [[ -d "$WS/$name/.git" ]]; then
    git -C "$WS/$name" fetch -q origin "$branch" && git -C "$WS/$name" checkout -q -B "$branch" "origin/$branch" \
      && git -C "$WS/$name" reset -q --hard "origin/$branch" && git -C "$WS/$name" clean -qfdx
    echo "workspace: reset $name to origin/$branch"
  else
    git clone -q --branch "$branch" "$url" "$WS/$name"
    echo "workspace: cloned $name ($branch)"
  fi
done

python3 "$HERE/rescan.py" --snapshot "$HERE/fixtures/2026-Q4-scan.json" --date 2026-10-12 --out "$HERE/out" >/dev/null
python3 - "$HERE/out/2026-Q4/evidence.json" <<'PY'
import json, sys
e = json.load(open(sys.argv[1]))
counts = {k: len(e[k]) for k in ("new", "carried", "closed", "accepted")}
assert counts == {"new": 44, "carried": 176, "closed": 0, "accepted": 2}, counts
print("replay check ok:", counts)
PY
rm -rf "$HERE/out"
echo "reset complete in $(( $(date +%s) - start ))s"
