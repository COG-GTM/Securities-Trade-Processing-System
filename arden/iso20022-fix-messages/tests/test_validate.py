from pathlib import Path

from validate import load_mapping, validate_fix

ROOT = Path(__file__).resolve().parents[1]


def test_sample_is_valid():
    mapping = load_mapping(ROOT / "mapping/settlement-cycle.yaml")
    assert validate_fix((ROOT / "samples/fix/AE-xlon-regular-way.fix").read_text(), mapping) == []


def test_iso_dates_rejected_in_tag64():
    mapping = load_mapping(ROOT / "mapping/settlement-cycle.yaml")
    assert validate_fix("8=FIX.4.4|63=3|64=2027-10-13|207=XLON|", mapping)
