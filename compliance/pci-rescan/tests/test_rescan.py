import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import rescan  # noqa: E402

SCOPE = {
    "repos": {
        "bank": {"pci_scope": "cde", "owner": "bank-eng", "security_owner": "appsec"},
        "stps": {"pci_scope": "connected", "owner": "post-trade-eng", "security_owner": "appsec"},
    },
    "remediation_days": {"critical": 30, "high": 30, "medium": 90, "low": 180},
}


def dep(repo, pkg, ver, fixed, sev="high", adv="GHSA-x"):
    return {"id": f"{repo}:{pkg}:{adv}", "repo": repo, "kind": "dependency", "source": "osv.dev", "advisory": adv,
            "aliases": [], "severity": sev, "title": adv, "package": pkg, "version": ver,
            "fixed_in": [fixed] if fixed else [], "location": "pom.xml", "published": "2026-08-01", "reference": ""}


def cred(repo):
    return {"id": f"{repo}:secret:application.properties:db.password", "repo": repo, "kind": "credential",
            "source": "secret check", "advisory": "CWE-798", "aliases": [], "severity": "high", "title": "cred",
            "package": None, "version": None, "fixed_in": [], "location": "application.properties:3",
            "published": None, "reference": ""}


def run(findings, entries, today="2026-10-12"):
    import datetime as dt
    return rescan.diff({"findings": findings}, {"entries": entries}, SCOPE, dt.date.fromisoformat(today))


def test_quarters():
    import datetime as dt
    assert rescan.quarter_of(dt.date(2026, 10, 12)) == "2026-Q4"
    assert rescan.prior_quarter("2026-Q4") == "2026-Q3"
    assert rescan.prior_quarter("2027-Q1") == "2026-Q4"


def test_new_carried_closed_accepted():
    carried, new, accepted = dep("bank", "a:b", "1.0", "1.1", adv="G1"), dep("bank", "c:d", "1.0", "1.2", adv="G2"), \
        dep("stps", "e:f", "1.0", None, adv="G3")
    entries = [
        {"id": carried["id"], "status": "open", "first_seen": "2026-07-01", "due": "2026-07-31", "owner": "alice"},
        {"id": accepted["id"], "status": "risk-accepted", "first_seen": "2026-07-01", "accepted_until": "2027-01-31",
         "acceptance_ref": "RA-1"},
        {"id": "bank:gone:G9", "status": "open", "first_seen": "2026-07-01", "due": "2026-07-31"},
    ]
    d = run([carried, new, accepted], entries)
    assert [f["id"] for f in d["new"]] == [new["id"]]
    assert d["carried"][0]["owner"] == "alice" and d["carried"][0]["due"] == "2026-07-31"
    assert [f["id"] for f in d["accepted"]] == [accepted["id"]]
    assert [f["id"] for f in d["closed"]] == ["bank:gone:G9"]


def test_expired_acceptance_is_new_again():
    f = dep("stps", "e:f", "1.0", "1.1", adv="G3")
    d = run([f], [{"id": f["id"], "status": "risk-accepted", "first_seen": "2026-01-01", "accepted_until": "2026-09-30"}])
    assert [x["id"] for x in d["new"]] == [f["id"]]


def test_fixed_finding_is_not_reopened_as_autofix_work():
    f = dep("bank", "a:b", "1.0", "1.1", adv="G1")
    d = run([f], [{"id": f["id"], "status": "fixed", "first_seen": "2026-04-01", "fixed_on": "2026-06-01"}])
    assert d["new"][0]["regression"] is True
    assert d["new"][0]["route"] == "human-owner"
    assert rescan.autofix_plan(d["new"]) == []


def test_routing_and_risk_priority():
    same_major = dep("bank", "t:tomcat", "10.1.28", "10.1.58", sev="critical", adv="G1")
    major = dep("bank", "s:webmvc", "6.1.12", "7.0.9", sev="critical", adv="G2")
    nofix = dep("stps", "l:lz4", "1.8.0", None, sev="medium", adv="G3")
    d = run([same_major, major, nofix, cred("bank")], [])
    by = {f["advisory"]: f for f in d["new"]}
    assert by["G1"]["route"] == "devin-autofix" and by["G1"]["priority"] == "P1" and by["G1"]["owner"] == "bank-eng"
    assert by["G2"]["route"] == "human-owner" and by["G2"]["owner"] == "appsec"
    assert by["G3"]["route"] == "watch-list" and by["G3"]["priority"] == "P3"
    assert by["CWE-798"]["route"] == "human-owner"
    assert by["G1"]["due"] == "2026-11-11" and by["G3"]["due"] == "2027-01-10"


def test_autofix_plan_groups_per_repo_and_takes_highest_fix():
    a = dep("bank", "j:databind", "2.17.2", "2.18.8", adv="G1")
    b = dep("bank", "j:databind", "2.17.2", "2.18.11", adv="G2")
    c = dep("stps", "j:databind", "2.18.2", "2.18.11", adv="G1")
    plan = rescan.autofix_plan(run([a, b, c], [])["new"])
    assert {p["repo"]: p["packages"]["j:databind"]["to"] for p in plan} == {"bank": "2.18.11", "stps": "2.18.11"}


