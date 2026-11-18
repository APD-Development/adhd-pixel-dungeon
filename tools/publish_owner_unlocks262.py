"""Reverify an existing APK and privately deliver it; never build or overwrite."""
import datetime,hashlib,json,os,pathlib,subprocess,sys,zipfile
from PIL import Image
from pypdf import PdfReader
REPO=os.environ['GITHUB_REPOSITORY'];SOURCE='e4ee7e6d213a997163ecc2520da0eea309a1a819'
RUN=38977979688;MATRIX=38977979689;CAPTURE=38979689121
CAPTURE_SOURCE='efa5443b9f59d24a173d0699829a887f34a28713'
NUMBER=262;TAG='apd-alpha-preview-262'
api=lambda p:json.loads(subprocess.check_output(['gh','api',p]))
assert api('repos/'+REPO)['private']
sys.path.insert(0,'tools')
from apd_existing_build_evidence import application_inputs,xml_summary,certificates
run=api(f'repos/{REPO}/actions/runs/{RUN}')
assert run['head_sha']==SOURCE and run['run_number']==NUMBER and run['status']=='completed' and run['conclusion']=='success',run
jobs=api(f'repos/{REPO}/actions/runs/{RUN}/jobs')['jobs']
assert all(any(j['name']==name and j['conclusion']=='success'for j in jobs)for name in ['verify-and-build','android-smoke-check'])
trees=[]
for sha in [SOURCE,os.environ['GITHUB_SHA'],'7368f13940690e599ca05c47a1cf29cca6beed09']:
    t=api(f'repos/{REPO}/git/trees/{sha}?recursive=1');assert not t.get('truncated');trees.append(application_inputs(t['tree']))
assert trees[0]==trees[1],'Delivery wrapper changes application inputs'
delta={p for p in set(trees[0])|set(trees[2])if trees[0].get(p)!=trees[2].get(p)}
assert delta=={'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Badges.java','core/src/main/assets/apd/releases/apd-alpha-preview.tsv'},delta
def artifact(run_id,name,folder):
    matches=[a for a in api(f'repos/{REPO}/actions/runs/{run_id}/artifacts')['artifacts']if a['name']==name and not a['expired']]
    assert len(matches)==1,name
    a=matches[0];p=pathlib.Path(folder+'.zip')
    with p.open('wb')as out:subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{a["id"]}/zip'],stdout=out,check=True)
    assert a['digest']=='sha256:'+hashlib.sha256(p.read_bytes()).hexdigest()
    zipfile.ZipFile(p).extractall(folder)
    return {'id':a['id'],'zip_sha256':a['digest']}
artifacts={name:artifact(RUN,name,folder)for name,folder in [('apd-codex-private-test-apk','candidate'),('apd-codex-core-tests','core-evidence'),('apd-codex-android-checks','native-evidence')]}
capture_run=api(f'repos/{REPO}/actions/runs/{CAPTURE}')
assert capture_run['head_sha']==CAPTURE_SOURCE and capture_run['status']=='completed' and capture_run['conclusion']=='success'
artifacts['capture-recovery']=artifact(CAPTURE,'apd-title-display-capture-gap','capture-recovery')
matrix=api(f'repos/{REPO}/actions/runs/{MATRIX}')
assert matrix['head_sha']==SOURCE and matrix['status']=='completed' and matrix['conclusion']=='failure'
matrix_jobs=api(f'repos/{REPO}/actions/runs/{MATRIX}/jobs')['jobs']
assert len(matrix_jobs)==5
assert [j['conclusion']for j in matrix_jobs if 'square-virtual'in j['name']]==['failure']
assert all(j['conclusion']=='success'for j in matrix_jobs if 'square-virtual'not in j['name'])
for display in ['short-landscape','m53-representative','compact-virtual','small-virtual','square-virtual']:
    artifacts[display]=artifact(MATRIX,'apd-title-display-'+display,'supplemental-'+display)
