#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later

import unittest
from unittest.mock import patch

from apd_deliver_reviewed_pair import finalize_visibility, reviewed_publication_requested, verify_release_source


class ReviewedDeliveryVisibilityTest(unittest.TestCase):
    def test_preserved_source_branch_must_resolve_to_exact_built_commit(self):
        spec = {'number': 432, 'source_commit': 'a' * 40, 'source_branch': 'archive/apd432-original-source'}
        for resolved in ('a' * 40, 'b' * 40):
            with patch('apd_deliver_reviewed_pair.api', return_value={'object': {'type': 'commit', 'sha': resolved}}):
                if resolved == spec['source_commit']:
                    verify_release_source('owner/repo', {'target_commitish': spec['source_branch']}, spec)
                else:
                    with self.assertRaises(AssertionError):
                        verify_release_source('owner/repo', {'target_commitish': spec['source_branch']}, spec)

    def test_unpinned_or_unrelated_source_branch_is_rejected(self):
        for spec, target in (({'number': 432, 'source_commit': 'a' * 40}, 'main'),
                             ({'number': 432, 'source_commit': 'a' * 40, 'source_branch': 'main'}, 'main'),
                             ({'number': 432, 'source_commit': 'a' * 40, 'source_branch': 'archive/apd432-original-source'}, 'archive/apd431-original-source')):
            with self.assertRaises(AssertionError):
                verify_release_source('owner/repo', {'target_commitish': target}, spec)

    def test_rejected_candidate_cannot_request_publication(self):
        for spec in ({}, {'publish': True}):
            with self.assertRaises(AssertionError):
                reviewed_publication_requested(spec, {'accepted': False, 'hold_reason': 'Actual charge wording defect'})

    def test_string_false_and_numeric_flags_are_rejected(self):
        for flag in ('false', 'true', 0, 1, None):
            with self.assertRaises(AssertionError):
                reviewed_publication_requested({'publish': flag}, {'accepted': True})

    def test_rejected_archive_never_calls_a_publication_command(self):
        publish = reviewed_publication_requested({'publish': False}, {'accepted': False, 'hold_reason': 'Actual charge wording defect'})
        current = {'id': 7, 'draft': True, 'published_at': None}
        with patch('apd_deliver_reviewed_pair.subprocess.run') as command, patch('apd_deliver_reviewed_pair.api') as api:
            self.assertIs(current, finalize_visibility('owner/repo', 'candidate', 'a' * 40, current, publish, 'notes.md'))
            command.assert_not_called()
            api.assert_not_called()

    def test_hold_cannot_claim_an_already_published_release_is_private(self):
        with self.assertRaises(AssertionError):
            finalize_visibility('owner/repo', 'candidate', 'a' * 40, {'draft': False}, False, 'notes.md')

    def test_publication_requires_readback_of_the_exact_source_tag(self):
        for resolved in ('a' * 40, 'b' * 40):
            current = {'id': 7, 'draft': True}
            published = {'id': 7, 'draft': False, 'published_at': '2026-11-10T00:00:00Z'}
            with patch('apd_deliver_reviewed_pair.subprocess.run') as command, patch('apd_deliver_reviewed_pair.api', side_effect=[[], published, {'object': {'type': 'commit', 'sha': resolved}}]):
                if resolved == 'a' * 40:
                    self.assertEqual(published, finalize_visibility('owner/repo', 'candidate', 'a' * 40, current, True, 'notes.md'))
                else:
                    with self.assertRaises(AssertionError):
                        finalize_visibility('owner/repo', 'candidate', 'a' * 40, current, True, 'notes.md')
                self.assertIn('--draft=false', command.call_args.args[0])


if __name__ == '__main__':
    unittest.main()
