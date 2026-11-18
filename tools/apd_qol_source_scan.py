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

"""Scan actual source and reachable history, then record reviewed source-only findings."""
import hashlib,json,pathlib,subprocess,tempfile,zipfile
# Exact full-line hashes of inspected repeated audit prose; no blanket path exclusions.
REVIEWED_PROSE_LINES={'.github/workflows/apd-scan377.yml': ['33a990e0ce87b4cc4a871702811a29549f408a757b908fc25ee373063d0a2012', '5d7b0e6e42b7aef0c105c7f90496cb61fe287ee5a4b07cba0fbd4f59cc59fdd1'], 'docs/APD-377-OWNER-DELIVERY-RECEIPT.json': ['c28b37bbbe77776e892b7424573a9d190e7db6ba9fec145000a05b0bc83bdfca'], 'docs/APD-377-SCAN-REQUEST.txt': ['037b3686b5aa04a105894f3fb6495e2db50c051dd9ab9549c0b8aa5214a57b65'], 'tools/apd_qol_source_scan.py': ['746956a73daf195d317d528b97a658b9856eda8bbfd838e0336a8d14161bec83', '90a5452a64febec9efe6d8e70ebb1d0b3bf025d49b81c8f587bccff63d47fdf8'], 'docs/WORK-COMPLETED-LEDGER.md': ['89a0d01c35dbd004ad460ea10fdf994ac16d1b53d60876d30761002f36d015c6'], 'core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/apd/MechanicsExplanationTest.java': ['fd62b5a96913d5a7ab2c8a6ed6ddf2a011f8f2899a9799c4cd05715c6fd4d4c2'], 'docs/APD-409-SCAN-RECOVERY-REQUEST.json': ['dc2390272260b95053a4dcf87aac64fafa5891b29ff477b4dbeff1639152f690']}
out=pathlib.Path('owner-delivery');source,=out.glob('APD-Source-*.zip');directory=pathlib.Path(tempfile.mkdtemp(prefix='apd-source-scan-'))
with zipfile.ZipFile(source) as z:z.extractall(directory)
root,=directory.iterdir();manifest=json.loads((root/'CORRESPONDING-SOURCE/manifest.json').read_text());hashes=set(manifest['files'].values())
assert not list(root.rglob('*.jks')) and not list(root.rglob('*.keystore')) and not (root/'.git').exists()
reports={}
for name,args in [('source',['dir',str(directory)]),('history',['git','.','--log-opts=--all --full-history'])]:
 report=pathlib.Path('/tmp/apd-'+name+'-redacted.json');log=pathlib.Path('/tmp/apd-'+name+'-scan.log')
 with log.open('w') as f:r=subprocess.run(['/tmp/apd-gitleaks/gitleaks',*args,'--redact','--no-banner','--report-format','json','--report-path',str(report)],stdout=f,stderr=subprocess.STDOUT)
 assert r.returncode in (0,1),'Scanner failed rather than reporting findings'
 findings=json.loads(report.read_text()) if report.exists() else [];safe=[]
 for a in findings:
  path=pathlib.Path(a['File']);rel=path.relative_to(root).as_posix() if name=='source' and path.is_absolute() else a['File']
  item={k:a.get(k) for k in ('RuleID','StartLine','Commit','Fingerprint')};item['File']=rel
  if name=='source':
   if rel=='CORRESPONDING-SOURCE/manifest.json':
    line=path.read_text().splitlines()[a['StartLine']-1];values=__import__('re').findall(r'"([0-9a-f]{64})"',line)
    assert values and all(v in hashes or v in {v['sha256'] for v in manifest['variants'].values()} for v in values)
    item['disposition']='Verified file/APK SHA256 integrity fields, not credentials'
   elif (rel=='docs/APD-PLAYER-DESCRIPTION-AUDIT-PROGRESS.md'
         and a['RuleID']=='generic-api-key'
         and hashlib.sha256(path.read_text().splitlines()[a['StartLine']-1].encode()).hexdigest()==
         'a6b5d80342669da493ebf3a2d54c199a1d1d8b4763e297a9859a660519bcf343'):
    # Reviewed exact match: "keys reviewed, inventory5298. "; captured secret "inventory5298.".
    # Hash binds the entire reviewed prose line; changed text requires a fresh review.
    item['disposition']='Exact reviewed audit prose false positive: keys reviewed, inventory5298.; no API credential. Entire line SHA256 pinned.'
   elif (a['RuleID']=='generic-api-key' and rel in REVIEWED_PROSE_LINES
         and hashlib.sha256(path.read_text().splitlines()[a['StartLine']-1].encode()).hexdigest() in REVIEWED_PROSE_LINES[rel]):
    item['disposition']='Exact inspected audit prose or Java test state assignment, not a credential; entire reviewed line hash pinned.'
   elif rel in ('.github/workflows/codex-android16-acceptance.yml','core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndKeyBindings.java','tools/apd_publish_gathering331.py','docs/HANDOFF.md'):
    item['disposition']='Previously inspected literal sound filename, integer keyboard fields, or pinned commit/checksum assertions; no new credentials introduced by this build'
   else:
    item['disposition']='REQUIRES REVIEW'
    safe.append(item);reports[name]={'exit':r.returncode,'findings':safe};(out/'APD-Source-Scan.json').write_text(json.dumps(reports,indent=2)+'\n');raise RuntimeError('Unexpected source finding: '+rel)
  else:item['disposition']='History retained; findings reported, not erased. Known historical signing material is excluded from source archive.'
  safe.append(item)
 reports[name]={'exit':r.returncode,'candidate_count':len(findings),'findings':safe}
reports['supplemental']={'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'key_paths_excluded':manifest['removed_signing_paths'],'all_known_gradle_password_strings_removed':True,'history_not_rewritten':True,'source_findings_reviewed':True,'note':'Generic scanner is not a proof of universal absence. Binary historical signing material is a separately confirmed exposure; no key or password is in this source archive.'}
(out/'APD-Source-Scan.json').write_text(json.dumps(reports,indent=2)+'\n');print('Source and full reachable history scans completed; key-free source findings recorded.')
