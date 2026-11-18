#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Repeat only original APD440 Alpha's six cases with complete, serialized image transfer."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import threading

import apd_recover439_440 as exact

RECOVERY_RUN = 43021022953
RECOVERY_SOURCE = '45b4520ced7bf719db2678bdb123f0de88de5019'
RECOVERY_WORKFLOW = '.github/workflows/apd-recover439-440.yml'
WORKFLOW = '.github/workflows/apd-recover440-alpha.yml'
BRANCH = 'codex/apd-phone-fixes-2026-10-09'
INSTRUMENTATION = 'e94c9e88ff655e5fb5a8db2bf7b3623f3514b591fdf8f27ccd6d7d71c4f5fd07'
SOURCE_ARCHIVE = 'c934cc74b8d6d4c995442e3ae86a7f16c72737d153b03407bfc86d7702a4f571'
REMOTE = '/sdcard/Pictures/APD-Test-Visuals'
ARTIFACTS = {
    'prepared': (13854014441, 'apd-recovery-440-original-pair-and-source',
                 'fa559df5a1faa29b84f3e748bfe68f7b12e097910f5ef8303e18885f01db64c6'),
    'developer': (13855504101, 'apd-recovery-440-native-Developer',
                  '53b9fec06a13d065727d5db26ba70fffe39974beae566b504e92603ff546bc8d'),
    'accepted439': (13855649249, 'apd-recovery-439-owner-review-pair',
                    '721b390d5fe6ff2606d1369e22f1ad0ecb5ba80f6a9439080bb0629b9cd3a49d'),
    'failed_alpha': (13855693372, 'apd-recovery-440-native-Alpha-Testers',
                     '47392dd591da64aee0f9787e65d3bc7756c04387eed888ee87a506dc31ab6bff'),
}


def check_artifact(value, key):
    identity, name, digest = ARTIFACTS[key]
    exact.require(value['id'] == identity and value['name'] == name
                  and value['digest'] == 'sha256:' + digest and not value['expired']
                  and value['workflow_run']['id'] == RECOVERY_RUN
                  and value['workflow_run']['head_sha'] == RECOVERY_SOURCE
                  and value['workflow_run']['head_branch'] == BRANCH,
                  'Pinned recovery artifact identity differs: ' + key)


def fetch(root, key, destination):
    identity, _, digest = ARTIFACTS[key]
    metadata = exact.api(f'actions/artifacts/{identity}')
    check_artifact(metadata, key)
    archive = root / (key + '.zip')
    with archive.open('wb') as stream:
        subprocess.run(['gh', 'api', f'repos/{exact.REPO}/actions/artifacts/{identity}/zip'],
                       stdout=stream, check=True)
    exact.require(exact.sha(archive) == digest, 'Recovery download digest differs: ' + key)
    exact.extract_archive(archive, destination)
    return metadata


def evidence_path(root, variant):
    return root / 'native' / ('apd-recovery-440-native-' + variant)


def check_native(folder, variant):
    proof = json.loads((folder / 'native-verification.json').read_text())
    exact.require(proof['number'] == 440 and proof['variant'] == variant
                  and proof['source_commit'] == exact.SPECS[440]['source']
                  and proof['apk_sha256'] == exact.SPECS[440]['hashes'][variant]
                  and proof['instrumentation_sha256'] == INSTRUMENTATION
                  and proof['completed_cases'] == [case[0] for case in exact.CASES]
                  and proof['native_recovery_complete'] is True
                  and proof['application_rebuilt'] is False and proof['instrumentation_rebuilt'] is False,
                  'Native identity or complete case set differs')
    for label, short_class, method, _ in exact.CASES:
        exact.check_instrumentation((folder / (label + '.txt')).read_text(), exact.CLASS_PREFIX + short_class, method)
    images = exact.check_images(folder / 'visuals')
    exact.require(images == proof['images'] and len(images) == 1408, 'Complete native image evidence differs')
    exact.require((folder / 'native-logcat.txt').stat().st_size > 0, 'Native logcat missing')
    return proof