failed=pathlib.Path('supplemental-square-virtual/title-instrumentation.txt').read_text()
assert 'Tests run: 1,  Failures: 1'in failed
assert 'ANR in com.android.launcher3'in pathlib.Path('supplemental-square-virtual/title-logcat.txt').read_text()
core=xml_summary('core-evidence');native=xml_summary('native-evidence')
assert core['tests']==1761 and native['tests']==37
recovery=xml_summary('capture-recovery');assert recovery['tests']==3
specific=[c for c in core['cases']if c['classname']=='com.shatteredpixel.shatteredpixeldungeon.ClassUnlockNotificationTest']
assert len(specific)==5
assert any(c['name']=='aRunCanBePlayedSavedContinuedAndEndedWithItsHistoryRecorded'for c in native['cases'])
artifacts['previous-native']=artifact(38975988571,'apd-title-display-full-regression','previous-native')
previous=xml_summary('previous-native')
ids=lambda r:{(c['classname'],c['name'])for c in r['cases']}
assert ids(previous)==ids(native),'Required Android coverage changed'
assert ids(recovery)<=ids(native),'Capture recovery must reuse existing passing methods'
required=json.loads(pathlib.Path('tools/test262-required-images.json').read_text())
assert len(required)==57
captures=[]
for name in required:
    matches=list(pathlib.Path('native-evidence').rglob(name))+list(pathlib.Path('capture-recovery').rglob(name));assert len(matches)==1,(name,matches)
    with Image.open(matches[0])as im:im.verify()
    captures.append(matches[0])
apk=next(pathlib.Path('candidate').glob('*.apk'));digest=hashlib.sha256(apk.read_bytes()).hexdigest()
assert digest=='5ad5beb717673a51a10a646c28b887ed54a4f78f8d25ee351834a39a29ecf8d2'
assert next(pathlib.Path('candidate').glob('*.apk.sha256')).read_text().split()[0]==digest
recovery_origin=json.loads(pathlib.Path('capture-recovery/title-evidence/acceptance-source.json').read_text())
assert recovery_origin['application_source']==SOURCE and recovery_origin['application_sha256']==digest and not recovery_origin['application_rebuilt'] and recovery_origin['protected_application_inputs_identical']
for display in ['short-landscape','m53-representative','compact-virtual','small-virtual','square-virtual']:
    origin=json.loads(pathlib.Path('supplemental-'+display,'title-evidence','acceptance-source.json').read_text())
    assert origin['application_source']==SOURCE and origin['application_sha256']==digest and not origin['application_rebuilt'] and origin['protected_application_inputs_identical']
    if display!='square-virtual':assert xml_summary('supplemental-'+display)['tests']==1
with zipfile.ZipFile(apk)as z:
    notes=z.read('assets/apd/releases/apd-alpha-preview.tsv').decode()
    assert 'Already available characters' in notes and 'earned progression' in notes
    assert any(line.split('\t')[1]=='Codex TEST 262'for line in z.read('assets/apd/history/index.tsv').decode().splitlines()if len(line.split('\t'))==4)
    assert 'Already playable characters'in z.read('assets/apd/history/codex-test-262.tsv').decode()
subprocess.run(['gh','release','download','apd-alpha-preview-261','--repo',REPO,'--pattern','ADHD-Pixel-Dungeon-Alpha-Preview-261.apk','--dir','fallback'],check=True)
old=pathlib.Path('fallback/ADHD-Pixel-Dungeon-Alpha-Preview-261.apk')
assert hashlib.sha256(old.read_bytes()).hexdigest()=='71eb6e057d4cdbaa194c8b37e9d243079e7e0849afc6eb164fd66c9187844833'
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
for path in [apk,old]:
    assert certificates(subprocess.check_output([str(signer),'verify','--print-certs',str(path)],text=True))==[cert]
    badging=subprocess.check_output([str(signer.parent/'aapt'),'dump','badging',str(path)],text=True)
    assert "name='com.charltonchris29.adhdpixeldungeon.indev' versionCode='920'"in badging