def test_decisions_group_one_package_across_repos():
    x = dep("bank", "s:webmvc", "6.1.12", "7.0.9", sev="critical", adv="G2")
    y = dep("stps", "s:webmvc", "6.2.2", "7.0.9", sev="critical", adv="G2")
    g = rescan.decisions(run([x, y], [])["new"])
    assert len(g) == 1 and g[0]["repos"] == ["bank", "stps"]


def test_ledger_roundtrip_makes_next_quarter_quiet(tmp_path):
    f = dep("bank", "a:b", "1.0", "1.1", adv="G1")
    d = run([f], [])
    ledger = rescan.next_ledger("2026-Q4", d, {"entries": []})
    import datetime as dt
    again = rescan.diff({"findings": [f]}, ledger, SCOPE, dt.date(2027, 1, 5))
    assert again["new"] == [] and len(again["carried"]) == 1


def test_secret_check_ignores_placeholders(tmp_path):
    res = tmp_path / "src/main/resources"
    res.mkdir(parents=True)
    (res / "application.properties").write_text(
        "spring.datasource.password=Test@123\nspring.mail.password=${MAIL_PASSWORD}\n# api.token=abc\n")
    fs = rescan.secret_findings("bank", tmp_path)
    assert [f["location"] for f in fs] == ["src/main/resources/application.properties:1"]


def test_recorded_snapshot_end_to_end(tmp_path):
    here = pathlib.Path(rescan.__file__).parent
    rc = rescan.main(["--snapshot", str(here / "fixtures/2026-Q4-scan.json"), "--date", "2026-10-12",
                      "--out", str(tmp_path)])
    assert rc == 0
    ev = json.loads((tmp_path / "2026-Q4/evidence.json").read_text())
    assert ev["meta"]["baseline"] == "2026-Q3"
    assert len(ev["new"]) == 44 and len(ev["closed"]) == 0 and len(ev["accepted"]) == 2
    decisions = json.loads((tmp_path / "2026-Q4/decisions.json").read_text())
    assert [g["subject"] for g in decisions] == ["org.springframework:spring-webmvc"]
    for name in ("evidence.md", "digest.md", "autofix-plan.json", "ledger-next.json", "MANIFEST.sha256"):
        assert (tmp_path / "2026-Q4" / name).exists()


def test_dependency_list_parser_handles_classifiers():
    text = ("   org.apache.kafka:kafka_2.13:jar:test:3.8.1:test -- module kafka [auto]\n"
            "   org.yaml:snakeyaml:jar:2.3:compile -- module org.yaml.snakeyaml\n")
    assert rescan.parse_dependency_list(text) == [("org.apache.kafka:kafka_2.13", "3.8.1"),
                                                   ("org.yaml:snakeyaml", "2.3")]


def test_regression_stays_with_a_human_next_quarter():
    import datetime as dt
    f = dep("bank", "a:b", "1.0", "1.1", adv="G1")
    q3 = {"entries": [{"id": f["id"], "status": "fixed", "first_seen": "2026-04-01", "fixed_on": "2026-06-01"}]}
    q4 = run([f], q3["entries"])
    assert q4["new"][0]["route"] == "human-owner"
    ledger = rescan.next_ledger("2026-Q4", q4, q3)
    assert [e["status"] for e in ledger["entries"] if e["id"] == f["id"]] == ["open"]
    q1 = rescan.diff({"findings": [f]}, ledger, SCOPE, dt.date(2027, 1, 5))
    assert q1["carried"][0]["route"] == "human-owner" and rescan.autofix_plan(q1["carried"]) == []


def test_critical_due_date_runs_from_advisory_release():
    f = dep("bank", "t:tomcat", "10.1.28", "10.1.58", sev="critical", adv="G1")
    f["published"] = "2026-08-26"
    assert run([f], [])["new"][0]["due"] == "2026-09-25"
    h = dep("bank", "j:core", "2.17.2", "2.18.11", sev="high", adv="G2")
    assert run([h], [])["new"][0]["due"] == "2026-11-11"


def test_missing_prior_ledger_fails_closed(tmp_path, capsys):
    import pytest
    here = pathlib.Path(rescan.__file__).parent
    args = ["--snapshot", str(here / "fixtures/2026-Q4-scan.json"), "--date", "2026-10-12",
            "--baseline-dir", str(tmp_path / "none"), "--out", str(tmp_path / "out")]
    with pytest.raises(SystemExit):
        rescan.main(args)
    assert rescan.main(args + ["--first-run"]) == 0


def test_snapshot_saved_into_new_directory_and_pack(tmp_path):
    here = pathlib.Path(rescan.__file__).parent
    snap = tmp_path / "does/not/exist/scan.json"
    rescan.main(["--snapshot", str(here / "fixtures/2026-Q4-scan.json"), "--date", "2026-10-12",
                 "--out", str(tmp_path / "out"), "--save-snapshot", str(snap)])
    assert snap.exists()
    assert "scan.json" in (tmp_path / "out/2026-Q4/MANIFEST.sha256").read_text()
