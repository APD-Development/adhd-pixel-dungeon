#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Complete two Developer native cases on frozen APD446; preserve separate evidence cohorts."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import traceback
import zipfile

import apd_candidate446 as candidate
import apd_recover439_440 as exact
import apd_scan_prepared_sources as scans
from apd_native_screenshot_transfer import IncrementalImages, REMOTE, sync_images

BRANCH = 'codex/apd-phone-fixes-2026-10-09'
WORKFLOW = '.github/workflows/apd-recover446-developer.yml'
SCRIPT = 'tools/apd_recover446_developer.py'
GUARDS = 'tools/test_apd_recover446_developer.py'
PLAN = Path('docs/APD-446-NATIVE-RECOVERY-PLAN.json')
REQUEST = 'docs/APD-446-NATIVE-RECOVERY-REQUEST.txt'
ORIGINAL_SOURCE = '1968913a441f53ab3652cffc05e131427d0c3703'
ORIGINAL_RUN = 43077773970
ORIGINAL_WORKFLOW = '.github/workflows/apd-candidate-446.yml'
SOURCE_SHA = '46468fab2fbcd598a53de3061fef3791affa9f34c56cdb73cad8b57f8b2651a5'
INSTRUMENTATION_SHA = '9988e7d6c26832e982dc15cee33b7b70699fc72d5d843d811cffb72ecba2ef0d'
APK_HASHES = {
    'Developer': 'f2fc48b7a657af4007ff0c4799b30f5c33625e3d921696a33d64bd8bae0fc9b3',
    'Alpha-Testers': '9712f4ab41792a2bb6ef67ab7332ba32735a6cd214e74756bd53f762bc43cc69',
}
PRE_NATIVE_COMMIT = '6eaa346b38e17c93abffade923875b37596f2ee3'
PRE_NATIVE_PATH = 'docs/APD-446-PRE-NATIVE-ACCEPTANCE-2026-11-11.json'
PRE_NATIVE_SHA = '9a7f8fb88bb6bea3927491cca7cf23207a24f4ccb6a184745b6e66892be5c5fe'
ORIGINAL_SCOPE_SHA = '6e624bdef3a151ca854a77e680f54a960903207b2e8b067cd8eb8fe48c1fc047'
SEED_FIXTURE = 'android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/SeedHistoryDeviceTest.java'
SEED_FIXTURE_SHA = '45208417464da8397aea86c94953caaec30fec88a6b5b264be0098ad4f732bfd'
ORIGINAL_ALPHA_RECEIPT_SHA = '2e969ea4b8b302c0c7e4b298a94dca4626032d861a1f3adbab3e57384f25c4f2'
ORIGINAL_DEV_FINAL_TRANSFER_SHA = 'c6b6ac2d18acff019b52eabe6a54acf627f5cdb96dba618141c8937f40c7b8c9'
CORE_TESTS, CORE_SUITES = 3247, 468
ORIGINAL_DEV_COUNT, RECOVERY_COUNT, COMPLETE_COUNT = 1457, 12, 1469
ORIGINAL_LABELS = ('qol', 'wall', 'seed', 'restart', 'gameplay', 'freeze', 'search-context')
RECOVERY_LABELS = ('seed-history', 'pylon')
ALL_CASES = tuple(candidate.NATIVE_CASES)
ORIGINAL_CASES = tuple(case for case in ALL_CASES if case[0] in ORIGINAL_LABELS)
RECOVERY_CASES = tuple(case for case in ALL_CASES if case[0] in RECOVERY_LABELS)
ARTIFACTS = {'built': {'bytes': 184965727,
           'id': 13881279010,
           'name': 'apd446-built-pair',
           'sha256': 'a726c1c09a23faa4209ef51ff743da32503098737221be5c71dcf3f25e3c23ce'},
 'original_alpha': {'bytes': 54416927,
                    'id': 13885126623,
                    'name': 'apd446-native-Alpha-Testers',
                    'sha256': '41fbb9c0be6f2e689761de883785f31b7f463e34baa4c8ba7d96d970a07c45a8'},
 'original_developer': {'bytes': 53797333,
                        'id': 13884437545,
                        'name': 'apd446-native-Developer',
                        'sha256': '230b565e0193b9aca9638abf713a9e69ae5a19ad9a2ecf915a70e4167689d334'},
 'scan': {'bytes': 11309,
          'id': 13882374338,
          'name': 'apd446-prepared-source-scan-evidence',
          'sha256': '6bdbb9ae223b950ce0c5133774eaf03bf645624ddf9dbe7eb9196cbf67c4b1f1'}}
HELPER_HASHES = {'tools/apd_build_history.py': 'a957d5d859277a9f5f76d5bfbfb7ffdd7db8f6e8377fbd40c39e0e55171101de',
 'tools/apd_candidate441.py': 'a59674f3f55d5d79e6a19264de94e8c26c644c81b3469383fc07ab01d6719997',
 'tools/apd_candidate445.py': '81aceb908918984c192c839294baa474bdb36dca4c4743b8be74fae31a830005',
 'tools/apd_candidate446.py': 'c40fd27913886c81df61783104057810cd13abde82c67aa12093bbb67547ed6c',
 'tools/apd_existing_build_evidence.py': '07c23a6093b0bd3eb203c0d2558df3c3d5c2e43524056bb089c444dcf504f15d',
 'tools/apd_native_screenshot_transfer.py': 'bdc1312ad65ecc69e90a8614d38d4ec469bf8b92e03d4ac45c8e59a2fb8dc713',
 'tools/apd_owner_delivery_policy.py': '9430d8f4f63ca3883d8d525e661137648546e1d3ea08852b7cfcc2ea797ca6d3',
 'tools/apd_qol_source_scan.py': '436dcfc230412e39e53b95e7301a7a8c78228ebb63a7db5f1afca070026c7734',
 'tools/apd_recover439_440.py': '18151f49436836d79edc3aa53d995272ee0c89c07c90e03b95f199bbfc010382',
 'tools/apd_scan_prepared_sources.py': 'fc0f14dcbbfed96e8e17b4636ad19ce3df2050d512166b69bcf828fba66268fd',
 'tools/check_android_json_abi.py': '53299017cefff8a37dbb04769c9feda7cc7625002101a46cb07102ac549a3896',
 'tools/validate_android_visuals.py': '86fab85f843464f68034334dbfc73fb04c8076058565c2535190866708fbe049'}
SEED_IMAGES = frozenset(['seed-history-entry.png', 'seed-history-matching-records.png', 'seed-history-latest-unknown-selection.png', 'seed-history-cleared-generated-selection.png', 'seed-history-ranking-copy-confirm.png', 'seed-history-ranking-copy-result.png', 'seed-history-unfinished-record.png'])
PYLON_IMAGES = frozenset(['pylon-native-inactive-visible.png', 'pylon-native-active-visible.png', 'pylon-native-manual-rat-preserved.png', 'pylon-native-ranged-pylon-melee-boss.png', 'pylon-native-hidden-excluded.png'])
RECOVERY_IMAGES = SEED_IMAGES | PYLON_IMAGES


def read(path):
    return json.loads(Path(path).read_text())


