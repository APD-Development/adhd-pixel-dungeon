"""Publish only the factual daily PDF; no APK build or APK publication."""
import datetime,hashlib,json,os,pathlib,subprocess,sys
from xml.sax.saxutils import escape
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer
from reportlab.lib.styles import getSampleStyleSheet
sys.path.insert(0,'tools')
from apd_existing_build_evidence import application_inputs
repo=os.environ['GITHUB_REPOSITORY'];out=pathlib.Path('report-delivery');out.mkdir(exist_ok=True)
def api(p):return json.loads(subprocess.check_output(['gh','api',p]))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert api('repos/'+repo)['private']
ev=json.loads(pathlib.Path('docs/APD-TEST330-DISPLAY-API36-VERIFICATION-2026-10-29.json').read_text())
source=ev['application']['source_commit'];assert source=='35e1c85fcea93381a4f107cafd4f89f5264af391'
trees=[api(f'repos/{repo}/git/trees/{s}?recursive=1') for s in (source,os.environ['GITHUB_SHA'])]
assert all(not t.get('truncated') for t in trees)
assert application_inputs(trees[0]['tree'])==application_inputs(trees[1]['tree'])
runs={}
for name,rid,expected in [('developer',40800528756,source),('player',40800528519,source),('final_matrix',40895514618,'cf8b24fb33955d925d2266beedc1f91e9ca14af8')]:
 r=api(f'repos/{repo}/actions/runs/{rid}');assert r['head_sha']==expected and r['status']=='completed' and r['conclusion']=='success'
 runs[name]={k:r.get(k) for k in ('id','head_sha','created_at','run_started_at','updated_at','conclusion')}
