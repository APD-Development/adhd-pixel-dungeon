package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.junit.*;
import java.util.Arrays;
import static org.junit.Assert.*;

public class RunicProjectingReachTest {
    private static class Flat extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }
    private Level priorLevel;
    private Hero priorHero;
    private Hero hero;
    private Flat level;
    private RunicBlade blade;
    @Before public void setup(){
        HeadlessTestSupport.start();
        priorLevel=Dungeon.level;priorHero=Dungeon.hero;
        level=new Flat(); level.setSize(35,35);
        Arrays.fill(level.map,Terrain.EMPTY);
        Arrays.fill(level.passable,true);Arrays.fill(level.visited,true);
        for(int i=0;i<1225;i++) if(i/35==0||i/35==34||i%35==0||i%35==34)level.passable[i]=false;
        PathFinder.setMapSize(35,35);
        Dungeon.level=level;
        hero=new Hero();hero.heroClass=HeroClass.DUELIST;Talent.initClassTalents(hero);hero.pos=15*35+8;hero.HT=hero.HP=30;
        Dungeon.hero=hero;
        blade=new RunicBlade();blade.level(6);blade.enchant(new Projecting());
        hero.belongings.abilityWeapon=blade;
    }
    @After public void cleanup(){
        hero.belongings.abilityWeapon=null;
        Dungeon.hero=priorHero;Dungeon.level=priorLevel;
        if(priorLevel!=null)PathFinder.setMapSize(priorLevel.width(),priorLevel.height());
    }
    @Test public void normalProjectingHasFiniteTwoTileReach() {
        assertEquals(2,blade.reachFactor(hero));
        assertTrue(blade.canReach(hero,hero.pos+2));
        assertFalse(blade.canReach(hero,hero.pos+3));
    }
    @Test public void plusSixSlashReachesEightTilesNotNineAndPreviewDoesNotConsume() {
        RunicBlade.RunicSlashTracker boost=Buff.affect(hero,RunicBlade.RunicSlashTracker.class);
        boost.boost=blade.runicSlashEnchantBoost();
        assertEquals(6f,boost.boost,0);
        for(int i=0;i<6;i++){
            assertEquals(8,blade.reachFactor(hero));
            assertTrue(blade.canReach(hero,hero.pos+8));
            assertFalse(blade.canReach(hero,hero.pos+9));
            assertSame("A reach calculation must not spend the actual attack's buff",
                boost,hero.buff(RunicBlade.RunicSlashTracker.class));
        }
        assertEquals(7f,Weapon.Enchantment.genericProcChanceMultiplier(hero),0.0001f);
        assertNull(hero.buff(RunicBlade.RunicSlashTracker.class));
        assertEquals(2,blade.reachFactor(hero));
    }
    @Test public void arcanaLevelsAdjustRunicRangeButNeverBypassItsBoundary() {
        for (int ringLevel:new int[]{0,2,8}) {
            RingOfArcana ring=new RingOfArcana();
            ring.level(ringLevel);ring.activate(hero);
            float power=RingOfArcana.enchantPowerMultiplier(hero);
            assertEquals(1+Projecting.additionalReach(power,1),blade.reachFactor(hero));
            RunicBlade.RunicSlashTracker tracker=Buff.affect(hero,RunicBlade.RunicSlashTracker.class);
            tracker.boost=blade.runicSlashEnchantBoost();
            int intended=1+Projecting.additionalReach(power+tracker.boost,1);
            assertEquals(intended,blade.reachFactor(hero));
            assertTrue(blade.canReach(hero,hero.pos+intended));
            assertFalse(blade.canReach(hero,hero.pos+intended+1));
            assertSame(tracker,hero.buff(RunicBlade.RunicSlashTracker.class));
            tracker.detach();
            Buff.detach(hero,RingOfArcana.Arcana.class);
        }
    }
    @Test public void finiteLevelCapsPowerOnlyBeyondAnyPhysicallyReachableTile() {
        int possible=level.length()-1;
        assertEquals(possible-1,Projecting.additionalReach(Float.POSITIVE_INFINITY,1));
        assertEquals(7,Projecting.additionalReach(7f,1));
        assertEquals(0,Projecting.additionalReach(Float.NaN,1));
    }
}
