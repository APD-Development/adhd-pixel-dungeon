#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
# GPLv3 or later; no warranty. See the root GPL license for the full terms.
"""Recover one already-built tester APK; prove source/hash, never assemble another application."""
import hashlib,json,os,pathlib,subprocess,time,zipfile
from apd_existing_build_evidence import application_inputs
repo=os.environ['GITHUB_REPOSITORY'];spec=json.loads(pathlib.Path('docs/APD-CORRECTIVE-PAIR-TESTER-REQUEST.json').read_text())
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
assert api('repos/'+repo)['private']
deadline=time.monotonic()+1200
while True:
 run=api(f'repos/{repo}/actions/runs/{spec["player"]}');assert run['head_sha']==spec['source']
 if run['status']=='completed':assert run['conclusion']=='success';break
 assert time.monotonic()<deadline,'Tester build not ready; no acceptance or publication claim'
 time.sleep(15)
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (spec['source'],os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
# Keep newer development out of instrumentation compiled for an older exact APK.
test_source=os.environ['GITHUB_SHA']
subprocess.run(['git','fetch','--no-tags','--depth=1','origin',spec['source']],check=True)
subprocess.run(['git','checkout','--detach',spec['source']],check=True)
subprocess.run(['git','checkout',test_source,'--','android/src/androidTest'],check=True)
changed=subprocess.check_output(['git','diff','--name-only',spec['source']],text=True).splitlines()
assert all(p.startswith('android/src/androidTest/') for p in changed)
pinned=api(f'repos/{repo}/git/trees/'+subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()+'?recursive=1')
assert not pinned.get('truncated') and application_inputs(trees[0]['tree'])==application_inputs(pinned['tree'])
records=api(f'repos/{repo}/actions/runs/{spec["player"]}/artifacts')['artifacts']
a,=[a for a in records if a['name']=='apd-player-matching-developer' and not a['expired']]
with open('tester-artifact.zip','wb') as out:subprocess.run(['gh','api',f'repos/{repo}/actions/artifacts/{a["id"]}/zip'],stdout=out,check=True)
assert 'sha256:'+hashlib.sha256(pathlib.Path('tester-artifact.zip').read_bytes()).hexdigest()==a['digest']
pathlib.Path('application').mkdir()
with zipfile.ZipFile('tester-artifact.zip') as z:
 apk,=[p for p in z.namelist() if p.endswith('.apk')];checksum,=[p for p in z.namelist() if p.endswith('.apk.sha256')]
 data=z.read(apk);digest=hashlib.sha256(data).hexdigest();assert z.read(checksum).decode().split()[0]==digest
 pathlib.Path('application/exact-tester.apk').write_bytes(data)
pathlib.Path('acceptance-source.json').write_text(json.dumps({'application_source':spec['source'],'application_run':spec['player'],'application_artifact':a['id'],'application_artifact_digest':a['digest'],'application_sha256':digest,'application_rebuilt':False,'application_inputs_unchanged':True,'test_source':os.environ['GITHUB_SHA'],'physical_phone_test':False},indent=2)+'\n')
print('Exact tester APK SHA256',digest)

with open(os.environ["GITHUB_ENV"],"a") as env:env.write("APD_TEST_NUMBER="+str(spec["number"])+"\n")
