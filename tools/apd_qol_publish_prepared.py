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

"""Publish only prepared verified private owner artifacts, preserving all prior assets."""
import datetime,hashlib,json,os,pathlib,subprocess,time,urllib.request,urllib.parse
from apd_github_response import json_response
from apd_owner_delivery_policy import require_apk_delivery_allowed
require_apk_delivery_allowed(pathlib.Path.cwd())
repo=os.environ['GITHUB_REPOSITORY'];out=pathlib.Path('owner-delivery');spec=json.loads(pathlib.Path(os.environ.get('APD_DELIVERY_SPEC','docs/APD-QOL-OWNER-DELIVERY.json')).read_text());number=spec['number'];tag=f'apd-alpha-preview-{number}'
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]));sha=lambda p:hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
assert api('repos/'+repo)['private'];scan=json.loads((out/'APD-Source-Scan.json').read_text());assert scan['supplemental']['source_findings_reviewed']
source,=out.glob('APD-Source-*.zip');assert scan['supplemental']['source_sha256']==sha(source)
proof=out/f'APD-Preview-{number}-Verification.json';evidence=json.loads(proof.read_text());assert evidence['source_commit']==spec['source'] and evidence['core_tests']==spec['core_tests'] and evidence['android_tests']==spec['android_tests']
files=[p for p in out.iterdir() if p.suffix in ('.apk','.sha256','.zip') or p.name=='APD-Source-Scan.json']
# GitHub does not create a draft release's tag until publication; establish and verify it explicitly.
ref=subprocess.run(['gh','api',f'repos/{repo}/git/ref/tags/{tag}'],text=True,capture_output=True)
assert ref.returncode==0,'Publication requires the pre-existing exact source tag; no reference creation or update is performed: '+ref.stderr
resolved=api(f'repos/{repo}/git/ref/tags/{tag}')['object'];assert resolved['type']=='commit' and resolved['sha']==spec['source']
matches=[r for r in api(f'repos/{repo}/releases?per_page=100') if r['tag_name']==tag]
assert len(matches)<=1,'Duplicate release tag; preserve existing releases'
if not matches:
 payload={'tag_name':tag,'name':spec.get('title',f'APD Alpha Preview {number}'),'body':(out/'release-notes.md').read_text(),'draft':True,'prerelease':True,'make_latest':'false'}
 request=urllib.request.Request(f'https://api.github.com/repos/{repo}/releases',data=json.dumps(payload).encode(),method='POST',headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Content-Type':'application/json','Accept':'application/vnd.github+json'})
 matches=[json_response(request,timeout=60)]
release,=matches
release_id=release['id']
def refresh():return api(f'repos/{repo}/releases/{release_id}')
def upload(p):
 url=release['upload_url'].split('{',1)[0]+'?'+urllib.parse.urlencode({'name':p.name})
 request=urllib.request.Request(url,data=p.read_bytes(),method='POST',headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Content-Type':'application/octet-stream','Accept':'application/vnd.github+json'})
 return json_response(request,timeout=300)
release=refresh();resolved=api(f'repos/{repo}/git/ref/tags/{tag}')['object'];assert resolved['type']=='commit' and resolved['sha']==spec['source']
existing={a['name']:a for a in release['assets']}
for p in files:
 if p.name in existing:assert existing[p.name].get('digest')=='sha256:'+sha(p),'Existing asset differs; never overwrite'
 else:upload(p)
release=refresh();existing={a['name']:a for a in release['assets']}
assert all(existing[p.name].get('digest')=='sha256:'+sha(p) for p in files)
if release['draft']:subprocess.run(['gh','api','--method','PATCH',f'repos/{repo}/releases/{release_id}','-F','draft=false','-F','prerelease=true','-f','make_latest=false'],check=True,stdout=subprocess.DEVNULL)
# Actual successful downloads, not metadata alone, establish delivery readiness.
verify=pathlib.Path('downloaded-back');verify.mkdir(exist_ok=True)
for pattern in tuple(f'APD-{label}-{number}.apk' for label in spec.get('variants',['Developer','Alpha-Testers']))+(source.name,):
 subprocess.run(['gh','release','download',tag,'--repo',repo,'--pattern',pattern,'--dir',str(verify)],check=True)
 assert sha(verify/pattern)==sha(out/pattern)
release=refresh();evidence.update({'release_id':release['id'],'asset_publication':[{k:a.get(k) for k in ('id','name','digest','created_at','browser_download_url')} for a in release['assets']],'published_at':release['published_at'],'downloaded_back_verified':True,'link_sent':False,'delivery_workflow_run':os.environ['GITHUB_RUN_ID'],'download_verified_utc':datetime.datetime.now(datetime.timezone.utc).isoformat()})
proof.write_text(json.dumps(evidence,indent=2)+'\n')
if proof.name not in {a['name'] for a in release['assets']}:upload(proof)
print('VERIFIED DOWNLOAD LINKS')
for label in spec.get('variants',['Developer','Alpha-Testers']):print(f'https://github.com/{repo}/releases/download/{tag}/APD-{label}-{number}.apk')
print(f'https://github.com/{repo}/releases/tag/{tag}')
