import importlib.util
import pathlib
import sys
import unittest


MODULE_PATH = pathlib.Path(__file__).with_name("apd_documentation_checkpoint_guard.py")
SPEC = importlib.util.spec_from_file_location("apd_documentation_checkpoint_guard", MODULE_PATH)
guard = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = guard
SPEC.loader.exec_module(guard)


SOURCE = "a" * 40
DEV = "b" * 64
PLAYER = "c" * 64


def handoff(ordinal=341, source=SOURCE, developer=DEV, player=PLAYER):
    return f"""## 2026-10-30T00:25Z — VERIFIED private APK{ordinal} pair published and re-downloaded; deadline MISSED
Application source {source}; producing run1/job2.
Developer: https://example.invalid/dev.apk SHA256 {developer}
Alpha-Testers: https://example.invalid/player.apk SHA256 {player}

## older entry
Application source {'d' * 40}
"""


def ledger(ordinal=341, source=SOURCE, developer=DEV, player=PLAYER):
    return f"""## completed TEST{ordinal} publication
Application source {source}.
Developer SHA256 {developer}.
Alpha-Testers SHA256 {player}.

## older checkpoint
details
"""


class DocumentationCheckpointGuardTest(unittest.TestCase):
    def test_exact_latest_checkpoint_passes(self):
        result = guard.verify_completed_ledger(handoff(), ledger())
        self.assertEqual(341, result.ordinal)
        self.assertEqual(SOURCE, result.application_source)

    def test_missing_latest_checkpoint_fails(self):
        with self.assertRaisesRegex(guard.CheckpointError, "lacks published TEST341"):
            guard.verify_completed_ledger(handoff(), ledger(338))

    def test_wrong_hash_in_matching_section_fails(self):
        with self.assertRaisesRegex(guard.CheckpointError, "Developer SHA256"):
            guard.verify_completed_ledger(handoff(), ledger(developer="d" * 64))

    def test_token_in_another_section_cannot_supply_evidence(self):
        mixed = "## TEST341 mentioned only\nno evidence\n\n" + ledger(338)
        with self.assertRaisesRegex(guard.CheckpointError, "application source"):
            guard.verify_completed_ledger(handoff(), mixed)

    def test_malformed_handoff_fails_closed(self):
        with self.assertRaisesRegex(guard.CheckpointError, "Alpha-Testers SHA256"):
            guard.latest_published_checkpoint(
                handoff().replace(f"Alpha-Testers: https://example.invalid/player.apk SHA256 {PLAYER}", "")
            )

    def test_first_published_header_is_authoritative(self):
        combined = handoff(341) + handoff(330, source="d" * 40)
        result = guard.verify_completed_ledger(combined, ledger(341))
        self.assertEqual(341, result.ordinal)


if __name__ == "__main__":
    unittest.main()
