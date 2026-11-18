#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build APD446 while preserving accepted APD444 and the unaccepted APD445 compiled history."""
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
from apd_candidate441 import check_transfers, REQUIRED_IMAGES as APD441_IMAGES
import apd_scan_prepared_sources as scans

NUMBER = 446
BRANCH = 'codex/apd-phone-fixes-2026-10-09'
# Exact frozen APD445 evidence pins. APD445 is unaccepted and was not delivered.
# The unchanged APD445 helper separately verifies the last accepted owner pair, APD444.
BASELINE = 'a0c2755a7a668f8b2f5f4181be50a8d53f40f525'
WORKFLOW = '.github/workflows/apd-candidate-446.yml'
PLAN = Path('docs/APD-446-BUILD-PLAN.json')
PREVIOUS_SOURCE = 'a0c2755a7a668f8b2f5f4181be50a8d53f40f525'
PREVIOUS_RUN = 43072342086
PREVIOUS_WORKFLOW = '.github/workflows/apd-candidate-445.yml'
PREVIOUS_ARTIFACT = 13880080743
PREVIOUS_DIGEST = '7b6efc1ff834af2bba23df9716477ba0784c6283c4bfddddaf482f20b572b848'
PREVIOUS_SOURCE_DIGEST = 'cce08754cd7ad4ee74eeff0ff5afebe4372e4ad6810eb8d22b23b0e7d46e9ec8'
PREVIOUS_INSTRUMENTATION = '27e17427176444a9a8cfa9f2c016457eb21278d349bb4c721200ed441968f3b3'
PREVIOUS_HASHES = {'Developer': '03b76001cb44435f323b95665a26d2b8866f87b81d01af52e8b32bffa75eb787', 'Alpha-Testers': '497e4ca1e2c195c43c08cd7fc32e456ca08d0ef459d9d8e637092dec61570ef9'}
PREVIOUS_STATUS_RECEIPT = 'docs/APD-445-ACCEPTANCE-HOLD-2026-11-11.json'
PREVIOUS_MECHANICAL_RECEIPT = 'docs/APD-445-PRE-NATIVE-ACCEPTANCE-2026-11-11.json'
PREVIOUS_RECEIPTS = {PREVIOUS_STATUS_RECEIPT:'03a1f79c088a149a3371aa7e487aa46ef106893a4c60990b104e31717639696a',PREVIOUS_MECHANICAL_RECEIPT:'ccb80f0d69e5d22552685089951ff8b368191a1769309143badd7ae9a447265a'}
PREVIOUS_SCAN = dict(run=PREVIOUS_RUN,source=PREVIOUS_SOURCE,artifact=13880340744,digest='88b7178fd4438d100994e9af046e25d88b603e3d03cf97cae5a501003f4e9ffd')
SCANNER_SCRIPT_DIGEST = '436dcfc230412e39e53b95e7301a7a8c78228ebb63a7db5f1afca070026c7734'
PREVIOUS_RECEIPT_COMMIT = '57720443ec56d9d6776afb663ca4c821fab76956'
KNOWN_GOOD_GUARD_SHA256 = '81aceb908918984c192c839294baa474bdb36dca4c4743b8be74fae31a830005'
PREVIOUS_ADDITIONAL_IMAGES = frozenset({
    'qol-ordinary-talent-LINGERING_MAGIC-bottom.png',
    'qol-ordinary-talent-LINGERING_MAGIC.png',
    'qol-ordinary-talent-LIQUID_AGILITY-bottom.png',
    'qol-ordinary-talent-LIQUID_AGILITY.png',
    'qol-ordinary-talent-PROVOKED_ANGER-bottom.png',
    'qol-ordinary-talent-PROVOKED_ANGER.png',
    'qol-ordinary-talent-SPEEDY_STEALTH-bottom.png',
    'qol-ordinary-talent-SPEEDY_STEALTH.png',
    'qol-ordinary-talent-VETERANS_INTUITION-bottom.png',
    'qol-ordinary-talent-VETERANS_INTUITION.png',
    'trap-discovery-liquid-agility-excluded-strength-after.png',
    'trap-discovery-liquid-agility-excluded-strength-before.png',
    'trap-discovery-liquid-agility-rank1-after.png',
    'trap-discovery-liquid-agility-rank1-before.png',
    'trap-discovery-liquid-agility-rank2-after.png',
    'trap-discovery-liquid-agility-rank2-before.png',
    'trap-discovery-seer-shot-foreign-branch-hidden.png',
    'trap-discovery-seer-shot-side-branch-before.png',
    'trap-discovery-seer-shot-side-branch-revealed.png',
})
RETAINED_IMAGES = set(APD441_IMAGES) | PREVIOUS_ADDITIONAL_IMAGES
RETAINED_IMAGES |= {
    'qol-ordinary-talent-EMPOWERED_STRIKE.png',
    'qol-ordinary-talent-EMPOWERED_STRIKE-bottom.png',
    'qol-ordinary-talent-ARCANE_VISION.png',
    'qol-ordinary-talent-ARCANE_VISION-bottom.png',
}
RETAINED_IMAGES |= {'search-context-observed-wall-first.png', 'search-context-rogue-rank0.png', 'search-context-metamorph-rank2-square.png', 'seed-history-latest-unknown-selection.png', 'seed-history-matching-records.png', 'pylon-native-ranged-pylon-melee-boss.png', 'search-context-metamorph-unacquired.png', 'search-context-rogue-unacquired-radius.png', 'search-context-movement-first.png', 'search-context-other-floor-memory.png', 'seed-history-ranking-copy-result.png', 'search-context-unseen-wall.png', 'search-context-open-area.png', 'search-context-rogue-rank2-square.png', 'seed-history-cleared-generated-selection.png', 'search-context-crystal-choice.png', 'search-context-rogue-rank1-radius.png', 'search-context-explicit-movement.png', 'pylon-native-manual-rat-preserved.png', 'seed-history-entry.png', 'seed-history-ranking-copy-confirm.png', 'pylon-native-hidden-excluded.png', 'search-context-confirmed-wall.png', 'pylon-native-inactive-visible.png', 'search-context-rogue-rank1-rounded.png', 'search-context-metamorph-rank1-radius.png', 'search-context-explicit-wall.png', 'search-context-crystal-vault.png', 'search-context-metamorph-rank1-rounded.png', 'search-context-explicit-search.png', 'pylon-native-active-visible.png'}
BASE_NATIVE_CASES = exact.CASES
ADDITIONAL_NATIVE_CASES = (
    ('search-context', 'SearchContextDeviceTest', 'resumeChooserUsesObservedContextAndDispatchesExplicitActions', 600),
    ('seed-history', 'SeedHistoryDeviceTest', 'seedEntryShowsOnlyMatchingRecordedHistory', 300),
    ('pylon', 'BossPylonTargetingDeviceTest', 'nativeIndicatorsPreserveManualTargetsAndExcludeHiddenOrInactivePylons', 240),
)
NATIVE_CASES = BASE_NATIVE_CASES + ADDITIONAL_NATIVE_CASES


