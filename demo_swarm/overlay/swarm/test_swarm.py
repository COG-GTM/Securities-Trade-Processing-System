import tempfile, unittest
from pathlib import Path

import swarm

XML = """<testsuite name="{cls}" tests="3" failures="1" errors="1" skipped="0">
  <testcase name="ok" classname="a.b.{cls}"/>
  <testcase name="red" classname="a.b.{cls}"><failure message="expected true"/></testcase>
  <testcase name="boom" classname="a.b.{cls}"><error message="NPE"/></testcase>
</testsuite>"""


class ParseReportsTest(unittest.TestCase):
    def test_counts_come_from_surefire_not_build_status(self):
        with tempfile.TemporaryDirectory() as d:
            Path(d, "TEST-a.b.Logic1SwarmTest.xml").write_text(XML.format(cls="Logic1SwarmTest"))
            cases, totals = swarm.parse_reports(d)
        self.assertEqual(totals, {"tests": 3, "failures": 1, "errors": 1, "skipped": 0})
        self.assertEqual(cases["Logic1SwarmTest#red"], "fail")
        self.assertEqual(cases["Logic1SwarmTest#boom"], "fail")
        self.assertEqual(cases["Logic1SwarmTest#ok"], "pass")

    def test_no_reports_means_zero_tests(self):
        with tempfile.TemporaryDirectory() as d:
            _, totals = swarm.parse_reports(d)
        self.assertEqual(totals["tests"], 0)

    def test_summarize_is_red_on_any_failure(self):
        self.assertEqual(swarm.summarize({}, {"tests": 2, "failures": 1, "errors": 0, "skipped": 0}), "RED")
        self.assertEqual(swarm.summarize({}, {"tests": 2, "failures": 0, "errors": 0, "skipped": 0}), "GREEN")

    def test_dedupe_merges_same_root_cause_only(self):
        raw = [
            {"id": "RACE-1", "persona": "RACE", "cwe": "CWE-001", "sink": "LedgerService#post", "root_cause": "missing-key-check",
             "severity": "critical", "disposition": "fix"},
            {"id": "LOGIC-2", "persona": "LOGIC", "cwe": "CWE-001", "sink": "LedgerService#post", "root_cause": "missing-key-check",
             "severity": "high", "disposition": "fix"},
            {"id": "LOGIC-1", "persona": "LOGIC", "cwe": "CWE-002", "sink": "QuoteService#update", "root_cause": "unbounded-input",
             "severity": "high", "disposition": "fix"},
            {"id": "SUPPLY-1", "persona": "SUPPLY", "cwe": "CWE-003", "sink": "example-lib@1.0.0", "root_cause": "cve",
             "severity": "critical", "disposition": "report-only"},
        ]
        rows = swarm.dedupe(raw)
        self.assertEqual([r["id"] for r in rows], ["RACE-1", "LOGIC-1", "SUPPLY-1"])
        self.assertEqual(rows[0]["duplicates"], ["LOGIC-2"])
        self.assertEqual(rows[0]["reported_by"], ["LOGIC", "RACE"])


class ConflictMarkerTest(unittest.TestCase):
    def test_code_mentioning_conflict_is_not_a_merge_conflict(self):
        clean = "changed in both\n  our    100644 a\n@@ -1 +1 @@\n+throw new ResponseStatusException(HttpStatus.CONFLICT);\n"
        self.assertIsNone(swarm.CONFLICT_MARKER.search(clean))

    def test_merge_markers_are_a_conflict(self):
        dirty = "changed in both\n@@ -1,3 +1,7 @@\n+<<<<<<< .our\n+a\n+=======\n+b\n+>>>>>>> .their\n"
        self.assertIsNotNone(swarm.CONFLICT_MARKER.search(dirty))


if __name__ == "__main__":
    unittest.main()