def write_new(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x') as stream:
        stream.write(json.dumps(value, indent=2) + '\n')


def file_record(path):
    path = Path(path)
    return dict(path=str(path), bytes=path.stat().st_size, sha256=exact.sha(path))


def validate_plan(value, ready=True):
    expected = dict(schema=1, number=446, purpose='complete_two_developer_cases_on_frozen_APD446',
        original_source=ORIGINAL_SOURCE, original_run=ORIGINAL_RUN, original_run_attempt=1,
        original_artifacts=ARTIFACTS, original_core_tests=CORE_TESTS, original_core_suites=CORE_SUITES,
        original_developer_cases=list(ORIGINAL_LABELS), recovery_developer_cases=list(RECOVERY_LABELS),
        original_alpha_cases=[case[0] for case in ALL_CASES], original_developer_verified_images=ORIGINAL_DEV_COUNT,
        fresh_recovery_images=RECOVERY_COUNT, complete_images_per_variant=COMPLETE_COUNT,
        recovery_required_images=sorted(RECOVERY_IMAGES), original_partial_developer_images_are_diagnostic_only=True,
        application_rebuilt=False, instrumentation_rebuilt=False, source_archive_rebuilt=False,
        release_publication_authorized=False)
    exact.require(set(value) == set(expected) | {'status', 'pending_review'}, 'Recovery plan fields differ')
    exact.require(all(value[key] == wanted for key, wanted in expected.items()), 'Frozen recovery scope differs')
    exact.require(value['status'] in {'draft', 'ready_for_recovery'} and isinstance(value['pending_review'], list),
                  'Recovery readiness is malformed')
    if ready:
        exact.require(value['status'] == 'ready_for_recovery' and not value['pending_review'],
                      'Recovery plan is a draft; review must finish before any operational stage')
    exact.require(tuple(case[0] for case in ORIGINAL_CASES) == ORIGINAL_LABELS
                  and tuple(case[0] for case in RECOVERY_CASES) == RECOVERY_LABELS
                  and len(ALL_CASES) == 9 and len(RECOVERY_IMAGES) == RECOVERY_COUNT,
                  'Pinned exact native case or image inventory changed')
    return value


def plan(ready=True):
    return validate_plan(read(PLAN), ready)


def identity():
    exact.require(os.environ['GITHUB_REPOSITORY'] == exact.REPO
                  and os.environ['GITHUB_REF'] == 'refs/heads/' + BRANCH, 'Wrong repository or branch')
    source, run, attempt = os.environ['GITHUB_SHA'], int(os.environ['GITHUB_RUN_ID']), int(os.environ['GITHUB_RUN_ATTEMPT'])
    exact.require(re.fullmatch('[0-9a-f]{40}', source) and run > 0 and attempt > 0,
                  'Exact recovery source, run and attempt are required')
    return source, run, attempt


def validate_changes(changes):
    metadata = {'AGENTS.md', 'WORK-COMPLETED-LEDGER.md', 'APD-PLAYER-DESCRIPTION-AUDIT.json',
                'APD-PLAYER-DESCRIPTION-AUDIT-PROGRESS.md'}
    new_tools = {SCRIPT, GUARDS, WORKFLOW}
    exact.require(changes, 'Recovery must have an explicit reviewed source checkpoint')
    for status, path in changes:
        exact.safe_name(path)
        exact.require(status in {'A', 'M'}, 'Deletion, rename or copy is outside verification-only scope')
        exact.require(path.startswith('docs/') or path in metadata or path in new_tools,
                      'Application, instrumentation or unrelated tool mutation is forbidden: ' + path)
        if path in new_tools:
            exact.require(status == 'A', 'Recovery files must be new relative to the frozen game source')
    exact.require(new_tools <= {path for _, path in changes}, 'Recovery helper, guards and workflow are required')


def verify_scope():
    plan()
    source, run, attempt = identity()
    exact.require(exact.output(['git', 'rev-parse', 'HEAD']).strip() == source, 'Checkout differs from recovery source')
    subprocess.run(['git', 'merge-base', '--is-ancestor', ORIGINAL_SOURCE, source], check=True)
    subprocess.run(['git', 'diff', '--exit-code'], check=True)
    subprocess.run(['git', 'diff', '--cached', '--exit-code'], check=True)
    rows = exact.output(['git', 'diff', '--name-status', '--no-renames', ORIGINAL_SOURCE, source]).splitlines()
    changes = [row.split('\t') for row in rows]
    exact.require(all(len(row) == 2 for row in changes), 'Unexpected changed-path encoding')
    validate_changes(changes)
    for path, digest in HELPER_HASHES.items():
        exact.require(exact.sha(path) == digest, 'Existing verification helper changed: ' + path)
    exact.require(exact.sha(candidate.PLAN) == ORIGINAL_SCOPE_SHA and exact.sha(SEED_FIXTURE) == SEED_FIXTURE_SHA,
                  'Frozen candidate plan or instrumentation source changed')
    return dict(recovery_source=source, recovery_run=run, recovery_attempt=attempt, recovery_workflow=WORKFLOW,
                original_game_source=ORIGINAL_SOURCE, original_game_run=ORIGINAL_RUN,
                zero_application_delta=True, zero_instrumentation_delta=True, changed_paths=changes,
                plan_sha256=exact.sha(PLAN), helper_sha256=dict(HELPER_HASHES))


def validate_artifact(record, pin, source=ORIGINAL_SOURCE, run=ORIGINAL_RUN):
    candidate.validate_artifact(record, 446, pin['id'], pin['sha256'], run, source, pin['name'])
    exact.require(record['size_in_bytes'] == pin['bytes'] and pin['bytes'] > 0
                  and record['workflow_run']['head_branch'] == BRANCH, 'Artifact size or branch differs')


def download(root, key, record, pin):
    archive = root / 'archives' / (key + '.zip')
    destination = root / 'inputs' / key
    exact.require(not archive.exists() and not destination.exists(), 'Never overwrite a retained archive or extraction')
    archive.parent.mkdir(parents=True, exist_ok=True)
    with archive.open('xb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{pin["id"]}/zip'],
                       stdout=stream, check=True)
    scans.verify_file(archive, pin['sha256'], pin['bytes'])
    destination.mkdir(parents=True, exist_ok=False)
    exact.extract_archive(archive, destination)
    write_new(root / 'metadata' / (key + '.json'), record)
    return dict(artifact=dict(pin), archive=file_record(archive), extracted=str(destination))