def check_prepared(root):
    prepared = root / 'prepared'
    _, checked = exact.check_pair(prepared / 'built', 440)
    proof = json.loads((prepared / 'preparation.json').read_text())
    exact.require(checked['instrumentation_sha256'] == INSTRUMENTATION
                  and proof['instrumentation_sha256'] == INSTRUMENTATION
                  and proof['source_archive_sha256'] == SOURCE_ARCHIVE
                  and exact.sha(prepared / 'source/APD-Source-440.zip') == SOURCE_ARCHIVE,
                  'Frozen instrumentation or corresponding source differs')
    return checked


def prepare(root):
    exact.require(os.environ['GITHUB_REPOSITORY'] == exact.REPO
                  and os.environ['GITHUB_REF'] == 'refs/heads/' + BRANCH, 'Wrong repository or branch')
    exact.require(json.loads(exact.output(['gh', 'api', f'repos/{exact.REPO}']))['private'],
                  'Private repository required')
    from apd_owner_delivery_policy import require_apk_delivery_allowed
    require_apk_delivery_allowed(Path.cwd())
    run = exact.api(f'actions/runs/{RECOVERY_RUN}')
    exact.require(run['head_sha'] == RECOVERY_SOURCE and run['path'] == RECOVERY_WORKFLOW
                  and run['status'] == 'completed' and run['conclusion'] == 'failure',
                  'Original partial recovery identity changed')
    jobs = exact.api(f'actions/runs/{RECOVERY_RUN}/jobs?per_page=100')['jobs']
    expected = {'prepare (439)': 'success', 'prepare (440)': 'success',
                'native (439, Developer)': 'success', 'native (439, Alpha-Testers)': 'success',
                'native (440, Developer)': 'success', 'native (440, Alpha-Testers)': 'failure',
                'package (439)': 'success', 'package (440)': 'failure'}
    exact.require(len(jobs) == 8 and {job['name']: job['conclusion'] for job in jobs} == expected,
                  'Retained recovery gates or original failure history differ')
    root.mkdir(parents=True, exist_ok=False)
    artifacts = {}
    for key, target in [('prepared', root / 'prepared'), ('developer', evidence_path(root, 'Developer')),
                        ('accepted439', root / 'accepted439')]:
        artifacts[key] = fetch(root, key, target)
    # Preserve the original failed artifact by reference. Its partial evidence is never used for acceptance.
    artifacts['failed_alpha'] = exact.api(f'actions/artifacts/{ARTIFACTS["failed_alpha"][0]}')
    check_artifact(artifacts['failed_alpha'], 'failed_alpha')
    check_prepared(root)
    check_native(evidence_path(root, 'Developer'), 'Developer')
    accepted = json.loads((root / 'accepted439/APD-439-Recovery-Verification.json').read_text())
    exact.require(accepted['number'] == 439 and accepted['source_commit'] == exact.SPECS[439]['source']
                  and str(accepted['recovery_run']) == str(RECOVERY_RUN)
                  and accepted['recovery_commit'] == RECOVERY_SOURCE and accepted['android_tests'] == 12
                  and accepted['native_recovery_complete'] is True
                  and accepted['application_rebuilt'] is False and accepted['instrumentation_rebuilt'] is False,
                  'Accepted APD439 recovery differs')
    for variant, digest in exact.SPECS[439]['hashes'].items():
        exact.require(exact.sha(root / 'accepted439' / f'APD-{variant}-439.apk') == digest,
                      'Accepted APD439 APK changed')
    exact.require(exact.sha(root / 'accepted439/APD-Source-439.zip') == accepted['source_archive_sha256'],
                  'Accepted APD439 source changed')
    exact.write_json(root / 'input-provenance.json', dict(original_run=run, original_jobs=jobs,
        artifacts=artifacts, retry_commit=os.environ['GITHUB_SHA'], retry_run=int(os.environ['GITHUB_RUN_ID']),
        retry_workflow=WORKFLOW, original_failed_evidence_retained=True))
    print('Frozen original 440 pair, six successful Developer cases, and accepted 439 package verified.', flush=True)


def image_name(name):
    # Test output includes Java nested-class '$' and MediaStore duplicate ' (1)' names.
    exact.require(bool(re.fullmatch(r'[A-Za-z0-9_$(). -]+\.png', name)) and '/' not in name
                  and '\\' not in name and not name.startswith('.'), 'Unsafe screenshot filename')
    return name


