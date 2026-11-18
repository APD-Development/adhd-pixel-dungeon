#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build APD441 once, then qualify and package that exact pair without publication."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile

import apd_recover439_440 as exact

NUMBER = 441
CORE_TESTS = 3190
CORE_SUITES = 463
WORKFLOW = '.github/workflows/apd-excess-charge-441.yml'
RECOVERY_RUN = 43021022953
RECOVERY_SOURCE = '45b4520ced7bf719db2678bdb123f0de88de5019'
CORRECTION_RUN = 43024704967
CORRECTION_SOURCE = 'bbb24627d7717b7fd0869c3a1e8b524d9ad856c3'
CORRECTION_WORKFLOW = '.github/workflows/apd-recover440-alpha.yml'
REQUIRED_EXCESS_IMAGES = {
    'qol-reviewed-talent-clear-EXCESS_CHARGE-normal.png',
    'qol-reviewed-talent-clear-EXCESS_CHARGE-normal-bottom.png',
    'qol-reviewed-talent-clear-EXCESS_CHARGE.png',
    'qol-reviewed-talent-clear-EXCESS_CHARGE-bottom.png',
}
REQUIRED_COUNTER_IMAGES = {'qol-counter-ability-normal.png', 'qol-counter-ability-normal-bottom.png'}
REQUIRED_IMAGES = REQUIRED_EXCESS_IMAGES | REQUIRED_COUNTER_IMAGES


def current_identity():
    exact.require(os.environ['GITHUB_REPOSITORY'] == exact.REPO, 'Wrong repository')
    source = os.environ['GITHUB_SHA']
    exact.require(re.fullmatch('[0-9a-f]{40}', source), 'Full producing SHA required')
    return source, int(os.environ['GITHUB_RUN_ID'])


def check_provenance(proof, source, run_id):
    exact.require(proof['number'] == NUMBER and proof['source_commit'] == source
                  and proof['producing_run'] == run_id and proof['workflow'] == WORKFLOW
                  and proof['core_tests'] == CORE_TESTS and proof['core_suites'] == CORE_SUITES
                  and proof['previous_source'] == exact.SPECS[440]['source']
                  and proof['recovery_run'] == RECOVERY_RUN and proof['recovery_source'] == RECOVERY_SOURCE
                  and proof['correction_run'] == CORRECTION_RUN and proof['correction_source'] == CORRECTION_SOURCE,
                  'Candidate provenance differs from independent workflow identity')
    exact.require(set(proof['apk_hashes']) == set(exact.FEATURES)
                  and all(re.fullmatch('[0-9a-f]{64}', value) for value in proof['apk_hashes'].values())
                  and re.fullmatch('[0-9a-f]{64}', proof['instrumentation_sha256']), 'Invalid candidate digests')


def configure(root):
    source, run_id = current_identity()
    proof = json.loads((root / 'built/evidence/candidate-provenance.json').read_text())
    check_provenance(proof, source, run_id)
    fetched_path = root / 'fetched-artifact.json'
    fetched = json.loads(fetched_path.read_text()) if fetched_path.exists() else {}
    if fetched:
        exact.require(fetched['workflow_run']['id'] == run_id and fetched['workflow_run']['head_sha'] == source
                      and fetched['name'] == 'apd441-built-pair' and not fetched['expired'], 'Fetched candidate identity differs')
    exact.SPECS[NUMBER] = dict(source=source, run=run_id, workflow=WORKFLOW, tests=CORE_TESTS,
                             suites=CORE_SUITES, hashes=proof['apk_hashes'], artifact=fetched.get('id'),
                             digest=fetched.get('digest', '').removeprefix('sha256:'))
    exact.require(exact.sha(root / 'built/evidence/instrumentation.apk') == proof['instrumentation_sha256'],
                  'Original candidate instrumentation changed')
    return proof


