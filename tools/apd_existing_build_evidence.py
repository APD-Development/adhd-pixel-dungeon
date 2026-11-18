"""Validate exact existing private build artifacts before delivery; never rebuild."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET


def github(path):
    return json.loads(subprocess.check_output(['gh', 'api', path]))


def xml_summary(folder):
    files = list(Path(folder).rglob('TEST-*.xml'))
    assert files, 'Missing test XML: ' + folder
    roots = [ET.parse(path).getroot() for path in files]
    totals = {key: sum(int(root.get(key, 0)) for root in roots)
              for key in ('tests', 'failures', 'errors', 'skipped')}
    assert totals['failures'] == totals['errors'] == totals['skipped'] == 0, totals
    totals['cases'] = [case.attrib for root in roots for case in root.iter('testcase')]
    assert totals['tests'] > 0 and totals['tests'] == len(totals['cases']), 'Incomplete test evidence'
    identities = {(case.get('classname'), case.get('name')) for case in totals['cases']}
    assert len(identities) == totals['tests'], 'Duplicate test evidence'
    assert not any(case.find(tag) is not None for root in roots for case in root.iter('testcase')
                   for tag in ('failure', 'error', 'skipped')), 'Unsuccessful test case'
    return totals


def output(command):
    return subprocess.check_output(command, text=True)


def certificates(signature):
    # SDK versions label APK signers differently; exclude source-stamp certificates.
    matches = re.findall(r'(?m)^(?:Signer #\d+|V[1234] Signer):? certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$', signature)
    return sorted({digest.lower() for digest in matches})


def application_inputs(tree):
    return {entry['path']: entry['sha'] for entry in tree
            if entry.get('type') != 'tree'
            and entry['path'].split('/', 1)[0] not in ('.github', 'docs', 'tools')
            and not entry['path'].startswith(('core/src/test/', 'android/src/androidTest/'))}


def main():
    repo = os.environ['GITHUB_REPOSITORY']
    source = os.environ['APD_SOURCE_SHA']
    run_id = os.environ['APD_SOURCE_RUN']
    number = int(os.environ.get('APD_TEST_NUMBER', '22'))
    core_count = int(os.environ.get('APD_CORE_TEST_COUNT', '931'))
    android_count = int(os.environ.get('APD_ANDROID_TEST_COUNT', '4'))
    rollback_number = int(os.environ.get('APD_ROLLBACK_NUMBER', '20'))
    rollback_sha = os.environ.get('APD_ROLLBACK_SHA256', '0a7d05dc4fb1e87bf502849914c94ae13c4ffeaeaacc8ca865d8c3122a5808eb')
    assert number > 0 and core_count > 0 and android_count > 0
    assert github('repos/' + repo)['private'], 'Private repository required'
    publication_commit = os.environ.get('GITHUB_SHA', source)
    # A release may tag this delivery-only commit. Verify that it has identical
    # application/build inputs; never relabel a different APK as tested source.
    application_trees = []
    for commit in (source, publication_commit):
        response = github(f'repos/{repo}/git/trees/{commit}?recursive=1')
        assert not response.get('truncated', False), 'Incomplete application source tree'
        application_trees.append(application_inputs(response['tree']))
    assert application_trees[0] == application_trees[1], 'Publication commit changes application/build inputs'
    run = github(f'repos/{repo}/actions/runs/{run_id}')
    assert run['head_sha'] == source and run['run_number'] == number
    assert run['status'] == 'completed' and run['conclusion'] == 'success'
    jobs = github(f'repos/{repo}/actions/runs/{run_id}/jobs')['jobs']
    for name in ('verify-and-build', 'android-smoke-check'):
        assert any(job['name'] == name and job['conclusion'] == 'success' for job in jobs), name
    artifacts = github(f'repos/{repo}/actions/runs/{run_id}/artifacts')['artifacts']
    expected = json.loads(os.environ['APD_SOURCE_ARTIFACTS_JSON']) if 'APD_SOURCE_ARTIFACTS_JSON' in os.environ else {'apd-codex-private-test-apk': 11676568789,
                'apd-codex-core-tests': 11676748354,
                'apd-codex-android-checks': 11677028840}
    for name, artifact_id in expected.items():
        assert any(a['name'] == name and a['id'] == artifact_id and not a['expired']
                   and a['workflow_run']['head_sha'] == source for a in artifacts), name
    core, android = xml_summary('core-reports'), xml_summary('android-reports')
    assert core['tests'] == core_count and android['tests'] == android_count
    for required in os.environ.get('APD_REQUIRED_ANDROID_METHODS', '').split(','):
        if required:
            assert any(case['name'] == required for case in android['cases']), 'Missing Android evidence: ' + required
    apk = Path(f'delivery/ADHD-Pixel-Dungeon-CODEX-TEST-{number}.apk')
    assert list(Path('delivery').glob('*.apk')) == [apk]
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    assert digest == Path(str(apk) + '.sha256').read_text().split()[0]
    old = Path(f'rollback/ADHD-Pixel-Dungeon-CODEX-TEST-{rollback_number}.apk')
    assert hashlib.sha256(old.read_bytes()).hexdigest() == rollback_sha
    build_tools = Path(os.environ['ANDROID_HOME']) / 'build-tools'
    signer = sorted(build_tools.glob('*/apksigner'))[-1]
    aapt = signer.parent / 'aapt'
    signatures = [output([str(signer), 'verify', '--print-certs', str(path)]) for path in (apk, old)]
    certs = [certificates(s) for s in signatures]
    print(json.dumps({'apk_sha256': digest, 'candidate_certificates': certs[0],
                      'rollback_certificates': certs[1], 'signature_output': signatures}, indent=2))
    assert certs[0] and certs[0] == certs[1], 'APK signing mismatch'
    packages = [re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",
                         output([str(aapt), 'dump', 'badging', str(path)])) for path in (apk, old)]
    assert all(packages), 'Missing Android package metadata'
    current, previous = [p.groups() for p in packages]
    assert current[0] == previous[0] == 'com.charltonchris29.adhdpixeldungeon.indev'
    assert int(current[1]) >= int(previous[1]), 'Version downgrade'
    assert current[2] == f'4.0.8-apd-INDEV-bC{number}-TEST', current
    evidence = {'source': source, 'publication_commit': publication_commit,
                'publication_app_tree_matches_source': True, 'run': run_id, 'apk_sha256': digest,
                'certificate_sha256': certs[0], 'package': current,
                'core': core, 'android': android,
                'build_started_utc': run['run_started_at'], 'build_completed_utc': run['updated_at']}
    Path(f'delivery/TEST{number}-Verification.json').write_text(json.dumps(evidence, indent=2))
    text = f"TEST{number} source: {source}. Existing verification/build run: https://github.com/{repo}/actions/runs/{run_id}. No APK rebuild performed during delivery.\n\n"
    text += f"Delivery verification commit {publication_commit}; all application/build input trees match the tested source exactly. Only delivery tooling, reports or workflows may differ.\n\n"
    text += f"Actual test XML: {core['tests']} core tests and {android['tests']} API 34 emulator tests; zero failures, errors or skipped tests. Debug assembly succeeded. This is not physical-phone verification or a complete normal-run playthrough.\n\n"
    text += f"APK: {apk.name}. SHA-256: {digest}. Application ID: {current[0]}. Version: {current[2]}. APK signatures verified; certificate SHA-256 matches TEST{rollback_number}: {', '.join(certs[0])}. Version code permits installing over TEST{rollback_number}. Existing save-data compatibility still requires phone testing.\n\n"
    text += f"CI started {run['run_started_at']} and completed {run['updated_at']} (UTC). This preparation reuses the exact verified artifact; consult the report for included and unfinished features.\n\n"
    text += f"Install over the existing APD app; never uninstall or clear its data. Preserve TEST{rollback_number} as rollback. Private test prerelease only; no online features enabled."
    Path('delivery/verification.txt').write_text(text)
    print(text)


if __name__ == '__main__':
    main()
