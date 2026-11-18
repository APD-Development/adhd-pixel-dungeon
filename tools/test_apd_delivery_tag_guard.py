#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Publication must reject a conflicting source tag before making a release visible."""
import subprocess
import unittest
from unittest.mock import patch

from apd_deliver_reviewed_pair import finalize_visibility


class DeliveryTagGuardTest(unittest.TestCase):
    repo = 'owner/repo'
    tag = 'apd-alpha-preview-433'
    source = 'a' * 40

    def run_case(self, refs, post_sha=None, lookup_error=None):
        published = {'id': 7, 'draft': False, 'published_at': '2026-11-10T20:00:00Z'}
        def response(path):
            if '/git/matching-refs/tags/' in path:
                if lookup_error:
                    raise lookup_error
                return refs
            if '/releases/' in path:
                return published
            if '/git/ref/tags/' in path:
                return {'object': {'type': 'commit', 'sha': post_sha or self.source}}
            raise AssertionError('Unexpected API path: ' + path)
        return patch('apd_deliver_reviewed_pair.api', side_effect=response)

    def ref(self, sha=None, kind='commit', suffix=''):
        return {'ref': 'refs/tags/' + self.tag + suffix,
                'object': {'type': kind, 'sha': sha or self.source}}

    def test_wrong_existing_tag_never_runs_publication(self):
        with self.run_case([self.ref('b' * 40)], post_sha='b' * 40), patch('apd_deliver_reviewed_pair.subprocess.run') as command:
            with self.assertRaises(AssertionError):
                finalize_visibility(self.repo, self.tag, self.source, {'id': 7, 'draft': True}, True, 'notes.md')
            command.assert_not_called()

    def test_annotated_or_duplicate_exact_tag_is_rejected_before_publication(self):
        for refs in ([self.ref(kind='tag')], [self.ref(), self.ref()]):
            with self.subTest(refs=refs), self.run_case(refs), patch('apd_deliver_reviewed_pair.subprocess.run') as command:
                with self.assertRaises(AssertionError):
                    finalize_visibility(self.repo, self.tag, self.source, {'id': 7, 'draft': True}, True, 'notes.md')
                command.assert_not_called()

    def test_absent_or_matching_tag_allows_normal_publication_and_readback(self):
        for refs in ([], [self.ref()], [self.ref('b' * 40, suffix='-held')]):
            with self.subTest(refs=refs), self.run_case(refs), patch('apd_deliver_reviewed_pair.subprocess.run') as command:
                result = finalize_visibility(self.repo, self.tag, self.source, {'id': 7, 'draft': True}, True, 'notes.md')
                self.assertFalse(result['draft'])
                command.assert_called_once()
                self.assertIn('--draft=false', command.call_args.args[0])

    def test_lookup_failure_is_not_treated_as_tag_absence(self):
        with self.run_case([], lookup_error=subprocess.CalledProcessError(1, ['gh', 'api'])), patch('apd_deliver_reviewed_pair.subprocess.run') as command:
            with self.assertRaises(subprocess.CalledProcessError):
                finalize_visibility(self.repo, self.tag, self.source, {'id': 7, 'draft': True}, True, 'notes.md')
            command.assert_not_called()

    def test_held_archive_neither_looks_up_tags_nor_publishes(self):
        current = {'id': 7, 'draft': True}
        with patch('apd_deliver_reviewed_pair.api') as api, patch('apd_deliver_reviewed_pair.subprocess.run') as command:
            self.assertIs(current, finalize_visibility(self.repo, self.tag, self.source, current, False, 'notes.md'))
            api.assert_not_called()
            command.assert_not_called()

    def test_existing_publication_is_read_back_without_editing(self):
        with self.run_case([]), patch('apd_deliver_reviewed_pair.subprocess.run') as command:
            result = finalize_visibility(self.repo, self.tag, self.source, {'id': 7, 'draft': False}, True, 'notes.md')
            self.assertFalse(result['draft'])
            command.assert_not_called()


if __name__ == '__main__':
    unittest.main()