def wait_for_recovery():
    original = exact.api(f'actions/runs/{RECOVERY_RUN}')
    exact.require(original['head_sha'] == RECOVERY_SOURCE
                  and original['path'] == '.github/workflows/apd-recover439-440.yml'
                  and original['status'] == 'completed' and original['conclusion'] == 'failure',
                  'Original partial recovery identity or retained failure history changed')
    original_jobs = exact.api(f'actions/runs/{RECOVERY_RUN}/jobs?per_page=100')['jobs']
    expected = {'prepare (439)': 'success', 'prepare (440)': 'success',
                'native (439, Developer)': 'success', 'native (439, Alpha-Testers)': 'success',
                'native (440, Developer)': 'success', 'native (440, Alpha-Testers)': 'failure',
                'package (439)': 'success', 'package (440)': 'failure'}
    exact.require(len(original_jobs) == 8
                  and {job['name']: job['conclusion'] for job in original_jobs} == expected,
                  'Successful original cases or failed Alpha evidence changed')
    until = time.monotonic() + 70 * 60
    while True:
        run = exact.api(f'actions/runs/{CORRECTION_RUN}')
        exact.require(run['head_sha'] == CORRECTION_SOURCE and run['path'] == CORRECTION_WORKFLOW,
                      'Exact Alpha recovery prerequisite identity changed')
        if run['status'] == 'completed':
            exact.require(run['conclusion'] == 'success', 'Corrective original-APK recovery did not pass every gate')
            break
        exact.require(time.monotonic() < until, 'Recovery prerequisite did not finish within 70 minutes')
        print('Waiting for the original APD440 Alpha cases and complete image transfer.', flush=True)
        time.sleep(15)
    jobs = exact.api(f'actions/runs/{CORRECTION_RUN}/jobs?per_page=100')['jobs']
    exact.require(len(jobs) == 1 and jobs[0]['name'] == 'recover-440-alpha'
                  and jobs[0]['conclusion'] == 'success', 'Complete corrective native/package job required')
    required = {'Verify frozen APKs, original core results and retained successful native evidence',
                'Run all six original Alpha cases and verify every transferred screenshot',
                'Require all original and new native evidence before packaging the exact pair'}
    exact.require(required <= {step['name'] for step in jobs[0]['steps'] if step['conclusion'] == 'success'},
                  'Corrective prerequisite gates incomplete')
    artifacts = exact.api(f'actions/runs/{CORRECTION_RUN}/artifacts?per_page=100')['artifacts']
    name = f'apd440-complete-owner-review-{CORRECTION_RUN}-{run["run_attempt"]}'
    packages = [item for item in artifacts if item['name'] == name]
    exact.require(len(packages) == 1, 'One exact completed original-pair recovery package required')
    package = packages[0]
    exact.require(not package['expired'] and package['workflow_run']['id'] == CORRECTION_RUN
                  and package['workflow_run']['head_sha'] == CORRECTION_SOURCE
                  and re.fullmatch('sha256:[0-9a-f]{64}', package['digest']),
                  'Completed original-pair recovery artifact identity differs')
    return original, original_jobs, dict(run=run, jobs=jobs, artifact=package)


