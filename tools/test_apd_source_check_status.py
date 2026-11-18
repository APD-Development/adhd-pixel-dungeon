# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
# GPLv3 or later; no warranty. See the root GPL license for full terms.
import unittest
from apd_source_check_status import payload,REPOSITORY

class SourceStatusEvidenceTest(unittest.TestCase):
    def proof(self):
        return dict(source_commit="a"*40,suites=446,tests=2743,failures=0,errors=0,skipped=0,
                    apk_assembled=False,physical_phone_tested=False)
    def build(self,status="success",proof=None):
        return payload(REPOSITORY,"a"*40,123,status,2743,self.proof() if proof is None else proof)
    def test_success_is_pinned_to_exact_source_and_run_without_native_claim(self):
        result=self.build();self.assertEqual("success",result["state"])
        self.assertTrue(result["target_url"].endswith("/actions/runs/123"))
        self.assertIn("compilation",result["description"]);self.assertIn("no APK",result["description"])
    def test_running_status_is_only_pending_even_with_passing_core_json(self):
        self.assertEqual("pending",self.build("running")["state"])
        self.assertEqual("pending",payload(REPOSITORY,"a"*40,123,"running",2743,None)["state"])
    def test_failure_or_cancelled_job_cannot_be_overridden_by_passing_core_json(self):
        self.assertEqual("failure",self.build("failure")["state"])
        self.assertEqual("error",self.build("cancelled")["state"])
    def test_success_rejects_stale_missing_failed_or_changed_evidence(self):
        for key,value in [("source_commit","b"*40),("tests",2742),("failures",1),("errors",1),
                          ("skipped",1),("suites",0),("apk_assembled",True),("physical_phone_tested",True)]:
            with self.subTest(key=key):
                proof=self.proof();proof[key]=value
                with self.assertRaises(ValueError):self.build(proof=proof)
        with self.assertRaises(ValueError):self.build(proof={})
        with self.assertRaises(ValueError):payload(REPOSITORY,"a"*40,123,"success",2743,None)
    def test_unexpected_identity_or_unknown_outcome_is_rejected(self):
        for repo,commit,run,status in [("another/repo","a"*40,123,"success"),(REPOSITORY,"not-a-sha",123,"success"),
                                      (REPOSITORY,"a"*40,0,"success"),(REPOSITORY,"a"*40,123,"pending")]:
            with self.assertRaises(ValueError):payload(repo,commit,run,status,2743,self.proof())
