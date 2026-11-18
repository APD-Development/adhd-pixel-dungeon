#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Supplemental native targeting checks on frozen APD444; never assemble a game APK."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import traceback
import zipfile

import apd_candidate444 as candidate
import apd_recover439_440 as exact
import apd_scan_prepared_sources as scans
from apd_native_screenshot_transfer import IncrementalImages, REMOTE, sync_images

BRANCH = 'codex/apd-phone-fixes-2026-10-09'
WORKFLOW = '.github/workflows/apd-pylon-native-444.yml'
SCRIPT = 'tools/apd444_pylon_native.py'
GUARDS = 'tools/apd444_pylon_native_guards.py'
PLAN = Path('docs/APD-444-PYLON-NATIVE-PLAN.json')
REQUEST = 'docs/APD-444-PYLON-NATIVE-REQUEST.txt'
FIXTURE = 'android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/BossPylonTargetingDeviceTest.java'
CLASS = exact.CLASS_PREFIX + 'BossPylonTargetingDeviceTest'
METHOD = 'nativeIndicatorsPreserveManualTargetsAndExcludeHiddenOrInactivePylons'
IMAGES = frozenset({
    'pylon-native-inactive-visible.png', 'pylon-native-active-visible.png',
    'pylon-native-manual-rat-preserved.png', 'pylon-native-ranged-pylon-melee-boss.png',
    'pylon-native-hidden-excluded.png',
})
ORIGINAL_SOURCE = '2b8787a2fb41fc6ecacbb77617bd5f1763d902b0'
ORIGINAL_RUN = 43057716359
ORIGINAL_WORKFLOW = '.github/workflows/apd-candidate-444.yml'
ORIGINAL_ARTIFACT = 13873030855
ORIGINAL_ARCHIVE_SHA = '9bf2e5ecd2fc705f1d481f863c0321c7e290f062dac06e4f7ef8757abb55fb45'
ORIGINAL_ARCHIVE_BYTES = 184694131
ORIGINAL_SOURCE_SHA = '87196a36c4425b1c65a76c3071d1ea84fd72d2eafdaab1eb63661e2f71ab1076'
ORIGINAL_INSTRUMENTATION = 'c30a9832f02aa9f5923e7646bf4bb027a47af21ab6354250ecf421bdf8214b31'
ORIGINAL_APKS = {
    'Developer': '6df0117a0614b6a8215cfc24dc6a003090bdd96d233eef4e3f3480cc19bf9ce0',
    'Alpha-Testers': 'a3f0dedf8be706d2e21ff4e391566ed1e3b6e90eb15726c9bb60075a28481162',
}
HELPER_HASHES = {'tools/apd_candidate444.py': 'c0fa2e4d92ccef7c69612c764fa26f9acc889989b727250bbec1f63af01acf9c', 'tools/apd_candidate441.py': 'a59674f3f55d5d79e6a19264de94e8c26c644c81b3469383fc07ab01d6719997', 'tools/apd_recover439_440.py': '15ed9518882591e81881a015db2d3bf64cef6e343229dca7bc78428ca51e3bc5', 'tools/apd_scan_prepared_sources.py': 'fc0f14dcbbfed96e8e17b4636ad19ce3df2050d512166b69bcf828fba66268fd', 'tools/apd_existing_build_evidence.py': '07c23a6093b0bd3eb203c0d2558df3c3d5c2e43524056bb089c444dcf504f15d', 'tools/apd_native_screenshot_transfer.py': 'bdc1312ad65ecc69e90a8614d38d4ec469bf8b92e03d4ac45c8e59a2fb8dc713', 'tools/validate_android_visuals.py': '86fab85f843464f68034334dbfc73fb04c8076058565c2535190866708fbe049', 'tools/check_android_json_abi.py': '53299017cefff8a37dbb04769c9feda7cc7625002101a46cb07102ac549a3896'}
ORIGINAL = Path('pylon444-original')
TEST = Path('pylon444-instrumentation')
NATIVE = Path('pylon444-native')
FINAL = Path('pylon444-supplemental')


def read(path):
    return json.loads(Path(path).read_text())


def write(path, value):
    exact.write_json(path, value)