def configure_native_cases():
    exact.require(tuple(case[0] for case in BASE_NATIVE_CASES)
                  == ('qol', 'wall', 'seed', 'restart', 'gameplay', 'freeze'),
                  'The six accepted predecessor native cases changed')
    exact.require(len(NATIVE_CASES) == 9 and len({case[0] for case in NATIVE_CASES}) == 9
                  and len({(case[1], case[2]) for case in NATIVE_CASES}) == 9,
                  'The nine required native cases must be unique')
    exact.require(exact.CASES in (BASE_NATIVE_CASES, NATIVE_CASES), 'Unexpected native inventory override')
    exact.CASES = NATIVE_CASES


def require_previous_pins():
    exact.require(isinstance(BASELINE,str) and re.fullmatch('[0-9a-f]{40}',BASELINE),
                  'The frozen APD445 baseline commit is still pending')
    exact.require(isinstance(PREVIOUS_SOURCE,str) and re.fullmatch('[0-9a-f]{40}',PREVIOUS_SOURCE)
                  and type(PREVIOUS_RUN) is int and PREVIOUS_RUN>0
                  and PREVIOUS_WORKFLOW=='.github/workflows/apd-candidate-445.yml',
                  'The exact retained APD445 source/run/workflow is still pending')
    exact.require(type(PREVIOUS_ARTIFACT) is int and PREVIOUS_ARTIFACT>0,
                  'The exact retained APD445 built artifact is still pending')
    exact.require(set(PREVIOUS_HASHES)==set(exact.FEATURES),'Both retained APK hashes are required')
    digests=[PREVIOUS_DIGEST,PREVIOUS_SOURCE_DIGEST,PREVIOUS_INSTRUMENTATION,
             KNOWN_GOOD_GUARD_SHA256,*PREVIOUS_HASHES.values()]
    exact.require(all(isinstance(v,str) and re.fullmatch('[0-9a-f]{64}',v) for v in digests),
                  'A retained digest or accepted APD444 guard hash is still pending')
    exact.require(isinstance(PREVIOUS_RECEIPT_COMMIT,str) and re.fullmatch('[0-9a-f]{40}',PREVIOUS_RECEIPT_COMMIT),
                  'The exact unaccepted predecessor receipt commit is still pending')
    exact.require(isinstance(PREVIOUS_SCAN,dict) and set(PREVIOUS_SCAN)=={'run','source','artifact','digest'}
                  and PREVIOUS_SCAN['run']==PREVIOUS_RUN and PREVIOUS_SCAN['source']==PREVIOUS_SOURCE
                  and type(PREVIOUS_SCAN['artifact']) is int and PREVIOUS_SCAN['artifact']>0
                  and isinstance(PREVIOUS_SCAN['digest'],str) and re.fullmatch('[0-9a-f]{64}',PREVIOUS_SCAN['digest']),
                  'The retained exact source-scan identity is still pending')
    paths=[PREVIOUS_STATUS_RECEIPT,PREVIOUS_MECHANICAL_RECEIPT]
    exact.require(all(isinstance(p,str) and p.startswith('docs/') for p in paths)
                  and len(set(paths))==2 and set(PREVIOUS_RECEIPTS)==set(paths),
                  'The unaccepted status and mechanical-evidence paths are still pending')
    exact.require(all(isinstance(v,str) and re.fullmatch('[0-9a-f]{64}',v) for v in PREVIOUS_RECEIPTS.values()),
                  'The retained predecessor receipt digests are still pending')


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
                  and plan['number'] == NUMBER and plan['base_head'] == BASELINE, 'Wrong 446 scope schema or baseline')
    if ready:
        exact.require(plan['status'] == 'ready_for_build' and not plan['pending_scope_items'],
                      'APD446 is a draft; finalize the actual tested scope before requesting a build')
    exact.require(type(plan['core_tests']) is int and plan['core_tests'] >= 3244
                  and type(plan['core_suites']) is int and plan['core_suites'] >= 468,
                  'APD445 core coverage cannot be reduced')
    paths = plan['application_paths']
    exact.require(isinstance(paths, list) and paths and len(paths) == len(set(paths)), 'Exact application path list required')
    for name in paths:
        exact.safe_name(name)
        exact.require(name.startswith('core/src/main/') and not name.startswith('core/src/main/assets/apd/history/'),
                      '446 application scope must list the actual reviewed core paths')
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
    reviewed = validate_plan(json.loads(PLAN.read_text()))
    require_previous_pins()
    return reviewed


