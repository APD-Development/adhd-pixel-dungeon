#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Recover original APD439/440 evidence and source without rebuilding or publishing."""
import argparse
from datetime import datetime, timezone
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET
import zipfile
import zlib

from validate_android_visuals import validate as validate_png
from apd_native_screenshot_transfer import IncrementalImages, REMOTE, sync_images

REPO = 'APD-Development/ShieldingShouldHaveWorked'
APP = 'com.charltonchris29.adhdpixeldungeon.indev'
CERT = '067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
CLASS_PREFIX = 'com.shatteredpixel.shatteredpixeldungeon.android.'
SPECS = {
    439: dict(source='8224872be92c44827282ff8ba6bde363d67f0826', run=42938370462,
              artifact=13819787406, digest='0439f100f337292e0847ce887e68c1f894de070b9d9672b273e2b078cf4c4f97',
              workflow='.github/workflows/apd-pylon-439.yml', tests=3179, suites=461,
              hashes={'Developer': '9c8a91c26894c5f8de3976c2e82925c5a94692e2d892d0ce4341d2ab4ced2b0d',
                      'Alpha-Testers': 'd19aa8b989fb7e2367c5df4c51301e44a3b83c79b8dde4ca844dd67f533d4552'}),
    440: dict(source='293b5a0d5fed957c1d953b301fadf576a766611b', run=42940060890,
              artifact=13819919599, digest='5f8f72f597c75368d377315b054e75d29593b67e1d44d9bfaeae2ea7c230f06f',
              workflow='.github/workflows/apd-attack-pylon-440.yml', tests=3180, suites=461,
              hashes={'Developer': '98655a8ca11f3477f8566bf86e63238aa5d4abc2bc0460002489ca716dc36315',
                      'Alpha-Testers': 'e3af4d88e13360d263487c33f38fce941f60f1c356c579d4570887b945f90dad'}),
}
CASES = (
    ('qol', 'QolItemFeedbackDeviceTest', 'grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively', 1800),
    ('wall', 'WallGeometryDeviceTest', 'observedGeometryAndRetainedMarksUseTheActualAndroidLayer', 240),
    ('seed', 'TrapDiscoveryDeviceTest', 'a_controlWritesEnabledPreference', 180),
    ('restart', 'TrapDiscoveryDeviceTest', 'b_freshProcessReadsEnabledAndCanDisable', 180),
    ('gameplay', 'TrapDiscoveryDeviceTest', 'c_runtimeDiscoveryRouteAndActualTrapEffect', 180),
    ('freeze', 'RemovedActorStallDeviceTest', 'cancelledGhoulAttackReleasesQueueButLiveAttackStaysPending', 240),
)
FEATURES = {'Developer': b'testBuild\ngrudge\ndemoUnlock\nclericPlayable\n',
            'Alpha-Testers': b'extendedPlayback\ngrudge\n'}


def require(value, message):
    if not value:
        raise RuntimeError(message)


def sha(path):
    digest = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def write_json(path, value):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(json.dumps(value, indent=2) + '\n')


def output(args, **kwargs):
    return subprocess.check_output(args, text=True, **kwargs)


def api(path):
    return json.loads(output(['gh', 'api', f'repos/{REPO}/{path}']))


def safe_name(name):
    path = PurePosixPath(name)
    require(bool(name) and not path.is_absolute() and '..' not in path.parts and '\\' not in name,
            'Unsafe archive entry')
    return path


def extract_archive(archive, destination):
    seen = set()
    with zipfile.ZipFile(archive) as packed:
        require(sum(item.file_size for item in packed.infolist()) < 1024 ** 3, 'Oversized artifact')
        for item in packed.infolist():
            path = safe_name(item.filename)
            require(str(path) not in seen, 'Duplicate artifact entry')
            seen.add(str(path))
            require((item.external_attr >> 16) & 0o170000 != 0o120000, 'Artifact symlink')
        packed.extractall(destination)


