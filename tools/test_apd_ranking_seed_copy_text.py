#!/usr/bin/env python3
# ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check the APD seed-copy dialog without changing the original seed/replay rules."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MESSAGES = ROOT / "core/src/main/assets/messages/windows/windows.properties"
KEY = "windows.wndranking$statstab.copy_seed_desc"


class RankingSeedCopyTextTest(unittest.TestCase):
    def test_apd_seed_copy_dialog_is_specific_and_preserves_rules(self):
        lines = MESSAGES.read_text(encoding="utf-8").splitlines()
        matching = [line.split("=", 1)[1] for line in lines if line.startswith(KEY + "=")]
        self.assertEqual(len(matching), 1, "The English seed dialog must have one key")
        body = matching[0].replace(r"\n", "\n")
        self.assertIn("ADHD Pixel Dungeon", body)
        self.assertNotIn("Shattered Pixel Dungeon", body)
        for phrase in (
            "local run history", "previous attempts", "deepest recorded floor",
            "unknown imported seed", "cannot earn badges", "Games Played",
            "bottom of Rankings", "APD versions", "scoring rules are unchanged",
        ):
            with self.subTest(phrase=phrase):
                self.assertIn(phrase, body)
        self.assertNotIn("penalty", body.lower())
        self.assertNotIn("multiplier", body.lower())
        self.assertNotIn("new scoring", body.lower())

    def test_seed_copy_action_remains_original_selection_and_history_notice(self):
        window = (ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndRanking.java").read_text(encoding="utf-8")
        self.assertIn('Messages.get(WndRanking.StatsTab.this, "copy_seed_desc")', window)
        self.assertIn('SPDSettings.customSeed(DungeonSeed.convertToCode(Dungeon.seed))', window)
        self.assertIn('SeedHistoryNotice.selected(SPDSettings.customSeed())', window)


if __name__ == "__main__":
    unittest.main()
