# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
from contextlib import contextmanager, redirect_stdout
import hashlib
import io
import json
import subprocess
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch
import zlib

import apd_native_screenshot_transfer as transfer
import apd_recover439_440 as exact
import test_apd_recover439_440 as fixtures


class NativeScreenshotTransferTest(unittest.TestCase):
    def test_manifest_keeps_nested_classes_and_duplicate_capture_names(self):
        names = ['qol-existing-status-clear-concise-ChampionEnemy$Growing.png', 'qol-doorless-floor1 (1).png']
        text = ''.join('a' * 64 + '  ' + name + '\n' for name in names)
        self.assertEqual(transfer.parse_manifest(text), {name: 'a' * 64 for name in names})
        for bad in ['', 'bad digest\n', 'a' * 64 + '  ../outside.png\n', text + text,
                    'a' * 64 + '  bad;command.png\n', 'a' * 64 + '  sub/inside.png\n']:
            with self.subTest(text=bad[:80]), self.assertRaises(RuntimeError):
                transfer.parse_manifest(bad)

    def test_individual_pull_validates_before_replacing_and_skips_matching_bytes(self):
        data = fixtures.RecoverySafeguardsTest.png()
        digest = hashlib.sha256(data).hexdigest()
        name = 'qol-doorless-floor1 (1).png'
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            target = directory / name
            target.write_bytes(b'stale')

            def pull(command, **kwargs):
                self.assertEqual(command[2], transfer.REMOTE + '/' + name)
                Path(command[3]).write_bytes(data)

            with patch.object(transfer.subprocess, 'run', side_effect=pull):
                with self.assertRaises(RuntimeError):
                    transfer.pull_image(directory, name, '0' * 64)
                self.assertEqual(target.read_bytes(), b'stale')
                transfer.pull_image(directory, name, digest)
            self.assertEqual(target.read_bytes(), data)
            self.assertFalse(list(directory.glob('*.partial')))
            with patch.object(transfer.subprocess, 'run') as no_pull:
                transfer.pull_image(directory, name, digest)
                no_pull.assert_not_called()
            target.unlink()

            def corrupt(command, **kwargs):
                Path(command[3]).write_bytes(b'not a PNG')

            with patch.object(transfer.subprocess, 'run', side_effect=corrupt), self.assertRaises(ValueError):
                transfer.pull_image(directory, name)
            self.assertFalse(target.exists())
            self.assertFalse(list(directory.glob('*.partial')))

    def test_final_sync_rejects_remote_changes_missing_images_and_extra_local_images(self):
        data = fixtures.RecoverySafeguardsTest.png()
        digest = hashlib.sha256(data).hexdigest()
        first, changed = digest + '  a.png\n', digest + '  b.png\n'
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            for manifests in ([first, changed], [first, first]):
                with patch.object(transfer, 'output', side_effect=manifests), \
                     patch.object(transfer, 'pull_image'), self.assertRaises(RuntimeError):
                    transfer.sync_images(directory)
            (directory / 'a.png').write_bytes(data)
            (directory / 'stale.png').write_bytes(data)
            with patch.object(transfer, 'output', side_effect=[first, first]), \
                 patch.object(transfer, 'pull_image'), self.assertRaises(RuntimeError):
                transfer.sync_images(directory)
            (directory / 'stale.png').unlink()
            with patch.object(transfer, 'output', side_effect=[first, first]), \
                 patch.object(transfer, 'pull_image'):
                self.assertEqual(transfer.sync_images(directory), {'a.png': digest})

    def test_manifest_transport_recovers_without_weakening_hash_checks(self):
        digest = 'a' * 64
        manifest = digest + '  valid.png\n'
        transient = subprocess.CalledProcessError(1, ['adb', 'shell', 'sha256sum'])
        with patch.object(transfer, 'output', side_effect=[transient, manifest, manifest]) as command, \
             patch.object(transfer.time, 'sleep') as pause, \
             patch.object(transfer, 'pull_image'):
            with tempfile.TemporaryDirectory() as temporary:
                file = Path(temporary) / 'valid.png'
                file.write_bytes(b'fixture')
                with patch.object(transfer, 'sha', return_value=digest):
                    self.assertEqual(transfer.sync_images(Path(temporary)), {'valid.png': digest})
            self.assertEqual(command.call_count, 3)
            pause.assert_called_once_with(1)

    def test_manifest_transport_fails_closed_after_three_errors(self):
        transient = subprocess.CalledProcessError(1, ['adb', 'shell', 'sha256sum'])
        with patch.object(transfer, 'output', side_effect=[transient] * 3) as command, \
             patch.object(transfer.time, 'sleep') as pause, \
             patch.object(transfer, 'pull_image') as pull:
            with self.assertRaises(subprocess.CalledProcessError):
                transfer.device_manifest('sha256sum -- *.png')
            self.assertEqual(command.call_count, 3)
            self.assertEqual(pause.call_count, 2)
            pull.assert_not_called()

    def test_malformed_manifest_is_not_retried(self):
        with patch.object(transfer, 'output', return_value='invalid') as command, \
             patch.object(transfer.time, 'sleep') as pause:
            with self.assertRaisesRegex(RuntimeError, 'Malformed device screenshot digest'):
                transfer.device_manifest('sha256sum -- *.png')
            command.assert_called_once()
            pause.assert_not_called()

    def test_collector_close_cannot_claim_a_live_transfer_stopped(self):
        with tempfile.TemporaryDirectory() as temporary, patch.object(transfer.threading, 'Thread') as thread:
            collector = transfer.IncrementalImages(Path(temporary))
            thread.return_value.is_alive.return_value = True
            with self.assertRaises(RuntimeError):
                collector.close()
            self.assertTrue(collector.stop.is_set())
            thread.return_value.join.assert_called_once_with(timeout=50)
            thread.return_value.is_alive.return_value = False
            collector.close()

    @contextmanager
    def native_device_boundary(self, *, fail_sync=False, invalid_pixels=False):
        """Run the real six-case runner; substitute only SDK/adb/device boundaries."""
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            destination = root / 'native'
            apk = root / 'built/owner-delivery/APD-Developer-439.apk'
            apk.parent.mkdir(parents=True)
            apk.write_bytes(b'unit-test application placeholder, never a deliverable')
            state = {'active': False, 'starts': 0, 'closes': 0, 'syncs': 0, 'commands': []}
            data = fixtures.RecoverySafeguardsTest.png(
                compressed_pixels=zlib.compress(b'') if invalid_pixels else None)

            class Collector:
                def __init__(self, directory):
                    directory.mkdir(parents=True, exist_ok=True)
                    self.thread = SimpleNamespace(start=self.start)

                def start(self):
                    assert not state['active'], 'A second collector overlapped the first'
                    state['active'] = True
                    state['starts'] += 1

                def close(self):
                    assert state['active'], 'Unexpected duplicate close'
                    state['active'] = False
                    state['closes'] += 1

            def run(command, **kwargs):
                state['commands'].append(command)
                self.assertNotEqual(command[:2], ['adb', 'pull'], 'Shared runner must not issue a bulk pull')
                if command[:4] == ['adb', 'shell', 'am', 'instrument']:
                    self.assertTrue(state['active'])
                    class_name, method = command[command.index('class') + 1].split('#')
                    kwargs['stdout'].write(fixtures.RecoverySafeguardsTest().native_text()
                                           .replace('example.NativeTest', class_name)
                                           .replace('expectedMethod', method))

            def sync(directory):
                self.assertFalse(state['active'], 'Foreground sync overlapped a live background collector')
                state['syncs'] += 1
                if fail_sync:
                    raise RuntimeError('Device screenshot transfer failed')
                for name in fixtures.RecoverySafeguardsTest.image_names:
                    (directory / name).write_bytes(data)
                (directory / ('case-' + str(state['syncs']) + '.png')).write_bytes(data)
                return {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in directory.glob('*.png')}

            process = Mock()
            process.wait.return_value = 0
            with patch.object(exact, 'check_pair', return_value=({}, {'instrumentation_sha256': 'a' * 64})), \
                 patch.object(exact, 'IncrementalImages', Collector), patch.object(exact, 'sync_images', side_effect=sync), \
                 patch.object(exact.subprocess, 'run', side_effect=run), patch.object(exact.subprocess, 'Popen', return_value=process), \
                 redirect_stdout(io.StringIO()):
                yield root, destination, state

    def test_shared_runner_serializes_and_records_all_six_complete_transfers(self):
        with self.native_device_boundary() as (root, destination, state):
            exact.native(439, 'Developer', root, destination)
            self.assertEqual((state['starts'], state['closes'], state['syncs']), (6, 6, 6))
            self.assertFalse(state['active'])
            receipt = json.loads((destination / 'native-verification.json').read_text())
            self.assertIs(receipt['complete_remote_image_manifest_verified'], True)
            self.assertEqual(receipt['completed_cases'], [case[0] for case in exact.CASES])
            for index, (label, _, _, _) in enumerate(exact.CASES, 1):
                report = json.loads((destination / (label + '-transfer.json')).read_text())
                self.assertEqual(len(report['remote_images']), len(fixtures.RecoverySafeguardsTest.image_names) + index)
            final = json.loads((destination / 'freeze-transfer.json').read_text())
            self.assertEqual(final['remote_images'], receipt['images'])

    def test_shared_runner_does_not_accept_a_failed_complete_transfer(self):
        with self.native_device_boundary(fail_sync=True) as (root, destination, state):
            with self.assertRaisesRegex(RuntimeError, 'transfer failed'):
                exact.native(439, 'Developer', root, destination)
            self.assertFalse(state['active'])
            self.assertEqual((state['starts'], state['closes'], state['syncs']), (1, 1, 1))
            self.assertFalse((destination / 'native-verification.json').exists())
            self.assertFalse(list(destination.glob('*-transfer.json')))

    def test_shared_runner_retains_stronger_final_pixel_validation(self):
        with self.native_device_boundary(invalid_pixels=True) as (root, destination, state):
            with self.assertRaisesRegex(RuntimeError, 'PNG pixel stream'):
                exact.native(439, 'Developer', root, destination)
            self.assertFalse((destination / 'native-verification.json').exists())
            self.assertEqual(state['syncs'], 6)


if __name__ == '__main__':
    unittest.main()