def plan(ready=True):
    value = read(PLAN)
    keys = {'schema', 'purpose', 'status', 'pending_review', 'fixture_path', 'fixture_sha256',
            'class', 'method', 'required_images', 'original_source', 'original_run',
            'original_artifact', 'original_artifact_sha256', 'original_core_tests', 'original_core_suites'}
    exact.require(set(value) == keys and value['schema'] == 1
                  and value['purpose'] == 'supplemental_native_targeting_on_frozen_APD444', 'Wrong supplemental plan')
    exact.require(value['fixture_path'] == FIXTURE and value['class'] == CLASS and value['method'] == METHOD
                  and len(value['required_images']) == 5 and set(value['required_images']) == IMAGES,
                  'Reviewed fixture identity or image set differs')
    exact.require(re.fullmatch('[0-9a-f]{64}', value['fixture_sha256']) is not None,
                  'Reviewed fixture digest required')
    exact.require((value['original_source'], value['original_run'], value['original_artifact'],
                   value['original_artifact_sha256'], value['original_core_tests'], value['original_core_suites'])
                  == (ORIGINAL_SOURCE, ORIGINAL_RUN, ORIGINAL_ARTIFACT, ORIGINAL_ARCHIVE_SHA, 3214, 466),
                  'Frozen application identity or original core counts differ')
    if ready:
        exact.require(value['status'] == 'ready_for_supplemental_native' and not value['pending_review'],
                      'Supplemental native plan is a draft; fixture and root review are still required')
    return value


def identity():
    exact.require(os.environ['GITHUB_REPOSITORY'] == exact.REPO
                  and os.environ['GITHUB_REF'] == 'refs/heads/' + BRANCH, 'Wrong repository or branch')
    source, run, attempt = os.environ['GITHUB_SHA'], int(os.environ['GITHUB_RUN_ID']), int(os.environ['GITHUB_RUN_ATTEMPT'])
    exact.require(re.fullmatch('[0-9a-f]{40}', source) and run > 0 and attempt > 0,
                  'Full supplemental source, run and attempt required')
    return source, run, attempt


def validate_changes(changes):
    metadata = {'AGENTS.md', SCRIPT, GUARDS, WORKFLOW}
    exact.require(changes and any(path == FIXTURE for _, path in changes), 'The standalone reviewed fixture must be added')
    exact.require(sum(path == FIXTURE for _, path in changes) == 1, 'Duplicate fixture change')
    for status, path in changes:
        exact.safe_name(path)
        exact.require(status in ('A', 'M'), 'Deletion, rename or copy is outside this verification scope')
        if path == FIXTURE:
            exact.require(status == 'A', 'The new standalone fixture must be an addition')
        else:
            exact.require(path.startswith('docs/') or path in metadata,
                          'Application or unrelated test change is forbidden: ' + path)


def verify_scope():
    reviewed = plan()
    source, run, attempt = identity()
    subprocess.run(['git', 'merge-base', '--is-ancestor', ORIGINAL_SOURCE, source], check=True)
    subprocess.run(['git', 'diff', '--exit-code'], check=True)
    subprocess.run(['git', 'diff', '--cached', '--exit-code'], check=True)
    rows = exact.output(['git', 'diff', '--name-status', '--no-renames', ORIGINAL_SOURCE, source]).splitlines()
    changes = [row.split('\t') for row in rows]
    exact.require(all(len(row) == 2 for row in changes), 'Unexpected changed-path encoding')
    validate_changes(changes)
    for name, digest in HELPER_HASHES.items():
        exact.require(exact.sha(Path(name)) == digest, 'Existing verification helper changed: ' + name)
    fixture = subprocess.check_output(['git', 'show', source + ':' + FIXTURE])
    exact.require(fixture == Path(FIXTURE).read_bytes()
                  and hashlib.sha256(fixture).hexdigest() == reviewed['fixture_sha256'], 'Fixture source digest differs')
    text = fixture.decode()
    exact.require(text.count('@Test') == 1 and ('void ' + METHOD + '(') in text
                  and 'class BossPylonTargetingDeviceTest' in text, 'Standalone exact test identity differs')
    return dict(instrumentation_source=source, instrumentation_run=run, instrumentation_attempt=attempt,
                original_game_source=ORIGINAL_SOURCE, zero_application_delta=True, changed_paths=changes,
                fixture_path=FIXTURE, fixture_sha256=reviewed['fixture_sha256'], class_name=CLASS, method=METHOD,
                plan_sha256=exact.sha(PLAN), helper_sha256=dict(HELPER_HASHES))