jobs=api(f'repos/{repo}/actions/runs/40895514618/jobs')['jobs'];assert len(jobs)==6 and all(j['conclusion']=='success' for j in jobs)
fallback=api(f'repos/{repo}/releases/tags/apd-alpha-preview-323');assert not fallback['draft']
assets={a['name']:a for a in fallback['assets']}
assert assets['APD-Developer-323.apk']['state']=='uploaded' and assets['APD-Developer-323.apk']['digest']=='sha256:f599e05202277cc2056255e1e6c8ca915eca68a8849af4959e633b678e515467'
now=datetime.datetime.now(datetime.timezone.utc);assert now>=datetime.datetime(2026,10,29,15,55,tzinfo=datetime.timezone.utc)
paragraphs=[
'APD development report — October 29, 2026 lunch test',
'At the15:57:15Z status review, TEST330 was compiled and accepted by the recorded automated/emulator checks, but had not been published or distributed. Physical-phone confirmation remains pending. A separate APK-delivery task can publish it afterward; consult its timestamped verification receipt for subsequent status. No new application code, APK build or completed feature was produced during this report-delivery task. The report reuses existing evidence rather than repeating builds.',
'Implemented in TEST330, relative to the published TEST329 owner diagnostic: the title background covers the viewport using uniform integer-physical-pixel scaling; menu buttons use the actual24x18 frame rather than stretching extra opaque swatch rows from the24x24 image; backend touches predating the new scene draw cannot activate that scene; orphan releases are rejected; cancelling a scale gesture restores its previous value; title unlock-history validation runs in a coalesced background check; scale changes are recorded in diagnostics.',
'Root causes: the actual phone report recorded a6793ms title rebuild followed by backend taps up to4949ms old reaching the new menu and opening Rankings. A synchronous full statistics-catalog read was found in title annotation construction. Its exact share of the stall, versus fonts or graphics, was not measured. The latest recorded process exit was user-requested; memory overflow has not been established. The visible menu strips came from opaque rows21–23 below the button frame, separated by transparent rows18–20. The fixed-size title plate also exposed margins at lower UI scale.',
'Application source: '+source+'. Developer build/regression run40800528756; paired Player40800528519. Retained core XML:2192 methods/422 suites, zero failures/errors/skips per variant. Main API34 Android:65 of65 passed, including actual delayed native tap rejection/fresh-tap operation, bug-report export and lifecycle tests. The synthetic72-run UIMSD benchmark measured225ms then114ms against the unchanged250ms limit; these are emulator timings, not new real-player phone measurements.',
'Final display acceptance: run40895514618 at instrumentation sourcecf8b24fb33955d925d2266beedc1f91e9ca14af8 passed all six configurations:1080x2408@420,360x560@160,320x480@160,720x720@320,960x320@160 on API34, and1216x2688@442 on API36. API36 passed three methods, including actual7x-to6x scale selection/cancellation and stale-tap/fresh-tap behavior. The final matrix retained26 valid screenshots; the short-landscape settings and API36 6x captures were visually reviewed. The application APK was reused unchanged.',
'Retained unsuccessful checks: earlier layout-source fingerprint checks failed before exact reviewed-patch normalization and original whitespace were restored. Initial Android16 emulator attempts failed before APD launch because the image required7372.80MB of userdata space with only about6787MB available; requesting4096MB did not override its minimum. Later test-only work freed disposable CI files. Additional instrumentation compile/producer-lookup attempts and native title/footer/settings timeouts remain recorded. The final short-landscape issue was a fixture coordinate mismatch: nativeY154 arrived at GDX Y136 because the Android content origin was18px lower. Commits6c7b7144932385eb1b6d8bf46f19026e76beb336 andcf8b24fb33955d925d2266beedc1f91e9ca14af8 corrected instrumentation/setup while retaining strict delivered DOWN/UP and exact-target assertions. Final acceptance supersedes those failures without deleting their evidence.',
'Candidate artifact: TEST330 Developer APK SHA2563e38b1164169e404c6606d2baa6d23f61fdb4e1625bb1be53c1424ba41c20842, original application artifact12865493153 in run40800528756. The protected application inputs still match the compiled source. Signing/application-ID configuration has not been changed; a fresh APK-delivery task must independently verify the actual candidate certificate, package ID and checksums before publication. This PDF task does not certify those checks as newly performed. No APK is attached to this report release.',
'Existing private fallback: TEST323 Developer asset '+assets['APD-Developer-323.apk']['browser_download_url']+'; SHA256f599e05202277cc2056255e1e6c8ca915eca68a8849af4959e633b678e515467. TEST323 is the last documented verified playable release; TEST329 is the latest published owner diagnostic and retained an unsuccessful performance check. TEST323 predates pending Forwarding shipments: do not downgrade a run containing an undelivered parcel, uninstall, or clear data. A signature match alone does not establish save-format safety. Exact TEST330 remains available in its private Actions artifact if fresh publication is blocked.',
'Potential regressions and limits: input during scene replacement is intentionally discarded; background cover can crop artwork edges on unusual aspect ratios; the unlock annotation may update on a later title rebuild after its asynchronous check. Physical nubia/RedMagic acceptance remains pending. No raw player history, saved run, scoring table,18-slider mechanic or loot-generation probability was changed by this UI correction. Both previously implemented loot scrolls and diagnostic export remain in the candidate.',
'Unfinished approved work: make UIMSD opening faster on the real phone; make selections in regular Settings respond faster; replace the Scroll of Gathering icon. These requests are recorded, not implemented in this delivery task. Icon inspection has begun, but no new artwork or production icon change has been committed. Do not present them as completed features or infer that the synthetic UIMSD timing establishes real-phone performance.',
'Relevant commits: application35e1c85fcea93381a4f107cafd4f89f5264af391; instrumentationcf8b24fb33955d925d2266beedc1f91e9ca14af8; verification handofffc4b4c0bde09ccc5e819719a084b37a44eeb28fa. Current report/source wrapper: '+os.environ['GITHUB_SHA']+'. No main or Claude branch was overwritten.',
'Honest timing: first verified clock check in this delivery task was2026-10-29T15:57:15Z (11:57:15a.m. New York), when today’s report release did not exist. This is already later than the11:55a.m. requirement; no on-time delivery claim is made. Exact prompt invocation time is not exposed here. Report generatedUTC '+now.isoformat()+'. Workflow timestamps: '+json.dumps(runs)+'. These timestamps do not establish continuous AI coding time. Asset publication time is recorded by GitHub separately; sending the verified link in chat is a separate final delivery action.'
]
styles=getSampleStyleSheet();flow=[]
for i,p in enumerate(paragraphs):flow.extend([Paragraph(escape(p),styles['Title' if i==0 else 'BodyText']),Spacer(1,10)])
pdf=out/'APD-Development-2026-10-29.pdf';SimpleDocTemplate(str(pdf)).build(flow)
sidecar=pathlib.Path(str(pdf)+'.sha256');sidecar.write_text(sha(pdf)+'  '+pdf.name+'\n')
status=out/'APD-Report-2026-10-29.json';status.write_text(json.dumps(dict(application_source=source,report_source=os.environ['GITHUB_SHA'],generated_utc=now.isoformat(),first_clock_check_utc='2026-10-29T15:57:15Z',invocation_time='not exposed',workflow=runs,physical_phone_verified=False,apk_published_by_this_task=False,pending=['Gathering icon','UIMSD opening performance','regular Settings selection performance'],report_sha256=sha(pdf),fallback=assets['APD-Developer-323.apk'],save_compatibility_caution='No downgrade with pending Forwarding parcels',link_sent=False),indent=2))
notes=out/'release-notes.txt';notes.write_text('Current factual lunch report. TEST330 source35e1c85:2192 core,65 main Android, six display profiles accepted. Physical phone/signing recheck and owner-requested performance/icon work remain separately stated. This is a late report delivery; no APK build or APK publication.\n')
tag='apd-lunch-report-2026-10-29'
exists=subprocess.run(['gh','release','view',tag,'--repo',repo],capture_output=True)
if exists.returncode==0:raise SystemExit('Report release already exists; preserve and verify its assets separately')
files=[pdf,sidecar,status]
subprocess.run(['gh','release','create',tag,'--repo',repo,'--target',os.environ['GITHUB_SHA'],'--prerelease','--latest=false','--title','APD private lunch development report — October29,2026','--notes-file',str(notes),*map(str,files)],check=True)
release=api(f'repos/{repo}/releases/tags/{tag}');assert release['prerelease'] and not release['draft']
for p in files:
 a,=[a for a in release['assets'] if a['name']==p.name]
 assert a['state']=='uploaded' and a['digest']=='sha256:'+sha(p) and a['size']==p.stat().st_size
 check=out/('download-check-'+p.name)
 with check.open('wb') as f:subprocess.run(['gh','api','-H','Accept: application/octet-stream',f'repos/{repo}/releases/assets/{a["id"]}'],stdout=f,check=True)
 assert sha(check)==sha(p)
 print('VERIFIED_DOWNLOAD',a['browser_download_url'])
(out/'publication.json').write_text(json.dumps(release,indent=2));print('ACTUAL_ASSET_PUBLICATION_UTC',release['published_at'])