def validate_original_run(run, jobs):
    exact.require(run['id'] == ORIGINAL_RUN and run['head_sha'] == ORIGINAL_SOURCE
                  and run['run_attempt'] == 1 and run['path'] == ORIGINAL_WORKFLOW
                  and run['head_branch'] == BRANCH and run['status'] == 'completed'
                  and run['conclusion'] == 'failure', 'The retained original failed run identity differs')
    expected = {'build': 'success', 'source-scan': 'success', 'native (Developer)': 'failure',
                'native (Alpha-Testers)': 'success', 'prepare-owner-review': 'skipped'}
    exact.require(len(jobs) == len(expected) and {job['name']: job['conclusion'] for job in jobs} == expected
                  and all(job['status'] == 'completed' and job['run_id'] == ORIGINAL_RUN
                          and job['run_attempt'] == 1 and job['head_sha'] == ORIGINAL_SOURCE for job in jobs),
                  'Original successful gates or retained failure status differ')
    by_name = {job['name']: job for job in jobs}
    required = {'build': ('Verify candidate scope and retained evidence safeguards',
                          'Require accepted APD444 and retain unaccepted APD445 compiled history',
                          'Run the exact complete core suite and compile every required target',
                          'Assemble one Developer and Alpha-Testers pair with the original signer'),
                'source-scan': ('Scan the exact APD446 source archive and reachable history',),
                'native (Alpha-Testers)': ('Run all nine native cases and verify complete device image transfers',)}
    for name, steps in required.items():
        for step in steps:
            rows = [item for item in by_name[name]['steps'] if item['name'] == step]
            exact.require(len(rows) == 1 and rows[0]['conclusion'] == 'success', 'Original required step did not pass')


def retained_pre_native():
    data = subprocess.check_output(['git', 'show', PRE_NATIVE_COMMIT + ':' + PRE_NATIVE_PATH])
    exact.require(hashlib.sha256(data).hexdigest() == PRE_NATIVE_SHA, 'Committed independent pre-native receipt changed')
    receipt = json.loads(data)
    exact.require(receipt['status'] == 'passed_pre_native_archive_source_and_original_build_evidence'
                  and receipt['number'] == 446 and receipt['source_commit'] == ORIGINAL_SOURCE
                  and receipt['producing_run'] == ORIGINAL_RUN and receipt['run_attempt'] == 1,
                  'Original independent pre-native identity differs')
    exact.require(receipt['existing_core_evidence'] == dict(tests=CORE_TESTS, suites=CORE_SUITES,
                  failures=0, errors=0, skipped=0, tests_rerun=False), 'Original full core gate differs')
    exact.require(receipt['predecessor_distinction']['accepted_owner_baseline']['number'] == 444
                  and receipt['predecessor_distinction']['retained_compiled_predecessor']['number'] == 445
                  and receipt['predecessor_distinction']['retained_compiled_predecessor']['accepted'] is False
                  and receipt['predecessor_distinction']['retained_compiled_predecessor']['delivered'] is False,
                  'Accepted444 and held445 were conflated')
    exact.require(receipt['compiled_history_and_player_notes']['both_previous_compiled_histories_preserved'] is True,
                  'Frozen compiled-history acceptance is missing')
    for variant, digest in APK_HASHES.items():
        exact.require(receipt['apk_files'][variant]['sha256'] == digest, 'Pre-native APK binding differs')
    exact.require(receipt['instrumentation_sha256'] == INSTRUMENTATION_SHA
                  and receipt['source_manifest']['source_sha256'] == SOURCE_SHA,
                  'Pre-native instrumentation or source binding differs')
    for key, pin_key in [('built_artifact', 'built'), ('source_scan_artifact', 'scan')]:
        value = receipt['artifacts'][key]
        exact.require(all(value[field] == ARTIFACTS[pin_key][field] for field in ('id', 'sha256', 'bytes', 'name')),
                      'Pre-native immutable artifact binding differs')
    return receipt


def configure_exact():
    exact.SPECS[446] = dict(source=ORIGINAL_SOURCE, run=ORIGINAL_RUN, workflow=ORIGINAL_WORKFLOW,
        artifact=ARTIFACTS['built']['id'], digest=ARTIFACTS['built']['sha256'],
        tests=CORE_TESTS, suites=CORE_SUITES, hashes=dict(APK_HASHES))
    candidate.configure_native_cases()


def check_frozen(root, sdk=True):
    configure_exact()
    prepared = root / 'inputs/built'
    prior = retained_pre_native()
    scope = candidate.validate_plan(read(candidate.PLAN))
    proof = read(prepared / 'built/evidence/candidate-provenance.json')
    candidate.check_provenance(proof, ORIGINAL_SOURCE, ORIGINAL_RUN, scope)
    exact.require(proof['apk_hashes'] == APK_HASHES and proof['instrumentation_sha256'] == INSTRUMENTATION_SHA,
                  'Original frozen provenance differs')
    for variant, digest in APK_HASHES.items():
        exact.require(exact.sha(prepared / 'built/owner-delivery' / f'APD-{variant}-446.apk') == digest,
                      'Frozen application bytes changed')
    exact.require(exact.sha(prepared / 'built/evidence/instrumentation.apk') == INSTRUMENTATION_SHA,
                  'Frozen test APK changed')
    source_zip = prepared / 'source/APD-Source-446.zip'
    exact.require(exact.sha(source_zip) == SOURCE_SHA, 'Frozen corresponding source changed')
    if sdk:
        assets, identity_proof = exact.check_pair(prepared / 'built', 446)
    else:
        assets, identity_proof = {}, read(prepared / 'preparation.json')
        for variant in APK_HASHES:
            with zipfile.ZipFile(prepared / 'built/owner-delivery' / f'APD-{variant}-446.apk') as packed:
                assets[variant] = {name[7:]: packed.read(name) for name in packed.namelist()
                                   if name.startswith('assets/') and not name.endswith('/')}
    exact.require(identity_proof['instrumentation_sha256'] == INSTRUMENTATION_SHA
                  and identity_proof['certificate_sha256'] == exact.CERT
                  and identity_proof['application_id'] == exact.APP, 'Original signer/test/app evidence differs')
    pins = dict(number=446, source_commit=ORIGINAL_SOURCE, artifact_run=ORIGINAL_RUN,
                artifact_head=ORIGINAL_SOURCE, artifact_name=ARTIFACTS['built']['name'],
                artifact_id=ARTIFACTS['built']['id'], artifact_sha256=ARTIFACTS['built']['sha256'],
                source_sha256=SOURCE_SHA)
    manifest = scans.validate_source_zip(source_zip, pins)
    exact.require(manifest == prior['source_manifest'], 'Original corresponding-source manifest differs')
    scanned = read(root / 'inputs/scan/prepared-source-scan-verification.json')
    exact.require(scanned['pins'] == pins and scanned['status'] == 'success'
                  and str(scanned['scan_workflow_run']) == str(ORIGINAL_RUN)
                  and scanned['scan_workflow_commit'] == ORIGINAL_SOURCE
                  and scanned['scanner_version'] == scans.SCANNER_VERSION
                  and scanned['scanner_archive_sha256'] == scans.SCANNER_ARCHIVE_SHA256
                  and scanned['existing_scanner_script_sha256'] == candidate.SCANNER_SCRIPT_DIGEST
                  and scanned['source_findings_reviewed'] is True and scanned['applications_rebuilt'] is False
                  and scanned['instrumentation_rebuilt'] is False and scanned['release_published'] is False,
                  'Original source scan binding or unchanged review rules differ')
    report = root / 'inputs/scan/APD-Source-Scan.json'
    exact.require(exact.sha(report) == scanned['scan_report_sha256'] == prior['source_scan']['report_sha256']
                  and exact.sha(root / 'inputs/scan/prepared-source-scan-verification.json')
                      == prior['source_scan']['receipt_sha256'], 'Original source scan receipt changed')
    scans.validate_scan_report(read(report), SOURCE_SHA)
    with zipfile.ZipFile(source_zip) as source:
        for name, value in assets['Developer'].items():
            if name == 'apd/features.txt':
                continue
            exact.require(assets['Alpha-Testers'][name] == value
                          and source.read('APD-Source-446/core/src/main/assets/' + name) == value,
                          'Compiled common asset or history differs from frozen source')
        exact.require(hashlib.sha256(source.read('APD-Source-446/' + SEED_FIXTURE)).hexdigest() == SEED_FIXTURE_SHA,
                      'Frozen Android seed fixture differs')
    return dict(original_game_source=ORIGINAL_SOURCE, original_game_run=ORIGINAL_RUN,
                apk_hashes=dict(APK_HASHES), instrumentation_sha256=INSTRUMENTATION_SHA,
                source_archive_sha256=SOURCE_SHA, source_manifest=manifest,
                pre_native_receipt_commit=PRE_NATIVE_COMMIT, pre_native_receipt_sha256=PRE_NATIVE_SHA,
                source_scan_receipt_sha256=prior['source_scan']['receipt_sha256'],
                compiled_history_verified_from_same_frozen_bytes=True, original_core=prior['existing_core_evidence'],
                original_compile_and_abi_evidence=prior['original_build_compile_and_abi_evidence'],
                sdk_signature_and_application_check_ran=sdk, original_signer=exact.CERT,
                application_rebuilt=False, instrumentation_rebuilt=False, source_archive_rebuilt=False)


