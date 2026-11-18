package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TormentedSpirit;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import static org.junit.Assert.*;

/** Player report: visible Wraiths not in Actor.chars cannot be attacked,
 * burned, bombed, thrown at or examined; Dread still sees Dungeon.level.mobs.
 */
public class WraithActorRegistrationTest {
    private static final class Empty extends Level {
        @Override protected boolean build(){return true;}
        @Override protected void createMobs(){}
        @Override protected void createItems(){}
    }
    private Level beforeLevel;
    private Hero beforeHero;
    private Empty level;
    private Hero hero;

    @Before public void setup() {
        HeadlessTestSupport.start();
        beforeLevel=Dungeon.level;
        beforeHero=Dungeon.hero;
        Actor.clear();
        level=new Empty();
        level.setSize(5,5);
        level.mobs=new HashSet<>();
        Arrays.fill(level.map,Terrain.EMPTY);
        Arrays.fill(level.passable,true);
        Arrays.fill(level.visited,true);
        level.heroFOV=new boolean[level.length()];
        Arrays.fill(level.heroFOV,true);
        hero=new Hero();
        hero.heroClass=HeroClass.WARRIOR;
        hero.pos=12;
        Dungeon.level=level;
        Dungeon.hero=hero;
        Actor.add(hero);
    }

    @After public void cleanup() {
        Actor.clear();
        Dungeon.level=beforeLevel;
        Dungeon.hero=beforeHero;
    }

    private Wraith orphanWraith(int pos) {
        Wraith wraith=new Wraith();
        wraith.adjustStats(9);
        wraith.pos=pos;
        level.mobs.add(wraith);
        return wraith;
    }

    @Test public void unregisteredWraithIsReattachedWithoutChangingStatsOrHealth() {
        Wraith w=orphanWraith(13);
        int hp=w.HP,ht=w.HT,defense=w.defenseSkill,position=w.pos;
        int id=w.apdPeekId();
        assertFalse(Actor.chars().contains(w));
        assertSame(w,Actor.repairMissingWraithAt(13,true));
        assertSame(w,Actor.findChar(13));
        assertTrue(Actor.chars().contains(w));
        assertSame(w,Actor.findById(w.id()));
        assertEquals(hp,w.HP);
        assertEquals(ht,w.HT);
        assertEquals(defense,w.defenseSkill);
        assertEquals(position,w.pos);
        assertEquals(1,level.mobs.size());
        assertTrue("Registration may assign a single actor ID but no new mob",id==0||id==w.id());
    }

    @Test public void alreadyRegisteredWraithDoesNotDuplicateOrResetCooldown() {
        Wraith w=orphanWraith(13);
        assertSame(w,Actor.repairMissingWraithAt(13,true));
        int id=w.id();
        float cooldown=w.cooldown();
        int actorCount=Actor.chars().size();
        for(int i=0;i<5;i++)assertSame(w,Actor.repairMissingWraithAt(13,true));
        assertEquals(actorCount,Actor.chars().size());
        assertEquals(id,w.id());
        assertEquals(cooldown,w.cooldown(),0.0001f);
    }

    @Test public void alternateTormentedSpiritAlsoReturnsToCombatAndInspection() {
        TormentedSpirit spirit=new TormentedSpirit();
        spirit.adjustStats(10);
        spirit.pos=13;
        level.mobs.add(spirit);
        assertSame(spirit,Actor.repairMissingWraithAt(13,true));
        assertSame(spirit,Actor.findChar(13));
        assertTrue(spirit.isAlive());
    }

    @Test public void falseSceneOrOffLevelOrDeadWraithIsNeverRevived() {
        Wraith w=orphanWraith(13);
        assertNull(Actor.repairMissingWraithAt(13,false));
        assertFalse(Actor.chars().contains(w));
        assertNull(Actor.repairMissingWraithAt(-1,true));
        assertNull(Actor.repairMissingWraithAt(level.length(),true));
        w.HP=0;
        assertNull(Actor.repairMissingWraithAt(13,true));
        assertFalse(Actor.chars().contains(w));
    }

    @Test public void repairNeverStealsSpaceFromRealActorsOrUnrelatedMobs() {
        Wraith w=orphanWraith(13);
        Rat rat=new Rat();
        rat.pos=13;
        Actor.add(rat);
        assertSame(rat,Actor.repairMissingWraithAt(13,true));
        assertFalse(Actor.chars().contains(w));
        assertSame(rat,Actor.findChar(13));

        Rat unrelated=new Rat();
        unrelated.pos=14;
        level.mobs.add(unrelated);
        assertNull(Actor.repairMissingWraithAt(14,true));
        assertFalse(Actor.chars().contains(unrelated));
    }

    @Test public void wraithRemainsDamageableByOrdinaryDamageSourcesAfterRepair() {
        Wraith w=orphanWraith(13);
        assertSame(w,Actor.repairMissingWraithAt(13,true));
        assertFalse(w.isInvulnerable(Rat.class));
        assertFalse(w.isInvulnerable(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class));
        assertEquals(1,w.HT);
        assertTrue(Actor.chars().contains(w));
    }
}
