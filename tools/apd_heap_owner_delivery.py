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
import datetime,hashlib,io,json,os,pathlib,re,subprocess,tarfile,zipfile
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
spec=json.loads(pathlib.Path('docs/APD-HEAP-OWNER-DELIVERY.json').read_text());source=spec['source'];number=spec['number'];tag=f'apd-alpha-preview-{number}'
assert api('repos/'+REPO)['private']
for field in ('developer','display','compile','native','stable'):
 r=api(f'repos/{REPO}/actions/runs/{spec[field]}');assert r['status']=='completed' and (r['conclusion']=='success' or field in ('developer','compile') and r['conclusion']=='failure') and (field in ('compile','native','stable') or r['head_sha']==source),(field,r['status'],r['conclusion'])
assert api(f'repos/{REPO}/actions/runs/{spec["developer"]}')['run_number']==number
producer_jobs=api(f'repos/{REPO}/actions/runs/{spec["developer"]}/jobs')['jobs']
assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in producer_jobs)
native_job,=[j for j in producer_jobs if j['name']=='android-smoke-check']
assert native_job['conclusion']=='failure' # admitted only by the exact actual console evidence below
original_wrapper_log=subprocess.check_output(['gh','api','--allow-escape-sequences',f'repos/{REPO}/actions/jobs/{native_job["id"]}/logs']).decode()
assert 'tools/run-corner-search-check.sh: line 4: rg: command not found' in original_wrapper_log and 'OK (1 test)' in original_wrapper_log
(OUT/'api34-original-wrapper-failure.txt').write_text(original_wrapper_log)

