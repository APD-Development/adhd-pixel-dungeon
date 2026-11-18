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
source=os.environ.get('APD_SOURCE_SHA',os.environ['GITHUB_SHA']);number=386;tag='apd-alpha-preview-386'
sha=lambda p:hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
repo=os.environ['GITHUB_REPOSITORY'];assert api('repos/'+repo)['private']
summary=xml_summary('core/build/test-results/test');assert summary['tests']==3040
assert len(list(pathlib.Path('core/build/test-results/test').glob('TEST-*.xml')))==457
signer=sorted(pathlib.Path(os.environ['ANDROID_HOME']).glob('build-tools/*/apksigner'))[-1]
expected='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
assets={};variants={};hashes={};files=[]
for label,version in [('Developer','4.0.8-apd-INDEV-bC386-TEST'),('Alpha-Testers','4.0.8-apd-INDEV-bP386')]:
 apk=OUT/f'APD-{label}-386.apk'
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
for label in ['developer','alpha']:
 text=pathlib.Path(f'evidence/{label}-qol.txt').read_text();assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text and 'FAILURES' not in text
for label in ['developer','alpha']:
 text=pathlib.Path(f'evidence/{label}-wall.txt').read_text()
 assert 'WallGeometryDeviceTest' in text and 'observedGeometryAndRetainedMarksUseTheActualAndroidLayer' in text
 assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text
 assert len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0$',text,re.M))==1
 assert not any(t in text for t in ['FAILURES','INSTRUMENTATION_FAILED','Process crashed','AssumptionFailure'])
for label in ['developer','alpha']:
 text=pathlib.Path(f'evidence/{label}-freeze.txt').read_text()
 assert 'RemovedActorStallDeviceTest' in text and 'cancelledGhoulAttackReleasesQueueButLiveAttackStaysPending' in text
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

proof=dict(source_commit=source,number=386,tag=tag,core_tests=3040,android_tests=12,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,signing_certificate=expected,apk_hashes=hashes,source_sha256=sha(sourcezip),physical_phone_verified=False,whole_semantic_audit_complete=False,selective_calculations_scope_corrected=True,new_application_apks_created=2,prepared_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),workflow_run=os.environ['GITHUB_RUN_ID'],native_scope='API34: actual automatic inventory slots, stack quantities, holster tier/upgrade/wand order and movement after identification; preserved item/armor/Arcana UI, real GhoulSprite scheduler, wall layers and three trap-setting/persistence/movement cases per variant. Twelve targeted cases; not a full Android suite or physical-phone test.',fallback_preserved=True)
(OUT/'APD-Preview-386-Verification.json').write_text(json.dumps(proof,indent=2)+'\n')
notes=f"""Concise ordinary descriptions for four exotic potions, Staff imbue confirmation, Shard of Oblivion and Warrior Shield. Live shield recharge time and saved reduction appear separately, with no negative wait or programmer terminology. Shard explains its capped equipment/use bonus, one temporary wand/thrown contribution per type and manual identification.

Rot Darts retain the real Sharpshooting damage increase but no longer claim a durability bonus: their actual five-use tips are unchanged. Staff confirmation explains the +0staff exception while preserving known/unknown result and loss warnings. All385 corrections are preserved.

All3040core tests/457suites passed. Twelve targeted API34 Android cases across both original signed variants include the actual four Staff confirmations, four potion windows, six shield states, three Flail counts, sixteen Duelist known/unknown inspection sets, nine trinkets and fifteen artifact curse descriptions, plus preserved inventory, wall, trap and scheduler checks. Helper windows are not counted as separate test cases. No physical-phone verification claimed.

No gameplay, balance, audio or new-panel changes. Existing mechanic-specific panels remain pending owner review. The comprehensive audit remains incomplete:336source units fully/partially reviewed,298corrected,141complete,286unresolved. Automated coverage is not individual review completion. The owner recalls delayed audio only in SPD; no APD reproduction or audio fix is claimed.385 remains the published fallback.

Exact source `{source}`, tag `{tag}`. Both APKs, hashes, corresponding source and verification attached. Install over the existing app; never uninstall or clear data. Original signing, application ID and versionCode920 preserved. GPLv3-or-later, no warranty. APD by Chris Charlton modifies Shattered Pixel Dungeon by Evan Debenham, based on Pixel Dungeon by Oleg Dolya/Watabou; no endorsement.
"""
(OUT/'release-notes.md').write_text(notes)
spec=dict(number=386,source=source,core_tests=3040,android_tests=12,title='APD386 — plain item effects and accurate recharge text',variants=['Developer','Alpha-Testers'])
pathlib.Path('docs/APD-386-DELIVERY-SPEC.json').write_text(json.dumps(spec,indent=2)+'\n')
print(json.dumps(proof))
