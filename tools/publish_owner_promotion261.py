"""Publish one already verified owner APK; never build or overwrite assets."""
import datetime, hashlib, json, os, pathlib, re, subprocess, sys, zipfile
from PIL import Image
from pypdf import PdfReader

REPO=os.environ['GITHUB_REPOSITORY']
SOURCE='7368f13940690e599ca05c47a1cf29cca6beed09'
RUN=38975391839
MATRIX=38975988571
NATIVE_SOURCE="655128758111d740994d86bdbfe1f8e819e3605a"
SMALL=38976735864
SMALL_SOURCE="8a44f5d2923e4390164817962944b3b9421e785f"
NUMBER=261
TAG='apd-alpha-preview-261'
ROOT=pathlib.Path('delivery');ROOT.mkdir(exist_ok=True)
api=lambda path:json.loads(subprocess.check_output(['gh','api',path]))
assert api('repos/'+REPO)['private'],'Private repository required'
sys.path.insert(0,'tools')
from apd_existing_build_evidence import application_inputs, xml_summary, certificates

def run(number):
    r=api(f'repos/{REPO}/actions/runs/{number}')
    assert r['head_sha']==(SOURCE if number==RUN else SMALL_SOURCE if number==SMALL else NATIVE_SOURCE) and r['status']=='completed',r
    if number==SMALL:assert r['conclusion']=='success',r
    if number==RUN:assert r['run_number']==NUMBER
    return r

primary=run(RUN);matrix=run(MATRIX);small=run(SMALL)
jobs=api(f'repos/{REPO}/actions/runs/{RUN}/jobs')['jobs']
for name in ['verify-and-build']:
    assert any(j['name']==name and j['conclusion']=='success'for j in jobs),name
jobs=api(f'repos/{REPO}/actions/runs/{MATRIX}/jobs')['jobs']
assert len(jobs)==6 and all(j['conclusion']=='success'for j in jobs if 'small-virtual'not in j['name'])
assert [j['conclusion']for j in jobs if 'small-virtual'in j['name']]==['failure']
small_jobs=api(f'repos/{REPO}/actions/runs/{SMALL}/jobs')['jobs']
assert len(small_jobs)==1 and small_jobs[0]['conclusion']=='success'
trees=[]
for sha in [SOURCE,os.environ['GITHUB_SHA']]:
    t=api(f'repos/{REPO}/git/trees/{sha}?recursive=1')
    assert not t.get('truncated');trees.append(application_inputs(t['tree']))
assert trees[0]==trees[1],'Delivery wrapper changes application inputs'
previous=api(f'repos/{REPO}/git/trees/8d9381e5116678f51fb2f2d70b23a681102a85bd?recursive=1')
assert not previous.get('truncated')
old_inputs=application_inputs(previous['tree'])
changed={p for p in set(old_inputs)|set(trees[0]) if old_inputs.get(p)!=trees[0].get(p)}
allowed={p for p in trees[0] if p.startswith(('core/src/main/assets/messages/scenes/','core/src/main/assets/messages/windows/'))and p.endswith('.properties')}
allowed.update('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/'+p for p in ['items/keys/WornKey.java','windows/WndVictoryCongrats.java','windows/WndSupportPrompt.java','scenes/SupporterScene.java'])
allowed.add('core/src/main/assets/apd/releases/apd-alpha-preview.tsv')
assert changed==allowed and len(changed)==51,changed^allowed


def artifact(run_id,name,destination):
    found=[a for a in api(f'repos/{REPO}/actions/runs/{run_id}/artifacts')['artifacts']if a['name']==name and not a['expired']]
    assert len(found)==1,name
    a=found[0];p=pathlib.Path(name+'.zip')
    with p.open('wb')as out:subprocess.run(['gh','api',f'repos/{REPO}/actions/artifacts/{a["id"]}/zip'],stdout=out,check=True)
    assert hashlib.sha256(p.read_bytes()).hexdigest()==a['digest'].removeprefix('sha256:')
    zipfile.ZipFile(p).extractall(destination)
    return {'id':a['id'],'zip_sha256':a['digest']}

artifacts={}
for name,where in [('apd-codex-private-test-apk','candidate'),('apd-codex-core-tests','core-evidence')]:
    artifacts[name]=artifact(RUN,name,where)
