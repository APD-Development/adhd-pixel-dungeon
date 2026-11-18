"""Publish owner-requested unchanged TEST323 APK pair to the private repository."""
import hashlib,json,os,pathlib,re,subprocess,zipfile,sys
def cmd(*a): return subprocess.check_output(a,text=True)
def api(p): return json.loads(cmd('gh','api',p))
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 repo=os.environ['GITHUB_REPOSITORY'];root=pathlib.Path('delivery');source='51fe6b502bee67893b9773eacddeb01eac778547'
 assert api('repos/'+repo)['private']
 sys.path.insert(0,'tools')
 from apd_existing_build_evidence import application_inputs
 trees=[api(f'repos/{repo}/git/trees/{ref}?recursive=1') for ref in (source,os.environ['GITHUB_SHA'])]
 assert all(not t.get('truncated') for t in trees)
 assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
 for rid,src in [(40737013809,source),(40737013887,source),(40744323333,'4c29331a19025f926d4785e822d4d5e04ee516c8')]:
  r=api(f'repos/{repo}/actions/runs/{rid}')
  assert r['head_sha']==src and r['status']=='completed' and r['conclusion']=='success'
 ev=json.loads((root/'TEST323-Verification.json').read_text())
 assert ev['source']==source and ev['publication_app_tree_matches_source']
 for key,count in [('core',2121),('android',62)]:
  x=ev[key];assert x['tests']==len(x['cases'])==len({(c['classname'],c['name']) for c in x['cases']})==count
  assert x['failures']==x['errors']==x['skipped']==0
 assert sha(root/'APD-Alpha-Preview-323.pdf')=='67a326beed77f5c603cb1606984dce81429f2abc4c3fbcafe0891534d2e43d9b'
 dev=root/'ADHD-Pixel-Dungeon-CODEX-TEST-323.apk';player=next(pathlib.Path('player').rglob('*.apk'))
 expected=['f599e05202277cc2056255e1e6c8ca915eca68a8849af4959e633b678e515467','ad5a2febbccc6460c5644fdd7fbc8c7f35d4cc26ed917c0730b09ed815b14d24']
 signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
 cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
 for p,e in zip([dev,player],expected):
  assert sha(p)==e
  assert cert in cmd(str(signer),'verify','--print-certs',str(p))
  s=cmd(str(signer.parent/'aapt'),'dump','badging',str(p))
  package=tuple(re.search(pattern,s).group(1) for pattern in (r"package: name='([^']+)'",r"versionCode='([^']+)'",r"versionName='([^']+)'"))
  print(p.name,package)
  assert package[:2]==('com.charltonchris29.adhdpixeldungeon.indev','920')
  if p==dev: assert package[2]=='4.0.8-apd-INDEV-bC323-TEST'
  # Player branding can differ; its exact APK bytes are pinned by the verified SHA above.
 with zipfile.ZipFile(dev) as a,zipfile.ZipFile(player) as b:
  hist=lambda z:{n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')}
  assert hist(a)==hist(b) and len(hist(a))==351
  assert 'assets/apd/features.txt' not in b.namelist()
 dest=root/'APD-Player-TEST323.apk';dest.write_bytes(player.read_bytes())
 for p in [dev,dest]:
  pathlib.Path(str(p)+'.sha256').write_text(sha(p)+'  '+p.name+'\n')
 notes="""Private owner test: two install-compatible APK variants, no new build or automatic tester distribution.
Both variants share one application ID; install one over your existing APD. Never uninstall or clear data.

TEST323: quicker menu returns and UIMSD Home drawing; unchanged APD Settings and game mechanics.
Verification: 2121 core cases per variant, 62 main API34 cases, five displays, 82 native images. Synthetic72-run emulator tap-to-Home162ms; no physical-phone latency claim.
Legacy cold-restart/death-order workflows remain failed before tests due to pinned historical/shallow-checkout configuration. They are not passing gates. Full limits are in the attached PDF and receipt.
Source: """+source
 (root/'owner-release-notes.txt').write_text(notes)
 tag='apd-alpha-preview-323'
 assert subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True).returncode!=0,'Existing release preserved'
 files=[dev,dest,pathlib.Path(str(dev)+'.sha256'),pathlib.Path(str(dest)+'.sha256'),root/'APD-Alpha-Preview-323.pdf',root/'TEST323-Verification.json']
 subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title','APD Alpha Preview 323 — private owner test','--notes-file',str(root/'owner-release-notes.txt')]+list(map(str,files)),check=True)
 release=api(f'repos/{repo}/releases/tags/{tag}')
 assert not release['draft'] and release['prerelease']
 for p in files:
  a=next(a for a in release['assets'] if a['name']==p.name)
  assert a['state']=='uploaded' and a['digest']=='sha256:'+sha(p) and a['size']==p.stat().st_size
  print(a['browser_download_url'])
 (root/'publication.json').write_text(json.dumps(release,indent=2))
if __name__=='__main__':main()
