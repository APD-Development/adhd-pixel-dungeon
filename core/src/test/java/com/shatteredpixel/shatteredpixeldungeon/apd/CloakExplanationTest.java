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

public class CloakExplanationTest {
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
    private static CloakOfShadows cloak(){CloakOfShadows cloak=new CloakOfShadows();cloak.levelKnown=cloak.cursedKnown=true;return cloak;}
    private static String saved(com.watabou.utils.Bundlable object){Bundle b=new Bundle();object.storeInBundle(b);return b.toString();}
    private static int progress(CloakOfShadows cloak){Bundle b=new Bundle();cloak.storeInBundle(b);return b.getInt("exp");}
    private CloakOfShadows.cloakStealth stealth(CloakOfShadows cloak)throws Exception{
        CloakOfShadows.cloakStealth buff=cloak.new cloakStealth();Field active=Artifact.class.getDeclaredField("activeBuff");active.setAccessible(true);active.set(cloak,buff);assertTrue(buff.attachTo(hero));return buff;
    }
    @Test public void realStealthChargesOnFirstUpdateThenEveryFourthAndInspectionDoesNotTick()throws Exception{
        CloakOfShadows cloak=cloak();hero.lvl=1;CloakOfShadows.cloakStealth stealth=stealth(cloak);float time=hero.cooldown();
        try{
            assertEquals(0,cloak.remainingStealthUpdates());assertEquals(3,cloak.currentCharges());
            String before=saved(cloak),heroBefore=saved(hero);Random.pushGenerator(812);int expected=Random.Int();Random.popGenerator();Random.pushGenerator(812);
            try{for(int i=0;i<10;i++){assertTrue(ArtifactExplanation.details(cloak).contains("next stealth update needs"));assertFalse(ArtifactExplanation.summary(cloak).isEmpty());}assertEquals(expected,Random.Int());}finally{Random.popGenerator();}
            assertEquals(before,saved(cloak));assertEquals(heroBefore,saved(hero));
            stealth.act();assertEquals(2,cloak.currentCharges());assertEquals(4,cloak.remainingStealthUpdates());assertEquals(10,progress(cloak));
            for(int i=0;i<3;i++)stealth.act();assertEquals(2,cloak.currentCharges());assertEquals(1,cloak.remainingStealthUpdates());
            assertTrue(CloakExplanation.details(cloak,hero).contains("about _1 turn_"));stealth.act();assertEquals(1,cloak.currentCharges());assertEquals(4,cloak.remainingStealthUpdates());assertEquals(20,progress(cloak));assertEquals(time,hero.cooldown(),0);
        }finally{stealth.detach();}
        assertEquals(-1,cloak.remainingStealthUpdates());assertEquals(0,hero.invisible);
    }
    @Test public void realExhaustionAndDueVersusNotDueDispellingMatchDescriptions()throws Exception{
        Field charge=Artifact.class.getDeclaredField("charge");charge.setAccessible(true);
        CloakOfShadows cloak=cloak();hero.lvl=1;CloakOfShadows.cloakStealth buff=stealth(cloak);buff.dispel();assertEquals(2,cloak.currentCharges());assertEquals(0,progress(cloak));assertEquals(0,hero.invisible);
        buff=stealth(cloak);buff.act();assertEquals(1,cloak.currentCharges());buff.dispel();assertEquals(1,cloak.currentCharges());assertEquals(10,progress(cloak));
        cloak=cloak();charge.setInt(cloak,1);buff=stealth(cloak);buff.act();assertEquals(0,cloak.currentCharges());assertEquals(1,hero.invisible);
        for(int i=0;i<4;i++)buff.act();assertEquals(0,cloak.currentCharges());assertEquals(0,hero.invisible);assertEquals(-1,cloak.remainingStealthUpdates());assertEquals(10,progress(cloak));assertEquals(1,((ControlledHero)hero).interruptions);
        cloak=cloak();buff=stealth(cloak);buff.detach();assertEquals(3,cloak.currentCharges());
    }
    @Test public void liveProgressMatchesRealPaymentsAcrossHeroAndCloakLevels()throws Exception{
        for(int step:new int[]{0,1,6,7,9,10})for(int heroLevel:new int[]{1,8,15,30}){
            CloakOfShadows cloak=cloak();for(int i=0;i<step;i++)cloak.upgrade();hero.lvl=heroLevel;
            int predicted=CloakExplanation.experiencePerPayment(cloak,hero),capacity=cloak.maximumCharges();assertTrue(predicted>=0);String text=CloakExplanation.details(cloak,hero);assertTrue(text,text.contains(predicted+" Cloak progress points"));
            CloakOfShadows.cloakStealth buff=stealth(cloak);
            try{buff.act();int gained=progress(cloak)+(cloak.level()>step?(step+1)*50:0);assertEquals(predicted,gained);assertTrue(cloak.level()==step||cloak.level()==step+1);assertEquals(Math.min(10,capacity+(cloak.level()>step?1:0)),cloak.maximumCharges());}finally{buff.detach();}
        }
    }
    @Test public void actualCloakBundleRoundTripPreservesActiveCountdownWithoutInspectionMutations()throws Exception{
        CloakOfShadows original=cloak();CloakOfShadows.cloakStealth buff=stealth(original);buff.act();buff.act();assertEquals(3,original.remainingStealthUpdates());
        Bundle bundle=new Bundle();original.storeInBundle(bundle);ByteArrayOutputStream bytes=new ByteArrayOutputStream();assertTrue(Bundle.write(bundle,bytes));Bundle loaded=Bundle.read(new ByteArrayInputStream(bytes.toByteArray()));CloakOfShadows copy=cloak();copy.restoreFromBundle(loaded);
        assertEquals(original.remainingStealthUpdates(),copy.remainingStealthUpdates());assertEquals(original.currentCharges(),copy.currentCharges());assertEquals(progress(original),progress(copy));String saved=saved(copy);assertTrue(CloakExplanation.details(copy,hero).contains("about _3 turns_"));assertEquals(saved,saved(copy));buff.detach();
    }
    @Test public void unknownCloakAndNoHeroWithholdUnavailablePersonalNumbers(){
        for(int level:new int[]{0,3,10}){CloakOfShadows cloak=cloak();for(int i=0;i<level;i++)cloak.upgrade();cloak.levelKnown=false;assertEquals("",CloakExplanation.summary(cloak));assertEquals("",CloakExplanation.details(cloak,hero));assertEquals(-1,CloakExplanation.experiencePerPayment(cloak,hero));cloak.levelKnown=true;assertTrue(CloakExplanation.details(cloak,null).contains("withheld without a loaded hero"));assertEquals(-1,CloakExplanation.experiencePerPayment(cloak,null));}
    }
    @Test public void realPassiveGainPausesWhileCloakedButExternalPowerStillAddsItsOwnProgress()throws Exception{
        CloakOfShadows cloak=cloak();hero.belongings.artifact=cloak;
        Field charge=Artifact.class.getDeclaredField("charge"),partial=Artifact.class.getDeclaredField("partialCharge");charge.setAccessible(true);partial.setAccessible(true);charge.setInt(cloak,1);partial.setFloat(cloak,0);
        cloak.activate(hero);CloakOfShadows.cloakRecharge recharge=hero.buff(CloakOfShadows.cloakRecharge.class);assertNotNull(recharge);recharge.act();float initial=partial.getFloat(cloak);assertTrue(initial>0);
        CloakOfShadows.cloakStealth buff=stealth(cloak);
        try{
            recharge.act();assertEquals(initial,partial.getFloat(cloak),0);int stored=cloak.currentCharges();String state=saved(cloak);ArtifactExplanation.details(cloak);assertEquals(state,saved(cloak));
            cloak.charge(hero,2);assertEquals(initial+.5f,partial.getFloat(cloak),.000001f);assertEquals(stored,cloak.currentCharges());assertEquals(0,cloak.remainingStealthUpdates());
        }finally{buff.detach();}
        float external=partial.getFloat(cloak);recharge.act();assertTrue(partial.getFloat(cloak)>external);
    }
}
