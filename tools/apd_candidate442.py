#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Draft APD442 infrastructure: one frozen pair, exact predecessor, no publication."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

import apd_recover439_440 as exact
from apd_candidate441 import check_transfers, REQUIRED_IMAGES as RETAINED_IMAGES
import apd_scan_prepared_sources as scans

NUMBER = 442
BRANCH = 'codex/apd-phone-fixes-2026-10-09'
BASELINE = '31e66d6c5bb9f8602f97dd051ec6d8f45c7929f3'
WORKFLOW = '.github/workflows/apd-candidate-442.yml'
PLAN = Path('docs/APD-442-BUILD-PLAN.json')
PREVIOUS_SOURCE = '748f9c990284b61f74b39c5111646bcb1d395183'
PREVIOUS_RUN = 43026044936
PREVIOUS_WORKFLOW = '.github/workflows/apd-excess-charge-441.yml'
PREVIOUS_ARTIFACT = 13860480452
PREVIOUS_DIGEST = 'a2f9775712bbe777239ba2c220a5ee972e360e44da79a97c9f0f6d9e9950acfc'
PREVIOUS_OWNER = 13861068719
PREVIOUS_OWNER_DIGEST = 'b16193bb202a2dbe17af4d808667576a0c11506abc0b91393669fadb7dd1ba4a'
PREVIOUS_SOURCE_DIGEST = '41b4ca10a168a49c3afcf4afc4f66c2facfff53b22668444c47ab9f5c303d1d0'
PREVIOUS_INSTRUMENTATION = 'cb840ea9f690092e06e59c32a47507474ecb525b6e93299b15134660fbf186e3'
PREVIOUS_HASHES = {
    'Developer': 'ca3133202969f2c2e7fe58d751fbac4a767207aaa0b96b902c05d5e7d2af0fde',
    'Alpha-Testers': 'f4fc33b038331969729a5d0bca5c6b52bb36d1ac451e19deb877f9c5f7f66370',
}
PREVIOUS_RECEIPTS = {
    'docs/APD-441-DELIVERY-2026-11-11.json': 'db43a912f0f0a015ae974f5ca6f29443404502d1b3dc995a385d5b38088ec75e',
    'docs/APD-441-MANUAL-VISUAL-REVIEW-2026-11-11.json': '20ae6067b3e63f48cd921329f10beb50227ac40198a4c9d229ffc9f2b53124f8',
}
PREVIOUS_SCAN = dict(run=43028854285, source='76866517a5e6ecd043d7ac3f36fe383d2cf9aaf3',
                     artifact=13859127394, digest='da78ecfd3b29fcce92a5a3c2b892a90e107dc5f6187852ff7a26252aec03fbf3')
SCANNER_SCRIPT_DIGEST = '436dcfc230412e39e53b95e7301a7a8c78228ebb63a7db5f1afca070026c7734'


def identity():
    exact.require(os.environ['GITHUB_REPOSITORY'] == exact.REPO
                  and os.environ['GITHUB_REF'] == 'refs/heads/' + BRANCH, 'Wrong repository or branch')
    source = os.environ['GITHUB_SHA']
    exact.require(re.fullmatch('[0-9a-f]{40}', source), 'Full producing SHA required')
    return source, int(os.environ['GITHUB_RUN_ID'])


def validate_plan(plan, ready=True):
    keys = {'schema', 'number', 'base_head', 'status', 'date', 'core_tests', 'core_suites',
            'application_paths', 'additional_required_images', 'changes', 'pending_scope_items'}
    exact.require(isinstance(plan, dict) and set(plan) == keys and plan['schema'] == 1
                  and plan['number'] == NUMBER and plan['base_head'] == BASELINE, 'Wrong 442 scope schema or baseline')
    if ready:
        exact.require(plan['status'] == 'ready_for_build' and not plan['pending_scope_items'],
                      'APD442 is a draft; finalize the actual tested scope before requesting a build')
    exact.require(type(plan['core_tests']) is int and plan['core_tests'] >= 3190
                  and type(plan['core_suites']) is int and plan['core_suites'] >= 463,
                  'APD441 core coverage cannot be reduced')
    paths = plan['application_paths']
    exact.require(isinstance(paths, list) and paths and len(paths) == len(set(paths)), 'Exact application path list required')
    for name in paths:
        exact.safe_name(name)
        exact.require(name.startswith('core/src/main/') and not name.startswith('core/src/main/assets/apd/history/'),
                      '442 application scope must list the actual reviewed core paths')
    images = plan['additional_required_images']
    exact.require(isinstance(images, list) and len(images) == len(set(images)), 'Duplicate additional screenshot names')
    for name in images:
        exact.require(re.fullmatch(r'[A-Za-z0-9_$(). -]+\.png', name) and not name.startswith('.'),
                      'Unsafe screenshot filename')
    if ready:
        exact.require(images, 'Declare the affected native screenshots before building')
    exact.require(isinstance(plan['changes'], list) and plan['changes'], 'Actual in-game change notes required')
    for row in plan['changes']:
        exact.require(set(row) == {'category', 'sprite', 'title', 'text'}
                      and all(isinstance(value, str) and value.strip() for value in row.values()),
                      'Invalid scoped change note')
    exact.require(re.fullmatch(r'\d{4}-\d{2}-\d{2}', plan['date']), 'Explicit build-note date required')
    return plan


