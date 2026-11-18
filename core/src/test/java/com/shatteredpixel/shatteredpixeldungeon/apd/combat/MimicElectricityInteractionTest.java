package com.shatteredpixel.shatteredpixeldungeon.apd.combat;

import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.*;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wondrous;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MimicSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.*;
import java.util.Arrays;
import org.junit.*;
import static org.junit.Assert.*;

/** Real curse/field/mimic dispatch; only rendering and status text are quieted. */
public class MimicElectricityInteractionTest {
    private static final int USER=112, TARGET=113, FAR=116;
    private static class Quiet extends CharSprite {
        // Real emitter start preserves its normal RNG draw; particles are never rendered/updated.
        private final Emitter particles=new Emitter();
        Quiet(){visible=false;new Group().add(this);}
        @Override public void showStatus(int c,String s,Object...args){}
        @Override public void showStatusWithIcon(int c,String s,int icon,Object...args){}
        @Override public void add(State state){}
        @Override public void remove(State state){}
        @Override public void idle(){}
        @Override public Emitter emitter(){return particles;}
        @Override public Emitter centerEmitter(){return particles;}
    }
    private static class TestHero extends Hero {
        @Override public void interrupt(){}
    }
    private static class Board extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }
    private static class Probe extends Mimic {
        int calls,input,hpLoss;Object source;
        @Override public void damage(int amount,Object from){
            calls++;input=amount;source=from;int before=HP;
            super.damage(amount,from);hpLoss=before-HP;
        }
        void immune(Class<?> effect){immunities.add(effect);}
    }
    private static class Target extends Char {
        Target(){HP=HT=1000;alignment=Alignment.ENEMY;pos=TARGET;sprite=new Quiet();}
    }
    private static class ZeroAttacker extends Char {
        boolean guaranteedHit;
        ZeroAttacker(){HP=HT=100;pos=USER;sprite=new Quiet();alignment=Alignment.ALLY;}
        @Override public int attackSkill(Char target){return guaranteedHit?INFINITE_ACCURACY:0;}
        @Override public int damageRoll(){return 0;}
    }
    private static class RevealSprite extends MimicSprite {
        boolean hidingNow(){return curAnim==hiding||curAnim==advancedHiding;}
    }
    private Hero oldHero;private Level oldLevel;private int oldDepth,oldChallenges;
    private boolean oldRecording,oldPositive;private Bundle oldRules;
    private Weapon weapon;

    @Before public void setup(){
        HeadlessTestSupport.start();oldHero=Dungeon.hero;oldLevel=Dungeon.level;
        oldDepth=Dungeon.depth;oldChallenges=Dungeon.challenges;
        oldRecording=SPDSettings.apdRecording();SPDSettings.apdRecording(false);
        oldPositive=WondrousResin.forcePositive;WondrousResin.forcePositive=false;
        oldRules=new Bundle();Dungeon.apdStoreCustomRules(oldRules);
        Dungeon.apdRestoreCustomRules(new Bundle());Dungeon.challenges=0;Dungeon.depth=3;
        Random.pushGenerator(36666);Actor.clear();
        Dungeon.hero=new TestHero();Dungeon.hero.HP=Dungeon.hero.HT=1000;
        Dungeon.hero.pos=USER;Dungeon.hero.sprite=new Quiet();
        Board board=new Board();board.setSize(15,15);Dungeon.level=board;
        Arrays.fill(board.map,Terrain.EMPTY);Arrays.fill(board.passable,true);
        for(int i=0;i<board.length();i++)if(i%15==0||i%15==14||i<15||i>=210){
            board.map[i]=Terrain.WALL;board.solid[i]=true;board.passable[i]=false;
        }
        board.heaps=new SparseArray<>();board.blobs=new java.util.HashMap<>();
        board.plants=new SparseArray<>();board.traps=new SparseArray<>();
        board.mobs=new java.util.HashSet<>();Dungeon.hero.fieldOfView=new boolean[225];
        Actor.add(Dungeon.hero);weapon=new Dagger();weapon.enchant(new Wondrous());
    }
    @After public void cleanup(){
        Actor.clear();Random.popGenerator();Dungeon.hero=oldHero;Dungeon.level=oldLevel;
        Dungeon.depth=oldDepth;Dungeon.challenges=oldChallenges;
        Dungeon.apdRestoreCustomRules(oldRules);SPDSettings.apdRecording(oldRecording);
        WondrousResin.forcePositive=oldPositive;
        assertEquals(OffensiveDamageOrigin.UNATTRIBUTED,OffensiveEffectContext.current());
    }
    private Probe mimic(int cell){Probe m=new Probe();m.setLevel(3);m.HP=m.HT=1000;
        m.pos=cell;m.sprite=new Quiet();Dungeon.level.mobs.add(m);Actor.add(m);hidden(m);return m;}
    private static void hidden(Mimic m){assertEquals(Char.Alignment.NEUTRAL,m.alignment);assertSame(m.PASSIVE,m.state);}
    private static void revealed(Mimic m){assertEquals(Char.Alignment.ENEMY,m.alignment);assertNotSame(m.PASSIVE,m.state);}
    private void waterLine(){for(int c=USER;c<=FAR;c++){Dungeon.level.water[c]=true;Dungeon.level.map[c]=Terrain.WATER;}}
    private Ballistica bolt(){return new Ballistica(USER,TARGET,Ballistica.STOP_TARGET);}
    private Electricity field(int cell,int density){Electricity e=new Electricity();e.seed(Dungeon.level,cell,density);return e;}
    private void tick(Electricity e,long seed){Random.pushGenerator(seed);try{e.act();}finally{Random.popGenerator();}}
    private void lightning(boolean positive,long seed){Random.pushGenerator(seed);try{
        assertTrue(new CursedWand.LightningBolt().effect(weapon,Dungeon.hero,bolt(),positive));
    }finally{Random.popGenerator();}}
    private long seedForProc(Class<? extends CursedWand.CursedEffect> effect){
        for(long seed=0;seed<100000;seed++){
            Random.pushGenerator(seed);try{
                if(Random.Float()>=1/8f)continue;
                boolean positive=Random.Float()<WondrousResin.positiveCurseEffectChance();
                new Emitter().burst(RainbowParticle.BURST,25);
                CursedWand.CursedEffect chosen=CursedWand.randomValidEffect(weapon,Dungeon.hero,bolt(),positive);
                if(effect.isInstance(chosen))return seed;
            }finally{Random.popGenerator();}
        }
        throw new AssertionError("No deterministic curse seed found");
    }

    @Test public void ordinaryShockingCanHitNeutralCreaturesButSparesCasterAlliesAndPrimary(){
        Target primary=new Target(),ally=new Target(),neutral=new Target(),enemy=new Target();ally.pos=114;ally.alignment=Char.Alignment.ALLY;neutral.pos=115;neutral.alignment=Char.Alignment.NEUTRAL;enemy.pos=116;for(Char ch:new Char[]{primary,ally,neutral,enemy})Actor.add(ch);
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking shocking=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking();weapon.enchant(shocking);weapon.levelKnown=weapon.cursedKnown=true;String description=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.EnchantmentExplanation.playerDescription(weapon,Dungeon.hero);assertTrue(description,description.contains("other nearby creatures"));
        long seed=0;for(;seed<1000;seed++){Random.pushGenerator(seed);float roll=Random.Float();Random.popGenerator();if(roll<1/3f)break;}assertTrue(seed<1000);Random.pushGenerator(seed);try{assertEquals(20,shocking.proc(weapon,Dungeon.hero,primary,20));}finally{Random.popGenerator();}
        assertEquals(1000,primary.HP);assertEquals(1000,ally.HP);assertEquals(1000,Dungeon.hero.HP);assertEquals(990,neutral.HP);assertEquals(990,enemy.HP);
    }
    @Test public void eldritchCanFrightenNeutralWitnessesButSparesAlliesAndPrimary(){
        Target primary=new Target(),ally=new Target(),neutral=new Target(),enemy=new Target();ally.pos=114;ally.alignment=Char.Alignment.ALLY;neutral.pos=115;neutral.alignment=Char.Alignment.NEUTRAL;enemy.pos=116;for(Char ch:new Char[]{primary,ally,neutral,enemy}){ch.fieldOfView=new boolean[225];ch.fieldOfView[USER]=true;Actor.add(ch);}
        com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Eldritch eldritch=new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Eldritch();weapon.enchant(eldritch);weapon.levelKnown=weapon.cursedKnown=true;String description=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.EnchantmentExplanation.playerDescription(weapon,Dungeon.hero);assertTrue(description,description.contains("creatures that witness"));long seed=0;for(;seed<1000;seed++){Random.pushGenerator(seed);float roll=Random.Float();Random.popGenerator();if(roll<.2f)break;}assertTrue(seed<1000);Random.pushGenerator(seed);try{assertEquals(20,eldritch.proc(weapon,Dungeon.hero,primary,20));}finally{Random.popGenerator();}
        assertNull(primary.buff(Terror.class));assertNull(ally.buff(Terror.class));assertNull(Dungeon.hero.buff(Terror.class));assertNotNull(neutral.buff(Terror.class));assertNotNull(enemy.buff(Terror.class));
    }
    @Test public void actualWondrousLightningProcDoesNotConductAlongConnectedWater(){
        waterLine();Target target=new Target();Actor.add(target);
        Probe adjacent=mimic(USER-1),far=mimic(FAR);long seed=seedForProc(CursedWand.LightningBolt.class);
        Random.pushGenerator(seed);try{assertEquals(7,new Wondrous().proc(weapon,Dungeon.hero,target,7));}
        finally{Random.popGenerator();}
        assertEquals(1,adjacent.calls);assertTrue(adjacent.hpLoss>0);assertTrue(adjacent.source instanceof Electricity);revealed(adjacent);
        assertEquals(0,far.calls);assertEquals(1000,far.HP);hidden(far);
        assertNull("Instant burst does not seed a conductive field",Dungeon.level.blobs.get(Electricity.class));
        System.out.println("Wondrous lightning reproduction seed="+seed+" adjacent_calls="+adjacent.calls+" far_calls="+far.calls);
    }
    @Test public void cursedBurstHitsDirectTargetAndBothNeighbourhoodsWithoutWater(){
        Probe direct=mimic(TARGET),byUser=mimic(USER-15-1),byTarget=mimic(TARGET+15+1);
        lightning(false,8119);
        for(Probe m:new Probe[]{direct,byUser,byTarget}){assertEquals(1,m.calls);assertTrue(m.hpLoss>=5);assertTrue(m.source instanceof Electricity);revealed(m);}
        assertNotNull(direct.buff(Paralysis.class));
    }
    @Test public void overlappingBurstAreasDamageEachMimicOnlyOnce(){
        Probe overlap=mimic(USER+15);lightning(false,8119);assertEquals(1,overlap.calls);revealed(overlap);
    }
    @Test public void burstReachIsUnchangedByConnectedDisconnectedOrDryTiles(){
        for(int terrain=0;terrain<3;terrain++){
            Arrays.fill(Dungeon.level.water,false);if(terrain==1)waterLine();
            if(terrain==2){Dungeon.level.water[USER]=true;Dungeon.level.water[FAR]=true;}
            Probe m=mimic(FAR);lightning(false,919);assertEquals(0,m.calls);hidden(m);
            Dungeon.level.mobs.remove(m);Actor.remove(m);
        }
    }
    @Test public void positiveOnlyBurstStillHitsNeutralDisguisedMimics(){
        Probe m=mimic(TARGET);int hp=Dungeon.hero.HP;lightning(true,8119);
        revealed(m);assertTrue(m.hpLoss>0);assertEquals(hp,Dungeon.hero.HP);
    }
    @Test public void connectedFieldReachesDistantWaterAndRevealsBeforeFirstHpDamage(){
        waterLine();Probe m=mimic(FAR);Electricity e=field(USER,10);tick(e,8119);
        assertEquals(9,e.cur[FAR]);assertEquals(0,m.calls);assertEquals(1000,m.HP);
        assertNotNull(m.buff(Paralysis.class));revealed(m);
        tick(e,8119);assertEquals(1,m.calls);assertTrue(m.source instanceof Electricity);assertTrue(m.hpLoss>0);
    }
    @Test public void disconnectedAndDiagonalOnlyWaterNeverCarryAField(){
        for(int pos:new int[]{FAR,USER+16}){
            Arrays.fill(Dungeon.level.water,false);Dungeon.level.water[USER]=true;Dungeon.level.water[pos]=true;
            Probe m=mimic(pos);Electricity e=field(USER,9);tick(e,8119);
            assertEquals(0,e.cur[pos]);assertEquals(0,m.calls);assertNull(m.buff(Paralysis.class));hidden(m);
            Dungeon.level.mobs.remove(m);Actor.remove(m);
        }
    }
    @Test public void connectedWaterDoesNotHitAMimicStandingOnAdjacentDryGround(){
        waterLine();Probe m=mimic(FAR+15);Electricity e=field(USER,9);tick(e,8119);
        assertTrue(e.cur[FAR]>0);assertEquals(0,e.cur[m.pos]);assertEquals(0,m.calls);hidden(m);
    }
    @Test public void zeroDirectDamageRevealsEvenWithoutAnyHpLoss(){
        Probe m=mimic(TARGET);RevealSprite sprite=new RevealSprite();m.sprite=sprite;sprite.visible=false;
        sprite.hideMimic(m);assertTrue(sprite.hidingNow());m.damage(0,new Electricity());
        assertEquals(1,m.calls);assertEquals(0,m.hpLoss);assertEquals(1000,m.HP);revealed(m);
        assertFalse("Real mimic animation leaves its chest disguise",sprite.hidingNow());
    }
    @Test public void successfulZeroDamageAttackDefenseProcReveals(){
        Probe m=mimic(TARGET);ZeroAttacker attacker=new ZeroAttacker();
        assertFalse("A missed attack does not reach the damage/defense handlers",attacker.attack(m));
        hidden(m);assertEquals(0,m.calls);
        attacker.guaranteedHit=true;assertTrue(attacker.attack(m));
        assertEquals(1,m.calls);assertEquals(0,m.input);assertEquals(1000,m.HP);revealed(m);
    }
    @Test public void fullShieldAbsorptionStillReveals(){
        Probe m=mimic(TARGET);Buff.affect(m,Barrier.class).setShield(50);hidden(m);
        m.damage(5,new Electricity());assertEquals(0,m.hpLoss);assertEquals(1000,m.HP);revealed(m);
    }
    @Test public void reachedFieldSkipsElectricalImmunityButDirectHandlerStillReveals(){
        waterLine();Probe m=mimic(FAR);m.immune(Electricity.class);Electricity e=field(USER,9);tick(e,8119);
        assertTrue(e.cur[FAR]>0);assertEquals(0,m.calls);assertNull(m.buff(Paralysis.class));hidden(m);
        m.damage(5,new Electricity());assertEquals(0,m.hpLoss);revealed(m);
    }
    @Test public void paralysisImmunityDefersFieldRevealUntilTheDamageTickEvenIfRollIsZero(){
        Probe m=mimic(TARGET);m.immune(Paralysis.class);Electricity e=field(TARGET,10);tick(e,8119);
        hidden(m);assertEquals(0,m.calls);assertNull(m.buff(Paralysis.class));
        long zero=-1;for(long s=0;s<10000;s++){Random.pushGenerator(s);try{if(Math.round(Random.Float(2+Dungeon.scalingDepth()/5f))==0){zero=s;break;}}finally{Random.popGenerator();}}
        assertTrue(zero>=0);tick(e,zero);assertEquals(1,m.calls);assertEquals(0,m.input);assertEquals(0,m.hpLoss);revealed(m);
    }
    @Test public void negativeStatusRevealsWithoutDamageWhilePositiveBuffDoesNot(){
        Probe m=mimic(TARGET);Buff.affect(m,Bless.class,5);hidden(m);
        Buff.affect(m,Paralysis.class,5);assertEquals(0,m.calls);assertEquals(1000,m.HP);revealed(m);
    }
    @Test public void beckoningDoesNotRevealADisguisedMimic(){
        Probe m=mimic(TARGET);m.beckon(USER);hidden(m);assertEquals(0,m.calls);
    }
    @Test public void realRandomAreaShockingBranchConductsWaterUnlikeLightningBolt(){
        waterLine();Probe m=mimic(FAR);long seed=0;
        for(;seed<10000;seed++){Random.pushGenerator(seed);try{if(Random.Int(3)==2)break;}finally{Random.popGenerator();}}
        assertTrue(seed<10000);Random.pushGenerator(seed);try{
            assertTrue(new CursedWand.RandomAreaEffect().effect(weapon,Dungeon.hero,bolt(),false));
        }finally{Random.popGenerator();}
        Electricity e=(Electricity)Dungeon.level.blobs.get(Electricity.class);assertNotNull(e);
        hidden(m);tick(e,8119);assertTrue(e.cur[FAR]>0);revealed(m);
        assertEquals(0,m.calls);tick(e,8119);assertEquals(1,m.calls);
        System.out.println("RandomAreaEffect shocking branch seed="+seed);
    }
    @Test public void otherWondrousEffectsUseTheirActualStatusAndReachRules(){
        Probe m=mimic(TARGET);assertTrue(new CursedWand.BurnAndFreeze().effect(weapon,Dungeon.hero,bolt(),true));
        assertEquals(0,m.calls);revealed(m);
        assertFalse("Random real-wand zaps are not valid Wondrous weapon procs",new CursedWand.RandomWand().valid(weapon,Dungeon.hero,bolt(),false));
    }
    @Test public void customDamageScalingKeepsVirtualMimicRevealAndSource(){
        for(int percent:new int[]{25,100,500}){
            Bundle all=new Bundle(),rules=new Bundle();CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.PLAYER_DAMAGE,percent).store(rules);all.put("apd_custom_rules",rules);Dungeon.apdRestoreCustomRules(all);
            Probe m=mimic(TARGET);Object source=new Electricity();CustomOffensiveHooks.raw(m,8,source,Dungeon.hero);
            assertEquals(Math.round(8*percent/100f),m.hpLoss);assertSame(source,m.source);revealed(m);
            Dungeon.level.mobs.remove(m);Actor.remove(m);
        }
    }
    @Test public void everyMimicVariantRevealsForZeroDamageAndKeepsItsOwnReaction(){
        for(Mimic m:new Mimic[]{new Mimic(),new GoldenMimic(),new CrystalMimic(),new EbonyMimic()}){
            m.setLevel(3);m.pos=TARGET;m.sprite=new Quiet();int hp=m.HP;Actor.add(m);
            m.damage(0,new Electricity());assertEquals(hp,m.HP);revealed(m);
            assertSame(m instanceof CrystalMimic?m.FLEEING:m.HUNTING,m.state);Actor.remove(m);
        }
    }
    @Test public void hiddenAndRevealedStatesSurviveSaveLoadWithoutMigration(){
        for(boolean hit:new boolean[]{false,true}){
            Mimic original=new Mimic();original.setLevel(3);original.pos=TARGET;original.sprite=new Quiet();
            if(hit)original.damage(0,new Electricity());
            Bundle stored=new Bundle();original.storeInBundle(stored);Mimic loaded=new Mimic();loaded.restoreFromBundle(stored);
            assertEquals(original.HP,loaded.HP);assertEquals(original.HT,loaded.HT);assertEquals(original.pos,loaded.pos);
            if(hit)revealed(loaded);else hidden(loaded);
        }
    }
}
