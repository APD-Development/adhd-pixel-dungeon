package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.watabou.utils.Bundle;
import org.junit.Test;
import static org.junit.Assert.*;
public class CustomDungeonRulesTest {
    @Test public void everySliderValueSurvivesBundleRoundTrip() {
        for (CustomDungeonRules.Slider s : CustomDungeonRules.Slider.values()) {
            for (int value=s.min; value<=s.max; value+=s.step()) {
                CustomDungeonRules rules=CustomDungeonRules.defaults().with(s,value);
                Bundle bundle=new Bundle(); rules.store(bundle);
                CustomDungeonRules restored=CustomDungeonRules.restore(bundle);
                assertArrayEquals(rules.values(), restored.values());
                assertEquals(rules.identityText(), restored.identityText());
            }
        }
    }
    @Test public void configurationIsImmutableAndIdentityIncludesAllSettings() {
        CustomDungeonRules neutral=CustomDungeonRules.defaults();
        int[] input=neutral.values(); CustomDungeonRules copy=new CustomDungeonRules(input);
        input[0]=500; assertEquals(100,copy.get(CustomDungeonRules.Slider.ENEMY_HP));
        assertTrue(neutral.isNeutral());
        for (CustomDungeonRules.Slider s : CustomDungeonRules.Slider.values()) {
            CustomDungeonRules modified=neutral.with(s,s.min);
            assertFalse(modified.isNeutral());
            assertNotEquals(neutral.identityText(),modified.identityText());
            assertEquals(s.defaultValue(),neutral.get(s));
        }
    }
    @Test public void missingHistoricalRulesStayUnknownAndFutureVersionsFail() {
        assertNull(CustomDungeonRules.restore(new Bundle()));
        Bundle future=new Bundle(); future.put("custom_rules_version",2);
        try { CustomDungeonRules.restore(future); fail(); } catch (IllegalArgumentException expected) { }
        Bundle incomplete=new Bundle(); incomplete.put("custom_rules_version",1);
        try { CustomDungeonRules.restore(incomplete); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void scrollCountPercentageAndBounds() {
        for (int n=1; n<=60; n++) {
            CustomDungeonRules r=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,n);
            assertEquals(n*100.0/15,r.percentage(CustomDungeonRules.Slider.UPGRADE_SCROLLS),0);
        }
        for (CustomDungeonRules.Slider s : CustomDungeonRules.Slider.values()) {
            assertFalse(s.valid(s.min-1)); assertFalse(s.valid(s.max+1));
        }
    }
}