def plan():
    return validate_plan(json.loads(PLAN.read_text()))


def register_previous():
    exact.SPECS[441] = dict(source=PREVIOUS_SOURCE, run=PREVIOUS_RUN, artifact=PREVIOUS_ARTIFACT,
                           digest=PREVIOUS_DIGEST, workflow=PREVIOUS_WORKFLOW, tests=3190, suites=463,
                           hashes=PREVIOUS_HASHES)


def validate_artifact(item, number, artifact_id, digest, run_id, source, name):
    exact.require(item['id'] == artifact_id and item['name'] == name and not item['expired']
                  and item['digest'] == 'sha256:' + digest and item['workflow_run']['id'] == run_id
                  and item['workflow_run']['head_sha'] == source, f'APD{number} artifact identity differs')


def verify_previous_acceptance():
    # These immutable records already accepted 441. Nothing here reruns or rebuilds 439/440/441.
    receipts = {}
    for path, digest in PREVIOUS_RECEIPTS.items():
        data = subprocess.check_output(['git', 'show', BASELINE + ':' + path])
        exact.require(hashlib.sha256(data).hexdigest() == digest, 'Previous acceptance receipt changed')
        receipts[path] = json.loads(data)
    delivered = receipts['docs/APD-441-DELIVERY-2026-11-11.json']
    exact.require(delivered['source_commit'] == PREVIOUS_SOURCE and delivered['original_run'] == PREVIOUS_RUN
                  and delivered['artifact_id'] == PREVIOUS_ARTIFACT and delivered['artifact_sha256'] == PREVIOUS_DIGEST
                  and delivered['source_archive_sha256'] == PREVIOUS_SOURCE_DIGEST
                  and delivered['manual_visual_review_complete'] is True and delivered['native_recovery_complete'] is True
                  and delivered['android_tests'] == 12 and delivered['source_scan_success'] is True
                  and delivered['saved_download_readback_sha256_verified'] is True, 'Previous 441 acceptance is incomplete')
    run = exact.api(f'actions/runs/{PREVIOUS_RUN}')
    exact.require(run['head_sha'] == PREVIOUS_SOURCE and run['path'] == PREVIOUS_WORKFLOW
                  and run['status'] == 'completed' and run['conclusion'] == 'success', 'Previous 441 producer differs')
    jobs = exact.api(f'actions/runs/{PREVIOUS_RUN}/jobs?per_page=100')['jobs']
    expected = {'build', 'native (Developer)', 'native (Alpha-Testers)', 'prepare-owner-review'}
    exact.require(len(jobs) == 4 and {job['name'] for job in jobs} == expected
                  and all(job['conclusion'] == 'success' for job in jobs), 'Previous 441 gates incomplete')
    built = exact.api(f'actions/artifacts/{PREVIOUS_ARTIFACT}')
    owner = exact.api(f'actions/artifacts/{PREVIOUS_OWNER}')
    validate_artifact(built, 441, PREVIOUS_ARTIFACT, PREVIOUS_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd441-built-pair')
    validate_artifact(owner, 441, PREVIOUS_OWNER, PREVIOUS_OWNER_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd441-owner-review-pair')
    scanned = exact.api(f'actions/runs/{PREVIOUS_SCAN["run"]}')
    exact.require(scanned['head_sha'] == PREVIOUS_SCAN['source']
                  and scanned['path'] == '.github/workflows/apd-439-441-source-scans.yml'
                  and scanned['status'] == 'completed' and scanned['conclusion'] == 'success', 'Previous exact source scan differs')
    scan_artifact = exact.api(f'actions/artifacts/{PREVIOUS_SCAN["artifact"]}')
    validate_artifact(scan_artifact, 441, PREVIOUS_SCAN['artifact'], PREVIOUS_SCAN['digest'],
                      PREVIOUS_SCAN['run'], PREVIOUS_SCAN['source'], 'apd441-prepared-source-scan-evidence')
    return dict(run=run, jobs=jobs, built=built, owner=owner, source_scan=scanned,
                source_scan_artifact=scan_artifact, receipt_hashes=PREVIOUS_RECEIPTS)


def application_paths(changed):
    metadata = ('.github/', 'docs/', 'tools/', 'core/src/test/', 'android/src/androidTest/',
                'core/src/main/assets/apd/history/')
    return {name for name in changed if name != 'AGENTS.md' and not name.startswith(metadata)}


def verify_previous_history(previous_apk, candidate_apk):
    prefix = 'assets/apd/history/'
    with zipfile.ZipFile(previous_apk) as previous, zipfile.ZipFile(candidate_apk) as candidate:
        previous_rows = previous.read(prefix + 'index.tsv').decode().splitlines()
        candidate_rows = candidate.read(prefix + 'index.tsv').decode().splitlines()
        exact.require(candidate_rows and candidate_rows[0].split('\t')[:2] == ['codex-test-442', 'Codex TEST 442']
                      and candidate_rows[1:] == previous_rows, 'Immediately previous compiled history index changed')
        for name in previous.namelist():
            if name.startswith(prefix) and name.endswith('.tsv') and name != prefix + 'index.tsv':
                exact.require(candidate.read(name) == previous.read(name), 'Previous compiled history record changed: ' + name)


def history(root):
    scope = plan()
    source, run_id = identity()
    subprocess.run(['git', 'merge-base', '--is-ancestor', BASELINE, source], check=True)
    subprocess.run(['git', 'merge-base', '--is-ancestor', PREVIOUS_SOURCE, source], check=True)
    from apd_owner_delivery_policy import require_apk_delivery_allowed
    require_apk_delivery_allowed(Path.cwd())
    exact.require(json.loads(exact.output(['gh', 'api', f'repos/{exact.REPO}']))['private'],
                  'Private repository required')
    existing = exact.api('actions/artifacts?name=apd442-built-pair&per_page=100')['artifacts']
    exact.require(not any(row['name'] == 'apd442-built-pair' for row in existing),
                  'A frozen APD442 pair already exists; recover it instead of assembling again')
    accepted = verify_previous_acceptance()
    register_previous()
    previous = Path('previous441')
    previous.mkdir(exist_ok=False)
    archive = previous / 'built-pair.zip'
    with archive.open('xb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{PREVIOUS_ARTIFACT}/zip'], stdout=stream, check=True)
    exact.require(exact.sha(archive) == PREVIOUS_DIGEST, 'Previous compiled APK archive changed')
    # APD441 is a candidate artifact with built/, source/ and preparation.json at its root.
    exact.extract_archive(archive, previous)
    _, previous_proof = exact.check_pair(previous / 'built', 441)
    exact.require(previous_proof['instrumentation_sha256'] == PREVIOUS_INSTRUMENTATION,
                  'Previous frozen instrumentation changed')
    exact.require(exact.sha(previous / 'source/APD-Source-441.zip') == PREVIOUS_SOURCE_DIGEST,
                  'Previous corresponding source changed')
    changed = exact.output(['git', 'diff', '--name-only', PREVIOUS_SOURCE, source]).splitlines()
    exact.require(application_paths(changed) == set(scope['application_paths']), 'Actual application delta differs from reviewed scope')
    from apd_build_history import merge_previous_apk, embed, validate
    merge_previous_apk(Path.cwd(), previous / 'built/owner-delivery/APD-Developer-441.apk', 'Codex TEST 441')
    ledger = dict(date=scope['date'], changes=[dict(row, paths=changed) for row in scope['changes']])
    validate(dict(status='ahead', files=[dict(filename=name) for name in changed], files_complete=True), ledger)
    embed(Path.cwd(), ledger, NUMBER)
    evidence = root / 'built/evidence'
    evidence.mkdir(parents=True, exist_ok=False)
    exact.write_json(evidence / 'source.json', dict(source_commit=source, previous_source=PREVIOUS_SOURCE,
                     accepted_baseline_head=BASELINE, changed_paths=changed))
    exact.write_json(root / 'previous441-acceptance.json', accepted)
    exact.write_json(root / 'changeset.json', ledger)


def check_provenance(proof, source, run_id, scope):
    exact.require(proof['number'] == NUMBER and proof['source_commit'] == source and proof['producing_run'] == run_id
                  and proof['workflow'] == WORKFLOW and proof['previous_source'] == PREVIOUS_SOURCE
                  and proof['previous_artifact_id'] == PREVIOUS_ARTIFACT and proof['previous_artifact_sha256'] == PREVIOUS_DIGEST
                  and proof['accepted_baseline_head'] == BASELINE and proof['scope_sha256'] == exact.sha(PLAN)
                  and proof['core_tests'] == scope['core_tests'] and proof['core_suites'] == scope['core_suites'],
                  'Candidate provenance differs from independent workflow and reviewed scope')
    exact.require(set(proof['apk_hashes']) == set(exact.FEATURES)
                  and all(re.fullmatch('[0-9a-f]{64}', value) for value in proof['apk_hashes'].values())
                  and re.fullmatch('[0-9a-f]{64}', proof['instrumentation_sha256']), 'Invalid candidate digests')


def configure(root):
    scope = plan()
    source, run_id = identity()
    proof = json.loads((root / 'built/evidence/candidate-provenance.json').read_text())
    check_provenance(proof, source, run_id, scope)
    fetched_file = root / 'fetched-artifact.json'
    fetched = json.loads(fetched_file.read_text()) if fetched_file.exists() else {}
    if fetched:
        validate_artifact(fetched, NUMBER, fetched['id'], fetched['digest'].removeprefix('sha256:'),
                          run_id, source, 'apd442-built-pair')
    exact.SPECS[NUMBER] = dict(source=source, run=run_id, workflow=WORKFLOW,
        tests=scope['core_tests'], suites=scope['core_suites'], hashes=proof['apk_hashes'],
        artifact=fetched.get('id'), digest=fetched.get('digest', '').removeprefix('sha256:'))
    exact.require(exact.sha(root / 'built/evidence/instrumentation.apk') == proof['instrumentation_sha256'],
                  'Frozen candidate instrumentation changed')
    return proof


def core():
    scope = plan()
    exact.SPECS[NUMBER] = dict(tests=scope['core_tests'], suites=scope['core_suites'])
    print(exact.check_core(Path('core/build/test-results/test'), NUMBER))


def freeze(root):
    scope = plan()
    source, run_id = identity()
    built = root / 'built'
    pair = built / 'owner-delivery'
    exact.require({path.name for path in pair.glob('*.apk')} == {f'APD-{v}-442.apk' for v in exact.FEATURES},
                  'Exactly one two-APK APD442 pair required')
    from apd_build_history import verify_compiled
    hashes, apks = {}, {}
    for variant in exact.FEATURES:
        path = pair / f'APD-{variant}-442.apk'
        verify_compiled(Path.cwd(), path)
        verify_previous_history(Path('previous441/built/owner-delivery/APD-Developer-441.apk'), path)
        hashes[variant] = exact.sha(path)
        apks[variant] = dict(sha256=hashes[variant], bytes=path.stat().st_size, cert=exact.CERT)
    shutil.copytree('core/build/test-results/test', built / 'core/build/test-results/test')
    proof = dict(number=NUMBER, source_commit=source, producing_run=run_id, workflow=WORKFLOW,
        previous_source=PREVIOUS_SOURCE, previous_artifact_id=PREVIOUS_ARTIFACT,
        previous_artifact_sha256=PREVIOUS_DIGEST, accepted_baseline_head=BASELINE, scope_sha256=exact.sha(PLAN),
        core_tests=scope['core_tests'], core_suites=scope['core_suites'], apk_hashes=hashes,
        instrumentation_sha256=exact.sha(built / 'evidence/instrumentation.apk'), new_application_apks_created=2)
    exact.write_json(built / 'evidence/apks.json', apks)
    exact.write_json(built / 'evidence/candidate-provenance.json', proof)
    configure(root)
    assets, verified = exact.check_pair(built, NUMBER)
    verified.update(new_application_apks_created=2, native_recovery_complete=False, manual_visual_review_complete=False)
    verified['source_archive_sha256'] = exact.source_archive(NUMBER, assets, verified, root / 'source')
    exact.write_json(root / 'preparation.json', verified)


def current_artifact(name, job_name):
    source, run_id = identity()
    run = exact.api(f'actions/runs/{run_id}')
    exact.require(run['head_sha'] == source and run['path'] == WORKFLOW, 'Candidate workflow identity differs')
    jobs = exact.api(f'actions/runs/{run_id}/jobs?per_page=100')['jobs']
    matches = [job for job in jobs if job['name'] == job_name]
    exact.require(len(matches) == 1 and matches[0]['conclusion'] == 'success', 'Producer gate did not succeed: ' + job_name)
    artifacts = exact.api(f'actions/runs/{run_id}/artifacts?name={name}&per_page=100')['artifacts']
    matches = [row for row in artifacts if row['name'] == name]
    exact.require(len(matches) == 1, 'Exactly one immutable artifact required: ' + name)
    artifact = matches[0]
    exact.require(re.fullmatch('sha256:[0-9a-f]{64}', artifact['digest']), 'Artifact digest is unavailable')
    validate_artifact(artifact, NUMBER, artifact['id'], artifact['digest'][7:], run_id, source, name)
    return artifact


def download_artifact(artifact, archive, destination):
    archive.parent.mkdir(parents=True, exist_ok=True)
    with archive.open('xb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{artifact["id"]}/zip'], stdout=stream, check=True)
    exact.require(exact.sha(archive) == artifact['digest'][7:], 'Downloaded immutable artifact changed')
    exact.extract_archive(archive, destination)


