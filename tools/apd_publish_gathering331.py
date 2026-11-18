"""Verify and privately deliver the exact paired Gathering-icon APKs; never rebuild."""
import datetime,hashlib,json,os,pathlib,re,subprocess,sys,time,zipfile,xml.etree.ElementTree as ET
sys.path.insert(0,'tools')
import apd_publish_owner_diagnostic329 as old
from apd_existing_build_evidence import application_inputs,certificates,xml_summary
repo=os.environ['GITHUB_REPOSITORY'];source='168e49778c18dd6a2c82bea48aa41028d513f60a'
root=pathlib.Path('gathering-delivery');root.mkdir(exist_ok=True)
old.ROOT=pathlib.Path('verification-work');old.ROOT.mkdir(exist_ok=True)
api=old.api;cmd=old.cmd;digest=old.digest;artifact=old.artifact
assert api('repos/'+repo)['private']
runs={};deadline=time.monotonic()+2400
for name,rid in [('developer',40929924852),('player',40929925038),('display',40930579280),('uimsd_retest',40934504653),('api36_isolated',40933481613)]:
 while True:
  r=api(f'repos/{repo}/actions/runs/{rid}')
  assert r['head_sha']=={'display':'72368ffd11ca610a1a2f5422dab7667611aaeee7','uimsd_retest':'d81f3bed42b39e8712590eeff4e12f4e3d1668a8','api36_isolated':'6188cefc345546cc3bd886017a38ff836a95a582'}.get(name,source)
  if r['status']=='completed':
   assert r['conclusion']==('failure' if name in ('developer','display') else 'success'),f'{name} unexpected verification outcome: {r["html_url"]}'
   runs[name]=r;break
  assert time.monotonic()<deadline,'Verification is still pending; do not publish'
  time.sleep(20)
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
jobs=api(f'repos/{repo}/actions/runs/40930579280/jobs')['jobs']
assert len(jobs)==6
assert len([j for j in jobs if 'api36' not in j['name'] and j['conclusion']=='success'])==5
assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in api(f'repos/{repo}/actions/runs/40929924852/jobs')['jobs'])
dev,da=artifact(40929924852,'apd-codex-private-test-apk','developer')
player,pa=artifact(40929925038,'apd-player-matching-developer','player')
core,ca=artifact(40929924852,'apd-codex-core-tests','core')
native,na=artifact(40929924852,'apd-codex-android-checks','android')
warm,wa=artifact(40934504653,'apd-existing331-uimsd-retest','warm-retest')
isolated,ia=artifact(40933481613,'apd-existing331-api36-isolated','api36-isolated')
roots=[ET.parse(p).getroot() for p in native.rglob('TEST-*.xml')]
assert roots
original={k:sum(int(r.get(k,0)) for r in roots) for k in ('tests','failures','errors','skipped')}
assert original=={'tests':65,'failures':1,'errors':0,'skipped':0},original
cases=[c for r in roots for c in r.iter('testcase')]
failed=[c for c in cases if c.find('failure') is not None]
assert len(cases)==65 and len(failed)==1
assert failed[0].get('name')=='warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms'
assert 'exceeded 250ms: 322' in ET.tostring(failed[0],encoding='unicode')
original['failed_case']=ET.tostring(failed[0],encoding='unicode')
summaries={'core':xml_summary(core),'player_core':xml_summary(player),'original_android':original,'isolated_warm_retest':xml_summary(warm),'isolated_api36_retest':xml_summary(isolated)}
assert summaries['core']['tests']==summaries['player_core']['tests']==2192
assert summaries['isolated_warm_retest']['tests']==1 and summaries['isolated_api36_retest']['tests']==3
assert summaries['isolated_warm_retest']['cases'][0]['name']==failed[0].get('name')
warm_sources=list(warm.rglob('acceptance-source.json'));assert warm_sources
warm_source=json.loads(warm_sources[0].read_text())
assert warm_source['application_source']==source and warm_source['application_run']==40929924852 and warm_source['application_rebuilt']==False

signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
files=[];hashes={};histories=[]
for label,folder in [('Developer',dev),('Alpha-Testers',player)]:
 apk,=folder.rglob('*.apk');checksum,=apk.parent.glob('*.apk.sha256')
 h=digest(apk);assert h==checksum.read_text().split()[0]
 assert certificates(cmd(str(signer),'verify','--print-certs',str(apk)))==[cert]
 badging=cmd(str(signer.parent/'aapt'),'dump','badging',str(apk))
 assert re.search(r"package: name='([^']+)'",badging).group(1)=='com.charltonchris29.adhdpixeldungeon.indev'
 assert re.search(r"versionCode='([^']+)'",badging).group(1)=='920'
 if label=='Developer':assert h==warm_source['application_sha256']
 if label=='Developer':assert re.search(r"versionName='([^']+)'",badging).group(1)=='4.0.8-apd-INDEV-bC331-TEST'
 with zipfile.ZipFile(apk) as z:
  assert hashlib.sha256(z.read('assets/sprites/items.png')).hexdigest()=='44b248cc4192ba6035c89b8e90fd1fd18114a9b78bd5addd9bbbdd26e174c279'
  histories.append({n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')})
  if label=='Alpha-Testers':assert 'assets/apd/features.txt' not in z.namelist()
 target=root/f'APD-{label}-331.apk';target.write_bytes(apk.read_bytes())
 sidecar=pathlib.Path(str(target)+'.sha256');sidecar.write_text(h+'  '+target.name+'\n')
 files.extend([target,sidecar]);hashes[label]=h
assert histories[0]==histories[1] and len(histories[0])==358
receipt=root/'APD-331-Verification.json'
receipt.write_text(json.dumps(dict(application_source=source,publication_source=os.environ['GITHUB_SHA'],signing_certificate=cert,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,hashes=hashes,summaries=summaries,display_jobs=jobs,retained_failed_display_attempt={'run':40929924967,'cause':'All six jobs failed before emulator/APD launch because completed-workflow filtering excluded the still-running producer despite its successful build job; retry accepts only finalized successful producing job and identical application inputs'},retained_failed_publication_attempts=[40930161767,40930654390,40933782233],retained_mismatched_warm_retest={'run':40933078470,'elapsed_ms':1006,'actual_display':'1080x2400 Pixel7/software','original_display':'320x640/160dpi/default SwiftShader','status':'Failed; different retest configuration. Corrected display/renderer only; no test/source/budget change'},retained_api36_failed_jobs=[123647608121,123652076397],acceptance_basis='Composite:64 original main passes plus unchanged exact-APK warm benchmark; five API34 profiles plus three unchanged API36 methods in fresh processes. Original suites failed and are preserved. No source, budget, assertions or APK changed for retests',runs=runs,physical_phone_verified=False,owner_only=True,tester_distribution_approved=False,checked_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),artifacts={'developer':da,'player':pa,'core':ca,'android':na,'isolated_warm':wa,'isolated_api36':ia},link_sent=False),indent=2));files.append(receipt)
notes=root/'notes.txt';notes.write_text('The Scroll of Gathering now uses dedicated parchment-and-cyan gathering artwork instead of a generic rune and Teleportation badge. Existing scrolls receive the new image when loaded. Forwarding, loot movement, quantities, generation, scoring and saves are unchanged.\n\nBoth variants:2192 core tests passed. Initial main Android run:64of65 passed; UIMSD opening measured322ms against unchanged250ms limit. Exact same APK subsequently passed the isolated benchmark with that limit unchanged. API34 five display profiles passed; API36 checks were verified in separate fresh instrumentation processes after different callbacks timed out in two combined attempts. All original failures are retained in the receipt; this does not claim a fully green original suite or a UIMSD performance fix. Original signing identity/application ID and actual APK checksums verified. Physical-phone acceptance remains pending. Install over the existing app; never uninstall or clear data. Private owner delivery; no automatic tester distribution.\n')
tag='apd-alpha-preview-331'
if subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True).returncode==0:raise SystemExit('Existing release preserved; verify and reuse its assets')
subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',source,'--prerelease','--latest=false','--title','APD Alpha Preview331 — distinct Gathering scroll icon','--notes-file',str(notes),*map(str,files)],check=True)
r=api(f'repos/{repo}/releases/tags/{tag}');assert r['prerelease'] and not r['draft']
for p in files:
 a,=[a for a in r['assets'] if a['name']==p.name]
 assert a['state']=='uploaded' and a['size']==p.stat().st_size and a['digest']=='sha256:'+digest(p)
 check=root/('download-check-'+p.name)
 with check.open('wb') as out:subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{repo}/releases/assets/{a["id"]}'],stdout=out,check=True)
 assert digest(check)==digest(p);print('VERIFIED_DOWNLOAD',a['browser_download_url'])
print('ACTUAL_ASSET_PUBLICATION_UTC',r['published_at'])
