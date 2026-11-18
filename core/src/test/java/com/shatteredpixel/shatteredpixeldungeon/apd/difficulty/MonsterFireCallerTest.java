package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.watabou.utils.Bundle;
import org.junit.*;
import static org.junit.Assert.*;
public class MonsterFireCallerTest {
 FireCallerProvenanceTest fixture=new FireCallerProvenanceTest();
 static class Fist extends YogFist.BurningFist {void fireAt(Char target){enemy=target;zap();}}
 @Before public void setup()throws Exception{fixture.setup();fixture.fixture.rules(200,300,500);}
 @After public void cleanup()throws Exception{fixture.cleanup();}
 void newborn(Elemental.NewbornFireElemental caster)throws Exception{caster.pos=260;java.lang.reflect.Field field=Elemental.NewbornFireElemental.class.getDeclaredField("targetingPos");field.setAccessible(true);field.setInt(caster,312);java.lang.reflect.Method zap=Elemental.NewbornFireElemental.class.getDeclaredMethod("zap");zap.setAccessible(true);zap.invoke(caster);}
 @Test public void newbornFieldsUseEnemySourceForBothWaterAndDryTiles()throws Exception{for(boolean water:new boolean[]{false,true}){Dungeon.level.water[312]=water;newborn(new Elemental.NewbornFireElemental());Fire f=fixture.produced();assertEquals(water?2:8,f.cur[312]);assertEquals(12,fixture.burn(f));f.fullyClear();}}
 @Test public void newbornSummonFieldsKeepTheirExplicitPlayerOrBossCreator()throws Exception{for(boolean boss:new boolean[]{false,true}){Elemental.NewbornFireElemental caster=new Elemental.NewbornFireElemental();caster.alignment=Char.Alignment.ALLY;caster.apdInheritOffensiveOrigin(boss?new Goo():Dungeon.hero);newborn(caster);Fire f=fixture.produced();assertEquals(boss?20:8,fixture.burn(f));f.fullyClear();}}
 @Test public void friendlyNewbornWithNoRecordedCreatorDoesNotInventPlayerOwnership()throws Exception{Elemental.NewbornFireElemental caster=new Elemental.NewbornFireElemental();caster.alignment=Char.Alignment.ALLY;newborn(caster);assertEquals(4,fixture.burn(fixture.produced()));}
 @Test public void burningFistZapFieldsUseBossInsteadOfEnemyDamage()throws Exception{Fist fist=new Fist();fist.pos=260;SpecialBombDamageProvenanceTest.Target enemy=new SpecialBombDamageProvenanceTest.Target(false);enemy.pos=313;fist.fireAt(enemy);assertEquals(20,fixture.burn(fixture.produced()));}
 @Test public void bossFireFieldCreatorSurvivesFireSaveAndRestore()throws Exception{Fist fist=new Fist();fist.pos=260;SpecialBombDamageProvenanceTest.Target enemy=new SpecialBombDamageProvenanceTest.Target(false);enemy.pos=313;fist.fireAt(enemy);Bundle state=new Bundle();fixture.produced().storeInBundle(state);Fire restored=new Fire();restored.restoreFromBundle(state);assertEquals(20,fixture.burn(restored));}
}
