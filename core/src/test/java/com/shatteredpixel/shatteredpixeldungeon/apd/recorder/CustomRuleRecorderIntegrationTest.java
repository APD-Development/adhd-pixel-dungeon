package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import com.watabou.utils.Bundle;
import org.junit.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class CustomRuleRecorderIntegrationTest {
 CustomDungeonRules preferences;int difficulty,challenges;boolean intro;
 @Before public void setup(){HeadlessTestSupport.start();preferences=SPDSettings.apdCustomRules();difficulty=SPDSettings.apdDifficulty();challenges=SPDSettings.challenges();intro=SPDSettings.intro();}
 @After public void cleanup(){ApdRecorder.reset();SPDSettings.apdCustomRules(preferences);SPDSettings.apdDifficulty(difficulty);SPDSettings.challenges(challenges);SPDSettings.intro(intro);}
 @Test public void allSixClassesRecordSavedRulesNotLaterPreferences()throws Exception{
  for(HeroClass clazz:HeroClass.values()){
   CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500).with(CustomDungeonRules.Slider.PLAYER_HP,75).with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,4);
   SPDSettings.apdCustomRules(selected);RecorderTestSupport.Session session=new RecorderTestSupport.Session();
   RecorderTestSupport.newRun(session,918271L+clazz.ordinal());
   // Choose the class through the real new-run selection, not by relabelling the recorded run.
   ApdRecorder.reset();ApdRecorder.configure(session.root,true);GamesInProgress.selectedClass=clazz;Dungeon.init();
   String id=ApdRecorder.currentRunId();assertNotNull(id);RecorderTestSupport.enterLevel(1);Dungeon.saveAll();
   SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,60));
   session.restartApp();Dungeon.loadGame(session.slot);Dungeon.level=Dungeon.loadLevel(session.slot);Dungeon.switchLevel(Dungeon.level,Dungeon.hero.pos);
   assertEquals(selected.preferenceText(),Dungeon.customRules().preferenceText());
   ReadResult raw=session.read(id);LifecycleEvents.RunStart start=RecorderTestSupport.runStart(raw);assertEquals(clazz.name(),start.heroClass);assertEquals(selected.preferenceText(),start.customRules);
   RunFacts facts=RunFactsBuilder.fromLog(raw,id);for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values())assertEquals(selected.get(slider),facts.numOr(CustomRuleFacts.key(slider.name()),-1));
   assertEquals(Ruleset.current().id,facts.textOr(FactKeys.RULESET,""));assertTrue(Ruleset.current().text.contains("exotic-scope=healing-absolute-v2"));assertEquals("experimental provisional",facts.textOr(CustomRuleFacts.SCORE_STATUS,""));assertTrue(facts.isKnown(CustomRuleFacts.MULTIPLIER));
  }
 }
 @Test public void everyDifficultyAndOriginalChallengeMaskAreRecordedAlongsideCustomValues()throws Exception{
  int[] masks={0,1,2,4,8,16,32,64,128,256,511};
  for(int level=1;level<=5;level++)for(int mask:masks){
   SPDSettings.apdDifficulty(level);CustomDungeonRules selected=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,1);SPDSettings.apdCustomRules(selected);RecorderTestSupport.Session session=new RecorderTestSupport.Session();RecorderTestSupport.newRun(session,59173+mask+level);
   ApdRecorder.reset();ApdRecorder.configure(session.root,true);SPDSettings.challenges(mask);Dungeon.init();String id=ApdRecorder.currentRunId();ReadResult read=session.read(id);LifecycleEvents.RunStart start=RecorderTestSupport.runStart(read);
   assertEquals(level,start.difficulty);assertEquals(mask,start.challenges);assertEquals(selected.preferenceText(),start.customRules);assertEquals(Dungeon.difficultyBalance,start.difficultyBalance);
   RunFacts f=RunFactsBuilder.fromLog(read,id);assertEquals(level,f.numOr(FactKeys.DIFFICULTY,-1));assertEquals(mask,f.numOr(FactKeys.CHALLENGES,-1));assertEquals(1,f.numOr(CustomRuleFacts.key(CustomDungeonRules.Slider.UPGRADE_SCROLLS.name()),-1));
  }
 }
 @Test public void derivedCacheDamageRebuildPreservesRawRulesAndExistingScore()throws Exception{
  SPDSettings.apdCustomRules(CustomDungeonRules.defaults());RecorderTestSupport.Session session=new RecorderTestSupport.Session();RecorderTestSupport.newRun(session,91921);String id=ApdRecorder.currentRunId();RecorderTestSupport.enterLevel(1);Statistics.totalScore=12345;ApdRecorder.onRunEnded(false,new Object());
  ReadResult raw=session.read(id);RunFacts facts=RunFactsBuilder.fromLog(raw,id);long original=facts.numOr(FactKeys.SCORE,-1);assertEquals(original,facts.numOr(CustomRuleFacts.ORIGINAL_SCORE,-2));assertEquals(original,facts.numOr(CustomRuleFacts.ADJUSTED_SCORE,-3));assertEquals(100,facts.numOr(CustomRuleFacts.MULTIPLIER,-1));
  byte[] before=Files.readAllBytes(session.log(id).toPath());DerivedStore derived=new DerivedStore(session.root);derived.load();Files.write(derived.cacheFile().toPath(),new byte[]{0,1,2,3});FactTable recovered=derived.load();assertEquals(1,recovered.apdRows.size());assertEquals(15,recovered.apdRows.get(0).numOr(CustomRuleFacts.key(CustomDungeonRules.Slider.UPGRADE_SCROLLS.name()),-1));assertArrayEquals(before,Files.readAllBytes(session.log(id).toPath()));
 }
}
