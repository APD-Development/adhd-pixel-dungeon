/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.GrassDewPickupTest;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.*;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.*;
import com.watabou.utils.*;
import com.watabou.utils.Random;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class RoseExplanationTest {
    public static class ControlledHero extends Hero {int interruptions;@Override public void interrupt(){interruptions++;}}
    private Hero oldHero,hero;private com.shatteredpixel.shatteredpixeldungeon.levels.Level oldLevel;private int oldChallenges;
    private Object oldHandler,oldNotes;private Field notesField;private com.watabou.noosa.Game oldGame;private Bundle oldRules;private HashSet<Class> oldDiscovered;
    private final Map<Field,Object> badgeFields=new HashMap<>();
    private final Map<Catalog,Map<Field,Map>> catalogFields=new HashMap<>();private Field journalDirty;private boolean oldJournalDirty;
    public static class Quiet extends HeroSprite {
        @Override public com.watabou.noosa.particles.Emitter centerEmitter(){return null;}
        @Override public void add(CharSprite.State state){} @Override public void remove(CharSprite.State state){}
        @Override public void sprint(float speed){} @Override public void showStatus(int color,String text,Object...args){}
        @Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
    }
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();notesField=com.shatteredpixel.shatteredpixeldungeon.journal.Notes.class.getDeclaredField("records");notesField.setAccessible(true);oldNotes=notesField.get(null);com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();oldGame=com.watabou.noosa.Game.instance;oldHero=Dungeon.hero;oldLevel=Dungeon.level;oldChallenges=Dungeon.challenges;Dungeon.challenges=0;
        oldHandler=Potion.apdHandler();Random.pushGenerator(831);try{Potion.initColors();}finally{Random.popGenerator();}
        oldDiscovered=Statistics.itemTypesDiscovered;Statistics.itemTypesDiscovered=new HashSet<>();
        oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);Dungeon.apdRestoreCustomRules(new Bundle());
        for(String name:new String[]{"global","local","saveNeeded"}){Field f=Badges.class.getDeclaredField(name);f.setAccessible(true);badgeFields.put(f,f.get(null));f.set(null,name.equals("saveNeeded")?false:new HashSet<>());}
        for(Catalog cat:Catalog.values()){Map<Field,Map> state=new HashMap<>();for(String name:new String[]{"seen","useCount"}){Field f=Catalog.class.getDeclaredField(name);f.setAccessible(true);state.put(f,new LinkedHashMap<>((Map)f.get(cat)));}catalogFields.put(cat,state);}
        journalDirty=com.shatteredpixel.shatteredpixeldungeon.journal.Journal.class.getDeclaredField("saveNeeded");journalDirty.setAccessible(true);oldJournalDirty=journalDirty.getBoolean(null);
        GrassDewPickupTest.TestLevel l=new GrassDewPickupTest.TestLevel();l.setSize(33,33);Arrays.fill(l.map,com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY);l.blobs=new HashMap<>();l.buildFlagMaps();l.mobs=new HashSet<>();l.heaps=new SparseArray<>();l.traps=new SparseArray<>();l.plants=new SparseArray<>();Dungeon.level=l;
        hero=new ControlledHero();hero.heroClass=HeroClass.WARRIOR;Talent.initClassTalents(hero);hero.pos=16*33+16;hero.HP=10;hero.HT=100;hero.STR=10;hero.fieldOfView=l.heroFOV;Dungeon.hero=hero;
        Field f=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");f.setAccessible(true);Object u=f.get(null);hero.sprite=(Quiet)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,Quiet.class);com.watabou.noosa.Game.instance=(com.watabou.noosa.Game)u.getClass().getMethod("allocateInstance",Class.class).invoke(u,com.watabou.noosa.Game.class);
    }
    @After public void restore()throws Exception{
        if(hero!=null)for(Buff b:new ArrayList<>(hero.buffs()))b.detach();
        Dungeon.hero=oldHero;Dungeon.level=oldLevel;Dungeon.challenges=oldChallenges;Dungeon.apdRestoreCustomRules(oldRules);Potion.apdHandler(oldHandler);Statistics.itemTypesDiscovered=oldDiscovered;
        for(Map.Entry<Field,Object> e:badgeFields.entrySet())e.getKey().set(null,e.getValue());
        for(Map.Entry<Catalog,Map<Field,Map>> entry:catalogFields.entrySet())for(Map.Entry<Field,Map> e:entry.getValue().entrySet()){Map map=(Map)e.getKey().get(entry.getKey());map.clear();map.putAll(e.getValue());}
        journalDirty.setBoolean(null,oldJournalDirty);notesField.set(null,oldNotes);com.watabou.noosa.Game.instance=oldGame;
    }
    private static DriedRose rose(){DriedRose r=new DriedRose();r.levelKnown=r.cursedKnown=true;return r;}
    private static String saved(com.watabou.utils.Bundlable object){Bundle b=new Bundle();object.storeInBundle(b);return b.toString();}
    private static void associate(DriedRose rose,DriedRose.GhostHero ghost)throws Exception{Field f=DriedRose.class.getDeclaredField("ghost");f.setAccessible(true);f.set(rose,ghost);}
    @Test public void predictedBaseStatsAndUnarmedBoundsMatchActualGhostsAcrossLevels(){
        for(int level=0;level<=10;level++){
            DriedRose rose=rose();for(int i=0;i<level;i++)rose.upgrade();
            for(int heroLevel:new int[]{1,15,30}){hero.lvl=heroLevel;DriedRose.GhostHero ghost=new DriedRose.GhostHero(rose);assertEquals(RoseExplanation.maximumHealth(rose),ghost.HT);assertEquals(ghost.HT,ghost.HP);assertEquals(heroLevel+9,ghost.attackSkill(null));assertEquals(heroLevel+4,ghost.defenseSkill(null));}
            DriedRose.GhostHero ghost=new DriedRose.GhostHero(rose);int min=Integer.MAX_VALUE,max=0;Random.pushGenerator(833+level);try{for(int i=0;i<4096;i++){int roll=ghost.damageRoll();min=Math.min(min,roll);max=Math.max(max,roll);}}finally{Random.popGenerator();}assertEquals(1,min);assertEquals(RoseExplanation.unarmedMaximum(rose),max);
        }
    }
    @Test public void liveGhostIconIsHealthNotTheStoredSummoningMeter()throws Exception{
        DriedRose rose=rose();DriedRose.GhostHero ghost=new DriedRose.GhostHero(rose);associate(rose,ghost);ghost.HP=ghost.HT/2;Field charge=Artifact.class.getDeclaredField("charge");charge.setAccessible(true);charge.setInt(rose,25);
        assertEquals("50%",rose.status());String before=saved(rose),ghostBefore=saved(ghost);String description=ArtifactExplanation.details(rose);assertTrue(description,description.contains("_25/100_"));assertTrue(description.contains("the ghost’s health percentage"));assertFalse(description.contains("A percent on the icon is this meter's fullness"));assertEquals(before,saved(rose));assertEquals(ghostBefore,saved(ghost));
    }
    @Test public void actualUpgradeHealingUsesExistingCapBeforeNextStatisticsRefresh()throws Exception{
        Method refresh=DriedRose.GhostHero.class.getDeclaredMethod("updateRose");refresh.setAccessible(true);
        for(int hp:new int[]{20,40}){DriedRose rose=rose();DriedRose.GhostHero ghost=new DriedRose.GhostHero(rose);associate(rose,ghost);ghost.HP=hp;rose.upgrade();assertEquals(1,rose.level());assertEquals(40,ghost.HT);assertEquals(Math.min(40,hp+8),ghost.HP);assertEquals(50,RoseExplanation.maximumHealth(rose));refresh.invoke(ghost);assertEquals(50,ghost.HT);assertEquals(Math.min(40,hp+8),ghost.HP);assertTrue(RoseExplanation.details(rose,hero).contains("maximum at that moment"));}
    }
    @Test public void hiddenTransferredEquipmentAndInspectionCannotLeakLevelsOrMutateState()throws Exception{
        String baseline=null;
        for(int hiddenLevel:new int[]{0,3,7}){
            DriedRose rose=rose();Field weapon=DriedRose.class.getDeclaredField("weapon");weapon.setAccessible(true);com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword sword=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();sword.level(hiddenLevel);sword.levelKnown=false;sword.cursedKnown=true;weapon.set(rose,sword);
            String before=saved(rose),heroBefore=saved(hero);Random.pushGenerator(619);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(619);
            try{String description=RoseExplanation.details(rose,hero);if(baseline==null)baseline=description;else assertEquals(baseline,description);for(int i=0;i<10;i++)ArtifactExplanation.details(rose);assertEquals(expected,Random.Int());}finally{Random.popGenerator();}assertEquals(before,saved(rose));assertEquals(heroBefore,saved(hero));
            Bundle b=new Bundle();rose.storeInBundle(b);ByteArrayOutputStream bytes=new ByteArrayOutputStream();assertTrue(Bundle.write(b,bytes));DriedRose copy=rose();copy.restoreFromBundle(Bundle.read(new ByteArrayInputStream(bytes.toByteArray())));assertFalse(copy.ghostWeapon().levelKnown);assertEquals(descriptionOf(rose),descriptionOf(copy));
        }
    }
    private String descriptionOf(DriedRose rose){return RoseExplanation.details(rose,hero);}
    public static class UpkeepProbe extends DriedRose.GhostHero {
        int received;Object source;
        UpkeepProbe(DriedRose rose){super(rose);}
        @Override public void damage(int amount,Object src){received=amount;source=src;HP=0;}
        boolean actOnce(){return act();}
    }
    @Test public void upkeepDescriptionMatchesActualUnequippedAndMagicImmuneGuard(){
        for(boolean worn:new boolean[]{false,true})for(boolean immune:new boolean[]{false,true}){
            Buff.detach(hero,MagicImmune.class);DriedRose rose=rose();hero.belongings.artifact=worn?rose:null;
            if(immune)Buff.affect(hero,MagicImmune.class);
            UpkeepProbe ghost=new UpkeepProbe(rose);boolean harms=!worn||immune;
            // Stop before unrelated ally movement: the probe marks itself dead when offered damage.
            // The no-damage control starts dead so the same actual guard still runs, then returns.
            if(!harms)ghost.HP=0;
            assertTrue(ghost.actOnce());assertEquals(harms?1:0,ghost.received);
            if(harms)assertEquals(DriedRose.GhostHero.NoRoseDamage.class,ghost.source.getClass());
            assertTrue(RoseExplanation.details(rose,hero).contains("1 damage before each action"));
            assertTrue(RoseExplanation.summary(rose).contains("Keep the Rose equipped"));
        }
    }
    @Test public void unknownRoseAndNoHeroWithholdUnavailableValues(){
        for(int level:new int[]{0,3,10}){DriedRose rose=rose();for(int i=0;i<level;i++)rose.upgrade();rose.levelKnown=false;assertEquals("",RoseExplanation.summary(rose));assertEquals("",RoseExplanation.details(rose,hero));assertEquals(-1,RoseExplanation.maximumHealth(rose));assertEquals(-1,RoseExplanation.unarmedMaximum(rose));rose.levelKnown=true;assertTrue(RoseExplanation.details(rose,null).contains("withheld without a loaded hero"));}
    }
}
