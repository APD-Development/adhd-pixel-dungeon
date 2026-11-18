#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>

"""Re-verify exact prepared bytes without rebuilding or relabelling their source."""
import hashlib,json,pathlib,subprocess,zipfile

def digest(path):return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()
def verify_prepared(archive,spec,out):
    assert digest(archive)==spec['artifact_sha256'],'Prepared archive differs'
    out=pathlib.Path(out);out.mkdir(exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        assert len(z.infolist())<=20
        for entry in z.infolist():
            assert pathlib.PurePosixPath(entry.filename).name==entry.filename and entry.filename not in ('.','..'),'Unsafe archive path'
            assert entry.file_size<=80000000 and not entry.is_dir(),'Unexpected prepared file'
            (out/entry.filename).write_bytes(z.read(entry))
    number=spec['number'];proof=json.loads((out/f'APD-Preview-{number}-Verification.json').read_text())
    assert proof['source_commit']==spec['source'] and proof['number']==number
    assert proof['core_tests']==spec['core_tests'] and proof['android_tests']==spec['android_tests']
    assert proof['application_id']==spec['application_id'] and proof['version_code']==spec['version_code']
    assert proof['signing_certificate']==spec['signing_certificate']
    for label,expected in spec['apk_hashes'].items():
        apk=out/f'APD-{label}-{number}.apk'
        assert digest(apk)==expected==proof['apk_hashes'][label]
        assert (out/(apk.name+'.sha256')).read_text().split()[0]==expected
    source=out/f'APD-Source-{number}.zip';assert digest(source)==spec['source_sha256']==proof['source_sha256']
    scan=json.loads((out/'APD-Source-Scan.json').read_text())
    assert scan['supplemental']['source_sha256']==spec['source_sha256'] and scan['supplemental']['source_findings_reviewed']
    assert not proof.get('physical_phone_verified')
    return proof

def main():
    import os,datetime
    repo=os.environ['GITHUB_REPOSITORY'];assert repo=='APD-Development/ShieldingShouldHaveWorked'
    spec=json.loads(pathlib.Path(os.environ['APD_DELIVERY_SPEC']).read_text())
    api=lambda path:json.loads(subprocess.check_output(['gh','api',path]))
    assert api('repos/'+repo)['private']
    artifact=api(f'repos/{repo}/actions/artifacts/{spec["artifact_id"]}')
    assert not artifact['expired'] and artifact['digest']=='sha256:'+spec['artifact_sha256']
    assert artifact['workflow_run']['id']==spec['prepared_run'] and artifact['workflow_run']['head_sha']==spec['prepared_source']
    archive=pathlib.Path('prepared-owner.zip')
    with archive.open('wb') as output:subprocess.run(['gh','api',f'repos/{repo}/actions/artifacts/{spec["artifact_id"]}/zip'],stdout=output,check=True)
    proof=verify_prepared(archive,spec,'owner-delivery')
    proof['prepared_artifact_reuse']={'artifact_id':spec['artifact_id'],'archive_sha256':spec['artifact_sha256'],'reverified_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'application_rebuilt':False,'prior_test_results_reused':True}
    pathlib.Path(f'owner-delivery/APD-Preview-{spec["number"]}-Verification.json').write_text(json.dumps(proof,indent=2)+'\n')
    note=pathlib.Path(spec['status_note']).read_text()
    with pathlib.Path('owner-delivery/release-notes.md').open('a') as output:output.write('\n\nCurrent delivery correction:\n'+note)
    print('Exact prepared APK/source bytes reverified; no application rebuilt or additional tests claimed.')
if __name__=='__main__':main()