def validate_run(record, source, run, attempt, workflow):
    exact.require(record['id'] == run and record['head_sha'] == source and record['path'] == workflow
                  and record['run_attempt'] == attempt, 'Wrong workflow source, run or current attempt')


def validate_artifact(item, artifact_id, digest, run, source, name):
    candidate.validate_artifact(item, 444, artifact_id, digest, run, source, name)
    exact.require(type(item['size_in_bytes']) is int and item['size_in_bytes'] > 0, 'Empty artifact')


def artifact_name(kind, attempt, variant=None):
    base = 'apd444-pylon-' + kind
    if variant is not None:
        exact.require(variant in ORIGINAL_APKS, 'Unknown original variant')
        base += '-' + variant
    return base + '-attempt-' + str(attempt)


def current_artifact(kind, job_name, variant=None):
    source, run, attempt = identity()
    record = exact.api(f'actions/runs/{run}')
    validate_run(record, source, run, attempt, WORKFLOW)
    jobs = exact.api(f'actions/runs/{run}/attempts/{attempt}/jobs?per_page=100')['jobs']
    matches = [job for job in jobs if job['name'] == job_name]
    exact.require(len(matches) == 1 and matches[0]['status'] == 'completed'
                  and matches[0]['conclusion'] == 'success', 'Exact producing job did not succeed')
    name = artifact_name(kind, attempt, variant)
    artifacts = exact.api(f'actions/runs/{run}/artifacts?name={name}&per_page=100')['artifacts']
    matches_a = [item for item in artifacts if item['name'] == name]
    exact.require(len(matches_a) == 1, 'One exact current-attempt artifact required')
    item = matches_a[0]
    exact.require(re.fullmatch('sha256:[0-9a-f]{64}', item['digest']), 'Artifact SHA-256 missing')
    validate_artifact(item, item['id'], item['digest'][7:], run, source, name)
    parsed = lambda value: datetime.fromisoformat(value.replace('Z', '+00:00'))
    exact.require(parsed(matches[0]['started_at']) <= parsed(item['created_at']) <= parsed(matches[0]['completed_at']),
                  'Artifact was not created during the current producer job')
    return item


def download(item, archive, destination):
    archive.parent.mkdir(parents=True, exist_ok=True)
    with archive.open('xb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{item["id"]}/zip'],
                       stdout=stream, check=True)
    scans.verify_file(archive, item['digest'][7:], item['size_in_bytes'])
    exact.extract_archive(archive, destination)


def register_original():
    exact.SPECS[444] = dict(source=ORIGINAL_SOURCE, run=ORIGINAL_RUN, artifact=ORIGINAL_ARTIFACT,
                           digest=ORIGINAL_ARCHIVE_SHA, workflow=ORIGINAL_WORKFLOW,
                           tests=3214, suites=466, hashes=ORIGINAL_APKS)


def check_original():
    register_original()
    exact.require(exact.sha(ORIGINAL / 'built-pair.zip') == ORIGINAL_ARCHIVE_SHA, 'Original archive changed')
    _, proof = exact.check_pair(ORIGINAL / 'built', 444)
    exact.require(proof['instrumentation_sha256'] == ORIGINAL_INSTRUMENTATION,
                  'The original instrumentation must remain unchanged')
    exact.require(exact.sha(ORIGINAL / 'source/APD-Source-444.zip') == ORIGINAL_SOURCE_SHA,
                  'Original corresponding source changed')
    prepared = read(ORIGINAL / 'preparation.json')
    exact.require(prepared['source_commit'] == ORIGINAL_SOURCE
                  and prepared['source_archive_sha256'] == ORIGINAL_SOURCE_SHA
                  and prepared['instrumentation_sha256'] == ORIGINAL_INSTRUMENTATION,
                  'Original preparation binding differs')
    return proof


