"""Deliver the exact verified private TEST341 pair; no builds, overwrites or PDF."""
import datetime,hashlib,json,os,pathlib,re,subprocess,sys,time,zipfile
sys.path.insert(0,'tools')
import apd_publish_owner_diagnostic329 as old
from apd_existing_build_evidence import application_inputs,certificates,xml_summary
from PIL import Image
repo=os.environ['GITHUB_REPOSITORY']
source=os.environ['APD_OWNER_SOURCE']
number=341
spec=json.loads(os.environ['APD_OWNER_RUNS'])
root=pathlib.Path('owner341-delivery');root.mkdir(exist_ok=True)
old.ROOT=pathlib.Path('verification-work');old.ROOT.mkdir(exist_ok=True)
api,cmd,artifact,digest=old.api,old.cmd,old.artifact,old.digest
assert api('repos/'+repo)['private'],'Private repository required'
# Preserve the already published fallback, independently of candidate acceptance.
fallback=api(f'repos/{repo}/releases/tags/apd-alpha-preview-330')
assert not fallback['draft'] and fallback['prerelease']
fallback_assets={}
for name,h in [('APD-Developer-330.apk','3e38b1164169e404c6606d2baa6d23f61fdb4e1625bb1be53c1424ba41c20842'),('APD-Alpha-Testers-330.apk','7cb850696d46ba5fe3d0c8c7185d783615f4d9e790d0795978fc9116ff57bcdf')]:
 a,=[a for a in fallback['assets'] if a['name']==name]
 assert a['state']=='uploaded' and a['digest']=='sha256:'+h
 fallback_assets[name]={'url':a['browser_download_url'],'sha256':h,'id':a['id']}
runs={}
for label,rid in spec.items():
 r=api(f'repos/{repo}/actions/runs/{rid}')
 expected_head=source if label in ('developer','player') else os.environ['APD_ACCEPTANCE_SOURCE']
 assert r['head_sha']==expected_head and r['status']=='completed',(label,r['status'],r['conclusion'])
 assert r['conclusion']=='success' or (label=='developer' and r['conclusion']=='failure'),(label,r['conclusion'])
 runs[label]=r
assert runs['developer']['run_number']==number
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
jobs=api(f'repos/{repo}/actions/runs/{spec["developer"]}/jobs')['jobs']
assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in jobs)
failed_original=[j for j in jobs if j['conclusion']=='failure']
assert len(failed_original)==1 and failed_original[0]['name']=='android-smoke-check'
original_compile_log=cmd('gh','run','view',str(spec['developer']),'--repo',repo,'--job',str(failed_original[0]['id']),'--log')
assert 'error: cannot find symbol' in original_compile_log and 'TitleMenuTapTimingDeviceTest' in original_compile_log
acceptance_tree=api(f'repos/{repo}/git/trees/{os.environ["APD_ACCEPTANCE_SOURCE"]}?recursive=1')
assert not acceptance_tree.get('truncated') and application_inputs(acceptance_tree['tree'])==application_inputs(trees[0]['tree'])
display_jobs=api(f'repos/{repo}/actions/runs/{spec["display"]}/jobs')['jobs']
assert len(display_jobs)==6 and all(j['conclusion']=='success' for j in display_jobs)
dev,da=artifact(spec['developer'],'apd-codex-private-test-apk','developer')
player,pa=artifact(spec['player'],'apd-player-matching-developer','player')
core,ca=artifact(spec['developer'],'apd-codex-core-tests','core')
native,na=artifact(spec['native'],'apd-codex-android-checks','android')
native_proof_file,=native.rglob('title-evidence/acceptance-source.json')
native_proof=json.loads(native_proof_file.read_text())
assert native_proof['application_source']==source and native_proof['application_run']==spec['developer']
assert native_proof['test_source']==os.environ['APD_ACCEPTANCE_SOURCE']
assert native_proof['application_rebuilt'] is False and native_proof['candidate_press_timing_injected'] is False
assert native_proof['input_handler_replaced'] is False and native_proof['protected_compilation_inputs_verified'] is True
summaries={'core':xml_summary(core),'player_core':xml_summary(player),'android':xml_summary(native)}
assert summaries['core']['tests']==summaries['player_core']['tests']==2229
assert summaries['android']['tests']==66
required={'resourceTooltipsShowExactPairsAndKeepActionsVisibleForNewAndLegacyHistory','titleControlsStaySeparateAndNativeChangesAndHideUiRemainUsable','warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms'}
cases=summaries['android']['cases']
assert any(c['classname'].endswith('ProgressionTooltipsDeviceTest') for c in cases)
assert all(any(c['name']==name for c in cases) for name in required)
visuals={}
for name in ('resource-strength-current.png','resource-upgrade-current.png','resource-strength-legacy.png','resource-upgrade-legacy.png','loot-scroll-gather-description.png','main-title-layout.png','options-confirmations.png'):
 p,=native.rglob(name)
 with Image.open(p) as im:
  assert im.size==(320,640);im.verify()
 visuals[name]=digest(p)
