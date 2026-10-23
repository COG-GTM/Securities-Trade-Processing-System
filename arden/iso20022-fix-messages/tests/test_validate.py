from pathlib import Path

from validate import load_mapping, load_mappings, sample_checks, validate_fix

ROOT = Path(__file__).resolve().parents[1]


def test_sample_is_valid():
    mapping = load_mapping(ROOT / "mapping/settlement-cycle.yaml")
    assert validate_fix((ROOT / "samples/fix/AE-xlon-regular-way.fix").read_text(), mapping) == []


def test_iso_dates_rejected_in_tag64():
    mapping = load_mapping(ROOT / "mapping/settlement-cycle.yaml")
    assert validate_fix("8=FIX.4.4|63=2|64=2027-10-12|207=XLON|", mapping)


def test_iso_dates_rejected_in_tag64_with_effective_dated_mappings():
    assert validate_fix("8=FIX.4.4|75=20271011|63=2|64=2027-10-12|207=XLON|", load_mappings(ROOT))


def test_every_sample_is_valid():
    checks = sample_checks(ROOT)
    assert {f.name for f, _ in checks} >= {"AE-xlon-regular-way.fix", "AE-xlon-regular-way-pre-go-live.fix"}
    assert any("archive" in f.parts for f, _ in checks)
    for f, mapping in checks:
        assert validate_fix(f.read_text(), mapping) == [], f


def test_archived_sample_is_t_plus_two_under_the_archived_mapping():
    archived = ROOT / "samples/archive/pre-2027-10-11/fix/AE-xlon-regular-way.fix"
    assert validate_fix(archived.read_text(), load_mapping(ROOT / "mapping/archive/settlement-cycle.pre-2027-10-11.yaml")) == []
    assert validate_fix(archived.read_text(), load_mappings(ROOT))


def test_tag63_follows_trade_date_for_in_scope_venues():
    mappings = load_mappings(ROOT)
    for mic in ("XLON", "XPAR", "XETR", "XSWX"):
        assert validate_fix(f"75=20271008|63=3|64=20271012|207={mic}|", mappings) == []
        assert validate_fix(f"75=20271011|63=2|64=20271012|207={mic}|", mappings) == []
        assert validate_fix(f"75=20271011|63=3|64=20271013|207={mic}|", mappings)


def test_north_america_unchanged_across_go_live():
    mappings = load_mappings(ROOT)
    for td in ("20271008", "20271011"):
        assert validate_fix(f"75={td}|63=2|64=20271012|207=XNYS|", mappings) == []
