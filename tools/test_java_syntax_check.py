"""Cheap native-source parsing catches syntax errors without an Android SDK."""
from pathlib import Path
import subprocess
import tempfile
import unittest

CHECKER = Path(__file__).with_name("JavaSyntaxCheck.java")

class JavaSyntaxCheckTest(unittest.TestCase):
    def parse(self, text):
        with tempfile.TemporaryDirectory(prefix="apd-java-syntax-") as folder:
            source = Path(folder) / "Fixture.java"
            source.write_text(text, encoding="utf-8")
            return subprocess.run(["java", str(CHECKER), str(source)],
                capture_output=True, text=True, timeout=30)

    def test_parsing_does_not_require_android_or_gdx_types(self):
        result = self.parse("import missing.android.Type; class Fixture { Type unknown; }")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("types/API compatibility not checked", result.stdout)

    def test_multiple_character_literal_is_rejected_before_a_remote_build(self):
        result = self.parse("class Fixture { char invalid = 'bad'; }")
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("syntax OK", result.stdout)

    def test_newer_source_syntax_is_not_accepted_as_java_11(self):
        result = self.parse("record Fixture(int value) {}")
        self.assertNotEqual(0, result.returncode)

if __name__ == "__main__":
    unittest.main()
