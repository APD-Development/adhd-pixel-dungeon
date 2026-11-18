#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""A successful draft creation must not rely on immediate list visibility."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import apd_deliver_reviewed_pair as delivery

class DraftResolutionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.notes = Path(self.tmp.name) / 'notes.md'
        self.notes.write_text('Exact reviewed pair; no rebuild.\n', encoding='utf-8')
        self.repo = 'owner/repo'
        self.tag = 'apd-alpha-preview-433'
        self.spec = {'number': 433, 'source_commit': 'a'*40, 'title': 'APD433'}
        self.draft = {'id': 42, 'tag_name': self.tag, 'target_commitish': 'a'*40,
                      'draft': True, 'assets': []}

    def test_created_draft_uses_response_id_when_inventory_is_stale(self):
        with patch.object(delivery, 'releases_for_tag', return_value=[]) as inventory, \
             patch.object(delivery.subprocess, 'run'), \
             patch.object(delivery.subprocess, 'check_output', return_value=json.dumps(self.draft).encode()) as create, \
             patch.object(delivery, 'api', return_value=self.draft) as read:
            self.assertEqual(self.draft, delivery.select_release(self.repo, self.tag, self.spec, self.notes))
            inventory.assert_called_once()
            create.assert_called_once()
            args = create.call_args.args[0]
            self.assertIn('POST', args)
            body = json.loads(create.call_args.kwargs['input'])
            self.assertIs(body['draft'], True)
            self.assertIs(body['prerelease'], True)
            self.assertEqual('false', body['make_latest'])
            self.assertEqual('a'*40, body['target_commitish'])
            read.assert_called_once_with('repos/owner/repo/releases/42')

    def test_pinned_recovery_reads_exact_id_without_creation_or_inventory(self):
        self.spec['release_id'] = 42
        with patch.object(delivery, 'api', return_value=self.draft) as read, \
             patch.object(delivery, 'releases_for_tag') as inventory, \
             patch.object(delivery.subprocess, 'check_output') as create:
            self.assertEqual(self.draft, delivery.select_release(self.repo, self.tag, self.spec, self.notes))
            read.assert_called_once_with('repos/owner/repo/releases/42')
            inventory.assert_not_called(); create.assert_not_called()

    def test_missing_pinned_release_never_creates_a_replacement(self):
        self.spec['release_id'] = 42
        with patch.object(delivery, 'api', side_effect=subprocess.CalledProcessError(1, ['gh'])) , \
             patch.object(delivery.subprocess, 'check_output') as create:
            with self.assertRaises(subprocess.CalledProcessError):
                delivery.select_release(self.repo, self.tag, self.spec, self.notes)
            create.assert_not_called()

    def test_invalid_pinned_ids_are_rejected(self):
        for value in (True, False, 0, -1, '42', None):
            with self.subTest(value=value), patch.object(delivery, 'api') as read:
                with self.assertRaises(AssertionError):
                    delivery.select_release(self.repo, self.tag, dict(self.spec, release_id=value), self.notes)
                read.assert_not_called()

    def test_wrong_recovery_tag_or_id_is_rejected(self):
        for change in ({'tag_name': 'another-build'}, {'id': 43}):
            with self.subTest(change=change), patch.object(delivery, 'api', return_value=dict(self.draft, **change)):
                with self.assertRaises(AssertionError):
                    delivery.select_release(self.repo, self.tag, dict(self.spec, release_id=42), self.notes)

    def test_creation_must_be_a_matching_unpublished_draft(self):
        for change in ({'tag_name': 'wrong'}, {'draft': False}, {'target_commitish': 'b'*40}, {'id': True}):
            with self.subTest(change=change), patch.object(delivery, 'releases_for_tag', return_value=[]), \
                 patch.object(delivery.subprocess, 'check_output', return_value=json.dumps(dict(self.draft, **change)).encode()), \
                 patch.object(delivery, 'api') as read:
                with self.assertRaises(AssertionError):
                    delivery.select_release(self.repo, self.tag, self.spec, self.notes)
                read.assert_not_called()

    def test_creation_readback_cannot_switch_release_or_tag(self):
        for change in ({'id': 43}, {'tag_name': 'wrong'}, {'draft': False}, {'target_commitish': 'b'*40}):
            with self.subTest(change=change), patch.object(delivery, 'releases_for_tag', return_value=[]), \
                 patch.object(delivery.subprocess, 'check_output', return_value=json.dumps(self.draft).encode()), \
                 patch.object(delivery, 'api', return_value=dict(self.draft, **change)):
                with self.assertRaises(AssertionError):
                    delivery.select_release(self.repo, self.tag, self.spec, self.notes)

    def test_existing_release_is_reused_and_errors_are_not_absence(self):
        with patch.object(delivery, 'releases_for_tag', return_value=[self.draft]), \
             patch.object(delivery.subprocess, 'check_output') as create:
            self.assertEqual(self.draft, delivery.select_release(self.repo, self.tag, self.spec, self.notes))
            create.assert_not_called()
        with patch.object(delivery, 'releases_for_tag', side_effect=RuntimeError('incomplete')), \
             patch.object(delivery.subprocess, 'check_output') as create:
            with self.assertRaises(RuntimeError):
                delivery.select_release(self.repo, self.tag, self.spec, self.notes)
            create.assert_not_called()

if __name__ == '__main__':
    unittest.main()
