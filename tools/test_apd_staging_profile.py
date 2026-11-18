import copy, json, os, pathlib, tempfile, unittest, textwrap
from unittest.mock import patch
workflow = (pathlib.Path(__file__).resolve().parents[1]/'.github/workflows/codex-alpha-preview-stage.yml').read_text()
resolver = workflow.split("      - name: Resolve this commit's successful explicit build and preparation profile",1)[1]
code = textwrap.dedent(resolver.split("          python3 - <<'PY'\n",1)[1].split('\n          PY',1)[0])

class ResolverChecks(unittest.TestCase):
    def run_case(self, mode):
        source = 'a' * 40
        names = ['apd-codex-core-tests', 'apd-codex-private-test-apk', 'apd-codex-android-checks']
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory); (root/'docs').mkdir(); (root/'docs/example.md').write_text('verified changes')
            config = {'core_tests':1691, 'android_tests':33, 'screenshots':42, 'rollback_number':226,
                      'rollback_sha256':'b'*64, 'rollback_tag':'apd-alpha-preview-226',
                      'rollback_asset':'APD226.apk', 'reports':['docs/example.md']}
            if mode == 'pinned_success': config['source_sha']=source
            if mode == 'bad_source': config['source_sha']='invalid'
            if mode == 'unsafe_path': config['reports'] = ['../outside.md']
            if mode == 'boolean_count': config['core_tests'] = True
            (root/'docs/APD-ALPHA-PREPARATION.json').write_text(json.dumps(config))
            envfile = root/'exports'
            def response(args):
                path = args[-1]
                if path == 'repos/test/private': return json.dumps({'private':mode!='public'}).encode()
                if path.endswith('/actions/workflows/codex-sprint-check.yml'): return b'{"id":17}'
                if '/actions/runs?' in path:
                    run = {'head_sha':source, 'workflow_id':17, 'head_branch':'codex/apd-phone-fixes-2026-10-09',
                           'status':'completed', 'conclusion':'failure' if mode=='failed' else 'success', 'id':51, 'run_number':229}
                    if mode=='stale_source': run['head_sha']='c'*40
                    return json.dumps({'workflow_runs':[run,copy.deepcopy(run)] if mode=='ambiguous' else [run]}).encode()
                if path.endswith('/actions/runs/51/artifacts'):
                    return json.dumps({'artifacts':[{'name':n,'id':i+1,'expired':False,
                        'workflow_run':{'head_sha':'c'*40 if mode=='wrong_artifact' else source}} for i,n in enumerate(names)]}).encode()
                raise AssertionError(path)
            previous = pathlib.Path.cwd()
            try:
                os.chdir(root)
                with patch.dict(os.environ, {'GITHUB_REPOSITORY':'test/private','GITHUB_SHA':('d'*40 if mode=='pinned_success' else source),'GITHUB_ENV':str(envfile)}), \
                     patch('subprocess.check_output', side_effect=response), \
                     patch('time.monotonic', side_effect=[0,2000]), \
                     patch('time.sleep', side_effect=AssertionError('Unexpected wait')):
                    if mode in ('success','pinned_success'):
                        exec(compile(code,'stage-resolver','exec'),{})
                        values=dict(line.split('=',1) for line in envfile.read_text().splitlines())
                        self.assertEqual(source,values['APD_SOURCE_SHA']);self.assertEqual('51',values['APD_SOURCE_RUN']);self.assertEqual('229',values['APD_TEST_NUMBER'])
                        self.assertEqual('1691',values['APD_CORE_TEST_COUNT'])
                        self.assertEqual(3,len(json.loads(values['APD_SOURCE_ARTIFACTS_JSON'])))
                    else:
                        with self.assertRaises((AssertionError,SystemExit)):
                            exec(compile(code,'stage-resolver','exec'),{})
                        self.assertFalse(envfile.exists(), 'Invalid evidence must not be exported')
            finally: os.chdir(previous)
    def test_pinned_source_is_preserved(self): self.run_case('pinned_success')
    def test_invalid_source_is_rejected(self): self.run_case('bad_source')
    def test_exact_success_exports_verified_identity(self): self.run_case('success')
    def test_stale_source_cannot_be_staged(self): self.run_case('stale_source')
    def test_failed_verification_cannot_be_staged(self): self.run_case('failed')
    def test_ambiguous_source_runs_are_rejected(self): self.run_case('ambiguous')
    def test_wrong_source_artifacts_are_rejected(self): self.run_case('wrong_artifact')
    def test_report_paths_cannot_escape_docs(self): self.run_case('unsafe_path')
    def test_public_repository_is_rejected(self): self.run_case('public')
    def test_boolean_test_counts_are_rejected(self): self.run_case('boolean_count')

if __name__=='__main__': unittest.main()
