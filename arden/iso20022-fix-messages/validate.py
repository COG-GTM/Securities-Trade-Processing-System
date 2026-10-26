"""Validate FIX/ISO samples against mapping/settlement-cycle.yaml (stdlib only)."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def load_mapping(path: Path) -> dict:
    markets, default = {}, {}
    for line in path.read_text().splitlines():
        m = re.match(r"\s+(X[A-Z]{3}): \{ cycle: (T\+\d), fix_settl_type: \"(\d)\"(?:, effective_from: (\d{4}-\d{2}-\d{2}), "
                     r"before: \{ cycle: (T\+\d), fix_settl_type: \"(\d)\" \})? \}", line)
        if m:
            markets[m.group(1)] = {"cycle": m.group(2), "fix_settl_type": m.group(3)}
            if m.group(4):
                markets[m.group(1)]["effective_from"] = m.group(4).replace("-", "")
                markets[m.group(1)]["before"] = {"cycle": m.group(5), "fix_settl_type": m.group(6)}
        d = re.match(r"default_fix_settl_type: \"(\d)\"", line)
        if d:
            default["fix_settl_type"] = d.group(1)
    return {"markets": markets, "default": default}


def validate_fix(text: str, mapping: dict) -> list[str]:
    """Tag 63 is checked against the mapping entry in force on the trade date (tag 75)."""
    fields = dict(f.split("=", 1) for f in text.strip().strip("|").split("|"))
    errors = []
    if not re.fullmatch(r"\d{8}", fields.get("64", "")):
        errors.append("tag 64 must be LocalMktDate YYYYMMDD")
    entry = mapping["markets"].get(fields.get("207"), mapping["default"])
    if "effective_from" in entry and fields.get("75", "") < entry["effective_from"]:
        entry = entry["before"]
    expected = entry["fix_settl_type"]
    if fields.get("63") != expected:
        errors.append(f"tag 63 should be {expected} for {fields.get('207')}")
    return errors


def main() -> int:
    root = Path(__file__).parent
    mapping = load_mapping(root / "mapping/settlement-cycle.yaml")
    rc = 0
    for f in sorted((root / "samples/fix").glob("*.fix")):
        errs = validate_fix(f.read_text(), mapping)
        print(f"{f.name}: {'ok' if not errs else '; '.join(errs)}")
        rc |= bool(errs)
    return rc


if __name__ == "__main__":
    sys.exit(main())
