"""Regressions for SDK signer output observed during TEST22 delivery."""
import unittest
import tempfile
from pathlib import Path
from apd_existing_build_evidence import application_inputs, certificates, xml_summary


class PublicationSourceTest(unittest.TestCase):
    def test_recursive_application_mapping_ignores_only_verified_test_and_documentation_paths(self):
        before = [{'path':'core','type':'tree','sha':'old-tree'},
                  {'path':'core/src/main/java/Game.java','type':'blob','sha':'app'},
                  {'path':'core/src/test/java/Simulation.java','type':'blob','sha':'old-test'},
                  {'path':'android/src/androidTest/java/DeviceTest.java','type':'blob','sha':'old-device'}]
        after = [{'path':'core','type':'tree','sha':'new-tree'},
                 {'path':'core/src/main/java/Game.java','type':'blob','sha':'app'},
                 {'path':'core/src/test/java/Simulation.java','type':'blob','sha':'new-test'},
                 {'path':'android/src/androidTest/java/DeviceTest.java','type':'blob','sha':'new-device'},
                 {'path':'docs/HANDOFF.md','type':'blob','sha':'new-doc'},
                 {'path':'.github/workflows/verify.yml','type':'blob','sha':'new-workflow'}]
        self.assertEqual(application_inputs(before), application_inputs(after))

    def test_main_assets_and_build_source_remain_protected_in_recursive_mapping(self):
        for path in ('core/src/main/java/Game.java','core/src/main/assets/test/data.json',
                     'android/src/main/AndroidManifest.xml','android/build.gradle','gradle/wrapper/gradle-wrapper.jar'):
            with self.subTest(path=path):
                self.assertNotEqual(application_inputs([{'path':path,'type':'blob','sha':'tested'}]),
                                    application_inputs([{'path':path,'type':'blob','sha':'changed'}]))

    def test_delivery_documentation_does_not_relabel_application_source(self):
        before = [{'path': 'core', 'sha': 'app'}, {'path': 'docs', 'sha': 'old'}]
        after = [{'path': 'core', 'sha': 'app'}, {'path': 'docs', 'sha': 'new'},
                 {'path': '.github', 'sha': 'workflow'}, {'path': 'tools', 'sha': 'delivery'}]
        self.assertEqual(application_inputs(before), application_inputs(after))

    def test_source_android_assets_or_build_configuration_changes_cannot_pass(self):
        for path in ('core', 'android', 'SPD-classes', 'services', 'desktop', 'gradle',
                     'build.gradle', 'settings.gradle', 'gradle.properties'):
            with self.subTest(path=path):
                self.assertNotEqual(application_inputs([{'path': path, 'sha': 'tested'}]),
                                    application_inputs([{'path': path, 'sha': 'changed'}]))


class TestEvidenceTest(unittest.TestCase):
    def evidence(self, xml):
        with tempfile.TemporaryDirectory() as folder:
            Path(folder, 'TEST-example.xml').write_text(xml)
            return xml_summary(folder)

    def test_completed_test_evidence_has_actual_cases(self):
        result = self.evidence('<testsuite tests="1"><testcase classname="Android" name="actualTouch"/></testsuite>')
        self.assertEqual(1, result['tests'])
        self.assertEqual('actualTouch', result['cases'][0]['name'])

    def test_failures_errors_and_skips_refuse_delivery(self):
        for attr in ('failures', 'errors', 'skipped'):
            with self.subTest(attr=attr), self.assertRaises(AssertionError):
                self.evidence(f'<testsuite tests="1" {attr}="1"><testcase classname="C" name="m"/></testsuite>')
        with self.assertRaises(AssertionError):
            self.evidence('<testsuite tests="1"><testcase classname="C" name="m"><failure/></testcase></testsuite>')

    def test_incomplete_and_duplicate_evidence_refuse_delivery(self):
        for xml in ('<testsuite tests="0"/>', '<testsuite tests="1"/>',
                    '<testsuite tests="2"><testcase classname="C" name="m"/><testcase classname="C" name="m"/></testsuite>'):
            with self.subTest(xml=xml), self.assertRaises(AssertionError):
                self.evidence(xml)

    def test_missing_report_refuses_delivery(self):
        with tempfile.TemporaryDirectory() as folder, self.assertRaises(AssertionError):
            xml_summary(folder)


class SignerOutputTest(unittest.TestCase):
    def test_numbered_and_v2_signers_report_the_same_certificate(self):
        digest = '067688b3ab6bd5820a67f4c1ba7d5859db578173771cf31d3252b3e4e012246e'
        for label in ('Signer #1', 'Signer #2:', 'V2 Signer:', 'V3 Signer:'):
            self.assertEqual([digest], certificates(f'{label} certificate SHA-256 digest: {digest}\n'))

    def test_missing_malformed_and_source_stamp_certificates_do_not_pass(self):
        self.assertEqual([], certificates('APK is verified'))
        self.assertEqual([], certificates('V2 Signer: certificate SHA-256 digest: 123\n'))
        self.assertEqual([], certificates('Source Stamp Signer: certificate SHA-256 digest: ' + 'a' * 64))

    def test_distinct_signers_are_preserved_and_cannot_match_a_single_signer(self):
        first = 'V2 Signer: certificate SHA-256 digest: ' + 'a' * 64
        second = 'Signer #2 certificate SHA-256 digest: ' + 'b' * 64
        self.assertNotEqual(certificates(first), certificates(first + '\n' + second))
        self.assertEqual(['a' * 64], certificates(first + '\n' + first))


if __name__ == '__main__':
    unittest.main()
