"""Private owner diagnostic delivery with explicit unresolved timing failure. Never builds or replaces assets."""
import datetime, hashlib, json, os, pathlib, re, subprocess, sys, zipfile
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

ROOT = pathlib.Path('owner-delivery')
REPO = os.environ['GITHUB_REPOSITORY']
def cmd(*args): return subprocess.check_output(args, text=True)
def api(path): return json.loads(cmd('gh', 'api', path))
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def corrected_native_evidence(initial, retry, summarize):
    roots=[ET.parse(p).getroot() for p in pathlib.Path(initial).rglob('TEST-*.xml')]
    assert roots
    totals={k:sum(int(r.get(k,0)) for r in roots) for k in ('tests','failures','errors','skipped')}
    cases=[c for r in roots for c in r.iter('testcase')]
    key=lambda c:(c.get('classname'),c.get('name'))
    assert len(cases)==totals['tests']==64 and len({key(c) for c in cases})==64
    assert not totals['errors'] and not totals['skipped']
    failed={key(c) for c in cases if c.find('failure') is not None}
    allowed={('com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest','warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms'),('com.shatteredpixel.shatteredpixeldungeon.android.DiagnosticReportDeviceTest','actualReportButtonSavesDeviceEvidenceAndMenusSurvivePauseResume')}
    assert failed and failed<=allowed and len(failed)==totals['failures']
    raw,=pathlib.Path(retry).rglob('title-instrumentation.txt')
    parts=re.split(r'INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)',raw.read_text())
    started=[];finished={};failure_blocks={}
    for i in range(1,len(parts),2):
        block=parts[i-1];code=int(parts[i])
        cls=re.findall(r'^INSTRUMENTATION_STATUS: class=(.+)$',block,re.M)
        name=re.findall(r'^INSTRUMENTATION_STATUS: test=(.+)$',block,re.M)
        if not cls or not name:continue
        ident=(cls[-1].strip(),name[-1].strip())
        if code==1:started.append(ident)
        else:
            assert code in (0,-2) and ident not in finished
            finished[ident]=code
            if code:failure_blocks[ident]=block
    assert len(started)==len(set(started))==len(finished)==3 and set(started)==set(finished)
    speed=('com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest','warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms')
    report=('com.shatteredpixel.shatteredpixeldungeon.android.DiagnosticReportDeviceTest','actualReportButtonSavesDeviceEvidenceAndMenusSurvivePauseResume')
    warmup=('com.shatteredpixel.shatteredpixeldungeon.android.AndroidBackDeviceTest','homeTextKeepsItsWordsAndFitsOversizedTokens')
    assert set(finished)=={speed,report,warmup} and finished[speed]==-2 and finished[report]==finished[warmup]==0
    timing=re.search(r'exceeded 250ms: (\d+)',failure_blocks[speed]);assert timing and int(timing.group(1))>250
    passed={key(c) for c in cases if c.find('failure') is None}
    passed.update(k for k,v in finished.items() if v==0)
    remaining={key(c) for c in cases}-passed;assert remaining=={speed} and len(passed)==63
    accepted=dict(tests=64,passed=63,failures=1,errors=0,skipped=0,cases=[dict(c.attrib,accepted_result='passed' if key(c) in passed else 'failed') for c in cases])
    provenance=dict(initial_full_run=totals,initial_failed_methods=sorted(failed),exact_apk_retest={'tests':3,'passed':2,'failures':1,'terminal_codes':[{"class":k[0],"method":k[1],"code":v} for k,v in finished.items()]},accepted_unique_methods=64,single_green_full_run=False,known_unresolved={'method':speed,'measured_ms':int(timing.group(1)),'budget_ms':250},fully_verified=False)
    return accepted,provenance

def artifact(run_id, name, folder):
    entries = api(f'repos/{REPO}/actions/runs/{run_id}/artifacts')['artifacts']
    entry, = [a for a in entries if a['name'] == name and not a['expired']]
    archive = ROOT / (folder + '.zip')
    with archive.open('wb') as out:
        subprocess.run(['gh', 'api', f'repos/{REPO}/actions/artifacts/{entry["id"]}/zip'], stdout=out, check=True)
    assert 'sha256:' + digest(archive) == entry['digest'], 'Artifact digest mismatch'
    target = ROOT / folder
    with zipfile.ZipFile(archive) as z:
        assert all(not pathlib.PurePosixPath(n).is_absolute() and '..' not in pathlib.PurePosixPath(n).parts for n in z.namelist())
        z.extractall(target)
    return target, entry

