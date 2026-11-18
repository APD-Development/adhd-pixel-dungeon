"""Publish existing TEST330 only after exact artifact, signature and regression checks."""
import datetime,hashlib,json,os,pathlib,re,subprocess,sys,zipfile
sys.path.insert(0,'tools')
import apd_publish_owner_diagnostic329 as old
from apd_existing_build_evidence import application_inputs,certificates,xml_summary
repo=os.environ['GITHUB_REPOSITORY'];source='35e1c85fcea93381a4f107cafd4f89f5264af391'
root=pathlib.Path('apk-delivery');root.mkdir(exist_ok=True);old.ROOT=pathlib.Path('verification-work');old.ROOT.mkdir(exist_ok=True)
api=old.api;cmd=old.cmd;digest=old.digest;artifact=old.artifact
assert api('repos/'+repo)['private']
now=datetime.datetime.now(datetime.timezone.utc);assert now>=datetime.datetime(2026,10,29,16,0,tzinfo=datetime.timezone.utc)
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
runs={}
for key,rid,sha in [('developer',40800528756,source),('player',40800528519,source),('display',40895514618,'cf8b24fb33955d925d2266beedc1f91e9ca14af8')]:
 r=api(f'repos/{repo}/actions/runs/{rid}');assert r['head_sha']==sha and r['status']=='completed' and r['conclusion']=='success';runs[key]=r
jobs=api(f'repos/{repo}/actions/runs/40895514618/jobs')['jobs'];assert len(jobs)==6 and all(j['conclusion']=='success' for j in jobs)
dev,da=artifact(40800528756,'apd-codex-private-test-apk','developer')
player,pa=artifact(40800528519,'apd-player-matching-developer','player')
core,ca=artifact(40800528756,'apd-codex-core-tests','core')
native,na=artifact(40800528756,'apd-codex-android-checks','android')
summaries={'core':xml_summary(core),'player_core':xml_summary(player),'android':xml_summary(native)}
assert summaries['core']['tests']==summaries['player_core']['tests']==2192 and summaries['android']['tests']==65
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e';files=[];hashes={};histories=[]
for label,folder in [('Developer',dev),('Alpha-Testers',player)]:
 apk,=folder.rglob('*.apk');checksum,=apk.parent.glob('*.apk.sha256');h=digest(apk);assert h==checksum.read_text().split()[0]
 if label=='Developer':assert h=='3e38b1164169e404c6606d2baa6d23f61fdb4e1625bb1be53c1424ba41c20842'
 assert certificates(cmd(str(signer),'verify','--print-certs',str(apk)))==[cert]
 badging=cmd(str(signer.parent/'aapt'),'dump','badging',str(apk))
 assert re.search(r"package: name='([^']+)'",badging).group(1)=='com.charltonchris29.adhdpixeldungeon.indev'
 assert re.search(r"versionCode='([^']+)'",badging).group(1)=='920'
 if label=='Developer':assert re.search(r"versionName='([^']+)'",badging).group(1)=='4.0.8-apd-INDEV-bC330-TEST'
 with zipfile.ZipFile(apk) as z:
  histories.append({n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')})
  if label=='Alpha-Testers':assert 'assets/apd/features.txt' not in z.namelist()
 target=root/f'APD-{label}-330.apk';target.write_bytes(apk.read_bytes());sidecar=pathlib.Path(str(target)+'.sha256');sidecar.write_text(h+'  '+target.name+'\n');files.extend([target,sidecar]);hashes[label]=h
assert histories[0]==histories[1] and len(histories[0])==357
receipt=root/'APD-330-Lunch-Verification.json';receipt.write_text(json.dumps(dict(application_source=source,publication_source=os.environ['GITHUB_SHA'],application_rebuilt=False,signing_certificate=cert,application_id='com.charltonchris29.adhdpixeldungeon.indev',version_code=920,hashes=hashes,summaries=summaries,display_verification=json.loads(pathlib.Path('docs/APD-TEST330-DISPLAY-API36-VERIFICATION-2026-10-29.json').read_text()),physical_phone_verified=False,owner_only=True,tester_distribution_approved=False,checked_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),artifacts={'developer':da,'player':pa,'core':ca,'android':na},link_sent=False),indent=2));files.append(receipt)
notes=root/'release-notes.txt';notes.write_text('TEST330: existing application source35e1c85fcea93381a4f107cafd4f89f5264af391, reused without rebuilding.2192core methods per variant;65main Android checks; six accepted emulator display configurations, including Android16 actual7x-to6x scale/cancel and delayed-tap tests. Signing certificate, application ID, versionCode and checksums verified against actual APK bytes. Fixes title scaling, stray button strips, stale input across scene rebuilds, cancelled scale gestures, and synchronous title unlock-history scanning.\n\nPhysical-phone confirmation remains pending. Gathering icon replacement and additional UIMSD/regular Settings performance work remain unfinished and are NOT in this APK. Install over the existing APD app; never uninstall or clear data. Avoid downgrading with pending Forwarding parcels. Private owner lunch test only; no automatic tester distribution or public release.\n\nCurrent lunch PDF: https://github.com/'+repo+'/releases/tag/apd-lunch-report-2026-10-29\n\nPublication during this workflow is not proof of on-time noon chat delivery. Actual publication and link delivery must be recorded separately.\n')
tag='apd-alpha-preview-330'
if subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True).returncode==0:raise SystemExit('Existing TEST330 release preserved; verify/reuse its assets separately')
subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title','APD Alpha Preview330 — interface scaling and input fixes','--notes-file',str(notes),*map(str,files)],check=True)
r=api(f'repos/{repo}/releases/tags/{tag}');assert r['prerelease'] and not r['draft']
for p in files:
 a,=[a for a in r['assets'] if a['name']==p.name];assert a['state']=='uploaded' and a['size']==p.stat().st_size and a['digest']=='sha256:'+digest(p)
 check=root/('download-check-'+p.name)
 with check.open('wb') as out:subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{repo}/releases/assets/{a["id"]}'],stdout=out,check=True)
 assert digest(check)==digest(p);print('VERIFIED_DOWNLOAD',a['browser_download_url'])
(root/'publication.json').write_text(json.dumps(r,indent=2));print('ACTUAL_ASSET_PUBLICATION_UTC',r['published_at'])
