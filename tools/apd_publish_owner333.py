"""Private owner delivery of exact TEST333 APKs after complete retained checks; no rebuild."""
import datetime,hashlib,json,os,pathlib,re,subprocess,sys,time,zipfile,xml.etree.ElementTree as ET
sys.path.insert(0,'tools')
import apd_publish_owner_diagnostic329 as old
from apd_existing_build_evidence import application_inputs,certificates,xml_summary
repo=os.environ['GITHUB_REPOSITORY'];source='3dfd3d3b51cab4794e921da579e6856d68da0758'
root=pathlib.Path('owner333-delivery');root.mkdir(exist_ok=True)
old.ROOT=pathlib.Path('verification-work');old.ROOT.mkdir(exist_ok=True)
api=old.api;cmd=old.cmd;artifact=old.artifact;digest=old.digest
assert api('repos/'+repo)['private']
spec={'developer':(40941203300,source),'player':(40941203143,source),'display':(40945375099,'c5ebacc2c1525a320883c3cdff5165c4a2dea702'),'full_native':(40945375203,'c5ebacc2c1525a320883c3cdff5165c4a2dea702'),
      'tooltips':(40942847446,'3cdc75c2a7e32a226bbb6fe70cf37e7cd8e9d21f')}
runs={};deadline=time.monotonic()+2400
for label,(rid,expected) in spec.items():
 while True:
  r=api(f'repos/{repo}/actions/runs/{rid}');assert r['head_sha']==expected
  if r['status']=='completed':
   assert r['conclusion']==('failure' if label=='developer' else 'success'),f'{label} verification failed: {r["html_url"]}'
   runs[label]=r;break
  assert time.monotonic()<deadline,'Verification incomplete; retain TEST330'
  time.sleep(20)
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
jobs=api(f'repos/{repo}/actions/runs/40945375099/jobs')['jobs']
assert len(jobs)==6 and all(j['conclusion']=='success' for j in jobs)
dev,da=artifact(40941203300,'apd-codex-private-test-apk','developer')
player,pa=artifact(40941203143,'apd-player-matching-developer','player')
core,ca=artifact(40941203300,'apd-codex-core-tests','core')
original_native,ona=artifact(40941203300,'apd-codex-android-checks','original-android')
native,na=artifact(40945375203,'apd-existing333-full-native','complete-native')
tooltips,ta=artifact(40942847446,'apd-existing333-resource-tooltips','tooltips')
roots=[ET.parse(p).getroot() for p in original_native.rglob('TEST-*.xml')]
original={k:sum(int(r.get(k,0)) for r in roots) for k in ('tests','failures','errors','skipped')}
assert original=={'tests':65,'failures':2,'errors':0,'skipped':0},original
failed=[c for r in roots for c in r.iter('testcase') if c.find('failure') is not None]
assert {c.get('name') for c in failed}=={'multipleColumnTapsRemainAboveTableUntilDone','evidenceBackRestoresEachTableAndSurvivesSceneRecreation'}
original['failures_retained']=[ET.tostring(c,encoding='unicode') for c in failed]
assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in api(f'repos/{repo}/actions/runs/40941203300/jobs')['jobs'])
summaries={'original_android':original,'core':xml_summary(core),'player_core':xml_summary(player),'android':xml_summary(native),'tooltips':xml_summary(tooltips)}
assert summaries['core']['tests']==summaries['player_core']['tests']==2206
assert summaries['android']['tests']==66 and summaries['tooltips']['tests']==1
from PIL import Image
for name in ('resource-strength-current.png','resource-upgrade-current.png','resource-strength-legacy.png','resource-upgrade-legacy.png'):
 p,=tooltips.rglob(name)
 with Image.open(p) as im:assert im.size==(320,640);im.verify()
