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

"""Deliver one already-tested private APK pair and exact key-free shared source. Never rebuild."""
import datetime,hashlib,io,json,os,pathlib,re,subprocess,tarfile,time,zipfile
from apd_existing_build_evidence import application_inputs,certificates,xml_summary
from PIL import Image
ROOT=pathlib.Path.cwd();REPO=os.environ['GITHUB_REPOSITORY'];OUT=ROOT/'owner-delivery';OUT.mkdir(exist_ok=True)
def api(path):return json.loads(subprocess.check_output(['gh','api',path]))
def cmd(*args):return subprocess.check_output(args,text=True)
def sha(p):return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
def artifact(run,name):
 records=api(f'repos/{REPO}/actions/runs/{run}/artifacts')['artifacts'];found=[a for a in records if a['name']==name and not a['expired']];assert len(found)==1,(run,name)
 a=found[0];z=ROOT/(str(a['id'])+'.zip')
 with z.open('wb') as f:subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{a["id"]}/zip'],stdout=f,check=True)
 assert a['digest']=='sha256:'+sha(z)
 folder=ROOT/('artifact-'+str(a['id']));folder.mkdir(exist_ok=True)
 with zipfile.ZipFile(z) as archive:archive.extractall(folder)
 return folder,a
spec=json.loads(pathlib.Path('docs/APD-QOL-OWNER-DELIVERY.json').read_text());source=spec['source'];number=spec['number'];tag=f'apd-alpha-preview-{number}'
assert api('repos/'+REPO)['private']
deadline=time.monotonic()+900
while True:
 native_run=api(f'repos/{REPO}/actions/runs/{spec["native"]}')
 if native_run['status']=='completed':break
 assert time.monotonic()<deadline,'Exact-APK retest has not completed; publication remains blocked'
 print('Waiting for already-running exact-APK acceptance; no application rebuild.',flush=True)
 time.sleep(20)
for field in ('player','display','native'):
 r=api(f'repos/{REPO}/actions/runs/{spec[field]}');assert r['status']=='completed' and r['conclusion']=='success',(field,r['status'],r['conclusion'])
 if field!='native':assert r['head_sha']==source