def fetch(root):
    artifact = current_artifact('apd442-built-pair', 'build')
    download_artifact(artifact, root / 'candidate-artifact.zip', root)
    exact.write_json(root / 'fetched-artifact.json', artifact)
    configure(root)
    exact.check_pair(root / 'built', NUMBER)


def validate_scan_record(record, root):
    source, run_id = identity()
    artifact = json.loads((root / 'fetched-artifact.json').read_text())
    prepared = json.loads((root / 'preparation.json').read_text())
    expected = dict(number=NUMBER, source_commit=source, artifact_run=run_id, artifact_head=source,
        artifact_name='apd442-built-pair', artifact_id=artifact['id'], artifact_sha256=artifact['digest'][7:],
        source_sha256=prepared['source_archive_sha256'])
    exact.require(record == expected, '442 source-scan record differs from independent candidate identity')
    exact.require(exact.sha(root / 'source/APD-Source-442.zip') == expected['source_sha256'], 'Candidate source ZIP changed')


def source_scan(root):
    # The existing request CLI remains limited to 439-441. This 442-only caller binds every field independently.
    fetch(root)
    source, run_id = identity()
    artifact = json.loads((root / 'fetched-artifact.json').read_text())
    prepared = json.loads((root / 'preparation.json').read_text())
    record = dict(number=NUMBER, source_commit=source, artifact_run=run_id, artifact_head=source,
        artifact_name='apd442-built-pair', artifact_id=artifact['id'], artifact_sha256=artifact['digest'][7:],
        source_sha256=prepared['source_archive_sha256'])
    validate_scan_record(record, root)
    exact.require(exact.sha(Path('tools/apd_qol_source_scan.py')) == SCANNER_SCRIPT_DIGEST,
                  'Existing source/history scan review rules changed')
    os.environ['APD_SCAN_BUILD_NUMBER'] = str(NUMBER)
    scans.scan(record, Path.cwd(), Path('source-scan-evidence/442'))


