"""Test simulator delivery guards using fake adb; these are not gameplay runs."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

RUNNER = Path(__file__).with_name('run-warrior-instrumentation.sh').resolve()


class InstrumentationGuardTest(unittest.TestCase):
    def run_script(self, hardware='ranchu', corrupt=False, source='verified', outcome='OK (1 test)', completed=True, seeds=''):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            binaries = root / 'simulator-binaries'
            binaries.mkdir()
            lines = []
            for name in ['android-debug.apk', 'android-debug-androidTest.apk']:
                payload = b'fixture binary, not an Android application'
                path = binaries / name
                path.write_bytes(payload)
                lines.append(hashlib.sha256(payload).hexdigest() + '  simulator-binaries/' + name + '\n')
            (binaries / 'SHA256SUMS').write_text(''.join(lines))
            if corrupt:
                (binaries / 'android-debug.apk').write_bytes(b'tampered fixture')
            (binaries / 'manifest.json').write_text(json.dumps({'sourceCommit': source}))
            row = dict(seed=1000000, completed=completed, elapsedMillis=1, trace=['action fixture'])
            (root / 'fixture.jsonl').write_text(json.dumps(row) + '\n')
            adb = root / 'adb'
            adb.write_text('#!/usr/bin/env bash\n'
                           'echo "$*" >> adb-calls.txt\n'
                           'case "$*" in\n'
                           '"shell getprop ro.hardware") echo "$FIXTURE_HARDWARE";;\n'
                           '"shell am instrument"*) echo "$FIXTURE_OUTCOME";;\n'
                           '"exec-out run-as"*) cat fixture.jsonl;;\n'
                           'esac\n')
            adb.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                       APD_SIMULATION_SOURCE='verified', APD_SIMULATION_COUNT='1', APD_SIMULATION_SEED='1000000', APD_SIMULATION_SEEDS=seeds,
                       FIXTURE_HARDWARE=hardware, FIXTURE_OUTCOME=outcome)
            result = subprocess.run(['bash', str(RUNNER)], cwd=root, env=env, capture_output=True, text=True)
            return result.returncode, (root / 'adb-calls.txt').read_text()

    def test_physical_device_refused_before_installation(self):
        status, calls = self.run_script(hardware='phone')
        self.assertNotEqual(0, status)
        self.assertNotIn('install', calls)

    def test_corrupt_binary_refused_before_installation(self):
        status, calls = self.run_script(corrupt=True)
        self.assertNotEqual(0, status)
        self.assertNotIn('install', calls)

    def test_wrong_source_refused_before_installation(self):
        status, calls = self.run_script(source='unverified')
        self.assertNotEqual(0, status)
        self.assertNotIn('install', calls)

    def test_verified_protocol_copies_evidence_without_uninstall_or_rebuild(self):
        status, calls = self.run_script()
        self.assertEqual(0, status)
        self.assertIn('shell am instrument', calls)
        self.assertNotIn('uninstall', calls)
        self.assertNotIn('clear', calls.replace('logcat -c', ''))

    def test_failed_instrumentation_and_incomplete_trace_fail_closed(self):
        for outcome, completed in [('FAILURES!!!', True), ('OK (1 test)', False), ('OK (1 test)', 'true')]:
            with self.subTest(outcome=outcome, completed=completed):
                status, calls = self.run_script(outcome=outcome, completed=completed)
                self.assertNotEqual(0, status)

    def test_explicit_seed_plan_is_forwarded_and_wrong_evidence_is_refused(self):
        status, calls = self.run_script(seeds='1000000')
        self.assertEqual(0, status)
        self.assertIn('-e simulationSeeds 1000000', calls)
        status, calls = self.run_script(seeds='1000001')
        self.assertNotEqual(0, status)


if __name__ == '__main__':
    unittest.main()