def hash_images(folder, selected=None):
    folder = Path(folder)
    paths = {str(path.relative_to(folder)): path for path in sorted(folder.rglob('*.png'))}
    if selected is not None:
        exact.require(set(selected) <= set(paths), 'A cohort image is absent')
        paths = {name: paths[name] for name in selected}
    result = {}
    for name, path in paths.items():
        exact.require(not path.is_symlink(), 'Linked image is forbidden')
        data = path.read_bytes()
        exact.require(len(data) > 2000, 'Native screenshot is empty or incomplete')
        exact.check_png_pixels(data)
        result[name] = hashlib.sha256(data).hexdigest()
    return result


def validate_transfer_maps(labels, transfers, images):
    exact.require(set(transfers) == set(labels), 'Every accepted cohort case needs its own transfer receipt')
    previous = {}
    for label in labels:
        value = transfers[label]
        current = value.get('remote_images')
        exact.require(isinstance(current, dict) and current
                      and all(isinstance(digest, str) and re.fullmatch('[0-9a-f]{64}', digest)
                              and images.get(name) == digest for name, digest in current.items()),
                      'A cohort transfer differs from actual image bytes')
        exact.require(all(current.get(name) == digest for name, digest in previous.items()),
                      'A cohort changed or lost an earlier transferred image')
        exact.require(value.get('started') and value.get('completed')
                      and datetime.fromisoformat(value['completed']) >= datetime.fromisoformat(value['started']),
                      'Per-case transfer timing is incomplete')
        previous = current
    exact.require(previous == images, 'Final complete cohort device manifest differs from its accepted images')


def check_cases(folder, cases):
    result = []
    for label, short, method, _ in cases:
        report = Path(folder) / (label + '.txt')
        exact.check_instrumentation(report.read_text(), exact.CLASS_PREFIX + short, method)
        result.append(dict(case=label, class_name=exact.CLASS_PREFIX + short, method=method, **file_record(report)))
    return result


def trap_processes(folder):
    text = (Path(folder) / 'native-logcat.txt').read_text(errors='replace')
    seed = re.findall(r'TRAP_PERSIST_SEED_PID=(\d+)', text)
    restart = re.findall(r'TRAP_PERSIST_VERIFY_PID=(\d+)', text)
    exact.require(len(seed) == len(restart) == 1 and seed[0] != restart[0], 'Retained trap restart process proof differs')
    return dict(seed=seed[0], restart=restart[0])


def check_original_alpha(folder):
    folder = Path(folder)
    receipt_path = folder / 'native-verification.json'
    exact.require(exact.sha(receipt_path) == ORIGINAL_ALPHA_RECEIPT_SHA, 'Original Alpha native receipt changed')
    receipt = read(receipt_path)
    exact.require(receipt['number'] == 446 and receipt['variant'] == 'Alpha-Testers'
                  and receipt['source_commit'] == ORIGINAL_SOURCE and receipt['apk_sha256'] == APK_HASHES['Alpha-Testers']
                  and receipt['instrumentation_sha256'] == INSTRUMENTATION_SHA
                  and receipt['completed_cases'] == [case[0] for case in ALL_CASES]
                  and receipt['native_recovery_complete'] is True and receipt['application_rebuilt'] is False
                  and receipt['instrumentation_rebuilt'] is False, 'Original Alpha exact native identity differs')
    cases = check_cases(folder, ALL_CASES)
    images = exact.check_images(folder / 'visuals')
    exact.require(len(images) == COMPLETE_COUNT and images == receipt['images'], 'Original Alpha full image inventory differs')
    transfers = {case[0]: read(folder / (case[0] + '-transfer.json')) for case in ALL_CASES}
    candidate.check_transfers(receipt, transfers)
    validate_transfer_maps([case[0] for case in ALL_CASES], transfers, images)
    return dict(cohort='original-alpha', variant='Alpha-Testers', source_commit=ORIGINAL_SOURCE,
                apk_sha256=APK_HASHES['Alpha-Testers'], instrumentation_sha256=INSTRUMENTATION_SHA,
                original_run=ORIGINAL_RUN, artifact=dict(ARTIFACTS['original_alpha']),
                accepted_cases=cases, images=images, image_count=len(images), trap_processes=trap_processes(folder),
                native_receipt_sha256=ORIGINAL_ALPHA_RECEIPT_SHA, complete_cohort_device_manifest_verified=True)


def check_original_developer(folder):
    folder = Path(folder)
    exact.require(not (folder / 'native-verification.json').exists()
                  and not (folder / 'seed-history-transfer.json').exists() and not (folder / 'pylon.txt').exists(),
                  'The pinned incomplete Developer outcome differs')
    final = folder / 'search-context-transfer.json'
    exact.require(exact.sha(final) == ORIGINAL_DEV_FINAL_TRANSFER_SHA, 'Original seven-case final transfer changed')
    selected = read(final)['remote_images']
    exact.require(len(selected) == ORIGINAL_DEV_COUNT and not set(selected) & RECOVERY_IMAGES,
                  'The retained seven-case image partition differs')
    images = hash_images(folder / 'visuals', selected)
    exact.require(images == selected, 'Original retained images differ from device hashes')
    cases = check_cases(folder, ORIGINAL_CASES)
    transfers = {label: read(folder / (label + '-transfer.json')) for label in ORIGINAL_LABELS}
    validate_transfer_maps(ORIGINAL_LABELS, transfers, images)
    diagnostic_report = folder / 'seed-history.txt'
    check_cases(folder, RECOVERY_CASES[:1])
    all_paths = {str(path.relative_to(folder / 'visuals')): path for path in (folder / 'visuals').rglob('*.png')}
    extra = set(all_paths) - set(images)
    exact.require(extra == SEED_IMAGES - {'seed-history-unfinished-record.png'},
                  'Pinned original partial seed image diagnostics differ')
    return dict(cohort='original-developer-seven', variant='Developer', source_commit=ORIGINAL_SOURCE,
                apk_sha256=APK_HASHES['Developer'], instrumentation_sha256=INSTRUMENTATION_SHA, original_run=ORIGINAL_RUN,
                artifact=dict(ARTIFACTS['original_developer']), accepted_cases=cases, images=images,
                image_count=len(images), trap_processes=trap_processes(folder),
                complete_cohort_device_manifest_verified=True,
                diagnostics_only=dict(seed_report=file_record(diagnostic_report),
                    partial_images={name: exact.sha(all_paths[name]) for name in sorted(extra)},
                    partial_image_count=len(extra), original_seed_case_passed_but_transfer_incomplete=True,
                    pylon_not_run=True, used_for_acceptance=False))


