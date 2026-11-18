import importlib.util,pathlib,tempfile,unittest
spec=importlib.util.spec_from_file_location('history',pathlib.Path(__file__).with_name('apd_build_history.py'));history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
class BuildHistoryTests(unittest.TestCase):
 def ledger(self):return {'date':'2026-10-11','previous_label':'TEST50','changes':[{'category':'Bug Fixes','sprite':'V061_BUGFIX','title':'Save protection','text':'Failed writes preserve existing saves.','paths':['core/src/main/java/Test.java']}]}
 def test_item_sprite_names_cannot_be_used_as_changelog_icons(self):
  ledger=self.ledger();ledger['changes'][0]['sprite']='THROWING_KNIFE'
  with self.assertRaisesRegex(ValueError,'Unknown changelog icon'):
   history.validate({'status':'ahead','files':[{'filename':'core/src/main/java/Test.java'}]},ledger)
 def test_native_item_sprite_prefix_is_accepted(self):
  ledger=self.ledger();ledger['changes'][0]['sprite']='ITEM_THROWING_KNIFE'
  history.validate({'status':'ahead','files':[{'filename':'core/src/main/java/Test.java'}]},ledger)
 def test_missing_application_change_rejected(self):
  with self.assertRaises(ValueError):history.validate({'status':'ahead','files':[{'filename':'android/src/main/AndroidManifest.xml'}]},self.ledger())
 def test_reviewed_changes_embedded_and_duplicate_build_replaced(self):
  ledger=self.ledger();history.validate({'status':'ahead','files':[{'filename':'core/src/main/java/Test.java'}]},ledger)
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=root/'core/src/main/assets/apd/history';assets.mkdir(parents=True);(assets/'index.tsv').write_text('build-1\tBuild 1\tUnknown\tIncomplete\n')
   history.embed(root,ledger,100);history.embed(root,ledger,100)
   self.assertEqual(1,(assets/'index.tsv').read_text().count('codex-test-100\t'))
   self.assertIn('Failed writes preserve existing saves.',(assets/'codex-test-100.tsv').read_text())
   self.assertNotIn('Changes since',(assets/'index.tsv').read_text())
 def test_truncated_comparison_rejected(self):
  with self.assertRaises(ValueError):history.validate({'status':'ahead','files':[{'filename':'docs/x'}]*300},self.ledger())
 def test_previous_apk_history_survives_and_reviewed_corrections_win(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=root/'core/src/main/assets/apd/history';assets.mkdir(parents=True)
   (assets/'index.tsv').write_text('codex-test-63\tCodex TEST 63\t2026-10-12\tCorrected attribution\n')
   (assets/'codex-test-63.tsv').write_text('Corrected exact build notes.\n')
   apk=root/'old.apk'
   with zipfile.ZipFile(apk,'w') as z:
    z.writestr('assets/apd/history/index.tsv','codex-test-64\tCodex TEST 64\t2026-10-12\tAPK change record\ncodex-test-63\tCodex TEST 63\t2026-10-12\tOld attribution\n')
    z.writestr('assets/apd/history/codex-test-64.tsv','Verified older notes.\n')
    z.writestr('assets/apd/history/codex-test-63.tsv','Outdated cumulative notes.\n')
   history.merge_previous_apk(root,apk,'Codex TEST 64')
   self.assertEqual('Verified older notes.\n',(assets/'codex-test-64.tsv').read_text())
   self.assertEqual('Corrected exact build notes.\n',(assets/'codex-test-63.tsv').read_text())
   history.merge_previous_apk(root,apk,'Codex TEST 64')
   self.assertEqual(1,(assets/'index.tsv').read_text().count('codex-test-64\t'))
 def test_wrong_previous_build_rejected(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);apk=root/'old.apk'
   with zipfile.ZipFile(apk,'w') as z:z.writestr('assets/apd/history/index.tsv','codex-test-63\tCodex TEST 63\t2026-10-12\tAPK change record\n')
   with self.assertRaises(ValueError):history.merge_previous_apk(root,apk,'Codex TEST 64')
class CompleteCoverageTests(unittest.TestCase):
 def row(self,path):return {'category':'Testing','sprite':'ICON_CHECKED','title':'Regression coverage','text':'Added regression coverage.','paths':[path]}
 def test_tests_tools_documents_and_workflows_cannot_be_omitted(self):
  for path in ['core/src/test/java/NewTest.java','android/src/androidTest/java/NewTest.java','tools/check.py','docs/notes.md','.github/workflows/check.yml','core/src/main/assets/image.png']:
   with self.assertRaises(ValueError):history.validate({'status':'ahead','files':[{'filename':path}]},{'changes':[]})
   self.assertEqual({path},history.validate({'status':'ahead','files':[{'filename':path}]},{'changes':[self.row(path)]}))
 def test_complete_large_diff_has_no_artificial_entry_limit(self):
  paths=['docs/file'+str(i) for i in range(450)]
  delta={'status':'ahead','files_complete':True,'files':[{'filename':p} for p in paths]}
  row=self.row(paths[0]);row['paths']=paths
  self.assertEqual(set(paths),history.validate(delta,{'changes':[row]}))
 def test_duplicate_and_tabbed_descriptions_are_rejected(self):
  row=self.row('x')
  with self.assertRaises(ValueError):history.validate({'status':'ahead','files':[{'filename':'x'}]},{'changes':[row,row]})
  row['text']='not\ta valid popup'
  with self.assertRaises(ValueError):history.validate({'status':'ahead','files':[{'filename':'x'}]},{'changes':[row]})
 def test_compiled_history_must_match_every_reviewed_file(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=root/'core/src/main/assets/apd/history';assets.mkdir(parents=True)
   (assets/'index.tsv').write_text('reviewed index');(assets/'build-37.tsv').write_text('all reviewed changes')
   good=root/'good.apk'
   with zipfile.ZipFile(good,'w') as z:
    for p in assets.iterdir():z.writestr('assets/apd/history/'+p.name,p.read_bytes())
   history.verify_compiled(root,good)
   bad=root/'bad.apk'
   with zipfile.ZipFile(bad,'w') as z:z.writestr('assets/apd/history/index.tsv','reviewed index');z.writestr('assets/apd/history/build-37.tsv','missing most changes')
   with self.assertRaises(ValueError):history.verify_compiled(root,bad)
class HistoricalAliasTests(unittest.TestCase):
 def test_old_private_alias_does_not_restore_a_missing_navigation_target(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=root/'core/src/main/assets/apd/history';assets.mkdir(parents=True)
   (assets/'index.tsv').write_text('private-test-3\tPrivate TEST 3\t2026-10-08\tRecovered history\n')
   (assets/'private-test-3.tsv').write_text('Recovered exact changes.\n')
   apk=root/'old.apk'
   with zipfile.ZipFile(apk,'w') as z:
    z.writestr('assets/apd/history/index.tsv','codex-test-67\tCodex TEST 67\t2026-10-12\tHistory\ntest-3\tPrivate TEST 3\t2026-10-08\tMissing placeholder\n')
    z.writestr('assets/apd/history/codex-test-67.tsv','Verified build67 notes.\n')
   history.merge_previous_apk(root,apk,'Codex TEST 67')
   self.assertNotIn('\ntest-3\t',(assets/'index.tsv').read_text())
   self.assertIn('private-test-3\t',(assets/'index.tsv').read_text())
   self.assertTrue((assets/'codex-test-67.tsv').is_file())
 def test_unresolved_missing_history_fails_before_android(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);apk=root/'old.apk'
   with zipfile.ZipFile(apk,'w') as z:z.writestr('assets/apd/history/index.tsv','codex-test-67\tCodex TEST 67\t2026-10-12\tMissing record\n')
   with self.assertRaisesRegex(ValueError,'missing record'):history.merge_previous_apk(root,apk,'Codex TEST 67')
class ChronologicalHistoryTests(unittest.TestCase):
 def test_imported_builds_do_not_jump_a_newer_failed_number(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=root/'core/src/main/assets/apd/history';assets.mkdir(parents=True)
   (assets/'index.tsv').write_text('codex-test-71\tCodex TEST 71\t2026-10-12\tNo retained APK\nbuild-1\tBuild 1\t2026-10-04\tHistory\n')
   (assets/'codex-test-71.tsv').write_text('Failed before assembly\n');(assets/'build-1.tsv').write_text('Historical record\n')
   apk=root/'previous.apk'
   with zipfile.ZipFile(apk,'w') as z:
    z.writestr('assets/apd/history/index.tsv','codex-test-72\tCodex TEST 72\t2026-10-12\tHistory\ncodex-test-70\tCodex TEST 70\t2026-10-12\tHistory\n')
    z.writestr('assets/apd/history/codex-test-72.tsv','Build72 changes\n');z.writestr('assets/apd/history/codex-test-70.tsv','Build70 changes\n')
   history.merge_previous_apk(root,apk,'Codex TEST 72')
   ids=[line.split('\t')[0] for line in (assets/'index.tsv').read_text().splitlines()]
   self.assertEqual(['codex-test-72','codex-test-71','codex-test-70','build-1'],ids)
if __name__=='__main__':unittest.main()



class PlayerReleaseNotesTests(unittest.TestCase):
 def fixture(self,root,label='APD Alpha Preview',date='',category='Bug Fixes',text='A specific player-facing fix.'):
  assets=root/'core/src/main/assets/apd/releases';assets.mkdir(parents=True)
  (assets/'index.tsv').write_text('apd-alpha-preview\t'+label+'\t'+date+'\tIn development; not a public release.\n')
  (assets/'apd-alpha-preview.tsv').write_text(category+'\tICON_INFO\tA specific fix\t'+text+'\n')
  return assets
 def test_preview_keeps_honest_status_and_does_not_rewrite_internal_records(self):
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);self.fixture(root)
   old=root/'core/src/main/assets/apd/history';old.mkdir(parents=True);record=old/'codex-test-222.tsv';record.write_text('Original complete internal record.\n')
   history.validate_player_notes(root)
   self.assertEqual('Original complete internal record.\n',record.read_text())
 def test_tool_branded_release_is_rejected(self):
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);self.fixture(root,label='APD Codex TEST 222')
   with self.assertRaises(ValueError):history.validate_player_notes(root)
 def test_internal_categories_are_rejected(self):
  for category in ('Testing','Development','Changes'):
   with tempfile.TemporaryDirectory() as folder:
    root=pathlib.Path(folder);self.fixture(root,category=category)
    with self.assertRaises(ValueError):history.validate_player_notes(root)
 def test_future_date_does_not_turn_preview_into_fake_release(self):
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);self.fixture(root,date='2026-10-16')
   with self.assertRaises(ValueError):history.validate_player_notes(root)
 def test_compiled_player_notes_must_match_reviewed_notes(self):
  import zipfile
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);assets=self.fixture(root);(root/'core/src/main/assets/apd/history').mkdir()
   good=root/'good.apk'
   with zipfile.ZipFile(good,'w') as z:
    for p in assets.iterdir():z.writestr('assets/apd/releases/'+p.name,p.read_bytes())
   history.verify_compiled(root,good)
   bad=root/'bad.apk'
   with zipfile.ZipFile(bad,'w') as z:
    z.writestr('assets/apd/releases/index.tsv',(assets/'index.tsv').read_bytes())
    z.writestr('assets/apd/releases/apd-alpha-preview.tsv','Bug Fixes\tICON_INFO\tMissing changes\tWrong content.\n')
   with self.assertRaises(ValueError):history.verify_compiled(root,bad)
 def test_missing_production_catalogue_is_rejected(self):
  with tempfile.TemporaryDirectory() as folder:
   root=pathlib.Path(folder);scene=root/'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/APDTextScene.java';scene.parent.mkdir(parents=True);scene.write_text('class APDTextScene {}')
   with self.assertRaises(ValueError):history.validate_player_notes(root)