badging=subprocess.check_output([str(signer.parent/'aapt'),'dump','badging',str(apk)],text=True)
assert "versionName='4.0.8-apd-INDEV-bC262-TEST'"in badging
root=pathlib.Path('delivery');root.mkdir()
name='ADHD-Pixel-Dungeon-Alpha-Preview-262.apk'
(root/name).write_bytes(apk.read_bytes());(root/(name+'.sha256')).write_text(digest+'  '+name+'\n')
now=datetime.datetime.now(datetime.timezone.utc).isoformat()
evidence={'source':SOURCE,'run':RUN,'workflow_conclusion':run['conclusion'],'producer_and_native_jobs_passed':True,'core':core,'android':native,'capture_recovery_run':CAPTURE,'capture_recovery_source':CAPTURE_SOURCE,'capture_recovery':recovery,'android_accepted_executions':44,'android_unique_methods':37,'artifacts':artifacts,'reviewed_required_images':57,'manual_visual_review_completed':True,'supplemental_display_run':MATRIX,'supplemental_display_passed':4,'supplemental_display_failed':1,'supplemental_failure':'Captured Quickstep launcher ANR blocked native Changes tap on square display; raw failure retained, not counted as accepted coverage.','small_options_capture_verified':False,'physical_phone_tested':False,'owner_save_install_over_tested':False,'apk_sha256':digest,'certificate_sha256':cert,'application_id':'com.charltonchris29.adhdpixeldungeon.indev','version_code':920,'application_inputs_identical_through_publication':True,'application_rebuilt_for_delivery':False,'prepared_at':now,'all_backlog_complete':False}
(root/'TEST262-Verification.json').write_text(json.dumps(evidence,indent=2))
report='''# APD Alpha Preview 262 — already-available character notifications

## Cause and correction
All six classes are playable in APD's INDEV alpha, but the inherited character unlock validators checked only their original global badges. A player could therefore be told that a character had just unlocked despite already being able to play it. Warrior is always available and has no unlock badge. The five original conditions are: Mage, use a Scroll of Upgrade; Rogue, ten surprise attacks; Huntress, ten thrown attacks; Duelist, eligible tier-two-or-higher weapon without a strength penalty; Cleric, fully cleanse cursed equipment.

The common badge presentation path now checks the corresponding class's actual availability. If it is already playable, the original badge is earned silently without an unlock log or popup. All validators, thresholds, character availability, seeded-run exclusions, unrelated achievements and badge persistence remain unchanged. No character is relocked and no progression is reset. A genuinely locked class under the original future policy still announces its normal unlock.

## Verified evidence
The complete core suite passed 1,761 unique methods, with zero failures, errors or skipped methods. Five permanent class-unlock regressions cover fresh alpha players and all six classes, original five conditions, identified/unidentified equipment, restored badge bundles, seeded-run exclusions, unrelated badge mapping and genuine original-policy announcements. The original exact source reproduces all five unwanted announcements and fails the fresh-player regression; the correction passes all five tests.

The 37 required API34 Android methods passed without failures, errors or skips. The existing real-run test now invokes all original validators in fresh/restored badge states, checks no unlock message is dispatched, confirms all six classes remain available and restores the test hero, badge and log state. The primary collector copied only 52 of 57 expected images: it omitted five existing scoring/hotbar captures. Staging run38977979687 correctly refused that incomplete image set and remains failed. The collector list is corrected for future runs, and three existing passing methods were repeated against the original signed APK to recover those five images, including the actual gameplay/class-notification check. The application was not rebuilt. All 57 required native screenshots were decoded and visually reviewed. The three repeated methods are part of the same 37 distinct identities, not additional unique coverage. This is emulator evidence, not physical-phone or owner-save install-over evidence. The required 37 method identities match the preceding required suite.

Debug compilation and exact immediate-predecessor APK history checks passed. Player-facing APD Changes includes the fix under Bug Fixes; compiled notes are checked in the actual APK. The only production changes from TEST261 are badge presentation and that release-note asset. Delivery reuses the tested artifact and verifies its hash, signing certificate, application ID and version code against delivered TEST261. It does not build another APK.

## Supplemental Android display limits
The separate five-display native menu matrix is not fully green: four methods passed and the square-display method failed when an Android Quickstep launcher ANR dialog blocked the Changes tap. Both the screenshot and Android log identify com.android.launcher3; the failed method and raw evidence are retained, not counted as accepted coverage. No application change was made to hide this failure. The small-display method passed its native layout and preference interaction assertions, but its Options screenshot shows the underlying title screen instead of the settings dialog; that image is not claimed as visual verification of the settings window. These supplemental failures and capture limits are separate from the successful required 37-method suite and do not establish an APD class-unlock defect. No physical Galaxy, RedMagic or virtual-phone performance is inferred.

## Preserved work and limits
TEST261's donation-promotion removal, original contributor credits, Waterskin/trap confirmation preferences, class lore, sliders, all 357 scoring values, save systems, player statistics and historical scores remain unchanged. Install over APD; never uninstall or clear data. TEST261 and its assets remain the fallback.

Earlier TEST260/261 fixture failures and the Quickstep-ANR display failure remain recorded in TEST261's PDF and evidence; they are not relabeled successful. Local verification also retains the original-source failing regression before the corrected five-test pass. This report does not claim the entire backlog is complete, physical-device verification, new audio fixes, completed storage optimization or complete historical reconstruction. No paid service, unfinished online feature, public release or automatic tester distribution is enabled.
'''
(root/'report.md').write_text(report)
(root/'verification.txt').write_text('Application source: '+SOURCE+'\nSuccessful workflow: '+str(RUN)+'\nAPK SHA-256: '+digest+'\nPrepared: '+now)
subprocess.run(['python3','tools/apd_bug_report_pdf.py',str(root/'report.md'),str(root/'APD-Alpha-Preview-262.pdf'),'--verification',str(root/'verification.txt')],check=True)
assert 1<=len(PdfReader(root/'APD-Alpha-Preview-262.pdf').pages)<=8
with zipfile.ZipFile(root/'APD-Android-Evidence-262.zip','w',zipfile.ZIP_DEFLATED)as z:
    for folder in ['core-evidence','native-evidence','capture-recovery']+['supplemental-'+d for d in ['short-landscape','m53-representative','compact-virtual','small-virtual','square-virtual']]:
        for p in pathlib.Path(folder).rglob('*'):
            if p.is_file():z.write(p,str(p))