producer=api(f'repos/{REPO}/actions/runs/{spec["developer"]}')
assert producer['head_sha']==source and producer['run_number']==number and producer['status']=='completed'
producer_jobs=api(f'repos/{REPO}/actions/runs/{spec["developer"]}/jobs')['jobs']
assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in producer_jobs)
original_job,=[j for j in producer_jobs if j['name']=='android-smoke-check']
assert producer['conclusion']=='failure' and original_job['conclusion']=='failure'
# The original single obsolete About expectation is preserved, not relabelled as passed.
original_log=subprocess.check_output(['gh','api','--allow-escape-sequences',f'repos/{REPO}/actions/jobs/{original_job["id"]}/logs']).decode('utf-8')
assert 'Tests run: 69,  Failures: 1' in original_log or re.search(r'Tests run:\s*69,\s*Failures:\s*1',original_log)
assert 'everyAboutPopupOpensScrollsAndCloses' in original_log
(OUT/'API34-original-failed-job-console.txt').write_text(original_log)
# Delivery tooling/docs may change; application/build inputs must remain exact.
trees=[api(f'repos/{REPO}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])];assert all(not x.get('truncated') for x in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
dev,da=artifact(spec['developer'],'apd-codex-private-test-apk');player,pa=artifact(spec['player'],'apd-player-matching-developer');core,ca=artifact(spec['developer'],'apd-codex-core-tests');native,na=artifact(spec['native'],'apd-test346-complete-api34-retry')
# Manual exact-APK instrumentation prints terminal records to the immutable job console.
# Convert those actual records to JUnit; do not infer success from a job status or screenshot.
if not list(native.rglob('TEST-*.xml')):
    import xml.etree.ElementTree as ET
    job,=[j for j in api(f'repos/{REPO}/actions/runs/{spec["native"]}/jobs')['jobs'] if j['name']=='exact-apk-full-api34']
    raw=subprocess.check_output(['gh','api','--allow-escape-sequences',f'repos/{REPO}/actions/jobs/{job["id"]}/logs']).decode('utf-8')
    clean=re.sub(r'\x1b\[[0-9;]*m','',raw)
    clean=re.sub(r'(?m)^\d{4}-\d\d-\d\dT[0-9:.]+Z\s*','',clean)
    parts=re.split(r'INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)',clean);cases=[]
    for i in range(0,len(parts)-1,2):
        block,code=parts[i],int(parts[i+1]);cls=re.findall(r'(?m)^INSTRUMENTATION_STATUS: class=(.+)$',block);name=re.findall(r'(?m)^INSTRUMENTATION_STATUS: test=(.+)$',block)
        if code==1:continue
        if cls and name:
            assert code==0,(cls[-1],name[-1],code)
            cases.append((cls[-1].strip(),name[-1].strip()))
    assert len(cases)==len(set(cases))==spec['android_tests'],len(cases)
    assert re.search(r'OK \('+str(len(cases))+r' tests?\)',clean)
    suite=ET.Element('testsuite',name='ExactAPKAndroidJUnit',tests=str(len(cases)),failures='0',errors='0',skipped='0')
    for cls,name in cases:ET.SubElement(suite,'testcase',classname=cls,name=name)
    ET.ElementTree(suite).write(native/'TEST-exact-apk.xml',encoding='utf-8',xml_declaration=True)
    (OUT/'API34-exact-apk-retest-job-console.txt').write_text(raw)
native_proof,=native.rglob('acceptance-source.json');np=json.loads(native_proof.read_text())
assert np['application_source']==source and np['application_run']==spec['developer'] and np['application_rebuilt'] is False and np['application_inputs_unchanged'] is True
native_tree=api(f'repos/{REPO}/git/trees/{np["test_source"]}?recursive=1');assert not native_tree.get('truncated')
assert application_inputs(trees[0]['tree'])==application_inputs(native_tree['tree'])
results={'core':xml_summary(core),'player_core':xml_summary(player),'android':xml_summary(native)}
assert results['core']['tests']==results['player_core']['tests']==spec['core_tests']==2287
assert results['android']['tests']==spec['android_tests']
original_clean=re.sub(r'(?m)^\d{4}-\d\d-\d\dT[0-9:.]+Z\s*','',re.sub(r'\x1b\[[0-9;]*m','',original_log))
original_parts=re.split(r'INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)',original_clean);original_cases={}
for i in range(0,len(original_parts)-1,2):
 block,code=original_parts[i],int(original_parts[i+1]);cls=re.findall(r'(?m)^INSTRUMENTATION_STATUS: class=(.+)$',block);name=re.findall(r'(?m)^INSTRUMENTATION_STATUS: test=(.+)$',block)
 if code!=1 and cls and name:original_cases[(cls[-1].strip(),name[-1].strip())]=code
assert len(original_cases)==69 and sum(code!=0 for code in original_cases.values())==1
assert [name for (cls,name),code in original_cases.items() if code!=0]==['everyAboutPopupOpensScrollsAndCloses']
assert set(original_cases)=={(c['classname'],c['name']) for c in results['android']['cases']},'Retest lost or replaced an original test'

assert any(c['name']=='grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively' for c in results['android']['cases'])
visual,va=artifact(spec['developer'],'apd-codex-android-checks')
images={}
assert set(spec['visual_review'])=={'qol-dew-settings.png','qol-force-weapon.png','qol-sharpshooting-weapon.png','qol-searched-wall.png','qol-offline-license.png'}
for image in native.rglob('*.png'):
 with Image.open(image) as im:im.verify()
for name,expected in spec['visual_review'].items():
 paths=list(visual.rglob(name));assert len(paths)==1;assert sha(paths[0])==expected
 with Image.open(paths[0]) as im:assert im.size==(320,640);im.verify()
 images[name]=expected
profiles={};jobs=api(f'repos/{REPO}/actions/runs/{spec["display"]}/jobs')['jobs'];assert len(jobs)==6 and all(j['conclusion']=='success' for j in jobs)
for name in ('m53-representative','compact-virtual','small-virtual','square-virtual','short-landscape','redmagic-representative-api36'):
 folder,a=artifact(spec['display'],'apd-title-display-'+name);summary=xml_summary(folder)
 assert summary['tests']==(3 if name.endswith('api36') else 1)
 proof,=folder.rglob('acceptance-source.json');p=json.loads(proof.read_text());assert p['application_source']==source and p['application_run']==spec['developer'] and p['application_rebuilt'] is False
 profiles[name]={'artifact':a['id'],'tests':summary['tests'],'application_sha256':p['application_sha256']}
 # All retained image files must decode, not only a representative capture.
 for image in folder.rglob('*.png'):
  with Image.open(image) as im:im.verify()
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
expected_cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
variants={};assets={};hashes={};files=[]
for label,folder in [('Developer',dev),('Alpha-Testers',player)]:
 apk,=folder.rglob('*.apk');assert certificates(cmd(str(signer),'verify','--print-certs',str(apk)))==[expected_cert]
 metadata=cmd(str(signer.parent/'aapt'),'dump','badging',str(apk));assert "name='com.charltonchris29.adhdpixeldungeon.indev'" in metadata and "versionCode='920'" in metadata
 version='4.0.8-apd-INDEV-b'+('C'+str(number)+'-TEST' if label=='Developer' else 'P'+str(number));assert "versionName='"+version+"'" in metadata
 target=OUT/f'APD-{label}-{number}.apk';target.write_bytes(apk.read_bytes());checksum=pathlib.Path(str(target)+'.sha256');checksum.write_text(sha(target)+'  '+target.name+'\n');files.extend([target,checksum]);hashes[label]=sha(target);variants[label]={'version_name':version,'sha256':sha(target)}
 with zipfile.ZipFile(apk) as z:
  assets[label]={n[7:]:z.read(n) for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
  assert assets[label]['legal/GPL-3.0.txt']==(ROOT/'LICENSE.txt').read_bytes()
  assert 'interfaces/apd_searched_wall.png' in assets[label]
  assert ('apd/features.txt' in assets[label])==(label=='Developer')
assert all(p['application_sha256']==hashes['Developer'] for p in profiles.values())
assert np['application_sha256']==hashes['Developer']
common=[a for a in assets['Developer'] if a!='apd/features.txt'];assert set(common)==set(assets['Alpha-Testers']) and all(assets['Developer'][n]==assets['Alpha-Testers'][n] for n in common)
# Package real producing source and every generated runtime asset, with only external signing substitutions.
snapshot={};removed=[]
with tarfile.open(fileobj=io.BytesIO(subprocess.check_output(['git','archive',source]))) as archive:
 for member in archive:
  if not member.isfile():continue
  if member.name.startswith('keystore/') or member.name.lower().endswith(('.jks','.keystore','.p12','.pfx')):removed.append(member.name);continue
  snapshot[member.name]=archive.extractfile(member).read()
config=snapshot['android/build.gradle'].decode();credentials=set(re.findall(r"(?:storePassword|keyPassword)\s+'([^']+)'",config));substitutions=[]
for n,b in list(snapshot.items()):
 try:s=b.decode('utf-8')
 except UnicodeDecodeError:continue
 for value in credentials:s=s.replace(value,'EXTERNAL_SIGNING_PASSWORD')
 if s.encode()!=b:substitutions.append(n);snapshot[n]=s.encode()
config=snapshot['android/build.gradle'].decode().replace("rootProject.file('keystore/apd-debug.jks')","rootProject.file(System.getenv('APD_SIGNING_KEYSTORE') ?: 'keystore/EXTERNAL-KEY-REQUIRED.jks')")
config=re.sub(r"storePassword\s+'EXTERNAL_SIGNING_PASSWORD'","storePassword System.getenv('APD_SIGNING_STORE_PASSWORD') ?: ''",config);config=re.sub(r"keyPassword\s+'EXTERNAL_SIGNING_PASSWORD'","keyPassword System.getenv('APD_SIGNING_KEY_PASSWORD') ?: System.getenv('APD_SIGNING_STORE_PASSWORD') ?: ''",config)
config=config.replace('signingConfig signingConfigs.apdDebug',"signingConfig project.hasProperty('apdUnsignedVerification') ? null : signingConfigs.apdDebug");snapshot['android/build.gradle']=config.encode()
for n in common:snapshot['core/src/main/assets/'+n]=assets['Developer'][n]
snapshot['CORRESPONDING-SOURCE/generated-inputs/apd/features.txt']=assets['Developer']['apd/features.txt']
readme=f'''APD source for both owner APK variants {number}\nProducing commit {source}; tag {tag}; versionCode920.\nJDK21; Android SDK36; included Gradle9.5 wrapper. Set ANDROID_HOME/local.properties.\nDeveloper: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdTestBuild -PapdBuild=C{number}\nAlpha-Testers: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdBuild=P{number}\nThese recipes build unsigned reconstructions. For an install-over update, supply the EXISTING external APD key via APD_SIGNING_KEYSTORE / APD_SIGNING_STORE_PASSWORD (optional separate key password) and omit -PapdUnsignedVerification. A different signing key cannot update existing installations; never uninstall or clear player data to work around signing.\nNo key/password/Git history is included. All generated history TSVs and runtime assets match the verified APKs. Developer features are generated by the preserved Gradle flag; the exact input is copied outside the asset root. Signing-only substitutions are explicit in manifest.json. Source reconstruction was not separately rebuilt; do not infer signed-byte reproducibility. GPLv3 or later, no warranty; original attribution/notices retained. Separate compliance audit findings remain documented; no public or external tester distribution was performed here.\n'''
snapshot['docs/SIGNING.md']=readme.encode();substitutions.append('docs/SIGNING.md (replace outdated signing advice with exact safe build instructions)')
snapshot['CORRESPONDING-SOURCE/README.txt']=readme.encode();manifest={'source_commit':source,'source_tag':tag,'variants':variants,'removed_signing_paths':removed,'credential_substitutions':substitutions,'signing_config_externalized':True,'generated_history_tsv_count':sum(n.startswith('core/src/main/assets/apd/history/') and n.endswith('.tsv') for n in snapshot),'files':{n:hashlib.sha256(b).hexdigest() for n,b in sorted(snapshot.items())}}
for n,b in snapshot.items():assert not any(v.encode() in b for v in credentials)
snapshot['CORRESPONDING-SOURCE/manifest.json']=(json.dumps(manifest,indent=2)+'\n').encode();sourcezip=OUT/f'APD-Source-{number}.zip'
with zipfile.ZipFile(sourcezip,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
 for n,b in sorted(snapshot.items()):
  info=zipfile.ZipInfo(f'APD-Source-{number}/'+n,date_time=(2026,11,2,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=(0o100755 if n=='gradlew' or n.endswith('.sh') else 0o100644)<<16;archive.writestr(info,b)
side=pathlib.Path(str(sourcezip)+'.sha256');side.write_text(sha(sourcezip)+'  '+sourcezip.name+'\n');files.extend([sourcezip,side])
# Preserve the existing verified fallback and verify its actual uploaded APK assets before any new write.
fallback=api(f'repos/{REPO}/releases/tags/apd-alpha-preview-341');assert fallback['id']==428527455
for label,h in [('Developer','ad6281b3e41fe414187ca40824157a5bbcf102d7d86e569dbdb8439e7f47d694'),('Alpha-Testers','639ce15ddb4b02d037cd29f995c9f38514e03cf577370cf38488f4602bfd65bd')]:assert any(a['name']==f'APD-{label}-341.apk' and a.get('digest')=='sha256:'+h for a in fallback['assets'])
evidence={'source_commit':source,'tag':tag,'number':number,'runs':{k:spec[k] for k in ('developer','player','display','native')},'test_summaries':results,'core_tests':2287,'android_tests':results['android']['tests'],'profiles':profiles,'signing_certificate':expected_cert,'application_id':'com.charltonchris29.adhdpixeldungeon.indev','version_code':920,'apk_hashes':hashes,'source_sha256':sha(sourcezip),'visual_review':images,'visual_capture_artifact':va['id'],'visual_capture_note':'Reviewed five QoL captures from original exact-APK suite; its unrelated obsolete About test failed and is retained. Full corrected same-APK acceptance is separately bound to native retest.','physical_phone_verified':False,'new_application_apks_created':2,'owner_private_delivery':True,'fallback_preserved':True,'prepared_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'original_native_failure':{'job':original_job['id'],'tests':69,'failures':1,'method':'everyAboutPopupOpensScrollsAndCloses','resolution':'Corrected test for existing Source & Licenses menu; complete 69-case retest passed against the exact original APK, without rebuilding'},'retained_auxiliary_failures':{'41520121956':'Obsolete stager rejected changed application inputs against a historical source; no APK test executed','41520121884':'Historical death workflow referenced an unfetched obsolete source; no APK test executed','41520121870':'Historical hotbar proof has obsolete source constraints; retained separately, not represented as passed'}}
receipt=OUT/f'APD-Preview-{number}-Verification.json';receipt.write_text(json.dumps(evidence,indent=2)+'\n');files.append(receipt)
notes=f'''Grass dew auto-pickup (separate normal pickup turn; visible-enemy safeguard and options), saved manual-search wall markers, and equipped-ring contribution readouts in affected-item descriptions, including Force and Sharpshooting. Includes Public Domain JSON compatibility and offline GPL notices. Ring formulas,357 slider multipliers, original scoring, saves/statistics and drop rates are unchanged.\n\nSource `{source}`; core2287 and API34{results['android']['tests']} tests passed; all six display profiles passed, including API36. This is automated/emulator evidence, not physical-phone acceptance. The original API34 run had one obsolete About-popup expectation (68/69 passed); the corrected complete 69-case suite passed against the same APK without rebuilding. Exact signatures/app ID verified. Install over APD; never uninstall or clear data.\n\n'''
for label in ('Developer','Alpha-Testers'):notes+=f'- [{label} APK](https://github.com/{REPO}/releases/download/{tag}/APD-{label}-{number}.apk) — SHA256 `{hashes[label]}`\n'
notes+=f'\n[Matching source for both variants](https://github.com/{REPO}/releases/download/{tag}/{sourcezip.name}) — SHA256 `{sha(sourcezip)}`. Build instructions and generated inputs are included; signing material is excluded. GPLv3 or later, no warranty; unofficial independent fork with original credits retained. Separate GPL audit findings, including historical key exposure and inherited license exceptions, remain recorded and are not declared closed by this owner preview.\n\nExisting341 fallback and all assets are preserved. Private owner delivery only; no automatic posting to external testers or public release.\n';body=OUT/'release-notes.md';body.write_text(notes)
# Preparation is separated from publication so scan and reviewed archive evidence can be inspected first.
print(json.dumps({'prepared':True,'files':[p.name for p in files],'hashes':hashes,'source_sha256':sha(sourcezip),'history_count':manifest['generated_history_tsv_count']}))