review=json.loads(pathlib.Path('docs/APD-TEST341-VISUAL-REVIEW.json').read_text())
assert review['application_source']==source and review['application_run']==spec['developer']
assert review['image_sha256']==visuals and review['reviewed'] is True
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
files=[];hashes={};histories=[]
for label,folder in [('Developer',dev),('Alpha-Testers',player)]:
 apk,=folder.rglob('*.apk');sidecar,=apk.parent.glob('*.apk.sha256');h=digest(apk)
 assert h==sidecar.read_text().split()[0]
 assert certificates(cmd(str(signer),'verify','--print-certs',str(apk)))==[cert]
 badging=cmd(str(signer.parent/'aapt'),'dump','badging',str(apk))
 assert re.search(r"package: name='([^']+)'",badging).group(1)=='com.charltonchris29.adhdpixeldungeon.indev'
 assert re.search(r"versionCode='([^']+)'",badging).group(1)=='920'
 if label=='Developer':assert re.search(r"versionName='([^']+)'",badging).group(1)=='4.0.8-apd-INDEV-bC341-TEST'
 with zipfile.ZipFile(apk)as z:
  assert hashlib.sha256(z.read('assets/sprites/items.png')).hexdigest()=='44b248cc4192ba6035c89b8e90fd1fd18114a9b78bd5addd9bbbdd26e174c279'
  assert b'items.potions.potionofstrength.pickup_pair=' in z.read('assets/messages/items/items.properties')
  dex=[z.read(n) for n in z.namelist() if re.fullmatch(r'classes\d*\.dex',n)]
  assert any(b'apd_progression_pickups_v1' in data for data in dex)
  assert any(b'acceptLegacy' in data and b'systemBackOwned' in data for data in dex)
  histories.append({n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')})
  if label=='Alpha-Testers':assert 'assets/apd/features.txt' not in z.namelist()
 target=root/f'APD-{label}-341.apk';target.write_bytes(apk.read_bytes())
 checksum=pathlib.Path(str(target)+'.sha256');checksum.write_text(h+'  '+target.name+'\n')
 files.extend([target,checksum]);hashes[label]=h
assert histories[0]==histories[1] and len(histories[0])==366
assert native_proof['application_sha256']==hashes['Developer']
# Bind every native display capture to the exact delivered developer APK.
display_evidence={}
display_artifacts=api(f'repos/{repo}/actions/runs/{spec["display"]}/artifacts')['artifacts']
expected_profiles=('m53-representative','compact-virtual','small-virtual','square-virtual','short-landscape','redmagic-representative-api36')
for profile in expected_profiles:
 name='apd-title-display-'+profile
 folder,entry=artifact(spec['display'],name,'display-'+profile)
 proof_file,=folder.rglob('acceptance-source.json')
 proof=json.loads(proof_file.read_text())
 assert proof['application_source']==source and proof['application_run']==spec['developer']
 assert proof['application_sha256']==hashes['Developer']
 assert proof['protected_application_inputs_identical'] is True and proof['application_rebuilt'] is False
 result=xml_summary(folder)
 assert result['tests']==(3 if profile.endswith('api36') else 1)
 images=list(folder.rglob('*.png'))
 assert len(images)==(7 if profile.endswith('api36') else 5),(profile,len(images))
 for image in images:
  with Image.open(image) as im:im.verify()
 display_evidence[profile]=dict(artifact=entry,provenance=proof,summary=result,images={image.name:digest(image) for image in images})
all_images=list(native.rglob('*.png'))
assert len(all_images)==92
for image in all_images:
 with Image.open(image) as im:im.verify()
receipt=root/'APD-341-Verification.json'
receipt.write_text(json.dumps(dict(application_source=source,publication_source=os.environ['GITHUB_SHA'],hashes=hashes,summaries=summaries,runs=runs,display_jobs=display_jobs,signer=cert,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,artifacts=dict(developer=da,player=pa,core=ca,android=na),visuals=visuals,display_evidence=display_evidence,native_proof=native_proof,retained_original_compile_failure=failed_original,acceptance_test_source=os.environ['APD_ACCEPTANCE_SOURCE'],fallback_assets=fallback_assets,retained_failures={'333':'Rejected; original two setup failures plus corrected full-suite UIMSD295/300ms failures and API36 delayed Back duplicate','334':'Failed before compilation: incomplete executable history','335':'Rejected341ms UIMSD opening; cancelled partial native29/65, not acceptance.','341-initial':'Native instrumentation compilation failed: a diagnostic wrapper referenced an absent generated helper. Test-only correction reuses the exact compiled signed APK; no game rebuild.','340-passive-prototype':'Run40975902877 FAILED first Changes navigation, zero timing corrections observed; unrelated system ANRs also logged; it is not acceptance.','340':'Unpublished:66 main checks and five display profiles passed; API36 title failed twice. Candidate-injected Button prototypes are not shipped-APK acceptance.','339':'Failed before compilation because predecessor index label was wrong; no APK produced.','338':'Rejected60/66 native with6failures, including362ms UIMSD and Back navigation. No338release. Exact-APK diagnostics and failed prototype retained; candidate-injected passing prototype is not shipped-APK acceptance.'},physical_phone_verified=False,owner_only=True,tester_distribution_approved=False,checked_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),link_sent=False),indent=2))
files.append(receipt)
notes=root/'notes.txt'
notes.write_text("Short presses no longer include frame time from before DOWN; genuine long holds retain their original threshold and behavior. Gathering now has its own parchment/cyan icon. Upgrade Scrolls and Strength Potions show regional pickup counts; Strength Potions identify collected or missing guaranteed early/late floor pairs. Older saves show unknown earlier history honestly. Healing text has correct line breaks. Android Back buttons receive complete presses, including a release around the first scene draw. Timestamp provenance rejects identified old copies while preserving a fresh fallback press. UIMSD reuses the existing APD title artwork to reduce cold rendering work. Unchanged interface mode, brightness and grid selections avoid unnecessary resets/redraws.\n\nVerified2229core tests per variant. Initial native instrumentation failed to compile because a diagnostic wrapper referenced an absent generated helper; the corrected test-only source exercised the exact existing signed APK without rebuilding or runtime injection. All66 main API34 checks including current/legacy resource windows, and six display profiles including API36. Original signing identity/application ID/versionCode, checksums and all366matching compiled-history files verified. Earlier TEST335 and338 warm openings failed their250ms budget (341ms and362ms). All prior failures remain documented. The owner7:30pm deadline was missed; these are late assets, not on-time delivery. A passing current emulator run does not establish consistent physical-phone latency. Physical-phone acceptance remains pending. The reported sustained phone black screen is not reproduced or proven to be memory exhaustion; existing diagnostic export remains included. This build does not establish that every ordinary Settings responsiveness issue is resolved. Install over existing APD; never uninstall or clear data. Do not downgrade runs with pending Forwarding parcels. Private owner delivery; no automatic tester distribution.\n")
tag='apd-alpha-preview-341'
existing=subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True)
if existing.returncode!=0:
 subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',source,'--prerelease','--latest=false','--title','APD Alpha Preview 341 — Gathering icon, resource tracking and responsive input','--notes-file',str(notes),*map(str,files)],check=True)
release=api(f'repos/{repo}/releases/tags/{tag}')
assert release['prerelease'] and not release['draft']
for p in files:
 matches=[a for a in release['assets'] if a['name']==p.name]
 if not matches and existing.returncode==0:
  subprocess.run(['gh','release','upload',tag,'--repo',repo,str(p)],check=True)
  release=api(f'repos/{repo}/releases/tags/{tag}');matches=[a for a in release['assets'] if a['name']==p.name]
 a,=matches
 if existing.returncode==0 and p.name.endswith('.json'):continue
 assert a['state']=='uploaded' and a['size']==p.stat().st_size and a['digest']=='sha256:'+digest(p)
 check=root/('download-check-'+p.name)
 with check.open('wb')as out:subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{repo}/releases/assets/{a["id"]}'],stdout=out,check=True)
 assert digest(check)==digest(p)
 print('VERIFIED_DOWNLOAD',a['browser_download_url'])
print('ACTUAL_ASSET_PUBLICATION_UTC',release['published_at'])
