# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import apd_recover440_alpha as retry
import test_apd_recover439_440 as fixtures


class AlphaRecoverySafeguardsTest(unittest.TestCase):
    def metadata(self):
        identity, name, digest = retry.ARTIFACTS['prepared']
        return dict(id=identity, name=name, digest='sha256:' + digest, expired=False,
                    workflow_run=dict(id=retry.RECOVERY_RUN, head_sha=retry.RECOVERY_SOURCE,
                                      head_branch=retry.BRANCH))

    def test_original_artifact_is_bound_to_exact_run_source_digest_and_name(self):
        original = self.metadata()
        retry.check_artifact(original, 'prepared')
        for key, value in [('id', 1), ('name', 'other'), ('digest', 'sha256:' + '0' * 64), ('expired', True)]:
            changed = copy.deepcopy(original)
            changed[key] = value
            with self.subTest(key=key), self.assertRaises(RuntimeError):
                retry.check_artifact(changed, 'prepared')
        for key, value in [('id', 1), ('head_sha', '0' * 40), ('head_branch', 'main')]:
            changed = copy.deepcopy(original)
            changed['workflow_run'][key] = value
            with self.subTest(key=key), self.assertRaises(RuntimeError):
                retry.check_artifact(changed, 'prepared')

    def test_manifest_keeps_previously_omitted_nested_class_and_duplicate_names(self):
        names = ['qol-existing-status-clear-concise-ChampionEnemy$Growing.png', 'qol-doorless-floor1 (1).png']
        text = ''.join('a' * 64 + '  ' + name + '\n' for name in names)
        self.assertEqual(retry.parse_manifest(text), {name: 'a' * 64 for name in names})
        for bad in ['', 'bad digest\n', 'a' * 64 + '  ../outside.png\n', text + text,
                    'a' * 64 + '  bad;command.png\n', 'a' * 64 + '  sub/inside.png\n']:
            with self.subTest(text=bad[:80]), self.assertRaises(RuntimeError):
                retry.parse_manifest(bad)

    def test_complete_suite_inventory_rejects_any_missing_or_extra_capture(self):
        complete = {f'qol-capture-{index}.png': 'a' * 64 for index in range(1408)}
        retry.complete_inventory(complete, complete)
        missing = dict(complete)
        missing.pop('qol-capture-1407.png')
        extra = dict(complete, **{'unexpected.png': 'a' * 64})
        for candidate in [missing, extra]:
            with self.assertRaises(RuntimeError):
                retry.complete_inventory(complete, candidate)
        with self.assertRaises(RuntimeError):
            retry.complete_inventory(missing, missing)

    def test_individual_transfer_validates_png_and_remote_hash_before_replace(self):
        data = fixtures.RecoverySafeguardsTest.png()
        digest = hashlib.sha256(data).hexdigest()
        name = 'qol-doorless-floor1 (1).png'
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            target = directory / name
            target.write_bytes(b'stale')
            def transfer(command, **kwargs):
                self.assertEqual(command[2], retry.REMOTE + '/' + name)
                Path(command[3]).write_bytes(data)
            with patch.object(retry.subprocess, 'run', side_effect=transfer):
                with self.assertRaises(RuntimeError):
                    retry.pull_image(directory, name, '0' * 64)
                self.assertEqual(target.read_bytes(), b'stale')
                retry.pull_image(directory, name, digest)
            self.assertEqual(target.read_bytes(), data)
            self.assertFalse(list(directory.glob('*.partial')))
            with patch.object(retry.subprocess, 'run') as no_transfer:
                retry.pull_image(directory, name, digest)
                no_transfer.assert_not_called()
            target.unlink()
            def corrupt(command, **kwargs):
                Path(command[3]).write_bytes(b'not a PNG')
            with patch.object(retry.subprocess, 'run', side_effect=corrupt), self.assertRaises(ValueError):
                retry.pull_image(directory, name)
            self.assertFalse(target.exists())

    def test_final_sync_rejects_remote_mutation_and_missing_local_capture(self):
        data = fixtures.RecoverySafeguardsTest.png()
        digest = hashlib.sha256(data).hexdigest()
        first = digest + '  a.png\n'
        changed = digest + '  b.png\n'
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            with patch.object(retry.exact, 'output', side_effect=[first, changed]), \
                 patch.object(retry, 'pull_image'), self.assertRaises(RuntimeError):
                retry.sync_images(directory)
            with patch.object(retry.exact, 'output', side_effect=[first, first]), \
                 patch.object(retry, 'pull_image'), self.assertRaises(RuntimeError):
                retry.sync_images(directory)
            (directory / 'a.png').write_bytes(data)
            with patch.object(retry.exact, 'output', side_effect=[first, first]), \
                 patch.object(retry, 'pull_image'):
                self.assertEqual(retry.sync_images(directory), {'a.png': digest})


if __name__ == '__main__':
    unittest.main()