def fetch_original():
    record = exact.api(f'actions/runs/{ORIGINAL_RUN}')
    exact.require(record['id'] == ORIGINAL_RUN and record['head_sha'] == ORIGINAL_SOURCE
                  and record['path'] == ORIGINAL_WORKFLOW, 'Original producer identity differs')
    jobs = exact.api(f'actions/runs/{ORIGINAL_RUN}/jobs?per_page=100')['jobs']
    built = [job for job in jobs if job['name'] == 'build']
    exact.require(len(built) == 1 and built[0]['conclusion'] == 'success', 'Original game build did not pass')
    item = exact.api(f'actions/artifacts/{ORIGINAL_ARTIFACT}')
    validate_artifact(item, ORIGINAL_ARTIFACT, ORIGINAL_ARCHIVE_SHA, ORIGINAL_RUN, ORIGINAL_SOURCE, 'apd444-built-pair')
    exact.require(item['size_in_bytes'] == ORIGINAL_ARCHIVE_BYTES, 'Original archive size differs')
    download(item, ORIGINAL / 'built-pair.zip', ORIGINAL)
    proof = check_original()
    write(ORIGINAL / 'checked-original.json', proof)
    write(ORIGINAL / 'artifact-metadata.json', item)
    return proof


def no_game_apk_outputs():
    outputs = Path('android/build/outputs/apk')
    unexpected = [str(path) for path in outputs.rglob('*.apk') if 'androidTest' not in path.relative_to(outputs).parts]
    exact.require(not unexpected, 'A game APK output exists; instrumentation-only scope violated')


def check_new_test_apk(path):
    signer, aapt = exact.sdk_tools()
    exact.check_signature(path, signer)
    meta = exact.output([str(aapt), 'dump', 'badging', str(path)])
    manifest = exact.output([str(aapt), 'dump', 'xmltree', str(path), 'AndroidManifest.xml'])
    exact.require(f"name='{exact.APP}.test'" in meta
                  and re.search(r'android:targetPackage(?:\([^)]*\))?\s*=\s*"' + re.escape(exact.APP) + '"', manifest)
                  and re.search(r'android:name(?:\([^)]*\))?\s*=\s*"androidx\.test\.runner\.AndroidJUnitRunner"', manifest),
                  'New instrumentation package, target or runner differs')
    digest = exact.sha(path)
    exact.require(digest != ORIGINAL_INSTRUMENTATION, 'New test APK must not replace or masquerade as original instrumentation')
    with zipfile.ZipFile(path) as packed:
        dex = [packed.read(name) for name in packed.namelist() if re.fullmatch(r'classes\d*\.dex', name)]
        descriptor = ('L' + CLASS.replace('.', '/') + ';').encode()
        exact.require(any(descriptor in data and METHOD.encode() in data for data in dex), 'New DEX lacks the exact reviewed test')
    return dict(sha256=digest, bytes=path.stat().st_size, certificate_sha256=exact.CERT,
                package=exact.APP + '.test', target_package=exact.APP,
                runner='androidx.test.runner.AndroidJUnitRunner', class_name=CLASS, method=METHOD)


def validate_test_record(record, scope, binary):
    exact.require(record['kind'] == 'new_supplemental_instrumentation_on_frozen_APD444'
                  and record['scope'] == scope and record['test_apk'] == binary,
                  'New test binary, source, fixture, run, attempt or plan differs')
    exact.require(record['original_game_source'] == ORIGINAL_SOURCE and record['original_game_run'] == ORIGINAL_RUN
                  and record['original_artifact_id'] == ORIGINAL_ARTIFACT
                  and record['original_artifact_sha256'] == ORIGINAL_ARCHIVE_SHA
                  and record['original_application_hashes'] == ORIGINAL_APKS
                  and record['original_instrumentation_sha256'] == ORIGINAL_INSTRUMENTATION
                  and record['original_source_archive_sha256'] == ORIGINAL_SOURCE_SHA,
                  'New test proof misidentifies the unchanged original game')
    exact.require(record['new_application_apks_created'] == 0 and record['new_instrumentation_apks_created'] == 1
                  and record['original_instrumentation_overwritten'] is False
                  and record['primary_444_acceptance_claimed'] is False
                  and record['original_core_tests'] == 3214 and record['original_core_suites'] == 466
                  and record['original_core_xml_rechecked_not_rerun'] is True
                  and record['new_native_execution_complete'] is False
                  and record['manual_visual_review_complete'] is False and record['physical_phone_verified'] is False
                  and record['published'] is False, 'Wrong supplemental-only boundary')