def validate_cohorts(original, recovery, alpha):
    exact.require(len(original) == ORIGINAL_DEV_COUNT and len(recovery) == RECOVERY_COUNT
                  and len(alpha) == COMPLETE_COUNT, 'Cohort image counts differ')
    exact.require(set(recovery) == RECOVERY_IMAGES and not set(original) & set(recovery),
                  'Fresh and original image partitions overlap or differ')
    exact.require(set(original) | set(recovery) == set(alpha), 'The aggregate lost or introduced a required image')
    exact.require(all(re.fullmatch('[0-9a-f]{64}', digest) for images in (original, recovery, alpha)
                      for digest in images.values()), 'Malformed cohort image digest')
    return {**original, **recovery}


def prepare(root):
    scope = verify_scope()
    exact.require(json.loads(exact.output(['gh', 'api', f'repos/{exact.REPO}']))['private'], 'Private repository required')
    from apd_owner_delivery_policy import require_apk_delivery_allowed
    require_apk_delivery_allowed(Path.cwd())
    run = exact.api(f'actions/runs/{ORIGINAL_RUN}')
    jobs = exact.api(f'actions/runs/{ORIGINAL_RUN}/attempts/1/jobs?per_page=100')['jobs']
    validate_original_run(run, jobs)
    root.mkdir(parents=True, exist_ok=False)
    fetched = {}
    for key, pin in ARTIFACTS.items():
        record = exact.api(f'actions/artifacts/{pin["id"]}')
        validate_artifact(record, pin)
        exact.require(record['created_at'] >= run['run_started_at'], 'Original artifact predates its attempt')
        fetched[key] = download(root, key, record, pin)
    frozen = check_frozen(root)
    alpha = check_original_alpha(root / 'inputs/original_alpha')
    developer = check_original_developer(root / 'inputs/original_developer')
    exact.require(set(alpha['images']) - set(developer['images']) == RECOVERY_IMAGES,
                  'Original cohort partition does not isolate exactly the twelve required recovery images')
    write_new(root / 'input-provenance.json', dict(scope=scope, original_run=run, original_jobs=jobs,
        artifacts=fetched, frozen=frozen, original_alpha=alpha, original_developer=developer,
        original_failure_preserved=True, diagnostics_reused_for_acceptance=False))
    print('Frozen APD446 bytes, complete Alpha and seven fully transferred Developer cases verified.', flush=True)


def check_recovery_log(path):
    text = Path(path).read_text(errors='replace')
    expected = ('APD446_SEED_HISTORY unfinished_exact_seed=true total_attempts=1 finished_attempts=0 '
                'unfinished_attempts=1 observed_main_depth=9 clean_unsealed=true no_terminal_record=true rankings_unchanged=true')
    exact.require(text.count(expected) == 1, 'Exact unfinished seed-history observations are missing')
    exact.require(text.count('APD445_OWNED_FIXTURE restored intro=') == 1
                  and 'original_history_and_rankings_unchanged=true' in text, 'Seed fixture cleanup observation is missing')
    captured = re.findall(r'NATIVE_PYLON_PREFERENCE stage=captured intro=(true|false) stored=(true|false)', text)
    restored = re.findall(r'NATIVE_PYLON_PREFERENCE stage=restored intro=(true|false)', text)
    exact.require(len(captured) == len(restored) == 1 and captured[0][0] == restored[0],
                  'Actual pylon preference capture and restore differ')
    expected_states = [('inactive-visible', 'DM300', 'DM300', 'true', 'false'),
                       ('active-visible', 'Pylon', 'Pylon', 'true', 'true'),
                       ('manual-rat-preserved', 'Rat', 'Rat', 'true', 'true'),
                       ('ranged-pylon-melee-boss', 'Pylon', 'DM300', 'true', 'true'),
                       ('hidden-pylon-excluded', 'DM300', 'DM300', 'false', 'true')]
    lines = [line.split('NATIVE_PYLON_TARGET ', 1)[1] for line in text.splitlines() if 'NATIVE_PYLON_TARGET ' in line]
    exact.require(len(lines) == len(expected_states), 'All five actual pylon selection observations are required')
    values = [dict(token.split('=', 1) for token in line.split()) for line in lines]
    for value, (stage, quick, melee, visible, enemy) in zip(values, expected_states):
        exact.require((value['stage'], value['quick'], value['melee'], value['pylonVisible'], value['pylonEnemy'])
                      == (stage, quick, melee, visible, enemy) and value['activeActorIsHero'] == 'true',
                      'Actual pylon target selection differs')
        exact.require(all(value[key] == values[0][key] for key in ('actorTime', 'heroTime', 'heroHp', 'bossHp', 'pylonHp')),
                      'The held pylon fixture spent time or changed health')
    return dict(seed_scope_marker=expected, pylon_preferences=dict(captured=captured[0][0],
                stored=captured[0][1], restored=restored[0]), pylon_states=values)


