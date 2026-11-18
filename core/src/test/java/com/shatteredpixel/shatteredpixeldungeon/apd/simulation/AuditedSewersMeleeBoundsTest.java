package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.watabou.utils.Random;
import org.junit.Test;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.Assert.*;

public class AuditedSewersMeleeBoundsTest {
    @Test public void optionalStatuesUseActualWeaponAndAugmentWithoutRollingDamage() throws Exception {
        HeadlessTestSupport.start();
        Field equipped=Statue.class.getDeclaredField("weapon"); equipped.setAccessible(true);
        for (Statue statue : new Statue[]{new Statue(),new ArmoredStatue()})
            for (MeleeWeapon weapon : new MeleeWeapon[]{new Sword(),new Dagger(),new Dirk(),new AssassinsBlade(),new Flail()})
                for (Weapon.Augment augment : Weapon.Augment.values()) {
                    weapon.level(2); weapon.augment=augment; equipped.set(statue,weapon);
                    Random.pushGenerator(91212); int expected;
                    try { expected=Random.Int(); } finally { Random.popGenerator(); }
                    Random.pushGenerator(91212);
                    int maximum;
                    try { maximum=AuditedSewersMeleeBounds.maximum(statue,2); assertEquals(expected,Random.Int()); }
                    finally { Random.popGenerator(); }
                    Random.pushGenerator(94102);
                    try { for (int i=0;i<100;i++) assertTrue(statue.damageRoll()<=maximum); }
                    finally { Random.popGenerator(); }
                }
    }
    @Test public void ebonyMimicSurpriseDamageIsNotMistakenForNormalDamage() {
        EbonyMimic mimic=new EbonyMimic(); mimic.setLevel(3);
        int surprise=AuditedSewersMeleeBounds.maximum(mimic,1);
        assertEquals(surprise,mimic.damageRoll());
        mimic.alignment=Char.Alignment.ENEMY;
        assertEquals(surprise/2,AuditedSewersMeleeBounds.maximum(mimic,1));
    }
    @Test public void everyStandardAndRareEarlySewersRotationHasAnAuditedBound() throws Exception {
        Method rotation = MobSpawner.class.getDeclaredMethod("standardMobRotation", int.class);
        rotation.setAccessible(true);
        for (int depth=1; depth<=4; depth++) {
            @SuppressWarnings("unchecked") List<Class<? extends Mob>> types = (List<Class<? extends Mob>>) rotation.invoke(null,depth);
            for (Class<? extends Mob> type : types) {
                assertTrue(AuditedSewersMeleeBounds.maximum(type.getDeclaredConstructor().newInstance(),depth)>0);
                Class<? extends Mob> rare = MobSpawner.RARE_ALTS.get(type);
                if (rare != null) assertTrue(AuditedSewersMeleeBounds.maximum(rare.getDeclaredConstructor().newInstance(),depth)>0);
            }
        }
        // addRareMobs can add a Thief on floor4; its alternate is a Bandit.
        assertEquals(10,AuditedSewersMeleeBounds.maximum(new Thief(),4));
        assertEquals(10,AuditedSewersMeleeBounds.maximum(new Bandit(),4));
        assertEquals(Piranha.class,PhantomPiranha.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Crab.class,HermitCrab.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Slime.class,CausticSlime.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Thief.class,Bandit.class.getMethod("damageRoll").getDeclaringClass());
    }
    @Test public void inheritedRareEnemyRollsUseAuditedParentWithoutGuessingUnknownSubclasses() throws Exception {
        assertEquals(Rat.class, Albino.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Rat.class, FetidRat.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Gnoll.class, GnollTrickster.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Crab.class, GreatCrab.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(4, AuditedSewersMeleeBounds.maximum(new Albino(), 1));
        assertEquals(4, AuditedSewersMeleeBounds.maximum(new FetidRat(), 2));
        assertEquals(6, AuditedSewersMeleeBounds.maximum(new GnollTrickster(), 3));
        assertEquals(10, AuditedSewersMeleeBounds.maximum(new GnollExile(), 3));
        assertEquals(GnollExile.class, GnollExile.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(7, AuditedSewersMeleeBounds.maximum(new GreatCrab(), 4));
        assertEquals(0, AuditedSewersMeleeBounds.maximum(null, 1));
        try { AuditedSewersMeleeBounds.maximum(new Rat(){}, 1); fail("unknown subclass accepted"); }
        catch (IllegalStateException expected) { }
    }
    @Test public void actualWraithRollBoundsTrackItsStoredLevelRatherThanCurrentFloor() {
        for (int level : new int[]{1, 2, 4, 10, 25}) {
          for (Wraith wraith : new Wraith[]{new Wraith(),new TormentedSpirit()}) {
            wraith.adjustStats(level);
            int maximum = AuditedSewersMeleeBounds.maximum(wraith, 2);
            int scaled = wraith instanceof TormentedSpirit ? Math.round(1.5f*level) : level;
            assertEquals(scaled + 2, maximum);
            Random.pushGenerator(8301 + level);
            try {
                for (int i = 0; i < 1000; i++) {
                    int roll = wraith.damageRoll();
                    assertTrue(roll >= 1 + scaled / 2 && roll <= maximum);
                }
            } finally { Random.popGenerator(); }
          }
        }
    }
    @Test public void observingBoundsDoesNotRollDamageOrAdvanceRandomGenerator() {
        Wraith wraith = new Wraith(); wraith.adjustStats(3);
        Mob[] mobs = {new Rat(), new Albino(), new Snake(), new Swarm(), new Gnoll(), new GnollExile(), new Crab(), new Piranha(), wraith, new Mimic()};
        Random.pushGenerator(83219);
        int expected;
        try { expected = Random.Int(); } finally { Random.popGenerator(); }
        Random.pushGenerator(83219);
        try {
            for (Mob mob : mobs) AuditedSewersMeleeBounds.maximum(mob, 3);
            assertEquals(expected, Random.Int());
        } finally { Random.popGenerator(); }
    }
    @Test public void mimicVariantsUseActualStoredLevelAndInheritedRolls() throws Exception {
        assertEquals(Mimic.class, GoldenMimic.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(CrystalMimic.class, CrystalMimic.class.getMethod("damageRoll").getDeclaringClass());
        assertEquals(Mimic.class, GoldenMimic.class.getMethod("attackSkill", Char.class).getDeclaringClass());
        assertEquals(Mimic.class, CrystalMimic.class.getMethod("attackSkill", Char.class).getDeclaringClass());
        for (Mimic mimic : new Mimic[]{new Mimic(),new GoldenMimic(),new CrystalMimic()}) {
            for (int level : new int[]{1,4,10}) {
                mimic.setLevel(level);
                int maximum = AuditedSewersMeleeBounds.maximum(mimic, 1);
                mimic.alignment = Char.Alignment.NEUTRAL;
                int neutral = mimic.damageRoll();
                if (mimic instanceof CrystalMimic) assertTrue(neutral <= maximum);
                else assertEquals("neutral roll is the exact maximum", maximum, neutral);
                assertEquals(Char.Alignment.NEUTRAL, mimic.alignment);
                mimic.alignment = Char.Alignment.ENEMY;
                Random.pushGenerator(1910 + level);
                try {
                    for (int i=0; i<1000; i++) assertTrue(mimic.damageRoll() <= maximum);
                } finally { Random.popGenerator(); }
            }
        }
    }
}
