"""Validate FIX/ISO samples against mapping/settlement-cycle.yaml (stdlib only).

The mapping is effective-dated: mapping/settlement-cycle.yaml applies from its ``effective_from`` trade date, archived
mappings in mapping/archive/ apply up to their ``effective_until``. The version is chosen from FIX tag 75 (TradeDate).
Archived golden samples (samples/archive/) record pre-migration output and are checked against the archived mapping.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path


def load_mapping(path: Path) -> dict:
    markets, default, effective = {}, {}, {}
    for line in path.read_text().splitlines():
        m = re.match(r"\s+(X[A-Z]{3}): \{ cycle: (T\+\d), fix_settl_type: \"(\d)\" \}", line)
        if m:
            markets[m.group(1)] = {"cycle": m.group(2), "fix_settl_type": m.group(3)}
        d = re.match(r"default_fix_settl_type: \"(\d)\"", line)
        if d:
            default["fix_settl_type"] = d.group(1)
        e = re.match(r"(effective_from|effective_until): (\d{4})-(\d{2})-(\d{2})", line)
        if e:
            effective[e.group(1)] = e.group(2) + e.group(3) + e.group(4)
    return {"markets": markets, "default": default, **effective}


def load_mappings(root: Path) -> list[dict]:
    return [load_mapping(root / "mapping/settlement-cycle.yaml")] + [
        load_mapping(p) for p in sorted((root / "mapping/archive").glob("*.yaml"))
    ]


def mapping_for(mappings: list[dict], trade_date: str) -> dict:
    for m in mappings:
        if m.get("effective_from", "") <= trade_date and trade_date <= m.get("effective_until", "99991231"):
            return m
    raise LookupError(f"no settlement-cycle mapping effective for trade date {trade_date}")


def validate_fix(text: str, mapping: dict | list[dict]) -> list[str]:
    fields = dict(f.split("=", 1) for f in text.strip().strip("|").split("|"))
    errors = []
    if not re.fullmatch(r"\d{8}", fields.get("64", "")):
        errors.append("tag 64 must be LocalMktDate YYYYMMDD")
    if isinstance(mapping, list):
        if not re.fullmatch(r"\d{8}", fields.get("75", "")):
            return errors + ["tag 75 must be LocalMktDate YYYYMMDD"]
        mapping = mapping_for(mapping, fields["75"])
    expected = mapping["markets"].get(fields.get("207"), mapping["default"])["fix_settl_type"]
    if fields.get("63") != expected:
        errors.append(f"tag 63 should be {expected} for {fields.get('207')}")
    return errors


ARCHIVED_SAMPLES = {"pre-2027-10-11": "mapping/archive/settlement-cycle.pre-2027-10-11.yaml"}


def sample_checks(root: Path) -> list[tuple[Path, dict | list[dict]]]:
    checks = [(f, load_mappings(root)) for f in sorted((root / "samples/fix").glob("*.fix"))]
    for name, mapping in ARCHIVED_SAMPLES.items():
        archived = load_mapping(root / mapping)
        checks += [(f, archived) for f in sorted((root / "samples/archive" / name / "fix").glob("*.fix"))]
    return checks


def main() -> int:
    root = Path(__file__).parent
    rc = 0
    for f, mapping in sample_checks(root):
        errs = validate_fix(f.read_text(), mapping)
        print(f"{f.relative_to(root)}: {'ok' if not errs else '; '.join(errs)}")
        rc |= bool(errs)
    return rc


if __name__ == "__main__":
    sys.exit(main())
