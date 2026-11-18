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

"""Recover exact371 APKs and completed core results; never build a game APK."""
import hashlib,json,os,pathlib,shutil,subprocess,zipfile
SOURCE='5b7e7250b63072e52c5cebd1a45ea9bbf1132aa4'
RUN=42404197897
ARTIFACT=13587595090
DIGEST='4104caacfb8901bcbe35f4688d51719e6615a335c481e49d4395a923e1f73693'
ROOT=pathlib.Path.cwd()
repo=os.environ['GITHUB_REPOSITORY']
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
assert api('repos/'+repo)['private']
produced=api(f'repos/{repo}/actions/runs/{RUN}');assert produced['head_sha']==SOURCE and produced['conclusion']=='failure'
jobs=api(f'repos/{repo}/actions/runs/{RUN}/jobs')['jobs'];job,=[j for j in jobs if j['id']==128498262300]
assert any(s['number']==6 and s['conclusion']=='success' for s in job['steps'])
assert any(s['number']==8 and s['conclusion']=='failure' for s in job['steps'])
# Application source is immutable. Only the named instrumentation fixture may change for clear capture.
changed=subprocess.check_output(['git','diff','--name-only',SOURCE,'HEAD'],text=True).splitlines()
allowed_test='android/src/androidTest/java/com/shatteredpixel/shatteredpixeldungeon/android/WallGeometryDeviceTest.java'
assert all(p.startswith(('docs/','tools/','.github/')) or p==allowed_test for p in changed),changed
meta=api(f'repos/{repo}/actions/artifacts/{ARTIFACT}')
assert meta['workflow_run']['id']==RUN and meta['workflow_run']['head_sha']==SOURCE and meta['digest']=='sha256:'+DIGEST and not meta['expired']
archive=ROOT/'retained371.zip'
with archive.open('wb') as out:subprocess.run(['gh','api',f'repos/{repo}/actions/artifacts/{ARTIFACT}/zip'],stdout=out,check=True)
assert hashlib.sha256(archive.read_bytes()).hexdigest()==DIGEST
with zipfile.ZipFile(archive) as z:
 for entry in z.infolist():
  assert (ROOT/entry.filename).resolve().is_relative_to(ROOT)
 z.extractall(ROOT)
if (ROOT/'evidence').exists():shutil.move(str(ROOT/'evidence'),str(ROOT/'original-evidence'))
(ROOT/'evidence').mkdir()
qol=(ROOT/'original-evidence/developer-qol.txt').read_text()
assert 'OK (1 test)' in qol and 'INSTRUMENTATION_CODE: -1' in qol and 'FAILURES' not in qol
shutil.copy(ROOT/'original-evidence/developer-qol.txt',ROOT/'evidence/developer-qol.txt')
proof=dict(application_source=SOURCE,original_producer_run=RUN,original_producer_conclusion='failure',retained_artifact_id=ARTIFACT,retained_artifact_sha256=DIGEST,application_rebuilt=False,new_game_apks_this_job=0,application_inputs_unchanged=True,test_source_unchanged=allowed_test not in changed,test_only_changed_paths=[p for p in changed if p==allowed_test],verification_source=os.environ['GITHUB_SHA'],original_interruption='adb error: closed during force-stop; emulator TCP5554 refused connection. Root cause of host disconnect not established.',apk_hashes={label:hashlib.sha256((ROOT/f'owner-delivery/APD-{label}-371.apk').read_bytes()).hexdigest() for label in ['Developer','Alpha-Testers']})
(ROOT/'evidence/reuse-provenance.json').write_text(json.dumps(proof,indent=2)+'\n')
print(json.dumps(proof))