def history(root):
    source, run_id = current_identity()
    from apd_owner_delivery_policy import require_apk_delivery_allowed
    require_apk_delivery_allowed(Path.cwd())
    repo = json.loads(exact.output(['gh', 'api', f'repos/{exact.REPO}']))
    exact.require(repo['private'], 'Private repository required')
    listed = exact.api('actions/artifacts?name=apd441-built-pair&per_page=100')['artifacts']
    previous = [item for item in listed if item['name'] == 'apd441-built-pair']
    exact.require(not previous, 'APD441 already has a built-pair artifact; recover it instead of rebuilding')
    recovery, jobs, correction = wait_for_recovery()
    spec = exact.SPECS[440]
    artifact = exact.api(f'actions/artifacts/{spec["artifact"]}')
    exact.require(artifact['digest'] == 'sha256:' + spec['digest'] and not artifact['expired']
                  and artifact['workflow_run']['head_sha'] == spec['source']
                  and artifact['workflow_run']['id'] == spec['run']
                  and artifact['name'] == 'apd440-built-pair', 'Previous compiled history artifact differs')
    previous_root = Path('previous440')
    previous_root.mkdir()
    archive = previous_root / 'original.zip'
    with archive.open('wb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{spec["artifact"]}/zip'],
                       stdout=stream, check=True)
    exact.require(exact.sha(archive) == spec['digest'], 'Previous archive download digest differs')
    exact.extract_archive(archive, previous_root / 'built')
    exact.check_pair(previous_root / 'built', 440)
    changed = exact.output(['git', 'diff', '--name-only', spec['source'], source]).splitlines()
    # This candidate corrects an existing description and adds coverage, never gameplay formulas.
    allowed_application = {'core/src/main/assets/messages/actors/actors.properties'}
    for name in changed:
        application = name.split('/', 1)[0] not in ('.github', 'docs', 'tools')
        application = application and not name.startswith(('core/src/test/', 'android/src/androidTest/',
                                                            'core/src/main/assets/apd/history/'))
        exact.require(not application or name in allowed_application, 'Unapproved APD441 application change: ' + name)
    from apd_build_history import merge_previous_apk, embed
    merge_previous_apk(Path.cwd(), previous_root / 'built/owner-delivery/APD-Developer-440.apk', 'Codex TEST 440')
    ledger = dict(date='2026-11-11', changes=[dict(category='Bug Fixes', sprite='ICON_PREFS',
        title='Excess Charge and Counter Ability descriptions',
        text='Excess Charge now describes its existing effective imbued-wand level calculation, temporary level bonuses, '
             'whole-point rounding, and preservation of a stronger Barrier. Counter Ability rank +2 now correctly says '
             '0.75 refunded charges. Existing combat formulas and saves are unchanged.',
        paths=changed)])
    embed(Path.cwd(), ledger, NUMBER)
    evidence = root / 'built/evidence'
    evidence.mkdir(parents=True, exist_ok=True)
    exact.write_json(evidence / 'source.json', dict(source_commit=source, previous_source=spec['source'], changed_paths=changed))
    exact.write_json(root / 'prerequisite-recovery.json', dict(run=recovery, jobs=jobs,
                     correction=correction, previous_artifact=artifact))
    exact.write_json(root / 'changeset.json', ledger)


def core():
    exact.SPECS[NUMBER] = dict(tests=CORE_TESTS, suites=CORE_SUITES)
    print(exact.check_core(Path('core/build/test-results/test'), NUMBER))


def freeze(root):
    source, run_id = current_identity()
    built = root / 'built'
    pair = built / 'owner-delivery'
    expected = {f'APD-{variant}-441.apk' for variant in exact.FEATURES}
    exact.require({path.name for path in pair.glob('*.apk')} == expected, 'Exactly two APD441 application APKs required')
    from apd_build_history import verify_compiled
    hashes = {}
    apks = {}
    for variant in exact.FEATURES:
        path = pair / f'APD-{variant}-441.apk'
        verify_compiled(Path.cwd(), path)
        hashes[variant] = exact.sha(path)
        apks[variant] = dict(sha256=hashes[variant], bytes=path.stat().st_size, cert=exact.CERT)
    reports = built / 'core/build/test-results/test'
    shutil.copytree('core/build/test-results/test', reports)
    proof = dict(number=NUMBER, source_commit=source, producing_run=run_id, workflow=WORKFLOW,
                 previous_source=exact.SPECS[440]['source'], recovery_run=RECOVERY_RUN,
                 recovery_source=RECOVERY_SOURCE, correction_run=CORRECTION_RUN,
                 correction_source=CORRECTION_SOURCE, core_tests=CORE_TESTS, core_suites=CORE_SUITES,
                 apk_hashes=hashes, instrumentation_sha256=exact.sha(built / 'evidence/instrumentation.apk'),
                 new_application_apks_created=2)
    exact.write_json(built / 'evidence/apks.json', apks)
    exact.write_json(built / 'evidence/candidate-provenance.json', proof)
    configure(root)
    assets, verified = exact.check_pair(built, NUMBER)
    verified.update(new_application_apks_created=2, native_recovery_complete=False,
                    manual_visual_review_complete=False)
    verified['source_archive_sha256'] = exact.source_archive(NUMBER, assets, verified, root / 'source')
    exact.write_json(root / 'preparation.json', verified)
    print('APD441 original pair frozen with full core, compile, signature, history and corresponding-source checks.')