def register_previous():
    exact.SPECS[445] = dict(source=PREVIOUS_SOURCE, run=PREVIOUS_RUN, artifact=PREVIOUS_ARTIFACT,
                           digest=PREVIOUS_DIGEST, workflow=PREVIOUS_WORKFLOW, tests=3244, suites=468,
                           hashes=PREVIOUS_HASHES)


def validate_artifact(item, number, artifact_id, digest, run_id, source, name):
    exact.require(item['id'] == artifact_id and item['name'] == name and not item['expired']
                  and item['digest'] == 'sha256:' + digest and item['workflow_run']['id'] == run_id
                  and item['workflow_run']['head_sha'] == source, f'APD{number} artifact identity differs')


def verify_previous_evidence():
    # APD444 remains the last accepted/delivered owner build. The pinned, unchanged guard
    # rechecks its full acceptance. APD445 contributes only frozen compiled history.
    import apd_candidate445 as accepted444
    exact.require(exact.sha(Path(accepted444.__file__))==KNOWN_GOOD_GUARD_SHA256,
                  'The accepted APD444 prerequisite guard changed')
    accepted = accepted444.verify_previous_acceptance()
    receipts={}
    for path,digest in PREVIOUS_RECEIPTS.items():
        data=subprocess.check_output(['git','show',PREVIOUS_RECEIPT_COMMIT+':'+path])
        exact.require(hashlib.sha256(data).hexdigest()==digest,'Retained predecessor receipt changed')
        receipts[path]=json.loads(data)
    hold=receipts[PREVIOUS_STATUS_RECEIPT]
    exact.require(hold['build']==445 and hold['source_commit']==PREVIOUS_SOURCE
                  and hold['producing_run']==PREVIOUS_RUN and hold['run_attempt']==1
                  and hold['status']=='candidate_not_accepted_not_deliverable'
                  and hold['hold_resolved'] is False and hold['apk_delivery_authorized_by_this_receipt'] is False,
                  'APD445 must remain explicitly unaccepted and undelivered')
    mechanical=receipts[PREVIOUS_MECHANICAL_RECEIPT]
    exact.require(mechanical['number']==445 and mechanical['source_commit']==PREVIOUS_SOURCE
                  and mechanical['producing_run']==PREVIOUS_RUN and mechanical['run_attempt']==1
                  and mechanical['workflow']==PREVIOUS_WORKFLOW
                  and mechanical['status']=='passed_pre_native_archive_source_and_original_build_evidence',
                  'Retained APD445 mechanical evidence identity differs')
    core=mechanical['existing_core_evidence']
    exact.require(all(core[k]==v for k,v in dict(tests=3244,suites=468,failures=0,errors=0,skipped=0).items()),
                  'Retained complete core evidence differs')
    for key,identifier,digest in [('built_artifact',PREVIOUS_ARTIFACT,PREVIOUS_DIGEST),
                                  ('source_scan_artifact',PREVIOUS_SCAN['artifact'],PREVIOUS_SCAN['digest'])]:
        item=mechanical['artifacts'][key]
        exact.require(item['id']==identifier and item['sha256']==digest,'Retained artifact receipt differs')
    exact.require({v:mechanical['apk_files'][v]['sha256'] for v in exact.FEATURES}==PREVIOUS_HASHES
                  and mechanical['instrumentation_sha256']==PREVIOUS_INSTRUMENTATION
                  and mechanical['source_manifest']['source_sha256']==PREVIOUS_SOURCE_DIGEST,
                  'Retained compiled bytes differ')
    signature=mechanical['original_sdk_signer_and_identity_evidence']
    exact.require(signature['certificate_sha256']==exact.CERT and signature['application_id']==exact.APP
                  and signature['version_code']==920,'Original signer/application identity differs')
    exact.require(mechanical['source_scan']['source_findings_reviewed'] is True
                  and mechanical['source_scan']['history_not_rewritten'] is True
                  and mechanical['compiled_history_and_player_notes']['both_previous_compiled_histories_preserved'] is True,
                  'Retained source/history checks are incomplete')
    run=exact.api(f'actions/runs/{PREVIOUS_RUN}')
    exact.require(run['head_sha']==PREVIOUS_SOURCE and run['path']==PREVIOUS_WORKFLOW
                  and run['run_attempt']==1 and run['status'] in {'in_progress','completed'},
                  'The retained APD445 producing identity differs; never rebuild it')
    jobs=exact.api(f'actions/runs/{PREVIOUS_RUN}/jobs?per_page=100')['jobs']
    expected={'build','native (Developer)','native (Alpha-Testers)','source-scan','prepare-owner-review'}
    names={j['name'] for j in jobs}
    exact.require(len(names)==len(jobs) and names<=expected and {'build','source-scan'}<=names,
                  'Retained producer job inventory differs')
    # Native diagnostics of this unaccepted build may still be running. They cannot establish
    # acceptance here and are not a dependency of its already frozen, signed compiled history.
    exact.require(all(j['status']=='completed' and j['conclusion']=='success'
                      for j in jobs if j['name'] in {'build','source-scan'}),
                  'Retained compile/signature/source jobs did not pass')
    built=exact.api(f'actions/artifacts/{PREVIOUS_ARTIFACT}')
    scanned=exact.api(f'actions/artifacts/{PREVIOUS_SCAN["artifact"]}')
    validate_artifact(built,445,PREVIOUS_ARTIFACT,PREVIOUS_DIGEST,PREVIOUS_RUN,PREVIOUS_SOURCE,'apd445-built-pair')
    validate_artifact(scanned,445,PREVIOUS_SCAN['artifact'],PREVIOUS_SCAN['digest'],PREVIOUS_RUN,PREVIOUS_SOURCE,
                      'apd445-prepared-source-scan-evidence')
    return dict(accepted_owner_baseline_number=444,accepted_owner_baseline=accepted,
                retained_compiled_predecessor_number=445,retained_predecessor_accepted=False,
                retained_predecessor_delivered=False,retained_native_acceptance_claimed=False,
                run=run,jobs=jobs,built=built,
                source_scan_artifact=scanned,receipt_commit=PREVIOUS_RECEIPT_COMMIT,receipt_hashes=PREVIOUS_RECEIPTS)