evidence,=tooltips.rglob('acceptance-source.json');tooltip_source=json.loads(evidence.read_text())
assert tooltip_source['application_source']==source and tooltip_source['application_run']==40941203300
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
 if label=='Developer':
  assert h==tooltip_source['application_sha256']
  assert re.search(r"versionName='([^']+)'",badging).group(1)=='4.0.8-apd-INDEV-bC333-TEST'
 with zipfile.ZipFile(apk)as z:
  assert hashlib.sha256(z.read('assets/sprites/items.png')).hexdigest()=='44b248cc4192ba6035c89b8e90fd1fd18114a9b78bd5addd9bbbdd26e174c279'
  assert b'items.potions.potionofstrength.pickup_pair=' in z.read('assets/messages/items/items.properties')
  assert any(b'apd_progression_pickups_v1' in z.read(n) for n in z.namelist() if re.fullmatch(r'classes\d*\.dex',n))
  histories.append({n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')})
  if label=='Alpha-Testers':assert 'assets/apd/features.txt' not in z.namelist()
 target=root/f'APD-{label}-333.apk';target.write_bytes(apk.read_bytes())
 checksum=pathlib.Path(str(target)+'.sha256');checksum.write_text(h+'  '+target.name+'\n')
 files.extend([target,checksum]);hashes[label]=h
assert histories[0]==histories[1] and len(histories[0])==360
receipt=root/'APD-333-Verification.json'
receipt.write_text(json.dumps(dict(application_source=source,publication_source=os.environ['GITHUB_SHA'],hashes=hashes,summaries=summaries,runs=runs,display_jobs=jobs,signer=cert,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,artifacts=dict(developer=da,player=pa,core=ca,original_android=ona,android=na,tooltips=ta),historical_auxiliary_jobs={'hotbar_run':40941203202,'death_run':40941203163,'status':'Failed before app launch; fixed TEST314-only comparisons to eda307d are not applicable to current production changes. Assertions and historical workflows preserved; not counted as current TEST333 evidence.'},retained_tooltip_compile_failure=40942121046,retained_display_failures=[40941203211,40944033352],verification_basis='Original65-method run failed two synthetic-fixture setup races. The complete same-APK66-method rerun waits for actual READY Home before injecting table data, and completed Settings draws before native targets. All original viewport/modal/timing assertions retained; no production/APK change for retests.',physical_phone_verified=False,owner_only=True,tester_distribution_approved=False,checked_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),link_sent=False),indent=2))
files.append(receipt)
notes=root/'notes.txt';notes.write_text('Upgrade Scrolls and Strength Potions now show collected amounts by original region. Strength Potions identify collected and missing guaranteed floor pairs; older saves clearly show unknown earlier history. Repeated pickups do not inflate counts. Healing-potion text uses proper line breaks. Gathering has its dedicated parchment/cyan icon. UIMSD freshness work is checked off the render thread with exact prepared-model handoff.\n\nFull2206core per variant and complete66-method exact-APK Android rerun passed, native current/legacy resource-window assertions and four screenshots, six display profiles. Original333main run63of65 is retained; two table fixtures were injecting data before async opening completed. Their setup now waits for real READY Home. Native Settings targets wait for completed window draws. All viewport, modal and250ms timing assertions are unchanged. No production change or new APK was made for these retests. Actual APK source, original signing identity, application ID, checksums and all360matching history files verified. Physical-phone acceptance remains pending. Install over existing APD; never uninstall or clear data. No downgrade with pending Forwarding parcels. Private owner delivery only; no automatic tester distribution.\n')
tag='apd-alpha-preview-333'
existing=subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True)
if existing.returncode!=0:
 subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',source,'--prerelease','--latest=false','--title','APD Alpha Preview333 — resource tracking and Gathering icon','--notes-file',str(notes),*map(str,files)],check=True)
r=api(f'repos/{repo}/releases/tags/{tag}');assert r['prerelease'] and not r['draft']
for p in files:
 matches=[a for a in r['assets'] if a['name']==p.name]
 if not matches and existing.returncode==0:
  subprocess.run(['gh','release','upload',tag,'--repo',repo,str(p)],check=True)
  r=api(f'repos/{repo}/releases/tags/{tag}');matches=[a for a in r['assets'] if a['name']==p.name]
 a,=matches
 # Existing receipts may have been generated by an independently verified delivery;
 # preserve them. Actual APK and checksum bytes must always match this verification.
 if existing.returncode==0 and p.name.endswith('.json'):continue
 assert a['state']=='uploaded' and a['size']==p.stat().st_size and a['digest']=='sha256:'+digest(p)
 check=root/('download-check-'+p.name)
 with check.open('wb')as out:subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{repo}/releases/assets/{a["id"]}'],stdout=out,check=True)
 assert digest(check)==digest(p);print('VERIFIED_DOWNLOAD',a['browser_download_url'])
print('ACTUAL_ASSET_PUBLICATION_UTC',r['published_at'])