artifacts['corrected-native']=artifact(MATRIX,'apd-title-display-full-regression','native-evidence')
core=xml_summary('core-evidence');native=xml_summary('native-evidence')
artifacts['previous-native']=artifact(38973264847,'apd-codex-android-checks','previous-native-evidence')
old_native=xml_summary('previous-native-evidence')
ids=lambda data:{(c['classname'],c['name'])for c in data['cases']}
assert ids(old_native)==ids(native) and len(ids(native))==37
assert core['tests']==len(core['cases'])==1756 and core['failures']==core['errors']==core['skipped']==0
assert native['tests']==len(native['cases'])==37 and native['failures']==native['errors']==native['skipped']==0
assert any(c['classname'].endswith('InheritedPromotionTest')for c in core['cases'])
assert any(c['name']=='aRunCanBePlayedSavedContinuedAndEndedWithItsHistoryRecorded'for c in native['cases'])
full_folder=pathlib.Path('native-evidence/title-evidence')
full_origin=json.loads((full_folder/'acceptance-source.json').read_text())
full_result=json.loads((full_folder/'device-acceptance-result.json').read_text())
assert full_result['methods']==37 and full_result['failures']==full_result['errors']==full_result['skipped']==0 and len(full_result['images'])==57
assert full_origin['application_source']==SOURCE and not full_origin['application_rebuilt'] and full_origin['protected_application_inputs_identical']
images=sorted(full_folder/n for n in full_result['images'])
assert len(images)==57,len(images)
assert {'boss-key-no-promotion.png','victory-no-promotion.png'}<=set(p.name for p in images)
for p in images:
    with Image.open(p)as im:im.verify()
displays={}
for name,size,density in [('m53-representative',[1080,2408],420),('compact-virtual',[360,560],160),('small-virtual',[320,480],160),('square-virtual',[720,720],320),('short-landscape',[960,320],160)]:
    folder=pathlib.Path('displays')/name
    artifacts[name]=artifact(SMALL if name=='small-virtual'else MATRIX,'apd-title-display-'+name,folder)
    e=folder/'title-evidence'
    origin=json.loads((e/'acceptance-source.json').read_text())
    result=json.loads((e/'device-acceptance-result.json').read_text())
    assert origin['application_source']==SOURCE and not origin['application_rebuilt'] and origin['protected_application_inputs_identical']
    assert result['methods']==1 and result['failures']==result['errors']==result['skipped']==0 and len(result['images'])==4
    for filename,pixels in result['images'].items():
        assert pixels==size,(name,pixels,size)
        with Image.open(e/filename)as im:im.verify()
    displays[name]={'origin':origin,'result':result,'display':(e/'display.txt').read_text()}

apk=next(pathlib.Path('candidate').glob('*.apk'))
digest=hashlib.sha256(apk.read_bytes()).hexdigest()
assert digest=='71eb6e057d4cdbaa194c8b37e9d243079e7e0849afc6eb164fd66c9187844833'
with zipfile.ZipFile(apk)as package:
    for member in package.namelist():
        if member.endswith(('.dex','.properties','.tsv','.html','.txt','.json')):
            body=package.read(member).lower()
            assert all(domain not in body for domain in [b'patreon.com',b'paypal.me',b'buymeacoffee.com',b'ko-fi.com']),member
            if member.endswith('.properties'):
                assert b'scenes.supporterscene.'not in body and b'scenes.titlescene.patreon_'not in body and b'windows.wndvictorycongrats.support_prompt='not in body,member

assert next(pathlib.Path('candidate').glob('*.apk.sha256')).read_text().split()[0]==digest
assert full_origin['application_sha256']==digest and all(d['origin']['application_sha256']==digest for d in displays.values())
subprocess.run(['gh','release','download','apd-alpha-preview-258','--repo',REPO,'--pattern','ADHD-Pixel-Dungeon-Alpha-Preview-258.apk','--dir','fallback'],check=True)
old=pathlib.Path('fallback/ADHD-Pixel-Dungeon-Alpha-Preview-258.apk')
assert hashlib.sha256(old.read_bytes()).hexdigest()=='37f38f0c6532c8a3b1357dd58747ae6a723b616992376ad6b0f6fcfcb76febc5'
signer=sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
cert='067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
for p in [apk,old]:
    assert certificates(subprocess.check_output([str(signer),'verify','--print-certs',str(p)],text=True))==[cert]
    metadata=subprocess.check_output([str(signer.parent/'aapt'),'dump','badging',str(p)],text=True)
    assert "name='com.charltonchris29.adhdpixeldungeon.indev' versionCode='920'"in metadata
