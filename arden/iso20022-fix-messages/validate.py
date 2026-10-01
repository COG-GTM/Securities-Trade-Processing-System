"""Validate FIX/ISO samples against mapping/settlement-cycle.yaml (stdlib only)."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def load_mapping(path: Path) -> dict:
    markets, default = {}, {}
    for line in path.read_text().splitlines():
        m = re.match(r"\s+(X[A-Z]{3}): \{ cycle: (T\+\d), fix_settl_type: \"(\d)\" \}", line)
        if m:
            markets[m.group(1)] = {"cycle": m.group(2), "fix_settl_type": m.group(3)}
        d = re.match(r"default_fix_settl_type: \"(\d)\"", line)
        if d:
            default["fix_settl_type"] = d.group(1)
    return {"markets": markets, "default": default}


def validate_fix(text: str, mapping: dict) -> list[str]:
    fields = dict(f.split("=", 1) for f in text.strip().strip("|").split("|"))
    errors = []
    if not re.fullmatch(r"\d{8}", fields.get("64", "")):
        errors.append("tag 64 must be LocalMktDate YYYYMMDD")
    expected = mapping["markets"].get(fields.get("207"), mapping["default"])["fix_settl_type"]
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