def check_core(folder, number):
    spec = SPECS[number]
    files = sorted(Path(folder).rglob('TEST-*.xml'))
    require(len(files) == spec['suites'], 'Unexpected original core suite count')
    roots = [ET.parse(path).getroot() for path in files]
    totals = {key: sum(int(root.get(key, 0)) for root in roots)
              for key in ('tests', 'failures', 'errors', 'skipped')}
    require(totals == dict(tests=spec['tests'], failures=0, errors=0, skipped=0), 'Original core results differ')
    cases = [case for root in roots for case in root.iter('testcase')]
    require(len(cases) == totals['tests'], 'Incomplete core case evidence')
    require(len({(case.get('classname'), case.get('name')) for case in cases}) == len(cases), 'Duplicate core cases')
    require(not any(case.find(tag) is not None for case in cases for tag in ('failure', 'error', 'skipped')),
            'Unsuccessful original core case')
    return totals


def check_instrumentation(text, class_name, method):
    require('OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text, 'Incomplete native result')
    require(re.search(r'^INSTRUMENTATION_STATUS: class=' + re.escape(class_name) + r'\r?$', text, re.M), 'Wrong native class')
    require(re.search(r'^INSTRUMENTATION_STATUS: test=' + re.escape(method) + r'\r?$', text, re.M), 'Wrong native method')
    codes = re.findall(r'^INSTRUMENTATION_STATUS_CODE: (-?\d+)\r?$', text, re.M)
    require(codes.count('0') == 1 and all(code in ('1', '0') for code in codes), 'Failed, skipped or duplicate native case')
    require(not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|AssumptionFailure|\bFAILURE\b', text),
            'Native failure marker')


def check_png_pixels(data):
    """Decode the PNG stream and require every complete row and legal filter byte."""
    dimensions = validate_png(data)
    require(data[12:16] == b'IHDR', 'PNG header is not first')
    width, height, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', data[16:29])
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}
    depths = {0: (1, 2, 4, 8, 16), 2: (8, 16), 3: (1, 2, 4, 8), 4: (8, 16), 6: (8, 16)}
    require(color in channels and depth in depths[color] and compression == filtering == 0
            and interlace in (0, 1), 'Invalid native PNG encoding')
    # Adam7 interlacing uses seven reduced images, each with its own row filters.
    passes = ((0, 0, 1, 1),) if interlace == 0 else (
        (0, 0, 8, 8), (4, 0, 8, 8), (0, 4, 4, 8), (2, 0, 4, 4),
        (0, 2, 2, 4), (1, 0, 2, 2), (0, 1, 1, 2))
    rows = []
    for x, y, step_x, step_y in passes:
        columns, count = max(0, (width - x + step_x - 1) // step_x), max(0, (height - y + step_y - 1) // step_y)
        if columns and count:
            rows.append(((columns * channels[color] * depth + 7) // 8 + 1, count))
    compressed = []
    at = 8
    while at < len(data):
        length = struct.unpack_from('>I', data, at)[0]
        if data[at + 4:at + 8] == b'IDAT':
            compressed.append(data[at + 8:at + 8 + length])
        at += length + 12
    expected = sum(length * count for length, count in rows)
    decoder = zlib.decompressobj()
    pixels = decoder.decompress(b''.join(compressed), expected + 1)
    require(decoder.eof and not decoder.unused_data and not decoder.unconsumed_tail
            and len(pixels) == expected, 'Incomplete or oversized PNG pixel stream')
    at = 0
    for length, count in rows:
        for _ in range(count):
            require(pixels[at] <= 4, 'Invalid PNG row filter')
            at += length
    return dimensions


def check_images(directory):
    images = sorted(Path(directory).rglob('*.png'))
    names = {path.name for path in images}
    required = {'wall-geometry-corner-no-false-check.png', 'wall-geometry-valid-searched-face.png',
                'wall-geometry-retained-after-zoom-pan-pause-rebuild.png', 'wall-geometry-door-no-stale-check.png',
                'trap-discovery-setting-on.png', 'trap-discovery-setting-restarted.png',
                'trap-discovery-setting-off.png', 'trap-discovery-runtime-actual-ooze.png'}
    require(required <= names, 'Missing wall/trap native screenshots')
    require(len([name for name in names if name.startswith('qol-automatic-inventory-')]) >= 5,
            'Missing inventory screenshots')
    manifest = {}
    for path in images:
        data = path.read_bytes()
        require(len(data) > 2000 and data.startswith(b'\x89PNG\r\n\x1a\n'), 'Invalid native screenshot')
        try:
            check_png_pixels(data)
        except (ValueError, zlib.error) as error:
            raise RuntimeError('Invalid native screenshot: ' + path.name) from error
        manifest[str(path.relative_to(directory))] = sha(path)
    return manifest


def sdk_tools():
    candidates = list((Path(os.environ['ANDROID_HOME']) / 'build-tools').glob('*/apksigner'))
    require(bool(candidates), 'Android signing verifier unavailable')
    signer = sorted(candidates, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.parent.name)))[-1]
    return signer, signer.parent / 'aapt'


def check_signature(path, signer):
    from apd_existing_build_evidence import certificates
    signed = output([str(signer), 'verify', '--verbose', '--print-certs', str(path)])
    require(certificates(signed) == [CERT], 'Original signing certificate differs')


def check_pair(root, number):
    spec = SPECS[number]
    proof = json.loads((root / 'evidence/apks.json').read_text())
    source = json.loads((root / 'evidence/source.json').read_text())
    require(source['source_commit'] == spec['source'], 'Original source proof differs')
    totals = check_core(root / 'core/build/test-results/test', number)
    signer, aapt = sdk_tools()
    assets = {}
    variants = {}
    for variant in FEATURES:
        apk = root / 'owner-delivery' / f'APD-{variant}-{number}.apk'
        digest = sha(apk)
        require(digest == spec['hashes'][variant] == proof[variant]['sha256'], 'Pinned APK hash differs')
        require(apk.stat().st_size == proof[variant]['bytes'] and proof[variant]['cert'] == CERT, 'APK metadata differs')
        check_signature(apk, signer)
        meta = output([str(aapt), 'dump', 'badging', str(apk)])
        version = f'4.0.8-apd-INDEV-bC{number}-TEST' if variant == 'Developer' else f'4.0.8-apd-INDEV-bP{number}'
        require(f"name='{APP}'" in meta and "versionCode='920'" in meta and f"versionName='{version}'" in meta,
                'APK application/version identity differs')
        with zipfile.ZipFile(apk) as packed:
            assets[variant] = {name[7:]: packed.read(name) for name in packed.namelist()
                               if name.startswith('assets/') and not name.endswith('/')}
        require(assets[variant]['apd/features.txt'] == FEATURES[variant], 'Variant feature flags differ')
        history = assets[variant]['apd/history/index.tsv'].decode().splitlines()
        require(history[0].split('\t')[1] == f'Codex TEST {number}', 'Wrong compiled history head')
        labels = {line.split('\t')[1] for line in history if len(line.split('\t')) == 4}
        require(all(f'Codex TEST {n}' in labels for n in range(420, number + 1)), 'Compiled history is incomplete')
        variants[variant] = dict(sha256=digest, bytes=apk.stat().st_size, version_name=version)
    common = set(assets['Developer']) - {'apd/features.txt'}
    require(common == set(assets['Alpha-Testers']) - {'apd/features.txt'}, 'Variant asset inventory differs')
    require(all(assets['Developer'][name] == assets['Alpha-Testers'][name] for name in common), 'Common APK assets differ')
    instrumentation = root / 'evidence/instrumentation.apk'
    check_signature(instrumentation, signer)
    meta = output([str(aapt), 'dump', 'badging', str(instrumentation)])
    manifest = output([str(aapt), 'dump', 'xmltree', str(instrumentation), 'AndroidManifest.xml'])
    require(f"name='{APP}.test'" in meta
            and re.search(r'android:targetPackage(?:\([^)]*\))?\s*=\s*"' + re.escape(APP) + '"', manifest),
            'Instrumentation target differs')
    return assets, dict(number=number, source_commit=spec['source'], original_run=spec['run'],
                       artifact_id=spec['artifact'], artifact_sha256=spec['digest'], core=totals,
                       core_suites=spec['suites'], variants=variants, certificate_sha256=CERT,
                       application_id=APP, version_code=920, instrumentation_sha256=sha(instrumentation),
                       application_rebuilt=False, instrumentation_rebuilt=False, published=False)


def sanitize_snapshot(snapshot):
    removed = []
    for name in list(snapshot):
        safe_name(name)
        if name.startswith('keystore/') or name.lower().endswith(('.jks', '.keystore', '.p12', '.pfx')):
            removed.append(name)
            del snapshot[name]
    original = snapshot['android/build.gradle'].decode()
    credentials = set(re.findall(r"(?:storePassword|keyPassword)\s+['\"]([^'\"]+)['\"]", original))
    require(credentials, 'Original signing configuration was not recognized')
    substitutions = []
    for name, blob in list(snapshot.items()):
        try:
            text = blob.decode('utf-8')
        except UnicodeDecodeError:
            continue
        for value in credentials:
            text = text.replace(value, 'EXTERNAL_SIGNING_PASSWORD')
        if text.encode() != blob:
            substitutions.append(name)
            snapshot[name] = text.encode()
    config = snapshot['android/build.gradle'].decode()
    require("rootProject.file('keystore/apd-debug.jks')" in config, 'Unknown signing keystore declaration')
    config = config.replace("rootProject.file('keystore/apd-debug.jks')",
                            "rootProject.file(System.getenv('APD_SIGNING_KEYSTORE') ?: 'keystore/EXTERNAL-KEY-REQUIRED.jks')")
    config = re.sub(r"storePassword\s+['\"]EXTERNAL_SIGNING_PASSWORD['\"]",
                    "storePassword System.getenv('APD_SIGNING_STORE_PASSWORD') ?: ''", config)
    config = re.sub(r"keyPassword\s+['\"]EXTERNAL_SIGNING_PASSWORD['\"]",
                    "keyPassword System.getenv('APD_SIGNING_KEY_PASSWORD') ?: System.getenv('APD_SIGNING_STORE_PASSWORD') ?: ''", config)
    config = config.replace('signingConfig signingConfigs.apdDebug',
                            "signingConfig project.hasProperty('apdUnsignedVerification') ? null : signingConfigs.apdDebug")
    snapshot['android/build.gradle'] = config.encode()
    require(all(not any(value.encode() in blob for value in credentials) for blob in snapshot.values()),
            'Signing credential remains in source snapshot')
    return removed, substitutions


def source_archive(number, assets, proof, destination):
    spec = SPECS[number]
    snapshot = {}
    raw = subprocess.check_output(['git', 'archive', spec['source']])
    with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
        for member in archive:
            safe_name(member.name)
            if member.isfile():
                snapshot[member.name] = archive.extractfile(member).read()
    removed, substitutions = sanitize_snapshot(snapshot)
    require(assets['Developer']['legal/GPL-3.0.txt'] == snapshot['LICENSE.txt'], 'APK license differs from source')
    for name, value in assets['Developer'].items():
        if name != 'apd/features.txt':
            snapshot['core/src/main/assets/' + name] = value
    snapshot['CORRESPONDING-SOURCE/generated-inputs/apd/features.txt'] = assets['Developer']['apd/features.txt']
    snapshot['CORRESPONDING-SOURCE/generated-inputs/alpha/apd/features.txt'] = assets['Alpha-Testers']['apd/features.txt']
    readme = (f'APD{number} corresponding source for the exact original Developer and Alpha-Testers APKs.\n'
              f'Producing commit: {spec["source"]}; producing run: {spec["run"]}.\n'
              'No application or instrumentation was rebuilt during recovery. Native recovery and manual image review '
              'are separate requirements; this source artifact alone does not establish native acceptance.\n'
              'JDK21, Android SDK36, included Gradle wrapper. Unsigned reconstruction commands:\n'
              f'./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdTestBuild -PapdBuild=C{number}\n'
              f'./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdBuild=P{number}\n'
              'Signed install-over builds require the EXISTING external APD keystore through APD_SIGNING_KEYSTORE, '
              'APD_SIGNING_STORE_PASSWORD and optional APD_SIGNING_KEY_PASSWORD; omit apdUnsignedVerification. '
              'Never substitute a different signing key, uninstall, or clear player data.\n'
              'All common runtime assets and generated history are copied byte-for-byte from the pinned APKs. '
              'Exact variant feature inputs are retained outside the asset root. Signing-only substitutions and removed '
              'key paths are enumerated in manifest.json. This reconstruction has not been rebuilt. '
              'No signed-byte reproducibility or physical-phone testing is claimed. '
              'GPLv3-or-later; original license and attribution preserved; no warranty.\n')
    snapshot['docs/SIGNING.md'] = readme.encode()
    snapshot['CORRESPONDING-SOURCE/README.txt'] = readme.encode()
    manifest = dict(source_commit=spec['source'], original_run=spec['run'], variants=proof['variants'],
                    removed_signing_paths=removed, credential_substitutions=substitutions,
                    signing_config_externalized=True, source_rebuilt=False,
                    files={name: hashlib.sha256(blob).hexdigest() for name, blob in sorted(snapshot.items())})
    snapshot['CORRESPONDING-SOURCE/manifest.json'] = (json.dumps(manifest, indent=2) + '\n').encode()
    destination.mkdir(parents=True, exist_ok=True)
    target = destination / f'APD-Source-{number}.zip'
    with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED) as packed:
        for name, blob in sorted(snapshot.items()):
            info = zipfile.ZipInfo(f'APD-Source-{number}/{name}', date_time=(2026, 11, 11, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o100755 if name == 'gradlew' or name.endswith('.sh') else 0o100644) << 16
            packed.writestr(info, blob)
    Path(str(target) + '.sha256').write_text(sha(target) + '  ' + target.name + '\n')
    # Reopen the final source deliverable and verify every manifest hash.
    with zipfile.ZipFile(target) as packed:
        for name, expected in manifest['files'].items():
            require(hashlib.sha256(packed.read(f'APD-Source-{number}/' + name)).hexdigest() == expected,
                    'Source archive readback differs')
    return sha(target)


def prepare(number, root):
    spec = SPECS[number]
    require(os.environ['GITHUB_REPOSITORY'] == REPO, 'Wrong repository')
    repo = json.loads(output(['gh', 'api', f'repos/{REPO}']))
    require(repo['private'], 'Private repository required')
    from apd_owner_delivery_policy import require_apk_delivery_allowed
    require_apk_delivery_allowed(Path.cwd())
    run = api(f'actions/runs/{spec["run"]}')
    require(run['head_sha'] == spec['source'] and run['path'] == spec['workflow'] and run['status'] == 'completed',
            'Original producer identity/status differs')
    require(run['conclusion'] == ('success' if number == 439 else 'failure'), 'Original result history differs')
    jobs = api(f'actions/runs/{spec["run"]}/jobs?per_page=100')['jobs']
    build_jobs = [job for job in jobs if job['name'] == 'build']
    require(len(build_jobs) == 1 and build_jobs[0]['conclusion'] == 'success', 'Original build not successful')
    required_steps = ['Run core suite and compile all required variants before building',
                      'Assemble Developer and Alpha-Testers; verify original signature and built-in history']
    require(all(any(step['name'] == name and step['conclusion'] == 'success' for step in build_jobs[0]['steps'])
                for name in required_steps), 'Original compilation/build gates incomplete')
    artifact = api(f'actions/artifacts/{spec["artifact"]}')
    require(artifact['id'] == spec['artifact'] and artifact['name'] == f'apd{number}-built-pair'
            and artifact['digest'] == 'sha256:' + spec['digest'] and not artifact['expired']
            and artifact['workflow_run']['id'] == spec['run']
            and artifact['workflow_run']['head_sha'] == spec['source'], 'Original artifact identity differs')
    root.mkdir(parents=True, exist_ok=True)
    archive = root / 'original-built-pair.zip'
    with archive.open('wb') as stream:
        subprocess.run(['gh', 'api', f'repos/{REPO}/actions/artifacts/{spec["artifact"]}/zip'], stdout=stream, check=True)
    require(sha(archive) == spec['digest'], 'Downloaded original artifact digest differs')
    built = root / 'built'
    extract_archive(archive, built)
    assets, proof = check_pair(built, number)
    proof['source_archive_sha256'] = source_archive(number, assets, proof, root / 'source')
    proof['native_recovery_complete'] = False
    proof['manual_visual_review_complete'] = False
    write_json(root / 'preparation.json', proof)
    write_json(root / 'original-artifact.json', artifact)
    write_json(root / 'original-run.json', run)
    write_json(root / 'original-jobs.json', jobs)
    print(f'APD{number} original pair, core evidence, signatures and corresponding source verified; native recovery pending.')


def native(number, variant, root, destination):
    assets, proof = check_pair(root / 'built', number)
    del assets
    destination.mkdir(parents=True, exist_ok=True)
    apk = root / 'built/owner-delivery' / f'APD-{variant}-{number}.apk'
    instrumentation = root / 'built/evidence/instrumentation.apk'
    subprocess.run(['adb', 'logcat', '-c'], check=True, timeout=20)
    log_stream = (destination / 'native-logcat.txt').open('w')
    logcat = subprocess.Popen(['adb', 'logcat', '-v', 'time'], stdout=log_stream, stderr=subprocess.STDOUT)
    collector = None
    completed = []
    try:
        subprocess.run(['adb', 'install', '-r', str(apk)], check=True, timeout=120)
        subprocess.run(['adb', 'install', '-r', str(instrumentation)], check=True, timeout=120)
        # This directory belongs to disposable emulator evidence, never player saves.
        subprocess.run(['adb', 'shell', 'rm', '-rf', REMOTE], check=True, timeout=20)
        for label, short_class, method, seconds in CASES:
            subprocess.run(['adb', 'shell', 'am', 'force-stop', APP], check=True, timeout=30)
            collector = IncrementalImages(destination / 'visuals')
            collector.thread.start()
            started = datetime.now(timezone.utc).isoformat()
            class_name = CLASS_PREFIX + short_class
            report = destination / (label + '.txt')
            with report.open('w') as stream:
                subprocess.run(['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                                class_name + '#' + method, APP + '.test/androidx.test.runner.AndroidJUnitRunner'],
                               stdout=stream, stderr=subprocess.STDOUT, check=True, timeout=seconds)
            check_instrumentation(report.read_text(), class_name, method)
            collector.close()
            collector = None
            remote_images = sync_images(destination / 'visuals')
            completed.append(label)
            write_json(destination / (label + '-transfer.json'), dict(started=started,
                       completed=datetime.now(timezone.utc).isoformat(), remote_images=remote_images))
            print(f'{label}: one passed case; {len(remote_images)} complete screenshots matched device SHA-256.', flush=True)
        images = check_images(destination / 'visuals')
        require(images == remote_images, 'Final remote manifest differs from accepted images')
        write_json(destination / 'native-verification.json', dict(number=number, variant=variant,
                   source_commit=SPECS[number]['source'], apk_sha256=sha(apk),
                   instrumentation_sha256=proof['instrumentation_sha256'], completed_cases=completed,
                   images=images, native_recovery_complete=True, manual_visual_review_complete=False,
                   application_rebuilt=False, instrumentation_rebuilt=False, physical_phone_verified=False,
                   complete_remote_image_manifest_verified=True))
    finally:
        if collector is not None:
            collector.close()
        try:
            with (destination / 'device-after.txt').open('w') as stream:
                subprocess.run(['adb', 'devices', '-l'], stdout=stream, stderr=subprocess.STDOUT, timeout=15)
        except (subprocess.SubprocessError, OSError):
            pass
        logcat.terminate()
        try:
            logcat.wait(timeout=10)
        except subprocess.TimeoutExpired:
            logcat.kill()
            logcat.wait()
        log_stream.close()


def finalize(number, root, native_root, destination):
    assets, proof = check_pair(root / 'built', number)
    del assets
    for variant in FEATURES:
        evidence = native_root / f'apd-recovery-{number}-native-{variant}'
        receipt = json.loads((evidence / 'native-verification.json').read_text())
        require(receipt['number'] == number and receipt['variant'] == variant
                and receipt['source_commit'] == SPECS[number]['source']
                and receipt['apk_sha256'] == SPECS[number]['hashes'][variant]
                and receipt['instrumentation_sha256'] == proof['instrumentation_sha256']
                and receipt['completed_cases'] == [case[0] for case in CASES]
                and receipt['native_recovery_complete'] is True, 'Native recovery identity/case set differs')
        for label, short_class, method, _ in CASES:
            check_instrumentation((evidence / (label + '.txt')).read_text(), CLASS_PREFIX + short_class, method)
        require(check_images(evidence / 'visuals') == receipt['images'], 'Native screenshot hashes changed')
        require((evidence / 'native-logcat.txt').stat().st_size > 0, 'Missing native logcat')
    destination.mkdir(parents=True, exist_ok=True)
    for variant in FEATURES:
        source = root / 'built/owner-delivery' / f'APD-{variant}-{number}.apk'
        target = destination / source.name
        shutil.copyfile(source, target)
        require(sha(target) == SPECS[number]['hashes'][variant], 'Final APK copy changed')
        Path(str(target) + '.sha256').write_text(sha(target) + '  ' + target.name + '\n')
    prepared = json.loads((root / 'preparation.json').read_text())
    sourcezip = root / 'source' / f'APD-Source-{number}.zip'
    require(sha(sourcezip) == prepared['source_archive_sha256'], 'Corresponding source archive changed')
    shutil.copyfile(sourcezip, destination / sourcezip.name)
    shutil.copyfile(Path(str(sourcezip) + '.sha256'), destination / (sourcezip.name + '.sha256'))
    accepted_cases = len(CASES) * len(FEATURES)
    proof.update(native_recovery_complete=True, android_tests=accepted_cases, manual_visual_review_complete=False,
                 source_archive_sha256=prepared['source_archive_sha256'], physical_phone_verified=False,
                 recovery_run=os.environ['GITHUB_RUN_ID'], recovery_commit=os.environ['GITHUB_SHA'])
    write_json(destination / f'APD-{number}-Recovery-Verification.json', proof)
    print(f'APD{number}: all {accepted_cases} exact-APK API34 cases accepted; manual image review remains required; no release published.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('prepare', 'native', 'finalize'))
    parser.add_argument('number', type=int, choices=tuple(SPECS))
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--variant', choices=tuple(FEATURES))
    parser.add_argument('--output', type=Path)
    parser.add_argument('--native-root', type=Path)
    args = parser.parse_args()
    if args.stage == 'prepare':
        prepare(args.number, args.root)
    elif args.stage == 'native':
        require(args.variant and args.output, 'Native variant/output required')
        native(args.number, args.variant, args.root, args.output)
    else:
        require(args.native_root and args.output, 'Final native evidence/output required')
        finalize(args.number, args.root, args.native_root, args.output)


if __name__ == '__main__':
    main()