def fetch_scan(root):
    artifact = current_artifact('apd442-prepared-source-scan-evidence', 'source-scan')
    download_artifact(artifact, root / 'source-scan-artifact.zip', root / 'source-scan-evidence')
    exact.write_json(root / 'source-scan-artifact.json', artifact)


def check_scan(root):
    folder = root / 'source-scan-evidence'
    receipt = json.loads((folder / 'prepared-source-scan-verification.json').read_text())
    validate_scan_record(receipt['pins'], root)
    source, run_id = identity()
    prepared = json.loads((root / 'preparation.json').read_text())
    exact.require(receipt['status'] == 'success' and str(receipt['scan_workflow_run']) == str(run_id)
                  and receipt['scan_workflow_commit'] == source and receipt['source_findings_reviewed'] is True
                  and receipt['source_sha256'] == prepared['source_archive_sha256']
                  and receipt['scanner_version'] == scans.SCANNER_VERSION
                  and receipt['scanner_archive_sha256'] == scans.SCANNER_ARCHIVE_SHA256
                  and receipt['existing_scanner_script_sha256'] == SCANNER_SCRIPT_DIGEST
                  and receipt['applications_rebuilt'] is False and receipt['instrumentation_rebuilt'] is False,
                  'Exact corresponding-source scan did not pass unchanged review rules')
    report_path = folder / 'APD-Source-Scan.json'
    exact.require(exact.sha(report_path) == receipt['scan_report_sha256'], 'Source scan report changed')
    scans.validate_scan_report(json.loads(report_path.read_text()), prepared['source_archive_sha256'])
    return receipt


