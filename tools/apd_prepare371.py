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

"""Prepare the authorized private owner pair, retaining exact evidence and source."""
import datetime,hashlib,io,json,os,pathlib,re,subprocess,tarfile,zipfile
from apd_existing_build_evidence import certificates,xml_summary
from apd_owner_delivery_policy import require_apk_delivery_allowed
ROOT=pathlib.Path.cwd();OUT=ROOT/'owner-delivery';OUT.mkdir(exist_ok=True)
require_apk_delivery_allowed(ROOT)
source=os.environ.get('APD_SOURCE_SHA',os.environ['GITHUB_SHA']);number=371;tag='apd-alpha-preview-371'
sha=lambda p:hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
repo=os.environ['GITHUB_REPOSITORY'];assert api('repos/'+repo)['private']
summary=xml_summary('core/build/test-results/test');assert summary['tests']==2918
assert len(list(pathlib.Path('core/build/test-results/test').glob('TEST-*.xml')))==447
signer=sorted(pathlib.Path(os.environ['ANDROID_HOME']).glob('build-tools/*/apksigner'))[-1]
expected='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
assets={};variants={};hashes={};files=[]
for label,version in [('Developer','4.0.8-apd-INDEV-bC371-TEST'),('Alpha-Testers','4.0.8-apd-INDEV-bP371')]:
 apk=OUT/f'APD-{label}-371.apk'
 cert=subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],text=True);assert certificates(cert)==[expected]
 meta=subprocess.check_output([str(signer.parent/'aapt'),'dump','badging',str(apk)],text=True)
 assert "name='com.charltonchris29.adhdpixeldungeon.indev'" in meta and "versionCode='920'" in meta and f"versionName='{version}'" in meta
 side=pathlib.Path(str(apk)+'.sha256');side.write_text(sha(apk)+'  '+apk.name+'\n');files.extend([apk,side]);hashes[label]=sha(apk);variants[label]=dict(version_name=version,sha256=sha(apk))
 with zipfile.ZipFile(apk) as z:
  assets[label]={n[7:]:z.read(n) for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
  assert assets[label]['legal/GPL-3.0.txt']==(ROOT/'LICENSE.txt').read_bytes()
assert assets['Developer']['apd/features.txt']==b'testBuild\ngrudge\ndemoUnlock\nclericPlayable\n'
assert assets['Alpha-Testers']['apd/features.txt']==b'extendedPlayback\n'
common=[a for a in assets['Developer'] if a!='apd/features.txt']
assert set(common)==set(assets['Alpha-Testers'])-{'apd/features.txt'}
assert all(assets['Developer'][n]==assets['Alpha-Testers'][n] for n in common)
# Require every native command to have independently completed; no incomplete or skipped evidence.
for label in ['developer','alpha']:
 for case in ['seed','restart','gameplay']:
  text=pathlib.Path(f'evidence/{label}-{case}.txt').read_text()
  assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text
  assert len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0$',text,re.M))==1
  assert not any(s in text for s in ['FAILURES','INSTRUMENTATION_FAILED','Process crashed','AssumptionFailure'])
text=pathlib.Path('evidence/developer-qol.txt').read_text();assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text and 'FAILURES' not in text
for label in ['developer','alpha']:
 text=pathlib.Path(f'evidence/{label}-wall.txt').read_text()
 assert 'WallGeometryDeviceTest' in text and 'observedGeometryAndRetainedMarksUseTheActualAndroidLayer' in text
 assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text
 assert len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0$',text,re.M))==1
 assert not any(t in text for t in ['FAILURES','INSTRUMENTATION_FAILED','Process crashed','AssumptionFailure'])
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
snapshot['CORRESPONDING-SOURCE/generated-inputs/alpha/apd/features.txt']=assets['Alpha-Testers']['apd/features.txt']
readme=f'''APD source for both owner APK variants {number}\nProducing commit {source}; tag {tag}; versionCode920.\nJDK21; Android SDK36; included Gradle9.5 wrapper. Set ANDROID_HOME/local.properties.\nDeveloper: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdTestBuild -PapdBuild=C{number}\nAlpha-Testers: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdBuild=P{number}\nThese recipes build unsigned reconstructions. For an install-over update, supply the EXISTING external APD key via APD_SIGNING_KEYSTORE / APD_SIGNING_STORE_PASSWORD (optional separate key password) and omit -PapdUnsignedVerification. A different signing key cannot update existing installations; never uninstall or clear player data to work around signing.\nNo key/password/Git history is included. All generated history TSVs and runtime assets match the verified APKs. Developer features are generated by the preserved Gradle flag; the exact input is copied outside the asset root. Signing-only substitutions are explicit in manifest.json. Source reconstruction was not separately rebuilt; do not infer signed-byte reproducibility. GPLv3 or later, no warranty; original attribution/notices retained. Separate compliance audit findings remain documented; no public or external tester distribution was performed here.\n'''
snapshot['docs/SIGNING.md']=readme.encode();substitutions.append('docs/SIGNING.md (replace outdated signing advice with exact safe build instructions)')
snapshot['CORRESPONDING-SOURCE/README.txt']=readme.encode();manifest={'source_commit':source,'source_tag':tag,'variants':variants,'removed_signing_paths':removed,'credential_substitutions':substitutions,'signing_config_externalized':True,'generated_history_tsv_count':sum(n.startswith('core/src/main/assets/apd/history/') and n.endswith('.tsv') for n in snapshot),'files':{n:hashlib.sha256(b).hexdigest() for n,b in sorted(snapshot.items())}}
for n,b in snapshot.items():assert not any(v.encode() in b for v in credentials)
snapshot['CORRESPONDING-SOURCE/manifest.json']=(json.dumps(manifest,indent=2)+'\n').encode();sourcezip=OUT/f'APD-Source-{number}.zip'
with zipfile.ZipFile(sourcezip,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
 for n,b in sorted(snapshot.items()):
  info=zipfile.ZipInfo(f'APD-Source-{number}/'+n,date_time=(2026,11,2,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=(0o100755 if n=='gradlew' or n.endswith('.sh') else 0o100644)<<16;archive.writestr(info,b)
side=pathlib.Path(str(sourcezip)+'.sha256');side.write_text(sha(sourcezip)+'  '+sourcezip.name+'\n');files.extend([sourcezip,side])

proof=dict(source_commit=source,number=371,tag=tag,core_tests=2918,android_tests=9,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,signing_certificate=expected,apk_hashes=hashes,source_sha256=sha(sourcezip),physical_phone_verified=False,whole_semantic_audit_complete=False,wall_marker_fix_complete=False,reproduced_unknown_geometry_false_marks_fixed=True,new_application_apks_created=2,prepared_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),workflow_run=os.environ['GITHUB_RUN_ID'],native_scope='API34: actual wall-layer regression and three trap-setting/persistence/movement cases in each variant; combined QoL UI case in Developer. Nine targeted cases; not a complete Android suite.',fallback_preserved=True)
if os.environ.get('APD_REUSE_371')=='true':
 proof.update(application_rebuilt=False,new_application_apks_created=0,original_pair_apks_created=2,retained_producer_run=42404197897,retained_producer_source=source,original_producer_conclusion='failure',original_interruption='adb closed during force-stop; emulator host connection lost; original failure retained',verification_source=os.environ['GITHUB_SHA'],graphics_environment='API34 software swiftshader, Vulkan disabled; separate VM per variant')
(OUT/'APD-Preview-371-Verification.json').write_text(json.dumps(proof,indent=2)+'\n')
notes=f"""Fixes a reproduced cause of incorrect wall-search feedback in370: unseen side tiles were treated as valid door frames and unseen approach tiles as valid passage space. Positive wall marks and per-wall feedback now require observed or remembered side frames and at least one observed approach. The hidden far side is never inspected. Saved uncertain investigations still resume with ordinary Search time/cost, without falsely declaring the location impossible. Existing history is retained and valid known marks survive loss of sight and save/reload. Search probabilities, turn costs, door generation and balance are unchanged.

Full2918 core tests/447 suites passed, including7 new regressions and generated secrets across6 seeds/26 floors, with one-sided visibility. Nine targeted API34 emulator cases passed on these exact APKs: actual Android wall-layer assertions in both variants, existing trap movement/preferences in both, and Developer combined QoL UI checks. Physical-phone acceptance remains open. This resolves the reproduced cases, not a universal claim that every wall report is solved.

Includes all completed work from370. The wider description/translations audit remains unfinished. Install over existing APD; never uninstall or clear player data. Application ID, signing identity and versionCode920 preserved.

Exact source `{source}`, tag `{tag}`. [Matching source](https://github.com/{repo}/releases/download/{tag}/APD-Source-371.zip), SHA256 `{sha(sourcezip)}`. Includes scripts/assets/generated history; signing material excluded. Unsigned reconstruction not separately rebuilt; signed-byte reproducibility not claimed. GPLv3-or-later, no warranty. Unofficial modified Shattered Pixel Dungeon by Evan Debenham, based on Pixel Dungeon by Oleg Dolya/Watabou; APD modifications by Chris Charlton. No endorsement. Existing compliance findings remain recorded.

Private owner publication only; existing370/369 releases/assets retained. No external tester posting or online activation.
"""
(OUT/'release-notes.md').write_text(notes)
spec=dict(number=371,source=source,core_tests=2918,android_tests=9,title='APD371 — observed-geometry wall search correction',variants=['Developer','Alpha-Testers'])
pathlib.Path('docs/APD-371-DELIVERY-SPEC.json').write_text(json.dumps(spec,indent=2)+'\n')
print(json.dumps(proof))