metadata=subprocess.check_output([str(signer.parent/'aapt'),'dump','badging',str(apk)],text=True)
assert "versionName='4.0.8-apd-INDEV-bC261-TEST'"in metadata
name='ADHD-Pixel-Dungeon-Alpha-Preview-261.apk'
target=ROOT/name
target.write_bytes(apk.read_bytes());pathlib.Path(str(target)+'.sha256').write_text(digest+'  '+name+'\n')
now=datetime.datetime.now(datetime.timezone.utc).isoformat()
verification={'source':SOURCE,'run':RUN,'original_workflow_conclusion':primary['conclusion'],'producer_job_conclusion':'success','corrected_native_run':MATRIX,'corrected_native_source':NATIVE_SOURCE,'core':core,'android':native,'apk_sha256':digest,'certificate_sha256':cert,'application_id':'com.charltonchris29.adhdpixeldungeon.indev','version_code':920,'publication_application_inputs_identical':True,'application_rebuilt_for_delivery':False,'physical_phone_tested':False}
(ROOT/'TEST261-Verification.json').write_text(json.dumps(verification,indent=2))
artifacts['failed-small-display']=artifact(MATRIX,'apd-title-display-small-virtual','failed-small-evidence')
status={'source':SOURCE,'primary_run':RUN,'display_run':MATRIX,'small_retry_run':SMALL,'small_retry_source':SMALL_SOURCE,'failed_small_display_method_count':1,'corrected_native_source':NATIVE_SOURCE,'original_workflow_conclusion':primary['conclusion'],'original_native_test_executions':0,'original_native_compile_failed':True,'artifacts':artifacts,'displays':displays,'core_unique_methods':1756,'android_unique_methods':37,'android_accepted_executions':42,'reviewed_required_images':77,'manual_visual_review_completed':True,'physical_phone_tested':False,'all_backlog_complete':False,'failed_TEST260_run':38974830422,'TEST260_apk_exists':False,'prepared_at':now}
(ROOT/'APD-Promotion-Removal-261.json').write_text(json.dumps(status,indent=2))
report="""# APD Alpha Preview 261 — tester promotion correction

## Fix and exact cause
Goo's Worn Key had inherited SPD's first-support-message hook. If its old support preference was false, pickup scheduled a developer greeting with a Patreon appeal and link. WornKey now uses the ordinary Key pickup path: journal registration, quantity, normal turn cost and Skeleton Key interactions are preserved. No boss, loot or key reward changes.

Victory also had a support paragraph/button leading to the same donation scene. That promotion is removed; the existing Close button now fills its row. Old support window/scene class names remain compatible but show ordinary credits or route to About. Removed 276 obsolete promotion keys from 46 verified resources (23 languages). About's Evan/SPD, Watabou, artist, music, sound, libGDX, copyright and GPL credits/links are unchanged. Historical SPD notes describing old donation features remain historical records, not active appeals.

## Actual verification
The complete core suite passed 1,756 unique methods, zero failures/errors/skips. The required native Android regression suite passed 37 methods, zero failures/errors/skips. The expanded real-run check picked up Worn Key with both old preference values without opening a support window, then opened and closed the revised victory dialog. Two new screenshots show those screens.

Five accepted same-APK display runs passed the native menu/touch/preference method at actual 1080×2408/420dpi, 360×560/160dpi, 320×480/160dpi, 720×720/320dpi and 960×320/160dpi. Accepted native coverage is 37 distinct methods across 42 executions. All 57 full-suite and 20 display images were decoded and visually reviewed. These are emulators, not physical-phone measurements.

Two new core regressions verify normal pickup quantity/cost, unchanged legacy preference, runtime donation URLs and localized appeals, and retained required attribution. Existing per-message Adult Humor clean/adult assertions remain intact.

## Failures retained
TEST260 ran 1,756 core methods: 1,754 passed and two failed; no TEST260 APK or Android run exists. The new source scan needed Gradle's declared source path because tests run from assets. The English safety test checked all retained messages, then failed its old more-than-5,000 inventory sanity check after 12 promotion keys were removed. It now checks the exact 4,998 remaining English values in nine files without removing any language-safety assertion. No production behavior changed for this retry.

The first TEST261 Android test compilation then failed on a missing checked-exception handler inside the new Runnable. That fixture was corrected without changing or rebuilding the application. The corrected native run uses the original signed TEST261 APK, verifies identical application inputs, and passes the same 37 method identities as the previous required suite. The original workflow remains failed; it is not relabeled green. Its producer/core success and the separate corrected native acceptance are recorded explicitly.

TEST259's extra display matrix failed before game tests because its wrapper omitted a required application receipt. The wrapper now verifies ZIP/APK hashes, records source identity, checks actual resolution on a tall emulator and dismisses Android's first-use full-screen notice. The failed run remains failed. TEST259 itself passed 1,754 core/37 Android/55 reviewed images and contains the earlier Markdown literal-backslash export correction; that fix is preserved, not attributed as new in 261.

The first 320×480 Pixel7-profile display run failed one method because Android's Quickstep launcher displayed an ANR dialog over APD and blocked touch input. The actual failed screenshot and log were inspected and retained. A clean small-profile emulator reran the exact same APK and 320×480 resolution successfully, with no disabled launcher/System UI, suppressed errors or application changes. The failed method is not counted as accepted coverage; it remains an additional unsuccessful execution. This is emulator failure evidence, not proof of an APD crash or of performance on the tester's phone.

Initial local fixture attempts required the native test library and isolated Notes state; a temporary generic-type compile error was corrected. Only successful recompiles/reruns are counted. The full repeated suite verifies the final corrected test sources.

## Compatibility, risks and unfinished work
Application ID, version code, raw checksum and signing certificate are verified against retained TEST258. Install over APD; never uninstall or clear data. Existing preference values, saved games, raw UIMSD histories, all 18 slider mechanics, 357 multiplier values and earned scores are unchanged.

No physical phone or owner active-save install-over is claimed. Language appeal removals were scoped by exact property prefixes; credits and unrelated dialogue remain. Human full-run/class/boss strategies, broader UIMSD audit/storage, armor-playthrough reproduction, inherited audio and historical gaps remain open. The separate controlled audio queue diagnostic proves only synthetic queue behavior, not the reported audible bug. No production audio change, public release, paid service, unfinished online feature or automatic tester distribution.
"""
(ROOT/'report.md').write_text(report)
(ROOT/'verification.txt').write_text('Application source: '+SOURCE+'\nVerification run: '+str(RUN)+'\nDisplay run: '+str(MATRIX)+'\nAPK SHA-256: '+digest+'\nPrepared: '+now)
subprocess.run(['python3','tools/apd_bug_report_pdf.py',str(ROOT/'report.md'),str(ROOT/'APD-Alpha-Preview-261.pdf'),'--verification',str(ROOT/'verification.txt')],check=True)
assert 1<=len(PdfReader(ROOT/'APD-Alpha-Preview-261.pdf').pages)<=12
with zipfile.ZipFile(ROOT/'APD-Android-Evidence-261.zip','w',zipfile.ZIP_DEFLATED)as z:
    for folder in ['core-evidence','native-evidence','previous-native-evidence','failed-small-evidence','displays']:
        for p in pathlib.Path(folder).rglob('*'):
            if p.is_file():z.write(p,str(p))