def fetch(root):
    source, run_id = current_identity()
    run = exact.api(f'actions/runs/{run_id}')
    exact.require(run['head_sha'] == source and run['path'] == WORKFLOW, 'Candidate producer run differs')
    jobs = exact.api(f'actions/runs/{run_id}/jobs?per_page=100')['jobs']
    build = [job for job in jobs if job['name'] == 'build']
    exact.require(len(build) == 1 and build[0]['conclusion'] == 'success', 'Candidate producer did not pass all build gates')
    listed = exact.api(f'actions/runs/{run_id}/artifacts?name=apd441-built-pair&per_page=100')['artifacts']
    artifacts = [item for item in listed if item['name'] == 'apd441-built-pair']
    exact.require(len(artifacts) == 1, 'Ambiguous original candidate artifact')
    artifact = artifacts[0]
    exact.require(artifact['name'] == 'apd441-built-pair' and not artifact['expired']
                  and artifact['workflow_run']['id'] == run_id and artifact['workflow_run']['head_sha'] == source
                  and re.fullmatch('sha256:[0-9a-f]{64}', artifact['digest']), 'Candidate artifact identity differs')
    root.mkdir(parents=True, exist_ok=True)
    archive = root / 'candidate-artifact.zip'
    with archive.open('wb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{artifact["id"]}/zip'], stdout=stream, check=True)
    exact.require('sha256:' + exact.sha(archive) == artifact['digest'], 'Original candidate artifact download changed')
    exact.extract_archive(archive, root)
    exact.write_json(root / 'fetched-artifact.json', artifact)
    configure(root)
    exact.check_pair(root / 'built', NUMBER)


def check_transfers(receipt, transfers):
    exact.require(receipt.get('complete_remote_image_manifest_verified') is True,
                  'Complete device screenshot manifest verification is required')
    labels = [case[0] for case in exact.CASES]
    exact.require(set(transfers) == set(labels), 'Every native case needs its transfer receipt')
    for label in labels:
        images = transfers[label].get('remote_images')
        exact.require(isinstance(images, dict) and images and set(images) <= set(receipt['images'])
                      and all(isinstance(value, str) and re.fullmatch('[0-9a-f]{64}', value)
                              for value in images.values()), 'Incomplete per-case image transfer evidence')
    exact.require(transfers[labels[-1]]['remote_images'] == receipt['images'],
                  'Final device image manifest differs from native acceptance')


def require_affected_images(evidence):
    receipt = json.loads((evidence / 'native-verification.json').read_text())
    transfers = {case[0]: json.loads((evidence / (case[0] + '-transfer.json')).read_text())
                 for case in exact.CASES}
    check_transfers(receipt, transfers)
    names = {Path(name).name for name in receipt['images']}
    exact.require(REQUIRED_IMAGES <= names, 'Incomplete affected talent-description screenshots')
    for name in REQUIRED_IMAGES:
        path = evidence / 'visuals' / name
        exact.require(path.is_file() and exact.sha(path) == receipt['images'][name], 'Affected talent screenshot changed')


def native(root, variant, destination):
    configure(root)
    exact.native(NUMBER, variant, root, destination)
    require_affected_images(destination)


