"""Regression cases for false/missing simulator evidence; these fixtures are not game runs."""
import base64
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

RUNNER = Path(__file__).with_name('run-warrior-simulation.sh')
PARSER = RUNNER.read_text().split("python3 - \"$apd_count\" \"$apd_seed\" <<'PY'\n", 1)[1].rsplit('\nPY', 1)[0]


class TraceRecoveryTest(unittest.TestCase):
    def row(self):
        return dict(seed=1000000, completed=True, observedHealingBeforeNormalFlyHealing=False,
                    normalFlyHealingObserved=False, terminalDepth=1, terminalHp=0, stopReason='death',
                    startedEpochMillis=1, finishedEpochMillis=2, elapsedMillis=1,
                    trace=['0:ATTACK:hp=20/20:unrelated Unicode é 雪' * 100])

    def chunks(self, row=None, episode=0):
        encoded = base64.b64encode(json.dumps(self.row() if row is None else row).encode()).decode()
        parts = [encoded[i:i+2000] for i in range(0, len(encoded), 2000)]
        return [f'10-10 19:00:00.000 I/APDSIM: APD_SIM_CHUNK:{episode}:{i}:{len(parts)}:{part}'
                for i, part in enumerate(parts)]

    def run_parser(self, lines, explicit=''):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'simulator-logcat.txt').write_text('\n'.join(lines))
            result = subprocess.run([sys.executable, '-c', PARSER, '1', '1000000'], cwd=root,
                                    capture_output=True, text=True, env={**os.environ, 'APD_SIMULATION_SEEDS': explicit})
            output = root / 'simulation-results.jsonl'
            return result.returncode, output.read_text() if output.exists() else None

    def test_actual_chunk_protocol_recovers_unicode_in_any_delivery_order(self):
        status, output = self.run_parser(list(reversed(self.chunks())))
        self.assertEqual(0, status)
        self.assertEqual(self.row(), json.loads(output))

    def test_selective_seed_plan_recovers_only_the_requested_episode(self):
        row = self.row(); row['seed'] = 1000042
        status, output = self.run_parser(self.chunks(row), '1000042')
        self.assertEqual(0, status); self.assertEqual(row, json.loads(output))
        self.assertNotEqual(0, self.run_parser(self.chunks(row), '1000043')[0])

    def test_incomplete_attempt_is_preserved_without_creating_passed_results(self):
        row = self.row(); row['completed'] = False; row['stopReason'] = 'game adapter failure'
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'simulator-logcat.txt').write_text('\n'.join(self.chunks(row)))
            result = subprocess.run([sys.executable, '-c', PARSER, '1', '1000000'], cwd=root,
                                    capture_output=True, env={**os.environ, 'APD_SIMULATION_SEEDS': ''})
            self.assertNotEqual(0, result.returncode)
            self.assertFalse((root / 'simulation-results.jsonl').exists())
            self.assertEqual(row, json.loads((root / 'simulation-attempts.jsonl').read_text()))

    def test_unknown_package_error_is_never_json_evidence(self):
        status, output = self.run_parser(['run-as: unknown package: com.charltonchris29.adhdpixeldungeon.indev'])
        self.assertNotEqual(0, status)
        self.assertIsNone(output)

    def test_missing_duplicate_extra_and_corrupt_chunks_fail_closed(self):
        good = self.chunks()
        cases = [good[:-1], good + [good[0]], good + self.chunks(episode=1),
                 ['APD_SIM_CHUNK:0:0:1:AAAA']]
        for lines in cases:
            with self.subTest(lines=len(lines)):
                status, output = self.run_parser(lines)
                self.assertNotEqual(0, status)
                self.assertIsNone(output)

    def test_wrong_seed_incomplete_missing_fields_and_bad_trace_are_rejected(self):
        for key, value in [('seed', 1000001), ('completed', False), ('completed', 'true'),
                           ('trace', 'not an action trace'), ('trace', [])]:
            row = self.row(); row[key] = value
            with self.subTest(key=key, value=value):
                status, output = self.run_parser(self.chunks(row))
                self.assertNotEqual(0, status)
                self.assertIsNone(output)
        row = self.row(); del row['terminalHp']
        status, output = self.run_parser(self.chunks(row))
        self.assertNotEqual(0, status)
        self.assertIsNone(output)


if __name__ == '__main__':
    unittest.main()