def prepare():
    scope = verify_scope()
    source, run, attempt = identity()
    validate_run(exact.api(f'actions/runs/{run}'), source, run, attempt, WORKFLOW)
    name = artifact_name('instrumentation', attempt)
    rows = exact.api(f'actions/runs/{run}/artifacts?name={name}&per_page=100')['artifacts']
    exact.require(not any(item['name'] == name for item in rows), 'Current-attempt test artifact already exists; retain it')
    no_game_apk_outputs()
    proof = fetch_original()
    write(TEST / 'scope-verification.json', scope)
    write(TEST / 'original-verification.json', proof)
    shutil.copyfile(ORIGINAL / 'artifact-metadata.json', TEST / 'original-artifact.json')


def freeze():
    scope = verify_scope()
    exact.require(read(TEST / 'scope-verification.json') == scope, 'Scope changed during instrumentation compilation')
    no_game_apk_outputs()
    check_original()
    source = Path('android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk')
    target = TEST / 'pylon-instrumentation.apk'
    exact.require(source.is_file() and not target.exists(), 'Exactly one new separately stored test APK required')
    shutil.copyfile(source, target)
    binary = check_new_test_apk(target)
    abi_log = TEST / 'android-json-abi.log'
    exact.require(abi_log.is_file()
                  and re.search(r'^Checked production classes: [1-9]\d* distinct JSON references: [1-9]\d*$', abi_log.read_text(), re.M)
                  and 'UNSUPPORTED_ANDROID_JSON_FLOAT_ABI' not in abi_log.read_text(),
                  'The unchanged compiled Android JSON ABI gate did not produce successful evidence')
    record = dict(kind='new_supplemental_instrumentation_on_frozen_APD444', scope=scope, test_apk=binary,
                  original_game_source=ORIGINAL_SOURCE, original_game_run=ORIGINAL_RUN,
                  original_artifact_id=ORIGINAL_ARTIFACT, original_artifact_sha256=ORIGINAL_ARCHIVE_SHA,
                  original_application_hashes=ORIGINAL_APKS, original_instrumentation_sha256=ORIGINAL_INSTRUMENTATION,
                  original_source_archive_sha256=ORIGINAL_SOURCE_SHA, original_core_tests=3214, original_core_suites=466,
                  original_core_xml_rechecked_not_rerun=True, json_abi_log_sha256=exact.sha(abi_log),
                  new_application_apks_created=0,
                  new_instrumentation_apks_created=1, original_instrumentation_overwritten=False,
                  primary_444_acceptance_claimed=False, new_native_execution_complete=False,
                  manual_visual_review_complete=False, physical_phone_verified=False, published=False)
    validate_test_record(record, scope, binary)
    write(TEST / 'instrumentation-verification.json', record)
    shutil.copyfile(FIXTURE, TEST / 'BossPylonTargetingDeviceTest.java')
    shutil.copyfile(PLAN, TEST / 'reviewed-plan.json')
    no_game_apk_outputs()
    check_original()


def fetch():
    scope = verify_scope()
    fetch_original()
    item = current_artifact('instrumentation', 'instrument')
    download(item, Path('pylon444-test-artifact.zip'), TEST)
    binary = check_new_test_apk(TEST / 'pylon-instrumentation.apk')
    record = read(TEST / 'instrumentation-verification.json')
    validate_test_record(record, scope, binary)
    exact.require(exact.sha(TEST / 'android-json-abi.log') == record['json_abi_log_sha256'],
                  'Compiled Android JSON ABI evidence changed')
    exact.require(exact.sha(TEST / 'BossPylonTargetingDeviceTest.java') == scope['fixture_sha256']
                  and exact.sha(TEST / 'reviewed-plan.json') == scope['plan_sha256'], 'Artifact source evidence differs')
    write(Path('pylon444-test-artifact.json'), item)
    return record


def check_case(text):
    exact.check_instrumentation(text, CLASS, METHOD)
    exact.require(re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\r?$', text, re.M) == ['1', '0'],
                  'Exactly one started and completed native case required')
    exact.require(set(re.findall(r'^INSTRUMENTATION_STATUS: class=([^\r\n]+)\r?$', text, re.M)) == {CLASS}
                  and set(re.findall(r'^INSTRUMENTATION_STATUS: test=([^\r\n]+)\r?$', text, re.M)) == {METHOD},
                  'Mixed or wrong native case identity')
    exact.require(re.findall(r'^INSTRUMENTATION_CODE: (-?\d+)\r?$', text, re.M) == ['-1'], 'Wrong native terminal code')
    exact.require(set(re.findall(r'^INSTRUMENTATION_STATUS: numtests=(\d+)\r?$', text, re.M)) == {'1'},
                  'Runner must declare exactly one selected case')