def finalize(root, native_root, destination):
    configure(root)
    check_root = Path('checked-native441')
    for variant in exact.FEATURES:
        evidence = native_root / f'apd441-native-{variant}'
        require_affected_images(evidence)
        shutil.copytree(evidence, check_root / f'apd-recovery-441-native-{variant}')
    exact.finalize(NUMBER, root, check_root, destination)
    original_receipt = destination / 'APD-441-Recovery-Verification.json'
    proof = json.loads(original_receipt.read_text())
    proof.update(new_application_apks_created=2, producing_run=int(os.environ['GITHUB_RUN_ID']),
                 new_candidate_build=True, original_pair_reused_for_native=True,
                 required_excess_charge_images=sorted(REQUIRED_EXCESS_IMAGES),
                 required_counter_ability_images=sorted(REQUIRED_COUNTER_IMAGES),
                 complete_remote_image_manifest_verified=True,
                 publication_authorized_by_this_workflow=False)
    exact.write_json(destination / 'APD-441-Verification.json', proof)
    original_receipt.unlink()
    print('APD441 exact pair passed all12 native cases; manual affected-image review still required. No publication.')


def self_test():
    source = 'a' * 40
    good = dict(number=441, source_commit=source, producing_run=1, workflow=WORKFLOW,
                previous_source=exact.SPECS[440]['source'], recovery_run=RECOVERY_RUN,
                recovery_source=RECOVERY_SOURCE, correction_run=CORRECTION_RUN,
                correction_source=CORRECTION_SOURCE, core_tests=CORE_TESTS, core_suites=CORE_SUITES,
                apk_hashes={name: 'b' * 64 for name in exact.FEATURES}, instrumentation_sha256='c' * 64)
    check_provenance(good, source, 1)
    for key, value in [('number', 440), ('source_commit', 'd' * 40), ('producing_run', 2),
                       ('workflow', 'different.yml'), ('core_tests', 3180), ('core_suites', 461),
                       ('recovery_source', 'e' * 40), ('correction_run', 1),
                       ('correction_source', 'f' * 40), ('instrumentation_sha256', 'truncated')]:
        bad = dict(good, **{key: value})
        try:
            check_provenance(bad, source, 1)
        except RuntimeError:
            continue
        raise AssertionError('Provenance mismatch was not rejected: ' + key)
    print('Ten candidate provenance corruption cases rejected.')
    receipt = dict(complete_remote_image_manifest_verified=True, images={'screen.png': 'b' * 64})
    transfers = {case[0]: dict(remote_images=dict(receipt['images'])) for case in exact.CASES}
    check_transfers(receipt, transfers)
    corruptions = [
        (dict(receipt, complete_remote_image_manifest_verified=False), transfers),
        (receipt, {key: value for key, value in transfers.items() if key != 'restart'}),
        (receipt, dict(transfers, qol=dict(remote_images={'unaccepted.png': 'b' * 64}))),
        (receipt, dict(transfers, freeze=dict(remote_images={'screen.png': 'c' * 64}))),
    ]
    for bad_receipt, bad_transfers in corruptions:
        try:
            check_transfers(bad_receipt, bad_transfers)
        except RuntimeError:
            continue
        raise AssertionError('Incomplete or substituted transfer evidence was not rejected')
    print('Four candidate native-transfer corruption cases rejected.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('history', 'core', 'freeze', 'fetch', 'native', 'finalize', 'self-test'))
    parser.add_argument('--root', type=Path, default=Path('candidate441'))
    parser.add_argument('--variant', choices=tuple(exact.FEATURES))
    parser.add_argument('--output', type=Path, default=Path('native441'))
    parser.add_argument('--native-root', type=Path, default=Path('native-evidence'))
    args = parser.parse_args()
    if args.stage == 'history': history(args.root)
    elif args.stage == 'core': core()
    elif args.stage == 'freeze': freeze(args.root)
    elif args.stage == 'fetch': fetch(args.root)
    elif args.stage == 'native':
        exact.require(args.variant, 'Native variant required')
        native(args.root, args.variant, args.output)
    elif args.stage == 'finalize': finalize(args.root, args.native_root, args.output)
    else: self_test()


if __name__ == '__main__':
    main()