def native(root):
    scope = verify_scope()
    provenance = read(root / 'input-provenance.json')
    exact.require(provenance['scope'] == scope, 'Recovery source or plan changed after original evidence preparation')
    frozen = check_frozen(root, sdk=False)
    destination = root / 'native-recovery'
    destination.mkdir(parents=True, exist_ok=False)
    write_new(destination / 'input-binding.json', dict(scope=scope, frozen=frozen,
              original_input_provenance_sha256=exact.sha(root / 'input-provenance.json'),
              original_artifacts=ARTIFACTS))
    apk = root / 'inputs/built/built/owner-delivery/APD-Developer-446.apk'
    instrumentation = root / 'inputs/built/built/evidence/instrumentation.apk'
    subprocess.run(['adb', 'wait-for-device'], check=True, timeout=120)
    installed = exact.output(['adb', 'shell', 'pm', 'list', 'packages'], timeout=30).splitlines()
    exact.require('package:' + exact.APP not in installed and 'package:' + exact.APP + '.test' not in installed,
                  'A fresh emulator with neither frozen package installed is required')
    profile = {key: exact.output(['adb', 'shell', 'getprop', key], timeout=20).strip()
               for key in ('ro.kernel.qemu', 'ro.build.version.sdk', 'ro.product.cpu.abi', 'persist.sys.locale', 'ro.product.locale')}
    exact.require(profile['ro.kernel.qemu'] == '1' and profile['ro.build.version.sdk'] == '34'
                  and profile['ro.product.cpu.abi'] == 'x86_64', 'Expected fresh API34 x86_64 emulator required')
    locale = profile['persist.sys.locale'] or profile['ro.product.locale']
    exact.require(locale.lower().startswith('en'), 'The existing English native fixture profile is required')
    write_new(destination / 'fresh-device.json', dict(profile=profile, both_packages_absent_before_install=True,
              old_screenshots_uploaded_to_device=False, old_application_data_copied=False))
    subprocess.run(['adb', 'logcat', '-c'], check=True, timeout=20)
    log_stream = (destination / 'native-logcat.txt').open('x')
    logcat = subprocess.Popen(['adb', 'logcat', '-v', 'time'], stdout=log_stream, stderr=subprocess.STDOUT)
    collector = None
    completed = []
    cleanup_errors = []
    try:
        subprocess.run(['adb', 'install', str(apk)], check=True, timeout=120)
        subprocess.run(['adb', 'install', str(instrumentation)], check=True, timeout=120)
        # Only fresh emulator evidence is removed. No old images or application data are pushed.
        subprocess.run(['adb', 'shell', 'rm', '-rf', REMOTE], check=True, timeout=20)
        for label, short_class, method, seconds in RECOVERY_CASES:
            subprocess.run(['adb', 'shell', 'am', 'force-stop', exact.APP], check=True, timeout=30)
            collector = IncrementalImages(destination / 'visuals')
            collector.thread.start()
            started = datetime.now(timezone.utc).isoformat()
            with (destination / (label + '.txt')).open('x') as stream:
                subprocess.run(['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                                exact.CLASS_PREFIX + short_class + '#' + method,
                                exact.APP + '.test/androidx.test.runner.AndroidJUnitRunner'],
                               stdout=stream, stderr=subprocess.STDOUT, check=True, timeout=seconds)
            exact.check_instrumentation((destination / (label + '.txt')).read_text(),
                                        exact.CLASS_PREFIX + short_class, method)
            collector.close()
            collector = None
            images = sync_images(destination / 'visuals')
            wanted = SEED_IMAGES if label == 'seed-history' else RECOVERY_IMAGES
            exact.require(set(images) == wanted, 'Fresh device emitted a missing, extra or old-cohort image')
            completed.append(label)
            write_new(destination / (label + '-transfer.json'), dict(started=started,
                completed=datetime.now(timezone.utc).isoformat(), remote_images=images,
                cohort='recovery-developer-two', recovery_run=scope['recovery_run'],
                recovery_attempt=scope['recovery_attempt'], recovery_source=scope['recovery_source']))
            print(f'{label}: one passed exact frozen-APK case; {len(images)} fresh device images verified.', flush=True)
    except BaseException as error:
        write_new(destination / 'failure.json', dict(status='incomplete_fresh_recovery_cohort',
            error_type=type(error).__name__, message=str(error), completed_transferred_cases=completed,
            application_rebuilt=False, instrumentation_rebuilt=False, acceptance_claimed=False))
        raise
    finally:
        if collector is not None:
            try:
                collector.close()
            except Exception as error:
                cleanup_errors.append(str(error))
        with (destination / 'device-after.txt').open('x') as stream:
            for command in (['adb', 'devices', '-l'], ['ps', '-eo', 'pid,ppid,rss,comm'], ['free', '-m']):
                try:
                    subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, timeout=20)
                except (OSError, subprocess.SubprocessError) as error:
                    stream.write(str(error) + '\n')
        logcat.terminate()
        try:
            logcat.wait(timeout=10)
        except subprocess.TimeoutExpired:
            logcat.kill()
            logcat.wait()
        log_stream.close()
        if cleanup_errors:
            write_new(destination / 'cleanup-errors.json', cleanup_errors)
    exact.require(not cleanup_errors, 'A screenshot collector did not retire cleanly')
    exact.require(completed == list(RECOVERY_LABELS), 'Both fresh cases must finish before acceptance')
    images = hash_images(destination / 'visuals')
    exact.require(set(images) == RECOVERY_IMAGES and len(images) == RECOVERY_COUNT, 'Fresh final image inventory differs')
    transfers = {label: read(destination / (label + '-transfer.json')) for label in RECOVERY_LABELS}
    validate_transfer_maps(RECOVERY_LABELS, transfers, images)
    observations = check_recovery_log(destination / 'native-logcat.txt')
    write_new(destination / 'native-recovery-verification.json', dict(schema=1, number=446,
        cohort='recovery-developer-two', variant='Developer', source_commit=ORIGINAL_SOURCE,
        original_run=ORIGINAL_RUN, recovery_source=scope['recovery_source'], recovery_run=scope['recovery_run'],
        recovery_attempt=scope['recovery_attempt'], recovery_workflow=WORKFLOW,
        original_built_artifact=ARTIFACTS['built'], apk_sha256=APK_HASHES['Developer'],
        instrumentation_sha256=INSTRUMENTATION_SHA, source_archive_sha256=SOURCE_SHA,
        completed_cases=completed, images=images, image_count=len(images), observations=observations,
        complete_cohort_device_manifest_verified=True, complete_remote_image_manifest_verified=True,
        fresh_device_sha256=exact.sha(destination / 'fresh-device.json'),
        input_binding_sha256=exact.sha(destination / 'input-binding.json'),
        application_rebuilt=False, instrumentation_rebuilt=False, source_archive_rebuilt=False,
        original_images_uploaded_to_device=False, old_application_data_copied=False,
        native_pair_acceptance_claimed=False, manual_visual_review_complete=False, published=False))


def artifact_name(kind, run, attempt):
    exact.require(kind in {'native', 'owner'} and run > 0 and attempt > 0, 'Invalid recovery artifact name identity')
    return f'apd446-developer-recovery-{kind}-run-{run}-attempt-{attempt}'


def current_native_artifact():
    source, run_id, attempt = identity()
    run = exact.api(f'actions/runs/{run_id}')
    exact.require(run['id'] == run_id and run['head_sha'] == source and run['run_attempt'] == attempt
                  and run['path'] == WORKFLOW and run['head_branch'] == BRANCH,
                  'Current recovery workflow source/run/attempt differs')
    jobs = exact.api(f'actions/runs/{run_id}/attempts/{attempt}/jobs?per_page=100')['jobs']
    selected = [job for job in jobs if job['name'] == 'recover-developer']
    exact.require(len(selected) == 1 and selected[0]['status'] == 'completed'
                  and selected[0]['conclusion'] == 'success' and selected[0]['head_sha'] == source
                  and selected[0]['run_id'] == run_id and selected[0]['run_attempt'] == attempt,
                  'The exact current-attempt recovery producer did not succeed')
    job = selected[0]
    for name in ('Verify original immutable APK, source and native evidence',
                 'Run only Developer seed-history and pylon with fresh screenshots'):
        steps = [step for step in job['steps'] if step['name'] == name]
        exact.require(len(steps) == 1 and steps[0]['conclusion'] == 'success', 'Required recovery producer step did not pass')
    name = artifact_name('native', run_id, attempt)
    matches = [item for item in exact.api(f'actions/runs/{run_id}/artifacts?per_page=100')['artifacts']
               if item['name'] == name]
    exact.require(len(matches) == 1, 'Exactly one current-attempt native recovery artifact is required')
    artifact = matches[0]
    exact.require(re.fullmatch('sha256:[0-9a-f]{64}', artifact['digest']), 'Current artifact digest is missing')
    pin = dict(id=artifact['id'], name=name, sha256=artifact['digest'][7:], bytes=artifact['size_in_bytes'])
    validate_artifact(artifact, pin, source, run_id)
    created = datetime.fromisoformat(artifact['created_at'])
    exact.require(datetime.fromisoformat(job['started_at']) <= created <= datetime.fromisoformat(job['completed_at']),
                  'Native recovery artifact is outside its current producer attempt')
    return artifact, pin, dict(run=run, producer_job=job)