def check_pylon_images(folder):
    paths = list(folder.glob('*.png'))
    exact.require({path.name for path in paths} == IMAGES and len(paths) == 5, 'Exactly the five reviewed pylon captures required')
    result = {}
    for path in paths:
        data = path.read_bytes()
        exact.require(len(data) > 2000 and exact.check_png_pixels(data) == (320, 640), 'Incomplete native capture or wrong viewport')
        result[path.name] = exact.sha(path)
    return result


def native(variant):
    scope = verify_scope()
    original = check_original()
    binary = check_new_test_apk(TEST / 'pylon-instrumentation.apk')
    record = read(TEST / 'instrumentation-verification.json')
    validate_test_record(record, scope, binary)
    item = read('pylon444-test-artifact.json')
    source, run, attempt = identity()
    validate_artifact(item, item['id'], item['digest'][7:], run, source, artifact_name('instrumentation', attempt))
    scans.verify_file(Path('pylon444-test-artifact.zip'), item['digest'][7:], item['size_in_bytes'])
    NATIVE.mkdir(parents=True, exist_ok=True)
    apk = ORIGINAL / 'built/owner-delivery' / f'APD-{variant}-444.apk'
    subprocess.run(['adb', 'logcat', '-c'], check=True, timeout=20)
    stream = (NATIVE / 'native-logcat.txt').open('w')
    logcat = subprocess.Popen(['adb', 'logcat', '-v', 'time'], stdout=stream, stderr=subprocess.STDOUT)
    collector = None
    succeeded = False
    try:
        subprocess.run(['adb', 'install', '-r', str(apk)], check=True, timeout=120)
        subprocess.run(['adb', 'install', '-r', str(TEST / 'pylon-instrumentation.apk')], check=True, timeout=120)
        # Only the disposable emulator screenshot directory is cleared, never app data or saves.
        subprocess.run(['adb', 'shell', 'rm', '-rf', REMOTE], check=True, timeout=20)
        subprocess.run(['adb', 'shell', 'am', 'force-stop', exact.APP], check=True, timeout=30)
        collector = IncrementalImages(NATIVE / 'visuals'); collector.thread.start()
        started = datetime.now(timezone.utc).isoformat()
        report = NATIVE / 'pylon.txt'
        with report.open('w') as output:
            subprocess.run(['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', CLASS + '#' + METHOD,
                            exact.APP + '.test/androidx.test.runner.AndroidJUnitRunner'],
                           stdout=output, stderr=subprocess.STDOUT, check=True, timeout=360)
        check_case(report.read_text())
        collector.close(); collector = None
        remote = sync_images(NATIVE / 'visuals')
        images = check_pylon_images(NATIVE / 'visuals')
        exact.require(images == remote, 'Complete pylon device/local manifests differ')
        write(NATIVE / 'pylon-transfer.json', dict(started=started, completed=datetime.now(timezone.utc).isoformat(),
                                                 remote_images=remote))
        check_original()
        exact.require(exact.sha(TEST / 'pylon-instrumentation.apk') == binary['sha256'], 'Test bytes changed during native execution')
        write(NATIVE / 'native-verification.json', dict(kind='supplemental_APD444_pylon_native', variant=variant,
            scope=scope, original_game_source=ORIGINAL_SOURCE, original_game_run=ORIGINAL_RUN,
            application_sha256=ORIGINAL_APKS[variant], original_instrumentation_sha256=ORIGINAL_INSTRUMENTATION,
            supplemental_instrumentation_sha256=binary['sha256'], supplemental_test_artifact=item,
            completed_class=CLASS, completed_method=METHOD, targeted_tests=1, images=images,
            complete_remote_image_manifest_verified=True, application_rebuilt=False, instrumentation_rebuilt_in_native_job=False,
            primary_444_acceptance_claimed=False, manual_visual_review_complete=False, physical_phone_verified=False))
        succeeded = True
    finally:
        try:
            if collector is not None:
                collector.close()
            if not succeeded:
                # Preserve completed device frames even when the case or a later check failed.
                # This transfer receipt never claims a passing test or complete five-frame set.
                try:
                    partial = sync_images(NATIVE / 'visuals')
                    write(NATIVE / 'failure-transfer.json', dict(native_case_accepted=False,
                          remote_images=partial, complete_device_local_manifest_at_failure=True))
                except (OSError, subprocess.SubprocessError, RuntimeError, ValueError) as error:
                    write(NATIVE / 'failure-transfer.json', dict(native_case_accepted=False,
                          complete_device_local_manifest_at_failure=False, transfer_error=str(error)))
        finally:
            try:
                with (NATIVE / 'device-after.txt').open('w') as output:
                    subprocess.run(['adb', 'devices', '-l'], stdout=output, stderr=subprocess.STDOUT, timeout=15)
            except (OSError, subprocess.SubprocessError):
                pass
            logcat.terminate()
            try:
                logcat.wait(timeout=10)
            except subprocess.TimeoutExpired:
                logcat.kill(); logcat.wait()
            stream.close()


