#!/usr/bin/env python3
"""Protect APD Score Breakdown from silently pretending that seed-reuse penalties are applied."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WINDOW = ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndScoreBreakdown.java"
SCORING = ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/apd/difficulty/CustomScorePresentation.java"


class ScoreBreakdownDisclosureTest(unittest.TestCase):
    def test_rankings_display_an_explicit_seed_reuse_line(self):
        source = WINDOW.read_text(encoding="utf-8")
        self.assertIn('statSlot(content,"Seed reuse deduction","-"+num.format(reuse.deduction)', source)
        self.assertIn('SeedReuseScore.restore(recordedGameData)', source)
        self.assertIn('statSlot(content,"Custom-rules adjusted score"', source)
        self.assertIn('statSlot(content,"Final earned score",num.format(reuse.earned)', source)

    def test_existing_custom_score_is_unchanged_and_never_passed_off_as_penalized(self):
        window = WINDOW.read_text(encoding="utf-8")
        scoring = SCORING.read_text(encoding="utf-8")
        self.assertIn('CustomScorePresentation.adjustedIdentityScore(recordedGameData,originalScore)', window)
        self.assertIn('CustomRuleMultipliers.adjusted', scoring) if 'CustomRuleMultipliers.adjusted' in scoring else self.assertIn('scoring.adjustedIdentityScore(original)', scoring)
        self.assertNotIn('SeedHistory.lookup(', window)
        self.assertNotIn('Math.pow(', window)

if __name__ == "__main__":
    unittest.main()