def require_images(folder):
    scope = plan()
    receipt = json.loads((folder / 'native-verification.json').read_text())
    transfers = {case[0]: json.loads((folder / (case[0] + '-transfer.json')).read_text()) for case in exact.CASES}
    check_transfers(receipt, transfers)
    required = set(RETAINED_IMAGES) | set(scope['additional_required_images'])
    exact.require(required <= set(receipt['images']), 'Existing or newly affected screenshots are missing')
    for name in required:
        exact.require(exact.sha(folder / 'visuals' / name) == receipt['images'][name], 'Required screenshot changed')


def native(root, variant, destination):
    configure(root)
    exact.native(NUMBER, variant, root, destination)
    require_images(destination)


def finalize(root, native_root, destination):
    configure(root)
    scanned = check_scan(root)
    checked = Path('checked-native442')
    for variant in exact.FEATURES:
        evidence = native_root / f'apd442-native-{variant}'
        require_images(evidence)
        shutil.copytree(evidence, checked / f'apd-recovery-442-native-{variant}')
    exact.finalize(NUMBER, root, checked, destination)
    receipt = destination / 'APD-442-Recovery-Verification.json'
    proof = json.loads(receipt.read_text())
    proof.update(new_application_apks_created=2, new_candidate_build=True, previous_source=PREVIOUS_SOURCE,
        previous_artifact_id=PREVIOUS_ARTIFACT, previous_artifact_sha256=PREVIOUS_DIGEST,
        producing_run=int(os.environ['GITHUB_RUN_ID']), scope_sha256=exact.sha(PLAN),
        required_affected_images=sorted(set(RETAINED_IMAGES) | set(plan()['additional_required_images'])),
        complete_remote_image_manifest_verified=True, source_scan_verified=True,
        source_scan_receipt_sha256=exact.sha(root / 'source-scan-evidence/prepared-source-scan-verification.json'),
        publication_authorized_by_this_workflow=False)
    exact.write_json(destination / 'APD-442-Verification.json', proof)
    exact.write_json(destination / 'APD-442-Source-Scan-Verification.json', scanned)
    receipt.unlink()
    print('APD442 exact pair: full core/compile/ABI/signature/history/source and 12 native cases passed; manual review remains required.')