def main():
    sys.path.insert(0, 'tools')
    from apd_existing_build_evidence import application_inputs, certificates, xml_summary
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
    from reportlab.lib.styles import getSampleStyleSheet
    from PIL import Image
    config = json.loads(pathlib.Path('docs/APD-COMBINED-OWNER-DELIVERY.json').read_text())
    source, number = config['source'], config['number']
    assert api('repos/' + REPO)['private'], 'Private repository required'
    ROOT.mkdir(exist_ok=True)
    trees = [api(f'repos/{REPO}/git/trees/{ref}?recursive=1') for ref in (source, os.environ['GITHUB_SHA'])]
    assert all(not t.get('truncated') for t in trees)
    assert application_inputs(trees[0]['tree']) == application_inputs(trees[1]['tree']), 'Application inputs changed after testing'
    run_records = {}
    for key in ('developer', 'player', 'display'):
        run = api(f'repos/{REPO}/actions/runs/{config[key]}')
        assert run['head_sha'] == source and run['status'] == 'completed', (key, run['conclusion'])
        assert run['conclusion']=='success' or (key=='developer' and config.get('native_retest')), (key,run['conclusion'])
        run_records[key] = run
    assert run_records['developer']['run_number'] == number
    jobs = api(f'repos/{REPO}/actions/runs/{config["developer"]}/jobs')['jobs']
    assert any(j['name']=='verify-and-build' and j['conclusion']=='success' for j in jobs)
    if not config.get('native_retest'): assert any(j['name']=='android-smoke-check' and j['conclusion']=='success' for j in jobs)
    profiles = api(f'repos/{REPO}/actions/runs/{config["display"]}/jobs')['jobs']
    assert len(profiles) == 5 and all(j['conclusion'] == 'success' for j in profiles)
    dev_dir, dev_artifact = artifact(config['developer'], 'apd-codex-private-test-apk', 'developer')
    player_dir, player_artifact = artifact(config['player'], 'apd-player-matching-developer', 'player')
    core_dir, _ = artifact(config['developer'], 'apd-codex-core-tests', 'core')
    native_dir, _ = artifact(config['developer'], 'apd-codex-android-checks', 'android')
    summaries = {k: xml_summary(v) for k, v in [('core', core_dir), ('player_core', player_dir)]}
    native_provenance=None
    native_description=f"Main API34 emulator: {config['android_count']} methods, zero failures/errors/skips."
    replacement_images=[]
    if config.get('native_retest'):
        retry_run=api(f'repos/{REPO}/actions/runs/{config["native_retest"]}')
        assert retry_run['head_sha']==config['native_retest_source'] and retry_run['status']=='completed' and retry_run['conclusion']=='failure' and config['diagnostic_candidate'] is True
        retry_tree=api(f'repos/{REPO}/git/trees/{config["native_retest_source"]}?recursive=1')
        assert not retry_tree.get('truncated') and application_inputs(retry_tree['tree'])==application_inputs(trees[0]['tree'])
        retry_dir,_=artifact(config['native_retest'],'apd-existing329-native-retest','native-retest')
        provenance,=retry_dir.rglob('exact329-source.json');proof=json.loads(provenance.read_text())
        assert proof['application_source']==source and proof['apk_sha256']==config['Developer_sha256'] and proof['application_rebuilt'] is False and proof['application_inputs_identical'] is True
        summaries['android'],native_provenance=corrected_native_evidence(native_dir,retry_dir,xml_summary)
        replacement_images=list(retry_dir.rglob('*.png'))
        assert {p.name for p in replacement_images}=={'diagnostic-report-button.png','diagnostic-report-saved.png'}
        run_records['native_retest']=retry_run
        native_description='Initial full API34 run:62 of64 passed. The unchanged signed APK then passed Home text and the complete report-export/pause/resume method; the UIMSD opening test still failed at306ms against250ms. Combined evidence covers63 of64 unique methods passing, with one unresolved performance failure. Neither the full workflow nor the targeted retry is all-green. This owner diagnostic APK is NOT fully verified or approved for tester distribution.'
    else: summaries['android']=xml_summary(native_dir)
    assert summaries['core']['tests'] == summaries['player_core']['tests'] == config['core_count']
    assert summaries['android']['tests'] == config['android_count']
    methods = {c['classname'].rsplit('.',1)[-1] for c in summaries['android']['cases']}
    assert {'DiagnosticReportDeviceTest', 'LootScrollDeviceTest', 'AndroidBackDeviceTest'} <= methods
    images_by_name={p.name:p for p in native_dir.rglob('*.png')}
    for p in replacement_images:images_by_name[p.name]=p
    images=list(images_by_name.values())
    assert len(images) == config['image_count']
    required = {'loot-scroll-gather-description.png','loot-scroll-gather-result.png','loot-scroll-forward-description.png','loot-scroll-pending-after-continue.png','diagnostic-report-button.png','diagnostic-report-saved.png'}
    assert required <= {p.name for p in images}
    for image in images:
        with Image.open(image) as im: im.load(); assert im.width >= 100 and im.height >= 100
    dev, = dev_dir.rglob('*.apk'); player, = player_dir.rglob('*.apk')
    signer = sorted((pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools').glob('*/apksigner'))[-1]
    expected_cert = '067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
    apks = []
    for kind, original in [('Developer', dev), ('Alpha-Testers', player)]:
        checksum, = original.parent.glob('*.apk.sha256')
        assert digest(original) == checksum.read_text().split()[0] == config[kind + '_sha256']
        assert certificates(cmd(str(signer),'verify','--print-certs',str(original))) == [expected_cert]
        badging = cmd(str(signer.parent/'aapt'),'dump','badging',str(original))
        assert re.search(r"package: name='([^']+)'",badging).group(1) == 'com.charltonchris29.adhdpixeldungeon.indev'
        assert re.search(r"versionCode='([^']+)'",badging).group(1) == '920'
        if kind == 'Developer': assert re.search(r"versionName='([^']+)'",badging).group(1) == f'4.0.8-apd-INDEV-bC{number}-TEST'
        target = ROOT / f'APD-{kind}-{number}.apk'; target.write_bytes(original.read_bytes()); apks.append(target)
        pathlib.Path(str(target)+'.sha256').write_text(digest(target)+'  '+target.name+'\n')
    with zipfile.ZipFile(dev) as a, zipfile.ZipFile(player) as b:
        history = lambda z: {n:z.read(n) for n in z.namelist() if n.startswith('assets/apd/history/') and n.endswith('.tsv')}
        assert history(a) == history(b)
        assert 'assets/apd/features.txt' not in b.namelist()
        assert len(history(a)) == config['history_files']
    text = [
        f'APD Alpha Preview {number}: OWNER DIAGNOSTIC CANDIDATE',
        'NOT FULLY VERIFIED. The requested scrolls, reproduced input corrections and diagnostic export are included. All2185 core methods per variant and five display profiles passed. One Android UIMSD speed check remains failed:282ms initially and306ms on retry versus250ms. The report-export method passed on the exact unchanged APK after post-resume fixture synchronization. TEST323 remains the last verified fallback. This private upload is for the owner to investigate the unconfirmed phone freeze, not an approved tester or public release.',
        'Both scrolls start fully identified for all six classes. Gathering collects eligible loose items beside downstairs. Forwarding preserves full item properties outside the next region shop; late use routes to the actual floor-20 Imp shop. Both stop generating after floor19. Provisional names, Teleportation-equivalent weights and six-energy conversion are owner-approved for testing.',
        'Keys, quest items, Lost Backpack, unopened containers and shop stock are excluded. Armed bombs block Forwarding without consuming the scroll. Pending parcels wait for a real shop and safe entrance approach. Journaled compound saves recover process-interrupted transfers; corrupt journals are retained and block overwriting. Sudden power-loss/directory-fsync guarantees have not been established.',
        'Reproduced input causes: static queued taps survived engine recreation; Back following a menu tap could reach the departing title before its requested scene opened. Fixes discard obsolete lifecycle input using cancellation and defer subsequent keys until the requested scene is installed.',
        'A separate injected-allocation regression found that the new diagnostic stack capture could replace the original failure with its own OutOfMemoryError. Capture is now guarded, preserves the original message, and reports when diagnostic detail is unavailable. This synthetic test does not establish the phone failure cause.',
        'The sustained black-screen/ten-second phone freeze is still unconfirmed. There is no evidence proving memory exhaustion. APD Settings > Create bug report exports build/device/APK hash, recent scene/loading/pause/resume/input timestamps, bounded errors/heap observations and self-package Android exit reasons. Reports are saved locally for voluntary sharing and omit saved games and raw player histories.',
        f'Exact application source: {source}. Developer workflow {config["developer"]}; player {config["player"]}; five-display workflow {config["display"]}. Complete core: {config["core_count"]} methods per variant, zero failures/errors/skips. '+native_description+f' {config["image_count"]} decoded and reviewed accepted captures; when using a retest, report images are replaced only by the two exact-APK retry captures. Physical RedMagic testing remains pending.',
        'Retained failures: initial core integration ran2183 methods with3 fixture failures; historical golden generation now explicitly retains its original twelve-scroll pool and eligible-depth conversion setup without rewriting expected hashes or removing assertions. The intermediate correction omitted cached combined defaults and was superseded. Historical supplemental cold-restart/death-order workflows still fail before tests on their pinned TEST314/shallow-checkout setup; those are not passed checks. No claim all workflows are green.',
        'Android preparation failures are retained: TEST327 instrumentation did not compile because a new test used nonexistent surface dimension fields. TEST328 ran64 methods with62 passing; two fixtures compared a cache wrapper against its contained model and failed to activate the report control, and the absent saved-report capture correctly failed image validation. Fixture corrections preserve exact model identity, both250ms budgets, touchscreen activation, file contents and every existing assertion. These failures were not represented as verified releases.',
        'Potential regressions: additional scrolls intentionally alter future random loot pools and derived seed-reference caches; old twelve rune assignments and original item properties are retained. Lifecycle input is discarded across pause/resume; fresh input and Back ordering have regression coverage. Newly added diagnostics are bounded and asynchronous but need real-device observation.',
        'Install one variant over the existing APD test application. They share the same application ID and signing certificate. Never uninstall or clear data. Older builds do not understand pending Forwarding shipments; avoid downgrading a run with undelivered parcels. Alpha-Testers is an asset label within this private owner release, not automatic tester distribution. Existing release assets and fallback TEST323 remain preserved.',
        'Timing: developer workflow started at '+run_records['developer']['run_started_at']+'; its last recorded workflow update is '+run_records['developer']['updated_at']+'. Player workflow started at '+run_records['player']['run_started_at']+'; last update '+run_records['player']['updated_at']+'. These are UTC workflow timestamps, not continuous coding time or proof of chat delivery. Work was interrupted by owner Stop and resumed on request. No claim of uninterrupted background work or exact-time scheduled delivery is made.'
    ]
    evidence = dict(config, summaries=summaries,native_provenance=native_provenance, artifacts={'developer':dev_artifact,'player':player_artifact}, signing_certificate=expected_cert, physical_device_verified=False, phone_freeze_cause_confirmed=False,fully_verified=False,owner_diagnostic_candidate=True,last_verified_fallback='TEST323',
                    report_generated_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                    workflow_times={k:{p:r[p] for p in ('id','created_at','run_started_at','updated_at','conclusion')} for k,r in run_records.items()})
    receipt = ROOT / f'APD-{number}-Verification.json'; receipt.write_text(json.dumps(evidence,indent=2))
    pdf = ROOT / f'APD-Alpha-Preview-{number}.pdf'; styles=getSampleStyleSheet()
    flow=[]
    for i, paragraph in enumerate(text): flow.extend([Paragraph(escape(paragraph),styles['Title' if i==0 else 'BodyText']),Spacer(1,10)])
    SimpleDocTemplate(str(pdf)).build(flow)
    notes = ROOT/'release-notes.txt';notes.write_text('\n\n'.join(text))
    tag=f'apd-alpha-preview-{number}'
    files=apks+[pathlib.Path(str(p)+'.sha256') for p in apks]+[pdf,receipt]
    assert subprocess.run(['gh','release','view',tag,'--repo',REPO],capture_output=True).returncode != 0, 'Existing release preserved; verify/reuse it separately'
    subprocess.run(['gh','release','create',tag,'--repo',REPO,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title',f'APD Alpha Preview {number} — owner diagnostic candidate','--notes-file',str(notes)]+[str(p) for p in files],check=True)
    release=api(f'repos/{REPO}/releases/tags/{tag}')
    assert not release['draft'] and release['prerelease']
    for path in files:
        asset,=[a for a in release['assets'] if a['name']==path.name]
        assert asset['state']=='uploaded' and asset['size']==path.stat().st_size and asset['digest']=='sha256:'+digest(path)
        # Verify the actual private downloadable bytes, not just publication metadata.
        downloaded=ROOT/('download-check-'+path.name)
        with downloaded.open('wb') as out:
            subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{REPO}/releases/assets/{asset["id"]}'],stdout=out,check=True)
        assert digest(downloaded)==digest(path)
        print(asset['browser_download_url'])
    (ROOT/'publication.json').write_text(json.dumps(release,indent=2))
if __name__=='__main__': main()
