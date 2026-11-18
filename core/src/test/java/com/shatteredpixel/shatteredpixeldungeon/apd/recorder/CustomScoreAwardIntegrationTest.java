package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
public class CustomScoreAwardIntegrationTest{
 private CustomDungeonRules saved;
 @Before public void before(){HeadlessTestSupport.start();saved=SPDSettings.apdCustomRules();}
 @After public void after(){ApdRecorder.reset();SPDSettings.apdCustomRules(saved);}
 @Test public void realSaveReloadRecordsOriginalAndFinalScoreUsingLockedRules()throws Exception{
  CustomDungeonRules rules=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500).with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,1);
  SPDSettings.apdCustomRules(rules);RecorderTestSupport.Session session=new RecorderTestSupport.Session();RecorderTestSupport.newRun(session,814722);String id=ApdRecorder.currentRunId();RecorderTestSupport.enterLevel(1);Dungeon.saveAll();
  SPDSettings.apdCustomRules(CustomDungeonRules.defaults());session.restartApp();Dungeon.loadGame(session.slot);Dungeon.level=Dungeon.loadLevel(session.slot);Dungeon.switchLevel(Dungeon.level,Dungeon.hero.pos);
  assertEquals(rules.preferenceText(),Dungeon.customRules().preferenceText());Statistics.totalScore=123456;
  long earned=CustomRuleMultipliers.adjusted(123456,CustomRuleMultipliers.combined(rules));
  Rankings.Record ranking=new Rankings.Record();ranking.score=123456;Rankings.INSTANCE.saveGameData(ranking);
  assertEquals(earned,ranking.earnedScore());assertEquals(earned,ranking.gameData.getLong("apd_custom_adjusted_score"));assertEquals(123456,ranking.gameData.getLong("apd_custom_original_score"));
  ApdRecorder.onRunEnded(false,new Object());RunFacts facts=RunFactsBuilder.fromLog(session.read(id),id);
  assertEquals(123456,facts.numOr(CustomRuleFacts.ORIGINAL_SCORE,-1));assertEquals(earned,facts.numOr(CustomRuleFacts.ADJUSTED_SCORE,-1));assertEquals(earned,facts.numOr(FactKeys.SCORE,-1));assertEquals(CustomRuleMultipliers.VERSION,facts.textOr(CustomRuleFacts.CALCULATION_VERSION,""));
 }
 @Test public void rankingsSortByAdjustedLongScoreWithoutMutatingOriginalRecords(){
  Rankings.Record harder=new Rankings.Record(),neutral=new Rankings.Record();harder.customSeed=neutral.customSeed="";harder.gameID="harder";neutral.gameID="neutral";harder.score=100000;neutral.score=150000;
  CustomDungeonRules rules=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500);Bundle settings=new Bundle();rules.store(settings);harder.gameData=new Bundle();harder.gameData.put("apd_custom_rules",settings);CustomScoreSnapshot.forNewRun(rules).store(harder.gameData);
  assertTrue(Rankings.scoreComparator.compare(harder,neutral)<0);assertEquals(100000,harder.score);assertEquals(150000,neutral.score);assertEquals(150000,neutral.earnedScore());
 }
 @Test public void originalScoringMathAndGameplayRngAreNotConsumedByDraftPreviews(){
  com.watabou.utils.Random.pushGenerator(883814);long expected;try{expected=com.watabou.utils.Random.Long();}finally{com.watabou.utils.Random.popGenerator();}
  com.watabou.utils.Random.pushGenerator(883814);try{for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values())for(int value=slider.min;value<=slider.max;value+=slider.step()){CustomDungeonRules draft=CustomDungeonRules.defaults().with(slider,value);CustomRulesPresentation.multiplierText(slider,value);CustomRuleMultipliers.combined(draft);CustomScoreSnapshot.forNewRun(draft);}assertEquals(expected,com.watabou.utils.Random.Long());}finally{com.watabou.utils.Random.popGenerator();}
 }
}
