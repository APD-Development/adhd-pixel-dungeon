package com.shatteredpixel.shatteredpixeldungeon.apd.recorder;

import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.*;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class DailyEarnedScoreHistoryTest {
 private final Rankings rankings=Rankings.INSTANCE;
 private ArrayList<Rankings.Record> records;
 private Rankings.Record latest,replay;
 private LinkedHashMap<Long,Integer> originals;
 private LinkedHashMap<Long,Long> earned;
 private CustomDungeonRules preferences;
 private byte[] file;
 private long lastDaily;
 private int lastRecord,total,won;
 private final Bundle previousRules=new Bundle(),previousStats=new Bundle();
 private com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero previousHero;
 private com.shatteredpixel.shatteredpixeldungeon.levels.Level previousLevel;
 private boolean previousDaily,previousReplay,previousIntro;
 private long previousSeed;private int previousDepth,previousBranch,previousSlot,previousChallengePrefs;
 private String previousSeedText;private HeroClass previousSelected;
 private int previousDifficulty,previousBalance,previousChallenges;
 @SuppressWarnings("unchecked") private Map<Long,Long> earned()throws Exception {
  Field f=Rankings.class.getDeclaredField("dailyEarnedScoreHistory");f.setAccessible(true);return (Map<Long,Long>)f.get(rankings);
 }
 @Before public void before()throws Exception {
  HeadlessTestSupport.start();preferences=SPDSettings.apdCustomRules();lastDaily=SPDSettings.lastDaily();
  previousDifficulty=Dungeon.difficulty;previousBalance=Dungeon.difficultyBalance;previousChallenges=Dungeon.challenges;
  Dungeon.apdStoreCustomRules(previousRules);Statistics.storeInBundle(previousStats);previousHero=Dungeon.hero;previousLevel=Dungeon.level;previousDaily=Dungeon.daily;previousReplay=Dungeon.dailyReplay;previousSeed=Dungeon.seed;previousSeedText=Dungeon.customSeedText;previousDepth=Dungeon.depth;previousBranch=Dungeon.branch;previousSlot=GamesInProgress.curSlot;previousSelected=GamesInProgress.selectedClass;previousIntro=SPDSettings.intro();previousChallengePrefs=SPDSettings.challenges();
  records=rankings.records;latest=rankings.latestDaily;replay=rankings.latestDailyReplay;
  lastRecord=rankings.lastRecord;total=rankings.totalNumber;won=rankings.wonNumber;
  originals=new LinkedHashMap<>(rankings.dailyScoreHistory);earned=new LinkedHashMap<>(earned());
  FileHandle f=FileUtils.getFileHandle(Rankings.RANKINGS_FILE);file=f.exists()?f.readBytes():null;
  rankings.records=new ArrayList<>();rankings.latestDaily=null;rankings.latestDailyReplay=null;rankings.dailyScoreHistory.clear();earned().clear();
 }
 @After public void after()throws Exception {
  ApdRecorder.reset();SPDSettings.apdCustomRules(preferences);SPDSettings.lastDaily(lastDaily);
  Dungeon.difficulty=previousDifficulty;Dungeon.difficultyBalance=previousBalance;Dungeon.challenges=previousChallenges;
  Dungeon.apdRestoreCustomRules(previousRules);Statistics.restoreFromBundle(previousStats);Dungeon.hero=previousHero;Dungeon.level=previousLevel;Dungeon.daily=previousDaily;Dungeon.dailyReplay=previousReplay;Dungeon.seed=previousSeed;Dungeon.customSeedText=previousSeedText;Dungeon.depth=previousDepth;Dungeon.branch=previousBranch;GamesInProgress.curSlot=previousSlot;GamesInProgress.selectedClass=previousSelected;SPDSettings.intro(previousIntro);SPDSettings.challenges(previousChallengePrefs);
  rankings.records=records;rankings.latestDaily=latest;rankings.latestDailyReplay=replay;
  rankings.lastRecord=lastRecord;rankings.totalNumber=total;rankings.wonNumber=won;
  rankings.dailyScoreHistory.clear();rankings.dailyScoreHistory.putAll(originals);earned().clear();earned().putAll(earned);
  FileHandle f=FileUtils.getFileHandle(Rankings.RANKINGS_FILE);if(file==null)f.delete();else f.writeBytes(file,false);
 }
 private Rankings.Record record(long seed,int score,CustomDungeonRules rules) {
  Rankings.Record r=new Rankings.Record();r.heroClass=HeroClass.WARRIOR;r.cause=Object.class;r.daily=true;r.score=score;
  r.gameID="daily-"+seed;r.customSeed="";r.date="2026-10-16";r.version="v4.0.8";r.gameData=new Bundle();r.gameData.put(Rankings.SEED,seed);
  if(rules!=null){Bundle b=new Bundle();rules.store(b);r.gameData.put("apd_custom_rules",b);CustomScoreSnapshot.forNewRun(rules).store(r.gameData);}
  return r;
 }
 private void store(long seed,Rankings.Record r)throws Exception {
  Method m=Rankings.class.getDeclaredMethod("recordDailyScore",long.class,Rankings.Record.class);m.setAccessible(true);m.invoke(rankings,seed,r);rankings.latestDaily=r;
 }
 private void reload() {rankings.records=null;rankings.dailyScoreHistory.clear();rankings.load();}
 private Bundle legacy(Rankings.Record latest) {
  Bundle b=new Bundle();b.put("records",new ArrayList<Rankings.Record>());b.put(Rankings.LATEST_DAILY,latest);
  b.put(Rankings.DAILY_HISTORY_DATES,new long[]{101,102});b.put(Rankings.DAILY_HISTORY_SCORES,new int[]{77,500});return b;
 }
 @Test public void actualDailySubmissionDisplaysItsEarnedScoreAndPreservesOriginal()throws Exception {
  CustomDungeonRules rules=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500);SPDSettings.apdCustomRules(rules);
  RecorderTestSupport.Session s=new RecorderTestSupport.Session();RecorderTestSupport.newRun(s,814723);RecorderTestSupport.enterLevel(1);
  Dungeon.daily=true;Dungeon.dailyReplay=false;Dungeon.seed=123456;
  rankings.submit(false,new Object());Rankings.Record r=rankings.latestDaily;
  assertTrue(r.earnedScore()>r.score);assertEquals(r.earnedScore(),rankings.dailyEarnedScore(123456));assertEquals(Integer.valueOf(r.score),rankings.dailyScoreHistory.get(123456L));
  SPDSettings.apdCustomRules(CustomDungeonRules.defaults());reload();assertEquals(r.earnedScore(),rankings.dailyEarnedScore(123456));
 }
 @Test public void longScoresAndNormalizedDailyDatesSurviveSaveWithoutTruncatingOriginalArrays()throws Exception {
  CustomDungeonRules rules=CustomDungeonRules.defaults();for(CustomDungeonRules.Slider s:CustomDungeonRules.Slider.values())rules=rules.with(s,CustomRuleMultipliers.individual(s,s.min).compareTo(CustomRuleMultipliers.individual(s,s.max))>0?s.min:s.max);
  long seed=DungeonSeed.TOTAL_SEEDS+101;Rankings.Record r=record(seed,1_000_000,rules);assertTrue(r.earnedScore()>Integer.MAX_VALUE);store(seed,r);
  store(102,record(102,500,CustomDungeonRules.defaults()));rankings.save();reload();assertEquals(r.earnedScore(),rankings.dailyEarnedScore(101));
  Bundle saved=FileUtils.bundleFromFile(Rankings.RANKINGS_FILE);assertArrayEquals(new long[]{101,102},saved.getLongArray(Rankings.DAILY_HISTORY_DATES));assertArrayEquals(new int[]{1_000_000,500},saved.getIntArray(Rankings.DAILY_HISTORY_SCORES));
 }
 @Test public void legacyScoresDoNotAcquireTodaysMultipliers()throws Exception {
  FileUtils.bundleToFile(Rankings.RANKINGS_FILE,legacy(record(102,500,null)));
  SPDSettings.apdCustomRules(CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500));reload();
  assertEquals(77,rankings.dailyEarnedScore(101));assertEquals(500,rankings.dailyEarnedScore(102));rankings.save();reload();assertEquals(77,rankings.dailyEarnedScore(101));
 }
 @Test public void latestFrozenScoreFromOlderBuildIsRetainedAfterAnotherDaily()throws Exception {
  Rankings.Record r=record(102,500,CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.ENEMY_HP,500));Bundle b=legacy(r);assertFalse(b.contains("apd_daily_earned_dates_v1"));
  FileUtils.bundleToFile(Rankings.RANKINGS_FILE,b);reload();assertEquals(r.earnedScore(),rankings.dailyEarnedScore(102));
  store(103,record(103,600,CustomDungeonRules.defaults()));rankings.save();reload();assertEquals(r.earnedScore(),rankings.dailyEarnedScore(102));assertEquals(77,rankings.dailyEarnedScore(101));assertEquals(Integer.valueOf(500),rankings.dailyScoreHistory.get(102L));
 }
 @Test public void malformedOptionalMetadataDoesNotReplaceOriginalScores()throws Exception {
  for(int variant=0;variant<3;variant++) {
   Bundle b=legacy(record(102,500,null));b.put("apd_daily_earned_dates_v1",variant==0?new long[]{101,102}:new long[]{variant==1?999:101});b.put("apd_daily_earned_scores_v1",new long[]{variant==2?-99:999999});
   FileUtils.bundleToFile(Rankings.RANKINGS_FILE,b);reload();assertEquals(77,rankings.dailyEarnedScore(101));assertEquals(500,rankings.dailyEarnedScore(102));
  }
 }
}