def check_recovery(folder, pin, scope):
    folder = Path(folder)
    receipt = read(folder / 'native-recovery-verification.json')
    exact.require(receipt['schema'] == 1 and receipt['number'] == 446 and receipt['cohort'] == 'recovery-developer-two'
                  and receipt['variant'] == 'Developer' and receipt['source_commit'] == ORIGINAL_SOURCE
                  and receipt['original_run'] == ORIGINAL_RUN and receipt['recovery_workflow'] == WORKFLOW
                  and all(receipt[key] == scope[key] for key in ('recovery_source', 'recovery_run', 'recovery_attempt')),
                  'Fresh cohort source/run/attempt differs')
    exact.require(receipt['original_built_artifact'] == ARTIFACTS['built']
                  and receipt['apk_sha256'] == APK_HASHES['Developer']
                  and receipt['instrumentation_sha256'] == INSTRUMENTATION_SHA
                  and receipt['source_archive_sha256'] == SOURCE_SHA, 'Fresh cohort is not bound to exact frozen bytes')
    exact.require(receipt['completed_cases'] == list(RECOVERY_LABELS)
                  and receipt['complete_cohort_device_manifest_verified'] is True
                  and receipt['complete_remote_image_manifest_verified'] is True
                  and all(receipt[key] is False for key in ('application_rebuilt', 'instrumentation_rebuilt',
                      'source_archive_rebuilt', 'original_images_uploaded_to_device', 'old_application_data_copied',
                      'native_pair_acceptance_claimed', 'published')), 'Fresh cohort completion or isolation differs')
    exact.require(exact.sha(folder / 'fresh-device.json') == receipt['fresh_device_sha256']
                  and exact.sha(folder / 'input-binding.json') == receipt['input_binding_sha256'],
                  'Fresh device or source binding receipt changed')
    device = read(folder / 'fresh-device.json')
    exact.require(device['both_packages_absent_before_install'] is True
                  and device['old_screenshots_uploaded_to_device'] is False
                  and device['old_application_data_copied'] is False, 'Fresh device isolation was not established')
    exact.require(read(folder / 'input-binding.json')['scope'] == scope, 'Recovery input/source plan changed')
    cases = check_cases(folder, RECOVERY_CASES)
    images = hash_images(folder / 'visuals')
    exact.require(set(images) == RECOVERY_IMAGES and len(images) == RECOVERY_COUNT
                  and images == receipt['images'] and receipt['image_count'] == RECOVERY_COUNT,
                  'Fresh cohort image bytes or exact inventory differ')
    transfers = {label: read(folder / (label + '-transfer.json')) for label in RECOVERY_LABELS}
    for transfer in transfers.values():
        exact.require(transfer['cohort'] == 'recovery-developer-two'
                      and all(transfer[key] == scope[key] for key in ('recovery_source', 'recovery_run', 'recovery_attempt')),
                      'Fresh transfer came from a different recovery attempt')
    validate_transfer_maps(RECOVERY_LABELS, transfers, images)
    observations = check_recovery_log(folder / 'native-logcat.txt')
    exact.require(observations == receipt['observations'], 'Fresh actual runtime observations changed')
    return dict(cohort='recovery-developer-two', variant='Developer', source_commit=ORIGINAL_SOURCE,
                apk_sha256=APK_HASHES['Developer'], instrumentation_sha256=INSTRUMENTATION_SHA, original_run=ORIGINAL_RUN,
                recovery_source=scope['recovery_source'], recovery_run=scope['recovery_run'],
                recovery_attempt=scope['recovery_attempt'], artifact=pin, accepted_cases=cases,
                images=images, image_count=len(images), observations=observations,
                native_receipt_sha256=exact.sha(folder / 'native-recovery-verification.json'),
                complete_cohort_device_manifest_verified=True)


def retain_built_members(root, destination):
    prepared = root / 'inputs/built'
    archive = root / 'archives/built.zip'
    remap = {
        'built/owner-delivery/APD-Developer-446.apk': 'APD-Developer-446.apk',
        'built/owner-delivery/APD-Alpha-Testers-446.apk': 'APD-Alpha-Testers-446.apk',
        'source/APD-Source-446.zip': 'APD-Source-446.zip',
    }
    files = []
    with zipfile.ZipFile(archive) as packed:
        members = [member for member in scans.safe_members(packed) if not member.is_dir()]
        exact.require(len(members) == 481, 'Original complete built-artifact member count differs')
        exact.require({str(path.relative_to(prepared)) for path in prepared.rglob('*') if path.is_file()}
                      == {member.filename for member in members}, 'Original extraction has a missing or extra file')
        for member in members:
            source_path = member.filename
            destination_path = remap.get(source_path, 'original-built/' + source_path)
            source, target = prepared / source_path, destination / destination_path
            expected = hashlib.sha256()
            with packed.open(member) as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b''):
                    expected.update(block)
            digest = expected.hexdigest()
            exact.require(source.stat().st_size == member.file_size and exact.sha(source) == digest,
                          'Original extracted build member changed')
            exact.require(not target.exists(), 'Never overwrite an owner-package member')
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            exact.require(target.stat().st_size == member.file_size and exact.sha(target) == digest,
                          'Retained original build member changed during copy')
            files.append(dict(source_path=source_path, destination_path=destination_path,
                              bytes=member.file_size, sha256=digest))
    exact.require(len({item['destination_path'] for item in files}) == len(files), 'Owner mapping has duplicate destinations')
    value = dict(schema=1, number=446, original_artifact=dict(ARTIFACTS['built']), file_count=len(files),
                 files=sorted(files, key=lambda item: item['source_path']), duplicate_apk_or_source_bytes_created=False)
    write_new(destination / 'original-built-manifest.json', value)
    return value


def copy_cohort_records(source, destination):
    source, destination = Path(source), Path(destination)
    result = []
    for path in sorted(source.rglob('*')):
        if not path.is_file() or 'visuals' in path.relative_to(source).parts:
            continue
        relative = path.relative_to(source)
        target = destination / relative
        exact.require(not target.exists(), 'Never overwrite a cohort record')
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)
        exact.require(exact.sha(path) == exact.sha(target), 'Cohort record copy changed')
        result.append(dict(path=str(relative), bytes=target.stat().st_size, sha256=exact.sha(target)))
    return result


