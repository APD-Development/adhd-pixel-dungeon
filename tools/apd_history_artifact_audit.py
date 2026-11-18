"""Read-only private APK evidence census; does not build or publish anything."""
import concurrent.futures,hashlib,json,os,re,subprocess,tempfile,zipfile
from pathlib import Path
REPO=os.environ['GITHUB_REPOSITORY']
def api(path):
 return json.loads(subprocess.check_output(['gh','api',path],stderr=subprocess.PIPE))
def pages(path,key=None):
 rows=[]
 for page in range(1,31):
  result=api(path+('&' if '?' in path else '?')+f'per_page=100&page={page}')
  part=result[key] if key else result;rows.extend(part)
  if len(part)<100:return rows
 raise RuntimeError('Pagination exceeded safety bound; census incomplete')
def fetch_binary(endpoint,path):
 with path.open('wb') as out:
  accept = 'application/octet-stream' if '/releases/assets/' in endpoint else 'application/vnd.github+json'
  subprocess.run(['gh','api','--method','GET',endpoint,'-H','Accept: '+accept],stdout=out,stderr=subprocess.PIPE,check=True)
def inspect_apk(path,record):
 digest=hashlib.sha256(path.read_bytes()).hexdigest()
 record['apk_sha256']=digest;record['apk_bytes']=path.stat().st_size
 declared=record.get('release_asset_digest')
 if declared and declared!='sha256:'+digest:raise ValueError('Release asset digest mismatch')
 tools=sorted((Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/aapt'))
 badging=subprocess.check_output([str(tools[-1]),'dump','badging',str(path)],stderr=subprocess.PIPE).decode('utf-8','replace')
 m=re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
 if not m:raise ValueError('Package metadata missing')
 record['package'],record['version_code'],record['version_name']=m.groups()
 with zipfile.ZipFile(path) as z:
  record['apk_entries']=[{'path':i.filename,'bytes':i.file_size,'crc':i.CRC} for i in z.infolist() if i.filename.startswith(('assets/apd/','META-INF/')) or i.filename in ('classes.dex','classes2.dex','AndroidManifest.xml')]
  texts={}
  for n in z.namelist():
   if n.startswith('assets/apd/') and n.endswith(('.txt','.tsv','.json')):
    if z.getinfo(n).file_size>2_000_000:continue
    texts[n]=z.read(n).decode('utf-8','replace')
  record['bundled_apd_text']=texts
 return record
runs=pages(f'repos/{REPO}/actions/runs','workflow_runs')
releases=pages(f'repos/{REPO}/releases')
records=[]
for namespace,name,maximum in [('legacy','Build APK',84),('codex','Codex APD sprint verification',67)]:
 selected={r['run_number']:r for r in runs if r['name']==name and r['run_number']<=maximum}
 for number in range(1,maximum+1):
  run=selected.get(number);tag=f'apd-build-{number}' if namespace=='legacy' else f'apd-codex-test-{number}'
  release=next((r for r in releases if r['tag_name']==tag),None)
  records.append({'namespace':namespace,'number':number,'run_id':run and run['id'],'run_source':run and run['head_sha'],'run_conclusion':run and run['conclusion'],'run_created_at':run and run['created_at'],'tag':tag,'release':release,'errors':[]})
for release in releases:
 if re.fullmatch('apd-test-[0-9]+',release['tag_name']):
  records.append({'namespace':'private','number':int(release['tag_name'].split('-')[-1]),'tag':release['tag_name'],'release':release,'errors':[]})
previous_path=Path('previous-history/apk-census.json')
previous=json.loads(previous_path.read_text()) if previous_path.exists() else []
previous_by_id={(r['namespace'],r['number']):r for r in previous}
def audit(record):
 old=previous_by_id.get((record['namespace'],record['number']))
 if old and not old['errors']:return old
 try:
  release=record.pop('release')
  if release:
   record.update(release_id=release['id'],release_target=release['target_commitish'],release_date=release['published_at'],release_notes=release['body'])
   ref=api(f"repos/{REPO}/git/ref/tags/{record['tag']}")['object']
   if ref['type']=='tag':ref=api(f"repos/{REPO}/git/tags/{ref['sha']}")['object']
   record['tag_commit']=ref['sha']
   assets=[a for a in release['assets'] if a['name'].endswith('.apk')]
   if len(assets)!=1:raise ValueError('Release does not contain exactly one APK')
   asset=assets[0];record.update(apk_location='release',release_asset_id=asset['id'],release_asset_name=asset['name'],release_asset_digest=asset.get('digest'))
   with tempfile.TemporaryDirectory() as temp:
    path=Path(temp)/'candidate.apk';fetch_binary(f"repos/{REPO}/releases/assets/{asset['id']}",path);inspect_apk(path,record)
  elif record.get('run_id'):
   artifacts=api(f"repos/{REPO}/actions/runs/{record['run_id']}/artifacts")['artifacts']
   record['artifacts']=[{'id':a['id'],'name':a['name'],'expired':a['expired']} for a in artifacts]
   candidates=[a for a in artifacts if ('apk' in a['name'].lower()) and not a['expired']]
   if not candidates:
    record['apk_location']='none';return record
   artifact=candidates[0];record.update(apk_location='artifact',artifact_id=artifact['id'],artifact_name=artifact['name'])
   with tempfile.TemporaryDirectory() as temp:
    archive=Path(temp)/'artifact.zip';fetch_binary(f"repos/{REPO}/actions/artifacts/{artifact['id']}/zip",archive)
    with zipfile.ZipFile(archive) as z:
     names=[n for n in z.namelist() if n.endswith('.apk')]
     if len(names)!=1:raise ValueError('Artifact does not contain exactly one APK')
     path=Path(temp)/'candidate.apk';path.write_bytes(z.read(names[0]));record['artifact_apk_name']=names[0]
     checksums=[n for n in z.namelist() if n.endswith('.apk.sha256')]
     if checksums:record['artifact_declared_sha256']=z.read(checksums[0]).decode().split()[0]
    inspect_apk(path,record)
    if record.get('artifact_declared_sha256') and record['artifact_declared_sha256']!=record['apk_sha256']:raise ValueError('Artifact checksum mismatch')
  else:record['apk_location']='none'
 except Exception as e:
  detail = e.stderr.decode('utf-8','replace')[:1000] if isinstance(e,subprocess.CalledProcessError) and e.stderr else str(e)
  record['errors'].append(type(e).__name__+': '+detail)
 return record
out=Path('history-evidence');out.mkdir(exist_ok=True)
completed=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
 for record in pool.map(audit,records):
  completed.append(record)
  print(record['namespace'],record['number'],record.get('apk_location'),record.get('version_name'),len(record['errors']),flush=True)
  (out/'apk-census.json').write_text(json.dumps(completed,indent=2))
summary={'records':len(completed),'inspected_apks':sum('apk_sha256' in r and not r['errors'] for r in completed),'without_apk':sum(r.get('apk_location')=='none' and not r['errors'] for r in completed),'errors':[{k:r.get(k) for k in ('namespace','number','errors')} for r in completed if r['errors']]}
(out/'summary.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary))
