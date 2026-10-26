from pathlib import Path

import pytest

from validate import load_mapping, validate_fix

ROOT = Path(__file__).resolve().parents[1]
MAPPING = load_mapping(ROOT / "mapping/settlement-cycle.yaml")
ARCHIVED = load_mapping(ROOT / "mapping/archive/settlement-cycle.pre-2027-10-11.yaml")


@pytest.mark.parametrize("sample", sorted((ROOT / "samples/fix").glob("*.fix")), ids=lambda p: p.name)
def test_sample_is_valid(sample):
    assert validate_fix(sample.read_text(), MAPPING) == []


@pytest.mark.parametrize("sample", sorted((ROOT / "samples/archive").rglob("*.fix")), ids=lambda p: p.name)
def test_archived_sample_is_valid_against_archived_mapping(sample):
    assert validate_fix(sample.read_text(), ARCHIVED) == []


def test_iso_dates_rejected_in_tag64():
    assert validate_fix("8=FIX.4.4|75=20271011|63=2|64=2027-10-12|207=XLON|", MAPPING)


@pytest.mark.parametrize("mic", ["XLON", "XPAR", "XETR", "XSWX"])
def test_eu_uk_ch_settl_type_is_gated_on_trade_date(mic):
    assert validate_fix(f"75=20271008|63=3|64=20271012|207={mic}|", MAPPING) == []
    assert validate_fix(f"75=20271011|63=2|64=20271012|207={mic}|", MAPPING) == []
    assert validate_fix(f"75=20271008|63=2|64=20271011|207={mic}|", MAPPING)
    assert validate_fix(f"75=20271011|63=3|64=20271013|207={mic}|", MAPPING)


def test_markets_outside_scope_unchanged():
    assert validate_fix("75=20271011|63=2|64=20271012|207=XNYS|", MAPPING) == []
    assert validate_fix("75=20271011|63=3|64=20271013|207=XTKS|", MAPPING) == []
