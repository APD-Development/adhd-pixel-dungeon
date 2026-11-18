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
import pathlib,subprocess,sys,tempfile,unittest

class InstrumentationEvidenceTest(unittest.TestCase):
    def run_check(self,text,expected_class=None):
        with tempfile.TemporaryDirectory() as folder:
            path=pathlib.Path(folder)/"result.txt";path.write_text(text)
            return subprocess.run([sys.executable,str(pathlib.Path(__file__).with_name("apd_check_single_instrumentation.py")),str(path),"actualCase"]+([] if expected_class is None else [expected_class]),capture_output=True).returncode
    def valid(self):return "INSTRUMENTATION_STATUS: class=com.shatteredpixel.shatteredpixeldungeon.android.TrapDiscoveryDeviceTest\nINSTRUMENTATION_STATUS: test=actualCase\nINSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_STATUS: test=actualCase\nINSTRUMENTATION_STATUS_CODE: 0\nOK (1 test)\nINSTRUMENTATION_CODE: -1\n"
    def test_explicit_freeze_class_passes_without_accepting_wrong_or_skipped_output(self):
        name="com.shatteredpixel.shatteredpixeldungeon.android.RemovedActorStallDeviceTest"
        text=self.valid().replace("TrapDiscoveryDeviceTest","RemovedActorStallDeviceTest")
        self.assertEqual(0,self.run_check(text,name))
        self.assertNotEqual(0,self.run_check(self.valid(),name))
        self.assertNotEqual(0,self.run_check(text+"AssumptionFailure",name))
        self.assertNotEqual(0,self.run_check(text.replace("INSTRUMENTATION_STATUS_CODE: 0","INSTRUMENTATION_STATUS_CODE: -2"),name))
    def test_expected_class_is_literal_and_must_be_a_java_class_name(self):
        self.assertNotEqual(0,self.run_check(self.valid(),".*"))
    def test_accepts_only_actual_complete_single_case(self):self.assertEqual(0,self.run_check(self.valid()))
    def test_rejects_incomplete_failed_or_skipped_runs(self):
        for text in ["OK (1 test)",self.valid().replace("INSTRUMENTATION_STATUS_CODE: 0","INSTRUMENTATION_STATUS_CODE: -2"),self.valid()+"AssumptionFailure"]:
            self.assertNotEqual(0,self.run_check(text))
    def test_rejects_duplicate_terminal_or_wrong_case(self):
        self.assertNotEqual(0,self.run_check(self.valid()+"INSTRUMENTATION_STATUS_CODE: 0\n"));self.assertNotEqual(0,self.run_check(self.valid().replace("actualCase","differentCase")));self.assertNotEqual(0,self.run_check(self.valid().replace("TrapDiscoveryDeviceTest","WrongClass")))

if __name__=="__main__":unittest.main()