def self_test():
    pending = json.loads(PLAN.read_text())
    validate_plan(pending, ready=False)
    good = copy.deepcopy(pending)
    good.update(status='ready_for_build', pending_scope_items=[], additional_required_images=['qol-fixture-normal.png'])
    validate_plan(good)
    rejected = 0
    for key, value in [('status', 'draft'), ('number', 441), ('base_head', '0' * 40),
                       ('core_tests', 3189), ('core_suites', 462), ('application_paths', []),
                       ('application_paths', ['../outside']), ('additional_required_images', []),
                       ('additional_required_images', ['../escape.png']), ('pending_scope_items', ['unresolved'])]:
        candidate = copy.deepcopy(good)
        candidate[key] = value
        try:
            validate_plan(candidate)
        except (RuntimeError, ValueError):
            rejected += 1
        else:
            raise AssertionError('Unsafe or unfinished candidate scope accepted: ' + key)
    exact.require(application_paths(['AGENTS.md', 'docs/x', 'tools/x', 'core/src/test/X.java',
                  'core/src/main/assets/messages/actors/actors.properties'])
                  == {'core/src/main/assets/messages/actors/actors.properties'}, 'Metadata path classification regressed')
    artifact = dict(id=PREVIOUS_ARTIFACT, name='apd441-built-pair', expired=False,
                    digest='sha256:' + PREVIOUS_DIGEST,
                    workflow_run=dict(id=PREVIOUS_RUN, head_sha=PREVIOUS_SOURCE))
    validate_artifact(artifact, 441, PREVIOUS_ARTIFACT, PREVIOUS_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd441-built-pair')
    for key, value in [('id', 1), ('name', 'apd440-built-pair'), ('expired', True), ('digest', 'sha256:' + '0' * 64)]:
        bad = dict(artifact, **{key: value})
        try:
            validate_artifact(bad, 441, PREVIOUS_ARTIFACT, PREVIOUS_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd441-built-pair')
        except RuntimeError:
            rejected += 1
        else:
            raise AssertionError('Changed previous artifact accepted: ' + key)
    source = 'a' * 40
    provenance = dict(number=NUMBER, source_commit=source, producing_run=1, workflow=WORKFLOW,
        previous_source=PREVIOUS_SOURCE, previous_artifact_id=PREVIOUS_ARTIFACT,
        previous_artifact_sha256=PREVIOUS_DIGEST, accepted_baseline_head=BASELINE,
        scope_sha256=exact.sha(PLAN), core_tests=good['core_tests'], core_suites=good['core_suites'],
        apk_hashes={variant: 'b' * 64 for variant in exact.FEATURES}, instrumentation_sha256='c' * 64)
    check_provenance(provenance, source, 1, good)
    for key, value in [('number', 441), ('source_commit', 'd' * 40), ('producing_run', 2),
                       ('workflow', 'wrong.yml'), ('previous_source', 'e' * 40), ('previous_artifact_id', 1),
                       ('previous_artifact_sha256', 'f' * 64), ('scope_sha256', '0' * 64), ('core_tests', 3189)]:
        try:
            check_provenance(dict(provenance, **{key: value}), source, 1, good)
        except RuntimeError:
            rejected += 1
        else:
            raise AssertionError('Changed candidate provenance accepted: ' + key)
    with tempfile.TemporaryDirectory() as directory:
        previous, current = Path(directory) / 'previous.apk', Path(directory) / 'current.apk'
        row = b'codex-test-441\tCodex TEST 441\t2026-11-11\tAPK change record\n'
        head = b'codex-test-442\tCodex TEST 442\t2026-11-11\tAPK change record\n'
        with zipfile.ZipFile(previous, 'w') as packed:
            packed.writestr('assets/apd/history/index.tsv', row)
            packed.writestr('assets/apd/history/codex-test-441.tsv', b'original record\n')
        for changed_record, changed_index in [(False, False), (True, False), (False, True)]:
            with zipfile.ZipFile(current, 'w') as packed:
                packed.writestr('assets/apd/history/index.tsv', head + (row.replace(b'441', b'440') if changed_index else row))
                packed.writestr('assets/apd/history/codex-test-441.tsv', b'changed\n' if changed_record else b'original record\n')
            try:
                verify_previous_history(previous, current)
            except RuntimeError:
                exact.require(changed_record or changed_index, 'Valid previous history was rejected')
                rejected += 1
            else:
                exact.require(not changed_record and not changed_index, 'Previous compiled history mutation accepted')
    print(f'{rejected} scope, previous-artifact and candidate-provenance corruption cases rejected.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('history', 'core', 'freeze', 'fetch', 'source-scan', 'fetch-scan', 'native', 'finalize', 'self-test'))
    parser.add_argument('--root', type=Path, default=Path('candidate442'))
    parser.add_argument('--variant', choices=tuple(exact.FEATURES))
    parser.add_argument('--output', type=Path, default=Path('native442'))
    parser.add_argument('--native-root', type=Path, default=Path('native-evidence'))
    args = parser.parse_args()
    if args.stage == 'self-test':
        self_test()
        return
    plan()  # No workflow stage runs while this scope remains a draft.
    if args.stage == 'core': core()
    elif args.stage == 'native':
        exact.require(args.variant, 'Native variant required')
        native(args.root, args.variant, args.output)
    elif args.stage == 'finalize': finalize(args.root, args.native_root, args.output)
    else: globals()[args.stage.replace('-', '_')](args.root)


if __name__ == '__main__':
    main()
