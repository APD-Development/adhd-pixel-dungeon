#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Offline rejection checks; these do not compile or execute game code."""
import copy
import json
from pathlib import Path
import sys
import tempfile
from unittest.mock import patch


def run(m):
    rejected = []
    def reject(name, call):
        try:
            call()
        except RuntimeError:
            rejected.append(name)
        else:
            raise AssertionError('Accepted invalid evidence: ' + name)
    reviewed = m.plan(ready=False)
    changes = [('A', m.FIXTURE), ('A', m.SCRIPT), ('A', m.GUARDS), ('A', m.WORKFLOW), ('M', 'docs/checkpoint.json')]
    m.validate_changes(changes)
    for path in ['core/src/main/java/Changed.java', 'core/src/main/assets/messages/actors/actors.properties',
                 'android/src/main/AndroidManifest.xml', 'android/build.gradle', 'build.gradle',
                 'keystore/apd-debug.jks', 'tools/apd_recover439_440.py',
                 'android/src/androidTest/java/OtherTest.java', '../outside']:
        reject('forbidden-source:' + path, lambda path=path: m.validate_changes(changes + [('M', path)]))
    for name, bad in [('missing-fixture', changes[1:]), ('modified-fixture', [('M', m.FIXTURE)]),
                       ('deleted-fixture', [('D', m.FIXTURE)]), ('duplicate-fixture', changes + [('A', m.FIXTURE)])]:
        reject(name, lambda bad=bad: m.validate_changes(bad))
    source, run, attempt = 'a' * 40, 1001, 2
    workflow = dict(id=run, head_sha=source, path=m.WORKFLOW, run_attempt=attempt)
    m.validate_run(workflow, source, run, attempt, m.WORKFLOW)
    for key, value in [('id', 1), ('head_sha', 'b' * 40), ('path', m.ORIGINAL_WORKFLOW), ('run_attempt', 1)]:
        bad = dict(workflow, **{key: value})
        reject('workflow:' + key, lambda bad=bad: m.validate_run(bad, source, run, attempt, m.WORKFLOW))
    name = m.artifact_name('instrumentation', attempt)
    artifact = dict(id=2001, name=name, expired=False, digest='sha256:' + 'c' * 64, size_in_bytes=123,
                    workflow_run=dict(id=run, head_sha=source))
    check = lambda value: m.validate_artifact(value, 2001, 'c' * 64, run, source, name)
    check(artifact)
    for key, value in [('id', 1), ('name', m.artifact_name('instrumentation', 1)), ('expired', True),
                       ('digest', 'sha256:' + 'd' * 64), ('size_in_bytes', 0),
                       ('workflow_run', dict(id=1, head_sha=source)), ('workflow_run', dict(id=run, head_sha='b' * 40))]:
        bad = dict(artifact, **{key: value})
        reject('artifact:' + key, lambda bad=bad: check(bad))
    scope = dict(instrumentation_source=source, instrumentation_run=run, instrumentation_attempt=attempt,
                 fixture_path=m.FIXTURE, fixture_sha256=reviewed['fixture_sha256'], class_name=m.CLASS,
                 method=m.METHOD, plan_sha256='e' * 64)
    binary = dict(sha256='f' * 64, bytes=1234, certificate_sha256=m.exact.CERT, package=m.exact.APP + '.test',
                  target_package=m.exact.APP, runner='androidx.test.runner.AndroidJUnitRunner', class_name=m.CLASS, method=m.METHOD)
    record = dict(kind='new_supplemental_instrumentation_on_frozen_APD444', scope=scope, test_apk=binary,
                  original_game_source=m.ORIGINAL_SOURCE, original_game_run=m.ORIGINAL_RUN,
                  original_artifact_id=m.ORIGINAL_ARTIFACT, original_artifact_sha256=m.ORIGINAL_ARCHIVE_SHA,
                  original_application_hashes=m.ORIGINAL_APKS, original_instrumentation_sha256=m.ORIGINAL_INSTRUMENTATION,
                  original_source_archive_sha256=m.ORIGINAL_SOURCE_SHA, original_core_tests=3214, original_core_suites=466,
                  original_core_xml_rechecked_not_rerun=True, new_application_apks_created=0,
                  new_instrumentation_apks_created=1, original_instrumentation_overwritten=False,
                  primary_444_acceptance_claimed=False, new_native_execution_complete=False,
                  manual_visual_review_complete=False, physical_phone_verified=False, published=False)
    m.validate_test_record(record, scope, binary)
    for key, value in [('instrumentation_source', 'b' * 40), ('instrumentation_run', 1), ('instrumentation_attempt', 1),
                       ('fixture_sha256', '0' * 64), ('class_name', m.CLASS + 'Other'), ('method', 'other'), ('plan_sha256', '0' * 64)]:
        bad = copy.deepcopy(record); bad['scope'][key] = value
        reject('test-scope:' + key, lambda bad=bad: m.validate_test_record(bad, scope, binary))
    for key, value in [('original_game_source', 'b' * 40), ('original_game_run', 1), ('original_artifact_id', 1),
                       ('original_artifact_sha256', '0' * 64), ('original_application_hashes', {}),
                       ('original_instrumentation_sha256', '0' * 64), ('original_source_archive_sha256', '0' * 64),
                       ('new_application_apks_created', 2), ('new_instrumentation_apks_created', 0),
                       ('original_instrumentation_overwritten', True), ('primary_444_acceptance_claimed', True),
                       ('original_core_tests', 1), ('original_core_suites', 1), ('published', True)]:
        bad = dict(record, **{key: value})
        reject('test-proof:' + key, lambda bad=bad: m.validate_test_record(bad, scope, binary))
    bad = copy.deepcopy(record); bad['test_apk']['sha256'] = '0' * 64
    reject('new-test-digest', lambda: m.validate_test_record(bad, scope, binary))
    status = f'INSTRUMENTATION_STATUS: class={m.CLASS}\nINSTRUMENTATION_STATUS: test={m.METHOD}\nINSTRUMENTATION_STATUS: numtests=1\n'
    report = status + 'INSTRUMENTATION_STATUS_CODE: 1\n' + status + 'INSTRUMENTATION_STATUS_CODE: 0\nOK (1 test)\nINSTRUMENTATION_CODE: -1\n'
    m.check_case(report); m.check_case(report.replace('\n', '\r\n'))
    for name, bad in [('wrong-class', report.replace(m.CLASS, m.CLASS + 'Other')),
                       ('wrong-method', report.replace(m.METHOD, 'other')), ('wrong-count', report.replace('numtests=1', 'numtests=2')),
                       ('skip', report.replace('STATUS_CODE: 0', 'STATUS_CODE: -3')),
                       ('duplicate-result', report + 'INSTRUMENTATION_STATUS_CODE: 0\n'),
                       ('wrong-terminal', report.replace('INSTRUMENTATION_CODE: -1', 'INSTRUMENTATION_CODE: 0')),
                       ('failure-marker', report + 'FAILURES!!!\n')]:
        reject('native-case:' + name, lambda bad=bad: m.check_case(bad))
    def forbidden(*args, **kwargs):
        raise AssertionError('Operational action ran while the draft was blocked')
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / 'plan.json'
        path.write_text(json.dumps(dict(reviewed, status='draft', pending_review=['root review'])))
        with patch.object(m, 'PLAN', path), patch.object(m.exact, 'api', forbidden), patch.object(m, 'identity', forbidden), \
                patch.object(m.subprocess, 'run', forbidden), patch.object(m.subprocess, 'check_output', forbidden):
            for stage in ['prepare', 'freeze', 'fetch', 'native', 'finalize']:
                with patch.object(sys, 'argv', ['guards', stage]):
                    reject('early-draft:' + stage, m.main)
    print(f'{len(rejected)} supplemental provenance, source-scope, exact-case and early-draft rejection checks passed.')
    return rejected