def application_paths(changed):
    metadata = ('.github/', 'docs/', 'tools/', 'core/src/test/', 'android/src/androidTest/',
                'core/src/main/assets/apd/history/')
    return {name for name in changed if name != 'AGENTS.md' and not name.startswith(metadata)}


def verify_previous_history(previous_apk, candidate_apk):
    prefix = 'assets/apd/history/'
    with zipfile.ZipFile(previous_apk) as previous, zipfile.ZipFile(candidate_apk) as candidate:
        previous_rows = previous.read(prefix + 'index.tsv').decode().splitlines()
        candidate_rows = candidate.read(prefix + 'index.tsv').decode().splitlines()
        exact.require(candidate_rows and candidate_rows[0].split('\t')[:2] == ['codex-test-446', 'Codex TEST 446']
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
    existing = exact.api('actions/artifacts?name=apd446-built-pair&per_page=100')['artifacts']
    exact.require(not any(row['name'] == 'apd446-built-pair' for row in existing),
                  'A frozen APD446 pair already exists; recover it instead of assembling again')
    accepted = verify_previous_evidence()
    register_previous()
    previous = Path('previous445')
    previous.mkdir(exist_ok=False)
    archive = previous / 'built-pair.zip'
    with archive.open('xb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{PREVIOUS_ARTIFACT}/zip'], stdout=stream, check=True)
    exact.require(exact.sha(archive) == PREVIOUS_DIGEST, 'Previous compiled APK archive changed')
    # APD445 is a candidate artifact with built/, source/ and preparation.json at its root.
    exact.extract_archive(archive, previous)
    _, previous_proof = exact.check_pair(previous / 'built', 445)
    exact.require(previous_proof['instrumentation_sha256'] == PREVIOUS_INSTRUMENTATION,
                  'Previous frozen instrumentation changed')
    exact.require(exact.sha(previous / 'source/APD-Source-445.zip') == PREVIOUS_SOURCE_DIGEST,
                  'Previous corresponding source changed')
    changed = exact.output(['git', 'diff', '--name-only', PREVIOUS_SOURCE, source]).splitlines()
    exact.require(application_paths(changed) == set(scope['application_paths']), 'Actual application delta differs from reviewed scope')
    from apd_build_history import merge_previous_apk, embed, validate
    merge_previous_apk(Path.cwd(), previous / 'built/owner-delivery/APD-Developer-445.apk', 'Codex TEST 445')
    ledger = dict(date=scope['date'], changes=[dict(row, paths=changed) for row in scope['changes']])
    validate(dict(status='ahead', files=[dict(filename=name) for name in changed], files_complete=True), ledger)
    embed(Path.cwd(), ledger, NUMBER)
    evidence = root / 'built/evidence'
    evidence.mkdir(parents=True, exist_ok=False)
    exact.write_json(evidence / 'source.json', dict(source_commit=source, previous_source=PREVIOUS_SOURCE,
                     retained_baseline_head=BASELINE, changed_paths=changed))
    exact.write_json(root / 'previous445-retained-evidence.json', accepted)
    exact.write_json(root / 'changeset.json', ledger)