assets=[name,name+'.sha256','APD-Alpha-Preview-262.pdf','TEST262-Verification.json','APD-Android-Evidence-262.zip']
assert not any(r['tag_name']==TAG for r in api(f'repos/{REPO}/releases?per_page=100')),'Preserve existing release assets'
subprocess.run(['gh','release','create',TAG,'--repo',REPO,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title','APD Alpha Preview 262 — character unlock notices','--notes','Private owner test: already-playable characters no longer show redundant unlock notifications. Availability, original progression and all prior fixes preserved. Full verification and limitations in PDF. Install over APD; never uninstall or clear data. No automatic tester distribution.']+[str(root/n)for n in assets],check=True)
release=api(f'repos/{REPO}/releases/tags/{TAG}');assert release['prerelease']and not release['draft']
for n in assets:
    a=next(a for a in release['assets']if a['name']==n)
    assert a['digest']=='sha256:'+hashlib.sha256((root/n).read_bytes()).hexdigest()
for n in [name,'APD-Alpha-Preview-262.pdf']:
    subprocess.run(['gh','release','download',TAG,'--repo',REPO,'--pattern',n,'--dir','published'],check=True)
    assert hashlib.sha256(pathlib.Path('published',n).read_bytes()).hexdigest()==hashlib.sha256((root/n).read_bytes()).hexdigest()
receipt={'source':SOURCE,'run':RUN,'apk_sha256':digest,'published_at':release['published_at'],'download_verified_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'apk_url':next(a['browser_download_url']for a in release['assets']if a['name']==name),'pdf_url':next(a['browser_download_url']for a in release['assets']if a['name']=='APD-Alpha-Preview-262.pdf'),'chat_link_sent':False,'physical_phone_tested':False}
pathlib.Path('publication-result.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2))