def finalize():
    record = fetch()
    source, run, attempt = identity()
    summaries = {}
    for variant in ORIGINAL_APKS:
        item = current_artifact('native', 'native (' + variant + ')', variant)
        folder = FINAL / variant
        download(item, FINAL / (variant + '.zip'), folder)
        result = read(folder / 'native-verification.json')
        exact.require(result['kind'] == 'supplemental_APD444_pylon_native' and result['variant'] == variant
                      and result['scope'] == record['scope'] and result['original_game_source'] == ORIGINAL_SOURCE
                      and result['original_game_run'] == ORIGINAL_RUN and result['application_sha256'] == ORIGINAL_APKS[variant]
                      and result['original_instrumentation_sha256'] == ORIGINAL_INSTRUMENTATION
                      and result['supplemental_instrumentation_sha256'] == record['test_apk']['sha256']
                      and result['supplemental_test_artifact'] == read('pylon444-test-artifact.json')
                      and result['targeted_tests'] == 1 and result['completed_class'] == CLASS
                      and result['completed_method'] == METHOD and result['application_rebuilt'] is False
                      and result['instrumentation_rebuilt_in_native_job'] is False
                      and result['complete_remote_image_manifest_verified'] is True
                      and result['primary_444_acceptance_claimed'] is False
                      and result['manual_visual_review_complete'] is False
                      and result['physical_phone_verified'] is False,
                      'Supplemental native source, binary, artifact or case binding differs')
        check_case((folder / 'pylon.txt').read_text())
        images = check_pylon_images(folder / 'visuals')
        exact.require(images == result['images'] == read(folder / 'pylon-transfer.json')['remote_images'],
                      'Final transfer/image manifests differ')
        exact.require((folder / 'native-logcat.txt').stat().st_size > 0, 'Missing native logcat')
        summaries[variant] = dict(native_artifact=item, receipt_sha256=exact.sha(folder / 'native-verification.json'),
                                  report_sha256=exact.sha(folder / 'pylon.txt'), images=images, targeted_tests=1)
    check_original()
    write(FINAL / 'supplemental-verification.json', dict(kind='supplemental_APD444_pylon_acceptance',
        instrumentation=record, supplemental_test_artifact=read('pylon444-test-artifact.json'), variants=summaries,
        targeted_tests=2, new_application_apks_created=0, original_application_source=ORIGINAL_SOURCE,
        original_core_tests=3214, original_core_suites=466, original_core_rerun=False,
        primary_444_acceptance_claimed=False, manual_visual_review_complete=False,
        physical_phone_verified=False, audio_verified=False, release_published=False))


def self_test():
    # Added by the isolated guard harness below. No live API, Gradle, adb or signer is invoked.
    from apd444_pylon_native_guards import run
    run(sys.modules[__name__])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('prepare', 'freeze', 'fetch', 'native', 'finalize', 'self-test'))
    parser.add_argument('--variant', choices=tuple(ORIGINAL_APKS))
    args = parser.parse_args()
    if args.stage == 'self-test':
        self_test(); return
    plan()  # Drafts reject before any API, build, download or device action.
    if args.stage == 'native':
        exact.require(args.variant is not None, 'Explicit original variant required')
        native(args.variant)
    else:
        globals()[args.stage]()


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        write(Path('pylon444-failure/failure.json'), dict(stage=sys.argv[1] if len(sys.argv) > 1 else None,
              error_type=type(error).__name__, message=str(error), recorded_utc=datetime.now(timezone.utc).isoformat(),
              application_rebuild_authorized=False, original_game_source=ORIGINAL_SOURCE))
        traceback.print_exc()
        raise SystemExit(1)