def parse_manifest(text):
    result = {}
    for line in text.splitlines():
        match = re.fullmatch(r'([0-9a-f]{64})  (.+)', line)
        exact.require(match is not None, 'Malformed device screenshot digest')
        name = image_name(match.group(2))
        exact.require(name not in result, 'Duplicate device screenshot name')
        result[name] = match.group(1)
    exact.require(result, 'Empty device screenshot manifest')
    return result


def complete_inventory(previous, current):
    exact.require(len(previous) == 1408 and set(current) == set(previous),
                  'Alpha screenshot inventory differs from complete original suite')


def pull_image(directory, name, digest=None):
    name = image_name(name)
    target = directory / name
    if target.exists() and (digest is None or exact.sha(target) == digest):
        return
    temporary = directory / (name + '.partial')
    try:
        subprocess.run(['adb', 'pull', REMOTE + '/' + name, str(temporary)],
                       stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, timeout=30, check=True)
        exact.validate_png(temporary.read_bytes())
        exact.require(digest is None or exact.sha(temporary) == digest, 'Transferred screenshot digest differs')
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)


class IncrementalImages:
    """One transfer at a time, stopped before mandatory complete manifest synchronization."""
    def __init__(self, directory):
        self.directory = directory
        self.directory.mkdir(parents=True, exist_ok=True)
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self.collect, daemon=True)

    def collect(self):
        while not self.stop.wait(5):
            try:
                names = exact.output(['adb', 'shell', 'ls', '-1', REMOTE],
                                     stderr=subprocess.DEVNULL, timeout=15).splitlines()
                for name in names:
                    if self.stop.is_set():
                        break
                    pull_image(self.directory, name)
            except (subprocess.SubprocessError, OSError, RuntimeError, ValueError):
                # A screenshot being written may not be complete yet; mandatory final synchronization verifies it.
                pass

    def close(self):
        self.stop.set()
        self.thread.join(timeout=50)
        exact.require(not self.thread.is_alive(), 'Background screenshot transfer did not stop')


def sync_images(directory):
    # The instrumentation has finished, so the remote set and bytes must remain stable.
    command = 'cd ' + REMOTE + ' && sha256sum -- *.png'
    before = parse_manifest(exact.output(['adb', 'shell', command], timeout=60))
    for name, digest in before.items():
        pull_image(directory, name, digest)
    after = parse_manifest(exact.output(['adb', 'shell', command], timeout=60))
    exact.require(before == after, 'Device screenshot files changed during final transfer')
    local = {path.name: exact.sha(path) for path in directory.glob('*.png')}
    exact.require(local == before, 'Complete device/local screenshot manifests differ')
    return before