def finalize(root, destination):
    scope = verify_scope()
    provenance = read(root / 'input-provenance.json')
    exact.require(provenance['scope'] == scope, 'Owner preparation source/plan differs from its prepared inputs')
    frozen = check_frozen(root)
    artifact, pin, producer = current_native_artifact()
    fetched = download(root, 'recovery', artifact, pin)
    alpha = check_original_alpha(root / 'inputs/original_alpha')
    original = check_original_developer(root / 'inputs/original_developer')
    recovery = check_recovery(root / 'inputs/recovery', pin, scope)
    developer_images = validate_cohorts(original['images'], recovery['images'], alpha['images'])
    required = set(candidate.RETAINED_IMAGES) | set(candidate.plan()['additional_required_images'])
    exact.require(len(required) == 61 and required <= set(developer_images) and required <= set(alpha['images']),
                  'Existing or newly affected image coverage is incomplete')
    accepted_cases = {
        'Developer': original['accepted_cases'] + recovery['accepted_cases'],
        'Alpha-Testers': alpha['accepted_cases'],
    }
    exact.require(all([case['case'] for case in cases] == [case[0] for case in ALL_CASES]
                      and len({(case['class_name'], case['method']) for case in cases}) == 9
                      for cases in accepted_cases.values()), 'The eighteen logical cases are not exactly covered')
    destination.mkdir(parents=True, exist_ok=False)
    built_manifest = retain_built_members(root, destination)
    for name, digest in [('APD-Developer-446.apk', APK_HASHES['Developer']),
                         ('APD-Alpha-Testers-446.apk', APK_HASHES['Alpha-Testers']),
                         ('APD-Source-446.zip', SOURCE_SHA)]:
        exact.require(exact.sha(destination / name) == digest, 'Owner delivery bytes differ from the original frozen file')
        with (destination / (name + '.sha256')).open('x') as stream:
            stream.write(digest + '  ' + name + '\n')
    cohorts = {'Developer': [original, recovery], 'Alpha-Testers': [alpha]}
    records = {}
    for key, cohort in [('original_developer', original), ('original_alpha', alpha), ('recovery', recovery)]:
        records[cohort['cohort']] = copy_cohort_records(root / 'inputs' / key,
                                                      destination / 'native-evidence' / cohort['cohort'])
    scans_folder = destination / 'source-scan-evidence'
    scans_folder.mkdir()
    for path in sorted((root / 'inputs/scan').iterdir()):
        exact.require(path.is_file(), 'Unexpected source-scan directory')
        shutil.copyfile(path, scans_folder / path.name)
        exact.require(exact.sha(path) == exact.sha(scans_folder / path.name), 'Retained scan record changed')
    prepared = read(root / 'inputs/built/preparation.json')
    source_scan = read(root / 'inputs/scan/prepared-source-scan-verification.json')
    images = {'Developer': developer_images, 'Alpha-Testers': alpha['images']}
    image_cohorts = {variant: {name: cohort['cohort'] for cohort in values for name in cohort['images']}
                    for variant, values in cohorts.items()}
    proof = dict(schema=1, number=446, source_commit=ORIGINAL_SOURCE, original_run=ORIGINAL_RUN,
        producing_run=ORIGINAL_RUN, original_run_attempt=1, original_workflow=ORIGINAL_WORKFLOW,
        original_producing_conclusion='failure', recovery_source=scope['recovery_source'],
        recovery_run=scope['recovery_run'], recovery_attempt=scope['recovery_attempt'], recovery_workflow=WORKFLOW,
        artifact_id=ARTIFACTS['built']['id'], artifact_sha256=ARTIFACTS['built']['sha256'],
        original_artifacts=dict(ARTIFACTS), recovery_native_artifact=pin,
        core=prepared['core'], core_suites=prepared['core_suites'], variants=prepared['variants'],
        certificate_sha256=exact.CERT, application_id=exact.APP, version_code=920,
        instrumentation_sha256=INSTRUMENTATION_SHA, source_archive_sha256=SOURCE_SHA,
        scope_sha256=ORIGINAL_SCOPE_SHA, recovery_scope_sha256=exact.sha(PLAN),
        pre_native_receipt_sha256=PRE_NATIVE_SHA, accepted_owner_baseline_number=444,
        retained_compiled_predecessor_number=445, retained_predecessor_accepted=False,
        retained_predecessor_delivered=False, original_application_apks_created=2,
        new_application_apks_created=0, new_candidate_build=False, application_rebuilt=False,
        instrumentation_rebuilt=False, source_archive_rebuilt=False,
        android_tests=18, accepted_case_count_by_variant={variant: len(cases) for variant, cases in accepted_cases.items()},
        native_recovery_complete=True,
        native_acceptance_mode='original_alpha_plus_original_seven_developer_and_fresh_two_developer',
        native_cohorts=cohorts, native_images=images, image_cohorts=image_cohorts,
        complete_remote_image_manifest_verified=False, complete_device_manifests_verified_per_cohort=True,
        aggregate_hash_proven_image_coverage_verified=True,
        single_developer_device_manifest=False, original_partial_seed_used_for_acceptance=False,
        required_affected_images=sorted(required), original_failure_preserved=True,
        original_built_member_manifest_sha256=exact.sha(destination / 'original-built-manifest.json'),
        original_built_member_count=built_manifest['file_count'], cohort_record_files=records,
        source_scan_verified=True, immutable_original_source_scan_reused=True,
        source_scan_receipt_sha256=exact.sha(root / 'inputs/scan/prepared-source-scan-verification.json'),
        source_candidate_count=source_scan['source_candidate_count'],
        retained_redacted_history_candidate_count=source_scan['history_candidate_count'],
        manual_visual_review_complete=False, physical_phone_verified=False, audio_verified=False,
        install_over_existing_save_verified=False, saved_download_readback_verified=False,
        published=False, publication_authorized_by_this_workflow=False, delivery_claimed=False)
    write_new(destination / 'APD-446-Verification.json', proof)
    write_new(destination / 'APD-446-Original-Input-Provenance.json', provenance)
    write_new(destination / 'APD-446-Recovery-Artifact-Provenance.json', dict(scope=scope,
              native_artifact_metadata=artifact, fetched=fetched, recovery_producer=producer, frozen=frozen))
    print('APD446:18 logical cases accepted through separate original and fresh cohorts; immutable owner files prepared. Manual review and delivery remain separate.', flush=True)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('prepare', 'native', 'finalize'))
    parser.add_argument('--root', type=Path, default=Path('recovery446'))
    parser.add_argument('--output', type=Path, default=Path('owner-review446-recovered'))
    args = parser.parse_args(argv)
    plan()  # Always reject draft scope before git, API, SDK, emulator or file-copy operations.
    try:
        if args.stage == 'prepare':
            prepare(args.root)
        elif args.stage == 'native':
            native(args.root)
        else:
            finalize(args.root, args.output)
    except BaseException as error:
        if args.root.exists():
            failure = args.root / (args.stage + '-failure.json')
            if not failure.exists():
                write_new(failure, dict(stage=args.stage, exception=type(error).__name__, message=str(error),
                          recovery_acceptance_claimed=False, original_failure_preserved=True,
                          application_rebuilt=False, instrumentation_rebuilt=False))
        raise


if __name__ == '__main__':
    main()
