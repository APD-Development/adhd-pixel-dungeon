#!/usr/bin/env python3
# ADHD Pixel Dungeon
# Copyright (C) 2026 Chris Charlton
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
"""Build one missing tester companion; preserve all already published assets."""
import datetime, hashlib, json, os, pathlib, re, subprocess, sys, urllib.parse, urllib.request, zipfile
from apd_existing_build_evidence import certificates, xml_summary
REPO=os.environ['GITHUB_REPOSITORY'];SOURCE='1d4bd7268fdb01dedadecddd1fcfd19d72ffa05c';TAG='apd-alpha-preview-350'
CERT='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
DEV_SHA='468d99f7041f81c21416222bf1718a742e476d9fdd707c1cd4ab25ee304122c6'
OUT=pathlib.Path('owner-delivery');OUT.mkdir(exist_ok=True);REFERENCE=pathlib.Path('player350-reference');REFERENCE.mkdir(exist_ok=True)
def run(*args):return subprocess.check_output(args,text=True)
def api(path):return json.loads(run('gh','api',path))
def sha(path):return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def release():return api(f'repos/{REPO}/releases/tags/{TAG}')
def download(name,directory):
 subprocess.run(['gh','release','download',TAG,'--repo',REPO,'--pattern',name,'--dir',str(directory)],check=True)
def check_tag():
 assert api('repos/'+REPO)['private'];ref=api(f'repos/{REPO}/git/ref/tags/{TAG}')['object'];assert ref['type']=='commit' and ref['sha']==SOURCE
