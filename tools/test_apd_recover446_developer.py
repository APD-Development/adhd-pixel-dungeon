#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Bounded pure rejection tests. Synthetic dictionaries are not Android evidence."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import apd_recover446_developer as recovery

REJECTIONS = []


class RecoveryGuards(unittest.TestCase):
    def reject(self, label, action):
        with self.subTest(corruption=label), self.assertRaises(RuntimeError):
            action()
        REJECTIONS.append(label)

    @classmethod
    def tearDownClass(cls):
        print('APD446_RECOVERY_GUARD_REJECTIONS=' + str(len(REJECTIONS)))

    def plan_fixture(self):
        path = Path(__file__).resolve().parents[1] / 'docs/APD-446-NATIVE-RECOVERY-PLAN.json'
        value = json.loads(path.read_text())
        value.update(status='ready_for_recovery', pending_review=[])
        return value

    def test_plan_requires_exact_original_and_narrow_scope(self):
        value = self.plan_fixture()
        recovery.validate_plan(value)
        for key, invalid in [('original_source', '0' * 40), ('original_run', 1), ('original_run_attempt', 2),
                ('original_core_tests', 1), ('original_core_suites', 1), ('original_developer_cases', []),
                ('recovery_developer_cases', ['qol']), ('recovery_required_images', []),
                ('application_rebuilt', True), ('instrumentation_rebuilt', True),
                ('source_archive_rebuilt', True), ('original_partial_developer_images_are_diagnostic_only', False),
                ('release_publication_authorized', True)]:
            bad = copy.deepcopy(value)
            bad[key] = invalid
            self.reject('plan-' + key, lambda bad=bad: recovery.validate_plan(bad))

    def test_draft_and_pending_review_block_every_operational_stage(self):
        value = self.plan_fixture()
        for state in ('draft', 'pending'):
            bad = copy.deepcopy(value)
            if state == 'draft':
                bad['status'] = 'draft'
            else:
                bad['pending_review'] = ['not reviewed']
            with tempfile.TemporaryDirectory() as folder:
                path = Path(folder) / 'plan.json'
                path.write_text(json.dumps(bad))
                with patch.object(recovery, 'PLAN', path), patch.object(recovery, 'verify_scope', side_effect=AssertionError('Operational dispatch')):
                    for stage in ('prepare', 'native', 'finalize'):
                        self.reject(state + '-' + stage, lambda stage=stage: recovery.main([stage]))

    def test_zero_application_and_instrumentation_delta_is_required(self):
        good = [('A', recovery.SCRIPT), ('A', recovery.GUARDS), ('A', recovery.WORKFLOW),
                ('A', recovery.REQUEST), ('M', 'AGENTS.md')]
        recovery.validate_changes(good)
        for status, name in [('M', 'core/src/main/java/Changed.java'),
                ('M', recovery.SEED_FIXTURE), ('M', 'tools/apd_candidate446.py'),
                ('M', recovery.SCRIPT), ('D', 'docs/old.json'), ('A', 'unrelated.txt')]:
            bad = [row for row in good if row[1] != name] + [(status, name)]
            self.reject('scope-' + name, lambda bad=bad: recovery.validate_changes(bad))

    def artifact_fixture(self):
        pin = recovery.ARTIFACTS['built']
        value = dict(id=pin['id'], name=pin['name'], digest='sha256:' + pin['sha256'],
                     size_in_bytes=pin['bytes'], expired=False,
                     workflow_run=dict(id=recovery.ORIGINAL_RUN, head_sha=recovery.ORIGINAL_SOURCE,
                                       head_branch=recovery.BRANCH))
        return pin, value

    def test_immutable_artifact_identity_rejects_wrong_bytes_source_and_variant(self):
        pin, value = self.artifact_fixture()
        recovery.validate_artifact(value, pin)
        for key, invalid in [('id', 1), ('name', 'another-artifact'), ('digest', 'sha256:' + '0' * 64),
                             ('size_in_bytes', 1), ('expired', True)]:
            bad = copy.deepcopy(value)
            bad[key] = invalid
            self.reject('artifact-' + key, lambda bad=bad: recovery.validate_artifact(bad, pin))
        for key, invalid in [('id', 1), ('head_sha', '0' * 40), ('head_branch', 'main')]:
            bad = copy.deepcopy(value)
            bad['workflow_run'][key] = invalid
            self.reject('artifact-run-' + key, lambda bad=bad: recovery.validate_artifact(bad, pin))

    def test_cohorts_are_disjoint_complete_and_exactly_match_alpha_names(self):
        original = {f'original-{n}.png': '0' * 64 for n in range(recovery.ORIGINAL_DEV_COUNT)}
        fresh = {name: '1' * 64 for name in recovery.RECOVERY_IMAGES}
        alpha = {name: '2' * 64 for name in set(original) | set(fresh)}
        result = recovery.validate_cohorts(original, fresh, alpha)
        self.assertEqual(len(result), recovery.COMPLETE_COUNT)
        variants = []
        bad = dict(original); bad.pop(next(iter(bad))); variants.append(('missing-original', bad, fresh, alpha))
        bad = dict(fresh); bad.pop(next(iter(bad))); variants.append(('missing-fresh', original, bad, alpha))
        bad = dict(original); bad.pop(next(iter(bad))); bad[next(iter(fresh))] = '1' * 64
        variants.append(('overlapping-cohorts', bad, fresh, alpha))
        bad = dict(fresh); bad['wrong.png'] = bad.pop(next(iter(bad)))
        variants.append(('wrong-fresh-name', original, bad, alpha))
        bad = dict(alpha); bad['unexpected.png'] = bad.pop(next(iter(bad)))
        variants.append(('wrong-complete-inventory', original, fresh, bad))
        bad = dict(fresh); bad[next(iter(bad))] = 'not-a-sha'
        variants.append(('malformed-image-digest', original, bad, alpha))
        for label, old, new, complete in variants:
            self.reject(label, lambda old=old, new=new, complete=complete: recovery.validate_cohorts(old, new, complete))

    def test_transfer_proofs_are_required_for_each_accepted_case(self):
        images = {name: '0' * 64 for name in recovery.RECOVERY_IMAGES}
        transfers = {
            'seed-history': dict(started='2026-11-11T19:00:00+00:00', completed='2026-11-11T19:00:20+00:00',
                                 remote_images={name: images[name] for name in recovery.SEED_IMAGES}),
            'pylon': dict(started='2026-11-11T19:00:21+00:00', completed='2026-11-11T19:00:40+00:00',
                          remote_images=dict(images)),
        }
        recovery.validate_transfer_maps(recovery.RECOVERY_LABELS, transfers, images)
        for label in ('missing-case', 'extra-case', 'changed-first-hash', 'lost-earlier-image', 'incomplete-final', 'backward-time'):
            bad = copy.deepcopy(transfers)
            if label == 'missing-case': bad.pop('seed-history')
            elif label == 'extra-case': bad['qol'] = copy.deepcopy(bad['pylon'])
            elif label == 'changed-first-hash': bad['seed-history']['remote_images'][next(iter(recovery.SEED_IMAGES))] = 'f' * 64
            elif label == 'lost-earlier-image': bad['pylon']['remote_images'].pop(next(iter(recovery.SEED_IMAGES)))
            elif label == 'incomplete-final': bad['pylon']['remote_images'].pop(next(iter(recovery.PYLON_IMAGES)))
            else: bad['pylon']['completed'] = '2026-11-11T18:00:00+00:00'
            self.reject('transfer-' + label, lambda bad=bad: recovery.validate_transfer_maps(recovery.RECOVERY_LABELS, bad, images))

    def test_raw_native_parser_rejects_incomplete_wrong_or_failed_case(self):
        name = recovery.exact.CLASS_PREFIX + 'SeedHistoryDeviceTest'
        method = 'seedEntryShowsOnlyMatchingRecordedHistory'
        good = ('INSTRUMENTATION_STATUS: class=' + name + '\nINSTRUMENTATION_STATUS: test=' + method
                + '\nINSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_STATUS_CODE: 0\nOK (1 test)\nINSTRUMENTATION_CODE: -1\n')
        recovery.exact.check_instrumentation(good, name, method)
        for label, text in [('failed', good.replace('STATUS_CODE: 0', 'STATUS_CODE: -2')),
                            ('incomplete', good.replace('OK (1 test)', '')),
                            ('wrong-method', good.replace(method, 'anotherTest'))]:
            self.reject('raw-' + label, lambda text=text: recovery.exact.check_instrumentation(text, name, method))

    def test_attempt_bound_names_cannot_reuse_previous_attempt(self):
        first = recovery.artifact_name('native', 100, 1)
        self.assertNotEqual(first, recovery.artifact_name('native', 100, 2))
        self.assertNotEqual(first, recovery.artifact_name('native', 101, 1))
        self.assertNotEqual(first, recovery.artifact_name('owner', 100, 1))
        for kind, run, attempt in [('unknown', 100, 1), ('native', 0, 1), ('native', 100, 0)]:
            self.reject('name-' + str((kind, run, attempt)), lambda kind=kind, run=run, attempt=attempt:
                        recovery.artifact_name(kind, run, attempt))


if __name__ == '__main__':
    unittest.main()