# Delivery tooling/docs may change; application/build inputs must remain exact.
trees=[api(f'repos/{REPO}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])];assert all(not x.get('truncated') for x in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
compile_source=api(f'repos/{REPO}/actions/runs/{spec["compile"]}')['head_sha']
compile_tree=api(f'repos/{REPO}/git/trees/{compile_source}?recursive=1');assert not compile_tree.get('truncated')
assert application_inputs(trees[0]['tree'])==application_inputs(compile_tree['tree'])
if spec['compile']==spec['developer']:
 compilation_job,=[j for j in api(f'repos/{REPO}/actions/runs/{spec["developer"]}/jobs')['jobs'] if j['name']=='verify-and-build']
 assert any(step['name']=='Complete core, Android debug/release and desktop checks' and step['conclusion']=='success' for step in compilation_job['steps'])
else:
 compile_reports,coa=artifact(spec['compile'],'apd-qol-source-verification')
 compile_proof,=compile_reports.rglob('qol-verification.json');cp=json.loads(compile_proof.read_text())
 assert cp['source_commit']==compile_source and cp['tests']==2302 and cp['apk_assembled'] is False
 assert xml_summary(compile_reports)['tests']==2302

dev,da=artifact(spec['developer'],'apd-codex-private-test-apk');core,ca=artifact(spec['developer'],'apd-codex-core-tests');native,na=artifact(spec['native'],'apd-test350-corner-retest')
# Manual exact-APK instrumentation prints terminal records to the immutable job console.
# Convert those actual records to JUnit; do not infer success from a job status or screenshot.
if not list(native.rglob('TEST-*.xml')):
    import xml.etree.ElementTree as ET
    job,=[j for j in api(f'repos/{REPO}/actions/runs/{spec["native"]}/jobs')['jobs'] if j['name']=='exact-apk-full-api34' and j['conclusion']=='success']
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
    (OUT/'API34-original-job-console.txt').write_text(raw)
np_file,=native.rglob('acceptance-source.json');np=json.loads(np_file.read_text())
assert np['application_source']==source and np['application_run']==spec['developer'] and np['application_rebuilt'] is False and np['application_inputs_unchanged'] is True
assert np['test_source']==api(f'repos/{REPO}/actions/runs/{spec["native"]}')['head_sha']
nt=api(f'repos/{REPO}/git/trees/{np["test_source"]}?recursive=1');assert not nt.get('truncated') and application_inputs(nt['tree'])==application_inputs(trees[0]['tree'])
stable,sa=artifact(spec['stable'],'apd-test350-stable-api36')
sp_file,=stable.rglob('acceptance-source.json');sp=json.loads(sp_file.read_text())
assert sp['application_source']==source and sp['application_run']==spec['developer'] and sp['application_rebuilt'] is False and sp['application_inputs_unchanged'] is True
assert sp['test_source']==api(f'repos/{REPO}/actions/runs/{spec["stable"]}')['head_sha']
st=api(f'repos/{REPO}/git/trees/{sp["test_source"]}?recursive=1');assert not st.get('truncated') and application_inputs(st['tree'])==application_inputs(trees[0]['tree'])
stable_console,=stable.rglob('corner-instrumentation.txt');stable_text=stable_console.read_text()
assert 'OK (1 test)' in stable_text and 'INSTRUMENTATION_STATUS_CODE: -2' not in stable_text and 'INSTRUMENTATION_STATUS_CODE: -3' not in stable_text
assert 'QolStableCaptureDeviceTest' in stable_text
for name,expected in spec['stable_visual_review'].items():
 path,=stable.rglob(name);assert sha(path)==expected
 with Image.open(path) as im:assert im.size==(1216,2688);im.verify()
results={'core':xml_summary(core),'android':xml_summary(native)}
assert results['core']['tests']==spec['core_tests']==2302
assert results['android']['tests']==spec['android_tests']
# The owner changed only corner marker eligibility after the green full gameplay suite.
baseline_source='03522e5a6813ea6881b0fb8b70a85c7806e3411e';baseline_run=41536788270
baseline=api(f'repos/{REPO}/actions/runs/{baseline_run}');assert baseline['head_sha']==baseline_source and baseline['conclusion']=='success'
bt=api(f'repos/{REPO}/git/trees/{baseline_source}?recursive=1');assert not bt.get('truncated')
ba,bb=application_inputs(bt['tree']),application_inputs(trees[0]['tree'])
changed={p for p in ba.keys()|bb.keys() if ba.get(p)!=bb.get(p)}
assert changed=={'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/qol/SearchedWallMarkers.java'}
baseline_job,=[j for j in api(f'repos/{REPO}/actions/runs/{baseline_run}/jobs')['jobs'] if j['name']=='android-smoke-check' and j['conclusion']=='success']
baseline_raw=subprocess.check_output(['gh','api','--allow-escape-sequences',f'repos/{REPO}/actions/jobs/{baseline_job["id"]}/logs']).decode()
baseline_clean=re.sub(r'(?m)^\d{4}-\d\d-\d\dT[0-9:.]+Z\s*','',re.sub(r'\x1b\[[0-9;]*m','',baseline_raw))
parts=re.split(r'INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)',baseline_clean);baseline_cases=[]
for i in range(0,len(parts)-1,2):
 block,code=parts[i],int(parts[i+1]);cls=re.findall(r'(?m)^INSTRUMENTATION_STATUS: class=(.+)$',block);name=re.findall(r'(?m)^INSTRUMENTATION_STATUS: test=(.+)$',block)
 if code==1:continue
 if cls and name:assert code==0;baseline_cases.append((cls[-1].strip(),name[-1].strip()))
assert len(baseline_cases)==len(set(baseline_cases))==69 and 'OK (69 tests)' in baseline_clean
assert any(name=='warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms' for cls,name in baseline_cases)
(OUT/'baseline349-complete-gameplay-console.txt').write_text(baseline_raw)

assert any(c['name']=='grassDewControlsRingBreakdownsSearchedWallAndOfflineLicenseRenderNatively' for c in results['android']['cases'])
visual,va=native,na
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
 assert summary['tests']==(4 if name.endswith('api36') else 1)
 proof,=folder.rglob('acceptance-source.json');p=json.loads(proof.read_text());assert p['application_source']==source and p['application_run']==spec['developer'] and p['application_rebuilt'] is False
 profiles[name]={'artifact':a['id'],'tests':summary['tests'],'application_sha256':p['application_sha256']}
 # All retained image files must decode, not only a representative capture.
 for image in folder.rglob('*.png'):
  with Image.open(image) as im:im.verify()
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
expected_cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
variants={};assets={};hashes={};files=[]
for label,folder in [('Developer',dev)]:
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
assert np['application_sha256']==sp['application_sha256']==hashes['Developer']
common=[a for a in assets['Developer'] if a!='apd/features.txt'];assert len(common)>0
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
readme=f'''APD source for the urgent Developer APK {number}\nProducing commit {source}; tag {tag}; versionCode920.\nJDK21; Android SDK36; included Gradle9.5 wrapper. Set ANDROID_HOME/local.properties.\nDeveloper: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdTestBuild -PapdBuild=C{number}\nThese recipes build unsigned reconstructions. For an install-over update, supply the EXISTING external APD key via APD_SIGNING_KEYSTORE / APD_SIGNING_STORE_PASSWORD (optional separate key password) and omit -PapdUnsignedVerification. A different signing key cannot update existing installations; never uninstall or clear player data to work around signing.\nNo key/password/Git history is included. All generated history TSVs and runtime assets match the verified APKs. Developer features are generated by the preserved Gradle flag; the exact input is copied outside the asset root. Signing-only substitutions are explicit in manifest.json. Source reconstruction was not separately rebuilt; do not infer signed-byte reproducibility. GPLv3 or later, no warranty; original attribution/notices retained. Separate compliance audit findings remain documented; no public or external tester distribution was performed here.\n'''
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
evidence={'source_commit':source,'tag':tag,'number':number,'runs':{k:spec[k] for k in ('developer','display','compile','native','stable')},'test_summaries':results,'core_tests':2302,'android_tests':results['android']['tests'],'profiles':profiles,'signing_certificate':expected_cert,'application_id':'com.charltonchris29.adhdpixeldungeon.indev','version_code':920,'apk_hashes':hashes,'source_sha256':sha(sourcezip),'visual_review':images,'baseline_full_gameplay':{'source':baseline_source,'run':baseline_run,'tests':69,'all_other_runtime_inputs_unchanged':True,'changed_paths':sorted(changed),'cases':baseline_cases},'stable_api36_visual_review':spec['stable_visual_review'],'stable_api36_artifact':sa['id'],'initial_android_wrapper_failure':{'run':spec['developer'],'job':native_job['id'],'reason':'rg command missing after OK (1 test); screenshot-copy stage was not reached','api34_case_passed':True,'api34_images_validated':True,'repaired_exact_apk_retest':spec['native'],'superseded_api36_capture_artifact':13187047441,'replacement_frame_settled_api36':spec['stable']},'physical_phone_verified':False,'new_application_apks_created':1,'owner_private_delivery':True,'fallback_preserved':True,'prepared_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'baseline_memory_probe':{'run':41530223216,'apd_launched':False,'reason':'Insufficient disk for emulator userdata; no reproduction or heap dump'},'phone_allocation_origin_confirmed':False,'failed_fresh_api36_probe':{'run':41542973206,'artifact':13188385115,'observed_failure':'AndroidGraphics pause synchronization timeout; app sent itself SIG9; no Java OOM in this run','cause_established':False},'retained_candidates':{'347':'67/69 Android methods; two prewarm-admission failures and rejected southern marker alignment; never published','348':'Verification cancelled after findings; never published','349':'All 69 methods and six profiles passed; unpublished because owner then clarified the partial corner-end markers'},'retained_auxiliary_failures':{'41520121956':'Obsolete stager rejected changed application inputs against a historical source; no APK test executed','41520121884':'Historical death workflow referenced an unfetched obsolete source; no APK test executed','41520121870':'Historical hotbar proof has obsolete source constraints; retained separately, not represented as passed'}}
receipt=OUT/f'APD-Preview-{number}-Verification.json';receipt.write_text(json.dumps(evidence,indent=2)+'\n');files.append(receipt)
notes=f'''Urgent memory stability and searched-wall correction. Gameplay releases unused statistics catalogs/results, cancels unfinished preloads and stale completions, and applies a conservative memory budget to optional title preparation. Lower and side searched-wall indicators follow the actual visible wall edges under existing fog/blockers. Search chances, timing, raw history, saves and scores are unchanged.

The owner's TEST346 diagnostic confirms Java heap exhaustion at256MiB while reportedly searching. Source inspection confirms avoidable catalog retention; the earlier real export retained29,104,416 JVM bytes for69,169 derived rows. That desktop measurement does not identify the exact phone allocation. BaselineAPI36 probe41530223216 never launched APD: emulator userdata had6814.48MB free versus7372.80MB required. Its failure is retained, not counted as APD evidence.

Exact source `{source}`; all2302 core tests, API34 targeted manual Search/corner/straight-wall/cache/resume acceptance ({results['android']['tests']} method), and all six display profiles passed. Complete69-case gameplay evidence is from TEST349; a source-tree guard proves that only marker-selection Java changed, and all memory/gameplay logic is identical. API36 also exercised actual manual Search, four marker orientations, cache release and pause/resume. The API34 APD case passed, then its host wrapper failed because rg was unavailable before screenshot copying. That failure is retained; a repaired exact-APK API34 rerun passed and validated its captures. The earlier fresh API36 run41542973206 failed when AndroidGraphics killed the process after a pause synchronization timeout; its logs/artifact13188385115 are retained, and no Java OOM appeared in that run. A subsequent successful exact-APK run is required separately. Fresh frame-settled API36 captures were also decoded and visually reviewed; earlier incomplete popup captures are not treated as accepted evidence. Automated/emulator evidence only; physical-phone acceptance remains pending.

Rejected candidate347 had two prewarm-admission failures in its69-case run and a visually misplaced marker. Candidate348 was cancelled; neither was published. TEST349 passed all69 methods with the original72-run and250ms assertions unchanged. This final candidate adds the owner-circled corner-end suppression; its targeted tests assert both omitted ends and retained straight-wall checks.

This single developer APK is today's third and final delivered APK, following the346 pair. Install over APD; never uninstall or clear player data.

'''
notes+=f'- [Developer APK](https://github.com/{REPO}/releases/download/{tag}/APD-Developer-{number}.apk) — SHA256 `{hashes["Developer"]}`\n'
notes+=f'\n[Matching key-free source](https://github.com/{REPO}/releases/download/{tag}/{sourcezip.name}) — SHA256 `{sha(sourcezip)}`. Build instructions and generated assets are included; signing material is excluded. GPLv3 or later, no warranty, original attribution retained. Separate full GPL audit findings remain unclosed. Previous341/330 fallback assets are preserved.\n'
body=OUT/'release-notes.md';body.write_text(notes)
print(json.dumps({'prepared':True,'hashes':hashes,'source_sha256':sha(sourcezip)}))
