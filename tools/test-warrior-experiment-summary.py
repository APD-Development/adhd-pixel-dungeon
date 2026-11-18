"""Artificial fixtures validate evidence accounting; they are never counted as game runs."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import subprocess
import sys

spec = importlib.util.spec_from_file_location('summary', Path(__file__).with_name('summarize-warrior-experiment.py'))
summary = importlib.util.module_from_spec(spec); spec.loader.exec_module(summary)
A, B = 'a'*40, 'b'*40


class SummaryTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(); self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
    def write(self, name, rows, source=A):
        folder=self.root/name; folder.mkdir()
        (folder/'manifest.json').write_text(json.dumps(dict(sourceCommit=source, verificationRunId='123')))
        (folder/'simulation-results.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in rows))
    def row(self, seed=10, completed=True, started=1):
        return dict(seed=seed, completed=completed, startedEpochMillis=started, finishedEpochMillis=started+20,
                    elapsedMillis=20, trace=['0:ATTACK'], stopReason='death' if completed else 'game adapter failure',
                    observedHealingBeforeNormalFlyHealing=completed, normalFlyHealingObserved=False, terminalHp=0)
    def read(self): return summary.summarize(self.root,10,3,{A,B})[0]
    def test_missing_and_failed_attempts_never_become_completed_seeds(self):
        self.write('first',[self.row(10),self.row(11,False)])
        r=self.read(); self.assertEqual(1,r['completedDistinctSeeds']); self.assertEqual([11,12],r['unresolvedSeeds'])
        self.assertFalse(r['allRequestedSeedsCompleted']); self.assertEqual(40,r['summedEpisodeElapsedMillis'])
    def test_recovery_retains_failure_and_counts_one_completed_seed(self):
        self.write('first',[self.row(10,False)]); self.write('recovery',[self.row(10,True,5),self.row(11),self.row(12)])
        r=self.read(); self.assertEqual(4,r['actualAttempts']); self.assertEqual(3,r['completedDistinctSeeds'])
        self.assertTrue(r['singleSourceComplete']); self.assertEqual(3,r['perSource'][0]['consumedHealingBeforeNormalFlyHealing'])
    def test_mirrored_artifacts_do_not_double_count_attempts_or_timing(self):
        self.write('first',[self.row()]); self.write('copy',[self.row()])
        r=self.read(); self.assertEqual(1,r['actualAttempts']); self.assertEqual(1,r['mirroredCopiesIgnored'])
        self.assertEqual(20,r['summedEpisodeElapsedMillis'])
    def test_conflicting_copies_are_rejected(self):
        self.write('first',[self.row()]); changed=self.row(); changed['trace']=['different']
        self.write('copy',[changed]); self.assertRaises(ValueError,self.read)
    def test_mixed_adapters_do_not_claim_a_single_source_balance_dataset(self):
        self.write('first',[self.row(10),self.row(11)],A); self.write('recovery',[self.row(12)],B)
        r=self.read(); self.assertTrue(r['allRequestedSeedsCompleted']); self.assertFalse(r['singleSourceComplete'])
        self.assertEqual([2,1],[g['completedDistinctSeeds'] for g in r['perSource']])
        output=self.root/'summary.json'
        process=subprocess.run([sys.executable,str(Path(__file__).with_name('summarize-warrior-experiment.py')),
                              str(self.root),'--first-seed','10','--count','3','--allowed-source',A,
                              '--allowed-source',B,'--output',str(output)],capture_output=True,text=True)
        self.assertEqual(1,process.returncode)
        self.assertTrue(output.is_file()); self.assertTrue(output.with_suffix('.attempts.jsonl').is_file())
    def test_play_profile_is_reported_and_legacy_absence_is_explicit(self):
        normal=self.row(); normal['profile']='warrior-normal-loot-critical-heal-v1'
        self.write('normal',[normal]); self.write('legacy',[self.row(11,started=2)])
        self.assertEqual(['legacy: unrecorded','warrior-normal-loot-critical-heal-v1'],self.read()['perSource'][0]['profiles'])
    def test_mixed_play_profiles_cannot_complete_one_balance_cohort(self):
        rows=[self.row(10),self.row(11),self.row(12)]
        for row,profile in zip(rows,['normal','normal','reckless']): row['profile']=profile
        self.write('first',rows)
        r=self.read()
        self.assertTrue(r['allRequestedSeedsCompleted'])
        self.assertFalse(r['singleSourceComplete'])
        self.assertEqual({'normal':2,'reckless':1},r['perSource'][0]['completedDistinctSeedsByProfile'])
    def test_complete_profile_keeps_its_own_outcomes_and_other_attempts(self):
        old=self.row(10); old.update(profile='reckless',observedHealingBeforeNormalFlyHealing=False)
        normal=[self.row(seed,started=2) for seed in [10,11,12]]
        for row in normal: row['profile']='normal'
        self.write('first',[old]+normal)
        r=self.read(); self.assertEqual(4,r['actualAttempts']); self.assertTrue(r['singleSourceComplete'])
        profiles={g['profile']:g for g in r['perSource'][0]['perProfile']}
        self.assertEqual(3,profiles['normal']['consumedHealingBeforeNormalFlyHealing'])
        self.assertEqual(0,profiles['reckless']['consumedHealingBeforeNormalFlyHealing'])
    def test_rare_encounters_and_failed_attempts_are_retained_without_fabricating_completion(self):
        failed=self.row(10,False); failed['encounterCounts']={'Albino':1,'Wraith':1}
        completed=self.row(11); completed['encounterCounts']={'GnollExile':1}
        self.write('first',[failed,completed]); r=self.read(); group=r['perSource'][0]
        self.assertEqual({'Albino':1,'Wraith':1,'GnollExile':1},group['encounterCountsAllAttempts'])
        self.assertEqual({'GnollExile':1},group['encounterCountsCompletedAttempts'])
        self.assertEqual(0,group['attemptsWithoutEncounterBreakdown'])
        self.assertEqual(2,r['actualAttempts']); self.assertEqual(1,r['completedDistinctSeeds'])
    def test_unapproved_or_missing_source_manifest_is_rejected(self):
        self.write('first',[self.row()], 'c'*40); self.assertRaises(ValueError,self.read)
        (self.root/'first'/'manifest.json').unlink(); self.assertRaises(ValueError,self.read)
    def test_invalid_completion_trace_timing_and_seed_are_rejected(self):
        cases=[('completed','true'),('trace',[]),('elapsedMillis',-1),('seed',99),('normalFlyHealingObserved',1),
               ('stopReason','action limit'),('terminalHp',5),('encounterCounts',{'Albino':-1}),('profile',None)]
        for key,value in cases:
            with self.subTest(key=key):
                r=self.row(); r[key]=value
                f=self.root/'first'; f.mkdir(exist_ok=True)
                (f/'manifest.json').write_text(json.dumps(dict(sourceCommit=A,verificationRunId=123)))
                (f/'simulation-results.jsonl').write_text(json.dumps(r)+'\n')
                self.assertRaises(ValueError,self.read)


if __name__ == '__main__': unittest.main()