phase=sys.argv[1];check_tag()
if phase=='prepare':
 r=release();assert not r['draft'] and r['prerelease'];(REFERENCE/'existing-assets.json').write_text(json.dumps({a['name']:{k:a.get(k) for k in ('id','digest','size')} for a in r['assets']}))
 for name in ('APD-Developer-350.apk','APD-Preview-350-Verification.json','APD-Source-350.zip.sha256','APD-Source-Scan.json'):download(name,REFERENCE)
 proof=json.loads((REFERENCE/'APD-Preview-350-Verification.json').read_text());assert proof['source_commit']==SOURCE and proof['downloaded_back_verified'] and proof['core_tests']==2302
 assert proof['apk_hashes']['Developer']==DEV_SHA==sha(REFERENCE/'APD-Developer-350.apk')
 assert proof['runs']['stable']==41547017507 and api(f'repos/{REPO}/actions/runs/41547017507')['conclusion']=='success'
 scan=json.loads((REFERENCE/'APD-Source-Scan.json').read_text());assert scan['supplemental']['source_findings_reviewed'] and scan['supplemental']['source_sha256']==proof['source_sha256']
 source_asset,=[a for a in r['assets'] if a['name']=='APD-Source-350.zip'];assert source_asset['digest']=='sha256:'+proof['source_sha256']
 subprocess.run(['git','worktree','add','--detach','application-source',SOURCE],check=True)
 with zipfile.ZipFile(REFERENCE/'APD-Developer-350.apk') as z:
  assert 'assets/apd/features.txt' in z.namelist()
  history=[n for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')];assert len(history)>100
  for n in history:
   path=pathlib.Path('application-source/core/src/main/assets')/n.removeprefix('assets/');path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(z.read(n))
 assert not pathlib.Path('application-source/core/src/main/assets/apd/features.txt').exists()
 print('Prepared exact source for one Alpha-Testers APK; no new developer build.')
elif phase=='verify':
 assert run('git','-C','application-source','rev-parse','HEAD').strip()==SOURCE
 changes=run('git','-C','application-source','diff','--name-only').splitlines();assert all(p.startswith('core/src/main/assets/apd/history/') for p in changes),changes
 results=xml_summary(pathlib.Path('application-source/core/build/test-results/test'));assert results['tests']==2302
 apk=pathlib.Path('application-source/android/build/outputs/apk/debug/android-debug.apk');dev=REFERENCE/'APD-Developer-350.apk'
 signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
 assert certificates(run(str(signer),'verify','--print-certs',str(apk)))==certificates(run(str(signer),'verify','--print-certs',str(dev)))==[CERT]
 badging=run(str(signer.parent/'aapt'),'dump','badging',str(apk));assert "name='com.charltonchris29.adhdpixeldungeon.indev'" in badging and "versionCode='920'" in badging and "versionName='4.0.8-apd-INDEV-bP350'" in badging
 with zipfile.ZipFile(dev) as d,zipfile.ZipFile(apk) as p:
  da={n:d.read(n) for n in d.namelist() if n.startswith('assets/') and n!='assets/apd/features.txt'};pa={n:p.read(n) for n in p.namelist() if n.startswith('assets/')}
  assert 'assets/apd/features.txt' not in p.namelist() and da==pa,'Runtime assets differ beyond developer-only marker'
 target=OUT/'APD-Alpha-Testers-350.apk';target.write_bytes(apk.read_bytes());pathlib.Path(str(target)+'.sha256').write_text(sha(target)+'  '+target.name+'\n')
 evidence={'source_commit':SOURCE,'tag':TAG,'variant':'Alpha-Testers','developer_sha256':DEV_SHA,'sha256':sha(target),'signing_certificate':CERT,'application_id':'com.charltonchris29.adhdpixeldungeon.indev','version_code':920,'version_name':'4.0.8-apd-INDEV-bP350','core_tests':2302,'runtime_assets_match_except_developer_marker':True,'developer_features_enabled':False,'physical_phone_verified':False,'build_workflow':int(os.environ['GITHUB_RUN_ID']),'prepared_at':now()}
 (OUT/'APD-Alpha-Testers-350-Verification.json').write_text(json.dumps(evidence,indent=2)+'\n')
 source_proof=json.loads((REFERENCE/'APD-Preview-350-Verification.json').read_text())
 (OUT/'APD-Alpha-Testers-350-Source.txt').write_text(f'Exact source commit: {SOURCE}\nSource tag: {TAG}\nMatching source: https://github.com/{REPO}/releases/download/{TAG}/APD-Source-350.zip\nSource SHA256: {source_proof["source_sha256"]}\nThis existing key-free archive contains the same complete application source and generated history/assets. Its source manifest originally describes the Developer variant; this companion uses the same files with the optional developer asset disabled.\nUnsigned reconstruction: ./gradlew :android:assembleDebug -PapdUnsignedVerification -PapdBuild=P350\nSupply the original external APD signing key using the documented signing environment variables and omit -PapdUnsignedVerification for install-over signing. Never uninstall or clear player data.\nNo separate reconstruction of the sanitized archive or signed-byte reproducibility is claimed. No class-specific scoring or online services enabled. GPLv3 or later; no warranty.\n')
 print('Verified tester APK',sha(target))
elif phase=='publish':
 text=pathlib.Path('player350-instrumentation.txt').read_text();assert 'OK (2 tests)' in text and not re.search(r'INSTRUMENTATION_STATUS_CODE: -(?:1|2|3)',text)
 assert 'QolStableCaptureDeviceTest' in text and 'aRunCanBePlayedSavedContinuedAndEndedWithItsHistoryRecorded' in text
 proof_path=OUT/'APD-Alpha-Testers-350-Verification.json';proof=json.loads(proof_path.read_text());proof.update({'android_tests':2,'android_api':34,'developer_api36_evidence':41547017507,'test_scope':'Actual tester APK: native QoL controls, searched markers, pause/resume and playable run/save/load/history. API36 evidence is from the matching Developer APK, not a separate tester API36 run.','publication_started_at':now()});proof_path.write_text(json.dumps(proof,indent=2)+'\n')
 r=release();before=json.loads((REFERENCE/'existing-assets.json').read_text());assets={a['name']:a for a in r['assets']}
 for name,old in before.items():assert all(assets[name].get(k)==v for k,v in old.items()),'Existing release asset changed'
 for p in sorted(OUT.iterdir()):
  if p.name in assets:assert assets[p.name]['digest']=='sha256:'+sha(p);continue
  url=r['upload_url'].split('{',1)[0]+'?'+urllib.parse.urlencode({'name':p.name});request=urllib.request.Request(url,data=p.read_bytes(),method='POST',headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Content-Type':'application/octet-stream','Accept':'application/vnd.github+json'})
  with urllib.request.urlopen(request,timeout=300) as response:uploaded=json.load(response)
  assert uploaded['digest']=='sha256:'+sha(p)
 r=release();assets={a['name']:a for a in r['assets']};assert all(assets[p.name]['digest']=='sha256:'+sha(p) for p in OUT.iterdir())
 directory=pathlib.Path('player350-downloaded-back');directory.mkdir();download('APD-Alpha-Testers-350.apk',directory);assert sha(directory/'APD-Alpha-Testers-350.apk')==proof['sha256']
 body=r.get('body') or '';marker='### Alpha-Testers 350 companion'
 if marker not in body:
  body+=f'\n\n{marker}\n[Download Alpha-Testers APK](https://github.com/{REPO}/releases/download/{TAG}/APD-Alpha-Testers-350.apk) · [Matching source](https://github.com/{REPO}/releases/download/{TAG}/APD-Source-350.zip)\nSame exact source as Developer350, with developer-only features disabled. 2302 core tests and two actual tester APK API34 native cases passed (QoL/search/pause/resume and playable save/load/history). API36 checks belong to the matching Developer APK; no physical-phone acceptance claim. Original signing/app ID retained; install over the existing app and never uninstall or clear data. [Tester verification](https://github.com/{REPO}/releases/download/{TAG}/APD-Alpha-Testers-350-Verification.json).\n'
  request={'body':body};patch=OUT/'release-notes-patch.json';patch.write_text(json.dumps(request));subprocess.run(['gh','api','--method','PATCH',f'repos/{REPO}/releases/{r["id"]}','--input',str(patch)],check=True,stdout=subprocess.DEVNULL);patch.unlink()
 proof.update({'downloaded_back_verified':True,'download_verified_at':now(),'asset_created_at':assets['APD-Alpha-Testers-350.apk']['created_at'],'direct_download':assets['APD-Alpha-Testers-350.apk']['browser_download_url'],'link_sent':False});(OUT/'APD-Alpha-Testers-350-Delivery-Receipt.json').write_text(json.dumps(proof,indent=2)+'\n')
 print('VERIFIED_DOWNLOAD',proof['direct_download'])
else:raise SystemExit('Unknown phase')
