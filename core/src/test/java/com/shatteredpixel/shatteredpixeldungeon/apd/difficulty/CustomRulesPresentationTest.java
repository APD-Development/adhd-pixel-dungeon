package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import org.junit.Test;
import static org.junit.Assert.*;
public class CustomRulesPresentationTest {
    @Test public void everyPermittedValueHasAnExactReversibleSliderPosition() {
        for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values()) {
            for(int v=slider.min;v<=slider.max;v+=slider.step())
                assertEquals(v,CustomRulesPresentation.value(slider,CustomRulesPresentation.position(slider,v)));
            assertThrows(IllegalArgumentException.class,()->CustomRulesPresentation.value(slider,-1));
            assertThrows(IllegalArgumentException.class,()->CustomRulesPresentation.value(slider,CustomRulesPresentation.positions(slider)+1));
        }
    }
    @Test public void everyWholeScrollCountDisplaysItsCountAndEquivalentPercentage() {
        for(int count=1;count<=60;count++)assertEquals(String.format(java.util.Locale.ROOT,"%d (%.2f%%)",count,count*100.0/15),
            CustomRulesPresentation.valueText(CustomDungeonRules.Slider.UPGRADE_SCROLLS,count));
        assertEquals("1 (6.67%)",CustomRulesPresentation.valueText(CustomDungeonRules.Slider.UPGRADE_SCROLLS,1));
        assertEquals("15 (100.00%)",CustomRulesPresentation.valueText(CustomDungeonRules.Slider.UPGRADE_SCROLLS,15));
        assertEquals("60 (400.00%)",CustomRulesPresentation.valueText(CustomDungeonRules.Slider.UPGRADE_SCROLLS,60));
    }
    @Test public void lockedHistoricalRunShowsItsOwnScoringVersionInsteadOfTodaysScrollPreview() {
        CustomDungeonRules.Slider scrolls=CustomDungeonRules.Slider.UPGRADE_SCROLLS;
        assertEquals("0.5609×",CustomRulesPresentation.multiplierTextForVersion(scrolls,60,CustomRuleMultipliers.PREVIOUS_VERSION));
        assertEquals("0.5703×",CustomRulesPresentation.multiplierTextForVersion(scrolls,59,CustomRuleMultipliers.INTERIM_VERSION));
        assertEquals("0.2000×",CustomRulesPresentation.multiplierTextForVersion(scrolls,60,CustomRuleMultipliers.INTERIM_VERSION));
        assertEquals("0.2172×",CustomRulesPresentation.multiplierTextForVersion(scrolls,59,CustomRuleMultipliers.VERSION));
        assertEquals("0.2000×",CustomRulesPresentation.multiplierTextForVersion(scrolls,60,CustomRuleMultipliers.VERSION));
        assertEquals("1.0000×",CustomRulesPresentation.multiplierTextForVersion(scrolls,15,CustomRuleMultipliers.PREVIOUS_VERSION));
        assertEquals("1.0000×",CustomRulesPresentation.multiplierTextForVersion(scrolls,15,CustomRuleMultipliers.INTERIM_VERSION));
        assertEquals("1.0000×",CustomRulesPresentation.multiplierTextForVersion(scrolls,15,CustomRuleMultipliers.VERSION));
        assertEquals("0.2172×",CustomRulesPresentation.multiplierText(scrolls,59));
        assertEquals("0.2000×",CustomRulesPresentation.multiplierText(scrolls,60));
    }
    @Test public void allEighteenGameplayControlsAreAvailableForNewRuns() {
        assertEquals(18,CustomDungeonRules.Slider.values().length);
        for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values())
            assertTrue(slider.name(),CustomRulesPresentation.gameplayImplemented(slider));
    }
}
