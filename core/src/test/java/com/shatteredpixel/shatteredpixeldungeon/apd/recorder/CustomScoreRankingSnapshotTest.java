package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.*;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomScoreRankingSnapshotTest{
 @Test public void realRankingSnapshotRetainsRunRulesAndOriginalScoreNotChangedPreferences()throws Exception{
  HeadlessTestSupport.start();CustomDungeonRules previous=SPDSettings.apdCustomRules();
  try{
   CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,60);SPDSettings.apdCustomRules(selected);RecorderTestSupport.Session session=new RecorderTestSupport.Session();RecorderTestSupport.newRun(session,182993);RecorderTestSupport.enterLevel(1);ApdRecorder.onRunEnded(false,new Object());Statistics.totalScore=12345;
   SPDSettings.apdCustomRules(CustomDungeonRules.defaults());Rankings.Record record=new Rankings.Record();record.score=12345;Rankings.INSTANCE.saveGameData(record);assertEquals(selected.preferenceText(),CustomDungeonRules.restore(record.gameData.getBundle("apd_custom_rules")).preferenceText());assertEquals(CustomRuleMultipliers.format(CustomRuleMultipliers.combined(selected)),CustomScorePresentation.multiplier(record.gameData));assertEquals(CustomRuleMultipliers.adjusted(12345,CustomRuleMultipliers.combined(selected)),record.earnedScore());assertEquals(12345,record.score);assertEquals(12345,Statistics.totalScore);
   assertEquals("?",CustomScorePresentation.multiplier(new com.watabou.utils.Bundle()));
  }finally{ApdRecorder.reset();SPDSettings.apdCustomRules(previous);}
 }
}