def check_provenance(proof, source, run_id, scope):
    exact.require(proof['number'] == NUMBER and proof['source_commit'] == source and proof['producing_run'] == run_id
                  and proof['workflow'] == WORKFLOW and proof['previous_source'] == PREVIOUS_SOURCE
                  and proof['previous_artifact_id'] == PREVIOUS_ARTIFACT and proof['previous_artifact_sha256'] == PREVIOUS_DIGEST
                  and proof['retained_baseline_head'] == BASELINE and proof['scope_sha256'] == exact.sha(PLAN)
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
                          run_id, source, 'apd446-built-pair')
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
    exact.require({path.name for path in pair.glob('*.apk')} == {f'APD-{v}-446.apk' for v in exact.FEATURES},
                  'Exactly one two-APK APD446 pair required')
    from apd_build_history import verify_compiled
    hashes, apks = {}, {}
    for variant in exact.FEATURES:
        path = pair / f'APD-{variant}-446.apk'
        verify_compiled(Path.cwd(), path)
        verify_previous_history(Path('previous445/built/owner-delivery/APD-Developer-445.apk'), path)
        hashes[variant] = exact.sha(path)
        apks[variant] = dict(sha256=hashes[variant], bytes=path.stat().st_size, cert=exact.CERT)
    shutil.copytree('core/build/test-results/test', built / 'core/build/test-results/test')
    proof = dict(number=NUMBER, source_commit=source, producing_run=run_id, workflow=WORKFLOW,
        previous_source=PREVIOUS_SOURCE, previous_artifact_id=PREVIOUS_ARTIFACT,
        previous_artifact_sha256=PREVIOUS_DIGEST, retained_baseline_head=BASELINE, scope_sha256=exact.sha(PLAN),
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
    artifact = current_artifact('apd446-built-pair', 'build')
    download_artifact(artifact, root / 'candidate-artifact.zip', root)
    exact.write_json(root / 'fetched-artifact.json', artifact)
    configure(root)
    exact.check_pair(root / 'built', NUMBER)


def validate_scan_record(record, root):
    source, run_id = identity()
    artifact = json.loads((root / 'fetched-artifact.json').read_text())
    prepared = json.loads((root / 'preparation.json').read_text())
    expected = dict(number=NUMBER, source_commit=source, artifact_run=run_id, artifact_head=source,
        artifact_name='apd446-built-pair', artifact_id=artifact['id'], artifact_sha256=artifact['digest'][7:],
        source_sha256=prepared['source_archive_sha256'])
    exact.require(record == expected, '446 source-scan record differs from independent candidate identity')
    exact.require(exact.sha(root / 'source/APD-Source-446.zip') == expected['source_sha256'], 'Candidate source ZIP changed')


def source_scan(root):
    # The existing request CLI remains limited to 439-441. This 446-only caller binds every field independently.
    fetch(root)
    source, run_id = identity()
    artifact = json.loads((root / 'fetched-artifact.json').read_text())
    prepared = json.loads((root / 'preparation.json').read_text())
    record = dict(number=NUMBER, source_commit=source, artifact_run=run_id, artifact_head=source,
        artifact_name='apd446-built-pair', artifact_id=artifact['id'], artifact_sha256=artifact['digest'][7:],
        source_sha256=prepared['source_archive_sha256'])
    validate_scan_record(record, root)
    exact.require(exact.sha(Path('tools/apd_qol_source_scan.py')) == SCANNER_SCRIPT_DIGEST,
                  'Existing source/history scan review rules changed')
    os.environ['APD_SCAN_BUILD_NUMBER'] = str(NUMBER)
    scans.scan(record, Path.cwd(), Path('source-scan-evidence/446'))


def fetch_scan(root):
    artifact = current_artifact('apd446-prepared-source-scan-evidence', 'source-scan')
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
    configure_native_cases()
    scope = plan()
    receipt = json.loads((folder / 'native-verification.json').read_text())
    transfers = {case[0]: json.loads((folder / (case[0] + '-transfer.json')).read_text()) for case in exact.CASES}
    check_transfers(receipt, transfers)
    required = set(RETAINED_IMAGES) | set(scope['additional_required_images'])
    exact.require(required <= set(receipt['images']), 'Existing or newly affected screenshots are missing')
    for name in required:
        exact.require(exact.sha(folder / 'visuals' / name) == receipt['images'][name], 'Required screenshot changed')


def native(root, variant, destination):
    configure_native_cases()
    configure(root)
    exact.native(NUMBER, variant, root, destination)
    require_images(destination)


def finalize(root, native_root, destination):
    configure_native_cases()
    configure(root)
    scanned = check_scan(root)
    checked = Path('checked-native446')
    for variant in exact.FEATURES:
        evidence = native_root / f'apd446-native-{variant}'
        require_images(evidence)
        shutil.copytree(evidence, checked / f'apd-recovery-446-native-{variant}')
    exact.finalize(NUMBER, root, checked, destination)
    receipt = destination / 'APD-446-Recovery-Verification.json'
    proof = json.loads(receipt.read_text())
    proof.update(new_application_apks_created=2, new_candidate_build=True, previous_source=PREVIOUS_SOURCE,
        previous_artifact_id=PREVIOUS_ARTIFACT, previous_artifact_sha256=PREVIOUS_DIGEST,
        producing_run=int(os.environ['GITHUB_RUN_ID']), scope_sha256=exact.sha(PLAN),
        required_affected_images=sorted(set(RETAINED_IMAGES) | set(plan()['additional_required_images'])),
        complete_remote_image_manifest_verified=True, source_scan_verified=True,
        source_scan_receipt_sha256=exact.sha(root / 'source-scan-evidence/prepared-source-scan-verification.json'),
        publication_authorized_by_this_workflow=False)
    exact.write_json(destination / 'APD-446-Verification.json', proof)
    exact.write_json(destination / 'APD-446-Source-Scan-Verification.json', scanned)
    receipt.unlink()
    print('APD446 exact pair: full core/compile/ABI/signature/history/source and 18 native cases passed; manual review remains required.')


def _self_test_with_fixture_pins():
    from unittest.mock import patch
    pending = json.loads(PLAN.read_text())
    validate_plan(pending, ready=False)
    good = copy.deepcopy(pending)
    good.update(status='ready_for_build', pending_scope_items=[], additional_required_images=['qol-fixture-normal.png'])
    validate_plan(good)
    rejected = 0
    pins = dict(BASELINE='a' * 40, PREVIOUS_RECEIPT_COMMIT='a' * 40, KNOWN_GOOD_GUARD_SHA256='b' * 64,
                PREVIOUS_INSTRUMENTATION='c' * 64,
                PREVIOUS_HASHES={variant: 'd' * 64 for variant in exact.FEATURES},
                PREVIOUS_STATUS_RECEIPT='docs/fixture-delivery.json',
                PREVIOUS_MECHANICAL_RECEIPT='docs/fixture-manual.json',
                PREVIOUS_RECEIPTS={'docs/fixture-delivery.json': 'e' * 64,
                                   'docs/fixture-manual.json': 'f' * 64})
    with patch.dict(globals(), pins):
        require_previous_pins()
        for key, value in [('BASELINE', None), ('PREVIOUS_SOURCE', None), ('PREVIOUS_RUN', None),
                           ('PREVIOUS_WORKFLOW', '.github/workflows/unrelated.yml'),
                           ('PREVIOUS_ARTIFACT', None), ('PREVIOUS_DIGEST', None),
                           ('PREVIOUS_RECEIPT_COMMIT', None), ('KNOWN_GOOD_GUARD_SHA256', None),
                           ('PREVIOUS_SOURCE_DIGEST', None),
                           ('PREVIOUS_INSTRUMENTATION', None),
                           ('PREVIOUS_HASHES', {variant: None for variant in exact.FEATURES}),
                           ('PREVIOUS_HASHES', {'Developer': 'd' * 64}),
                           ('PREVIOUS_STATUS_RECEIPT', None), ('PREVIOUS_MECHANICAL_RECEIPT', None),
                           ('PREVIOUS_RECEIPTS', {}), ('PREVIOUS_SCAN', {}),
                           ('PREVIOUS_SCAN', dict(PREVIOUS_SCAN, artifact=None)),
                           ('PREVIOUS_SCAN', dict(PREVIOUS_SCAN, digest=None)),
                           ('PREVIOUS_SCAN', dict(PREVIOUS_SCAN, run=1)),
                           ('PREVIOUS_SCAN', dict(PREVIOUS_SCAN, source='0' * 40))]:
            with patch.dict(globals(), {key: value}):
                try:
                    require_previous_pins()
                except RuntimeError:
                    rejected += 1
                else:
                    raise AssertionError('Incomplete predecessor evidence accepted: ' + key)
    for key, value in [('status', 'draft'), ('number', 445), ('base_head', '0' * 40),
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
    artifact = dict(id=PREVIOUS_ARTIFACT, name='apd445-built-pair', expired=False,
                    digest='sha256:' + PREVIOUS_DIGEST,
                    workflow_run=dict(id=PREVIOUS_RUN, head_sha=PREVIOUS_SOURCE))
    validate_artifact(artifact, 445, PREVIOUS_ARTIFACT, PREVIOUS_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd445-built-pair')
    for key, value in [('id', 1), ('name', 'apd440-built-pair'), ('expired', True), ('digest', 'sha256:' + '0' * 64)]:
        bad = dict(artifact, **{key: value})
        try:
            validate_artifact(bad, 445, PREVIOUS_ARTIFACT, PREVIOUS_DIGEST, PREVIOUS_RUN, PREVIOUS_SOURCE, 'apd445-built-pair')
        except RuntimeError:
            rejected += 1
        else:
            raise AssertionError('Changed previous artifact accepted: ' + key)
    source = 'a' * 40
    provenance = dict(number=NUMBER, source_commit=source, producing_run=1, workflow=WORKFLOW,
        previous_source=PREVIOUS_SOURCE, previous_artifact_id=PREVIOUS_ARTIFACT,
        previous_artifact_sha256=PREVIOUS_DIGEST, retained_baseline_head=BASELINE,
        scope_sha256=exact.sha(PLAN), core_tests=good['core_tests'], core_suites=good['core_suites'],
        apk_hashes={variant: 'b' * 64 for variant in exact.FEATURES}, instrumentation_sha256='c' * 64)
    check_provenance(provenance, source, 1, good)
    for key, value in [('number', 445), ('source_commit', 'd' * 40), ('producing_run', 2),
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
        row = b'codex-test-445\tCodex TEST 445\t2026-11-11\tAPK change record\n'
        head = b'codex-test-446\tCodex TEST 446\t2026-11-11\tAPK change record\n'
        with zipfile.ZipFile(previous, 'w') as packed:
            packed.writestr('assets/apd/history/index.tsv', row)
            packed.writestr('assets/apd/history/codex-test-445.tsv', b'original record\n')
        for changed_record, changed_index in [(False, False), (True, False), (False, True)]:
            with zipfile.ZipFile(current, 'w') as packed:
                packed.writestr('assets/apd/history/index.tsv', head + (row.replace(b'445', b'440') if changed_index else row))
                packed.writestr('assets/apd/history/codex-test-445.tsv', b'changed\n' if changed_record else b'original record\n')
            try:
                verify_previous_history(previous, current)
            except RuntimeError:
                exact.require(changed_record or changed_index, 'Valid previous history was rejected')
                rejected += 1
            else:
                exact.require(not changed_record and not changed_index, 'Previous compiled history mutation accepted')
    print(f'{rejected} scope, previous-artifact and candidate-provenance corruption cases rejected.')


def self_test():
    """Exercise guards with explicit fixtures; pending predecessor pins stay unset."""
    from unittest.mock import patch
    pending = json.loads(PLAN.read_text())
    validate_plan(pending, ready=False)
    pins = dict(BASELINE='a' * 40, PREVIOUS_ARTIFACT=4001, PREVIOUS_DIGEST='1' * 64,
                PREVIOUS_RECEIPT_COMMIT='a' * 40, KNOWN_GOOD_GUARD_SHA256='2' * 64,
                PREVIOUS_SOURCE_DIGEST='3' * 64, PREVIOUS_INSTRUMENTATION='4' * 64,
                PREVIOUS_HASHES={variant: '5' * 64 for variant in exact.FEATURES},
                PREVIOUS_RECEIPTS={PREVIOUS_STATUS_RECEIPT: '6' * 64,
                                   PREVIOUS_MECHANICAL_RECEIPT: '7' * 64},
                PREVIOUS_SCAN=dict(run=PREVIOUS_RUN, source=PREVIOUS_SOURCE,
                                   artifact=4003, digest='8' * 64))
    # Never write fixture pins into the draft or claim these records are actual APD445 evidence.
    with tempfile.TemporaryDirectory() as directory:
        fixture_plan = Path(directory) / 'scope.json'
        pending['base_head'] = pins['BASELINE']
        fixture_plan.write_text(json.dumps(pending))
        with patch.dict(globals(), dict(pins, PLAN=fixture_plan)):
            _self_test_with_fixture_pins()

    original = exact.CASES
    try:
        exact.CASES = BASE_NATIVE_CASES
        configure_native_cases()
        configure_native_cases()  # Repeated image/native/finalize checks retain the same inventory.
        exact.require(exact.CASES == NATIVE_CASES and len(exact.CASES) * len(exact.FEATURES) == 18,
                      'Configured native inventory differs')
        for broken in (NATIVE_CASES[:-1], NATIVE_CASES[:-1] + (NATIVE_CASES[0],)):
            with patch.dict(globals(), NATIVE_CASES=broken):
                try:
                    configure_native_cases()
                except RuntimeError:
                    pass
                else:
                    raise AssertionError('Missing or duplicate native case accepted')
        exact.CASES = BASE_NATIVE_CASES[:-1]
        try:
            configure_native_cases()
        except RuntimeError:
            pass
        else:
            raise AssertionError('Unexpected native inventory override accepted')
    finally:
        exact.CASES = original
    print('Three native-inventory corruption cases rejected; all nine exact cases retained.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('history', 'core', 'freeze', 'fetch', 'source-scan', 'fetch-scan', 'native', 'finalize', 'self-test'))
    parser.add_argument('--root', type=Path, default=Path('candidate446'))
    parser.add_argument('--variant', choices=tuple(exact.FEATURES))
    parser.add_argument('--output', type=Path, default=Path('native446'))
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