assets=[name,name+'.sha256','APD-Alpha-Preview-261.pdf','TEST261-Verification.json','APD-Promotion-Removal-261.json','APD-Android-Evidence-261.zip']
assert not any(r['tag_name']==TAG for r in api(f'repos/{REPO}/releases?per_page=100')),'Preserve any existing release and assets'
subprocess.run(['gh','release','create',TAG,'--repo',REPO,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title','APD Alpha Preview 261 — uninterrupted boss rewards','--notes','Private owner test: removed inherited boss-key and victory donation promotions; original contributor credits and normal rewards preserved. Complete tests, screenshots, failures and compatibility details are in the PDF. Install over APD; never uninstall or clear data. No automatic tester distribution.']+[str(ROOT/n)for n in assets],check=True)
release=api(f'repos/{REPO}/releases/tags/{TAG}');assert release['prerelease']and not release['draft']
for n in assets:
    a=next(a for a in release['assets']if a['name']==n)
    assert a['digest']=='sha256:'+hashlib.sha256((ROOT/n).read_bytes()).hexdigest()
for n in [name,'APD-Alpha-Preview-261.pdf']:
    subprocess.run(['gh','release','download',TAG,'--repo',REPO,'--pattern',n,'--dir','published'],check=True)
    assert hashlib.sha256(pathlib.Path('published',n).read_bytes()).hexdigest()==hashlib.sha256((ROOT/n).read_bytes()).hexdigest()
receipt={'source':SOURCE,'run':RUN,'original_workflow_conclusion':primary['conclusion'],'corrected_native_source':NATIVE_SOURCE,'display_run':MATRIX,'small_retry_run':SMALL,'apk_sha256':digest,'published_at':release['published_at'],'download_verified_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'apk_url':next(a['browser_download_url']for a in release['assets']if a['name']==name),'pdf_url':next(a['browser_download_url']for a in release['assets']if a['name']=='APD-Alpha-Preview-261.pdf'),'chat_link_sent':False,'physical_phone_tested':False}
pathlib.Path('publication-result.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2))