def native(root):
    check_prepared(root)
    previous = check_native(evidence_path(root, 'Developer'), 'Developer')
    destination = evidence_path(root, 'Alpha-Testers')
    destination.mkdir(parents=True, exist_ok=False)
    apk = root / 'prepared/built/owner-delivery/APD-Alpha-Testers-440.apk'
    instrumentation = root / 'prepared/built/evidence/instrumentation.apk'
    subprocess.run(['adb', 'logcat', '-c'], check=True, timeout=20)
    log_stream = (destination / 'native-logcat.txt').open('w')
    logcat = subprocess.Popen(['adb', 'logcat', '-v', 'time'], stdout=log_stream, stderr=subprocess.STDOUT)
    completed = []
    collector = None
    try:
        subprocess.run(['adb', 'install', '-r', str(apk)], check=True, timeout=120)
        subprocess.run(['adb', 'install', '-r', str(instrumentation)], check=True, timeout=120)
        # A fresh emulator is required; never merge screenshots from a previous application run.
        subprocess.run(['adb', 'shell', 'rm', '-rf', REMOTE], check=True, timeout=20)
        for label, short_class, method, seconds in exact.CASES:
            subprocess.run(['adb', 'shell', 'am', 'force-stop', exact.APP], check=True, timeout=30)
            collector = IncrementalImages(destination / 'visuals')
            collector.thread.start()
            started = datetime.now(timezone.utc).isoformat()
            with (destination / (label + '.txt')).open('w') as stream:
                subprocess.run(['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                                exact.CLASS_PREFIX + short_class + '#' + method,
                                exact.APP + '.test/androidx.test.runner.AndroidJUnitRunner'],
                               stdout=stream, stderr=subprocess.STDOUT, check=True, timeout=seconds)
            exact.check_instrumentation((destination / (label + '.txt')).read_text(),
                                        exact.CLASS_PREFIX + short_class, method)
            collector.close()
            collector = None
            images = sync_images(destination / 'visuals')
            completed.append(label)
            exact.write_json(destination / (label + '-transfer.json'), dict(started=started,
                             completed=datetime.now(timezone.utc).isoformat(), remote_images=images))
            print(f'{label}: one passed case; {len(images)} complete screenshots matched device SHA-256.', flush=True)
        images = exact.check_images(destination / 'visuals')
        complete_inventory(previous['images'], images)
        exact.write_json(destination / 'native-verification.json', dict(number=440, variant='Alpha-Testers',
            source_commit=exact.SPECS[440]['source'], apk_sha256=exact.sha(apk),
            instrumentation_sha256=INSTRUMENTATION, completed_cases=completed, images=images,
            native_recovery_complete=True, manual_visual_review_complete=False, application_rebuilt=False,
            instrumentation_rebuilt=False, physical_phone_verified=False, retry_run=os.environ['GITHUB_RUN_ID'],
            retry_commit=os.environ['GITHUB_SHA'], original_recovery_run=RECOVERY_RUN,
            complete_remote_image_manifest_verified=True))
    finally:
        if collector is not None:
            collector.close()
        with (destination / 'device-after.txt').open('w') as stream:
            for command in (['adb', 'devices', '-l'], ['ps', '-eo', 'pid,ppid,rss,comm'], ['free', '-m']):
                try:
                    subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, timeout=20)
                except (OSError, subprocess.SubprocessError):
                    pass
        logcat.terminate()
        try:
            logcat.wait(timeout=10)
        except subprocess.TimeoutExpired:
            logcat.kill()
            logcat.wait()
        log_stream.close()


def finalize(root):
    developer = check_native(evidence_path(root, 'Developer'), 'Developer')
    alpha = check_native(evidence_path(root, 'Alpha-Testers'), 'Alpha-Testers')
    complete_inventory(developer['images'], alpha['images'])
    exact.require(alpha['complete_remote_image_manifest_verified'] is True
                  and str(alpha['retry_run']) == os.environ['GITHUB_RUN_ID']
                  and alpha['retry_commit'] == os.environ['GITHUB_SHA'], 'Retry provenance or image inventory differs')
    for label, _, _, _ in exact.CASES:
        transfer = json.loads((evidence_path(root, 'Alpha-Testers') / (label + '-transfer.json')).read_text())
        exact.require(transfer['remote_images'] and set(transfer['remote_images']) <= set(alpha['images']),
                      'Incomplete per-case transfer evidence')
    final_transfer = json.loads((evidence_path(root, 'Alpha-Testers') / 'freeze-transfer.json').read_text())
    exact.require(final_transfer['remote_images'] == alpha['images'], 'Final remote manifest differs from accepted images')
    check_prepared(root)
    exact.finalize(440, root / 'prepared', root / 'native', root / 'owner-review')
    provenance = json.loads((root / 'input-provenance.json').read_text())
    provenance.update(all_original_pairs_automatically_accepted=True, android_tests_per_build=12,
                      developer_evidence_reused=True, alpha_cases_rerun=[case[0] for case in exact.CASES],
                      manual_visual_review_complete=False, published=False)
    exact.write_json(root / 'owner-review/APD-439-440-Composite-Recovery.json', provenance)
    print('Original APD439 remains accepted; original APD440 now has all 12 cases and complete screenshots.', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('prepare', 'native', 'finalize'))
    parser.add_argument('--root', type=Path, required=True)
    args = parser.parse_args()
    globals()[args.stage](args.root)


if __name__ == '__main__':
    main()
