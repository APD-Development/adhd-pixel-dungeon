package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecordedRuleSettingsTest{
 @Test public void frozenWireSchemaMatchesAllCurrentValuesAndPreservesAllScrollCounts(){for(int count=1;count<=60;count++){CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,count);RecordedRuleSettings record=RecordedRuleSettings.parse(selected.preferenceText());assertEquals(selected.identityText(),record.identityText());assertEquals(selected.isNeutral(),record.isNeutral());for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values())assertEquals(selected.get(slider),record.get(slider.name()));}try{RecordedRuleSettings.KEYS.set(0,"changed");fail();}catch(UnsupportedOperationException expected){}}
 @Test public void missingFutureAndInvalidVersionsNeverAcquireCurrentDefaults(){for(String text:new String[]{"","2,15","1,15","bad"})try{RecordedRuleSettings.parse(text);fail();}catch(IllegalArgumentException expected){}RecordedRuleSettings known=RecordedRuleSettings.parse(CustomDungeonRules.defaults().preferenceText());assertNull(known.scoring(0,"","",""));try{known.scoring(1,"NEUTRAL_IDENTITY","unknown-formula","1.00");fail();}catch(IllegalArgumentException expected){}}
}
