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

"""Reuse only complete same-APK runtime results from the cancelled visual-review run."""
import hashlib,json,os,pathlib,re,subprocess,zipfile
repo=os.environ['GITHUB_REPOSITORY'];api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
run=42405439725;source='9f958d4344c3a0a155a0fd84424c28441b3920ac'
r=api(f'repos/{repo}/actions/runs/{run}');assert r['head_sha']==source and r['conclusion']=='cancelled'
jobs=api(f'repos/{repo}/actions/runs/{run}/jobs')['jobs']
for name in ['native (Developer)','native (Alpha-Testers)']:assert any(j['name']==name and j['conclusion']=='success' for j in jobs)
rows=[('developer',13587351633,'8386eb2da8b0828643cca234ab70421b1d5baff26c2abd3b4dfc2b07017b53df'),('alpha',13587915005,'a5dc4cab6c24815fe6e36fb75851ef7845de119b5a5590db381d37511cc4065c')]
for label,ident,digest in rows:
 a=api(f'repos/{repo}/actions/artifacts/{ident}');assert a['workflow_run']['id']==run and a['digest']=='sha256:'+digest and not a['expired']
 p=pathlib.Path(f'completed-{label}.zip')
 with p.open('wb') as out:subprocess.run(['gh','api',f'repos/{repo}/actions/artifacts/{ident}/zip'],stdout=out,check=True)
 assert hashlib.sha256(p.read_bytes()).hexdigest()==digest
 with zipfile.ZipFile(p) as z:
  proof=json.loads(z.read('reuse-provenance.json'));assert proof['application_source']==os.environ['APD_SOURCE_SHA'] and proof['verification_source']==source and not proof['application_rebuilt']
  for case in ['seed','restart','gameplay']:
   name=f'{label}-{case}.txt';text=z.read(name).decode();assert 'OK (1 test)' in text and 'INSTRUMENTATION_CODE: -1' in text and len(re.findall(r'^INSTRUMENTATION_STATUS_CODE: 0$',text,re.M))==1
   assert not any(t in text for t in ['FAILURES','INSTRUMENTATION_FAILED','Process crashed','AssumptionFailure'])
   pathlib.Path('evidence',name).write_text(text)
pathlib.Path('evidence/retained-complete-runtime.json').write_text(json.dumps(dict(run=run,whole_run_conclusion='cancelled for unobscured screenshot review before publication',both_native_jobs='success',retained_cases=6,apk_source=os.environ['APD_SOURCE_SHA'],test_source=source,new_application_apks_created=0),indent=2)+'\n')
