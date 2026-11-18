package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
public class CustomScoreRecordTest {
    @Test public void neutralIdentityIsExactAndDoesNotReapplyChallenges() {
        CustomScoreRecord r=CustomScoreRecord.preserve(CustomDungeonRules.defaults(),523818);
        assertEquals(1.0,r.multiplier(),0);
        assertEquals(Integer.valueOf(523818),r.adjustedScore());
        assertEquals(523818,r.earnedScore());
    }
    @Test public void everyNonNeutralSliderPreservesEarnedScoreWithoutInventedReward() {
        for (CustomDungeonRules.Slider s : CustomDungeonRules.Slider.values()) {
            CustomScoreRecord r=CustomScoreRecord.preserve(CustomDungeonRules.defaults().with(s,s.min),523818);
            assertNull(r.multiplier()); assertNull(r.adjustedScore());
            assertEquals(523818,r.earnedScore());
            assertEquals(CustomScoreRecord.Status.UNRATED_PENDING_CALIBRATION,r.status);
            Bundle b=new Bundle(); r.store(b); CustomScoreRecord restored=CustomScoreRecord.restore(b);
            assertEquals(r.rules.identityText(),restored.rules.identityText());
            assertEquals(r.status,restored.status); assertEquals(r.originalScore,restored.originalScore);
        }
    }
    @Test public void historicalUnknownIsNotCertifiedNeutral() {
        CustomScoreRecord r=CustomScoreRecord.preserve(null,523818);
        Bundle b=new Bundle(); CustomScoreRecord.preserve(CustomDungeonRules.defaults(),1).store(b);
        r.store(b); r=CustomScoreRecord.restore(b);
        assertEquals(CustomScoreRecord.Status.HISTORICAL_SETTINGS_UNKNOWN,r.status);
        assertNull(r.rules); assertNull(r.multiplier()); assertNull(r.adjustedScore());
        assertEquals(523818,r.earnedScore());
    }
}
