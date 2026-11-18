package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Signal;
import org.junit.*;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;

public class ClassUnlockNotificationTest {
    private Object oldGlobal, oldDirty;
    private Hero oldHero;
    private String oldVersion, oldSeed;
    private int oldUpgrades, oldSneaks, oldThrown;
    private Signal<String> oldLog;
    private final List<String> messages = new ArrayList<>();
    private Field global, dirty;
    private static final Badges.Badge[] UNLOCKS = {Badges.Badge.UNLOCK_MAGE,
        Badges.Badge.UNLOCK_ROGUE, Badges.Badge.UNLOCK_HUNTRESS,
        Badges.Badge.UNLOCK_DUELIST, Badges.Badge.UNLOCK_CLERIC};

    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        global=Badges.class.getDeclaredField("global");global.setAccessible(true);
        dirty=Badges.class.getDeclaredField("saveNeeded");dirty.setAccessible(true);
        oldGlobal=global.get(null);oldDirty=dirty.get(null);
        global.set(null,new HashSet<Badges.Badge>());
        oldVersion=Game.version;Game.version="4.0.8-apd-INDEV-bC262-TEST";
        oldSeed=Dungeon.customSeedText;Dungeon.customSeedText="";
        oldHero=Dungeon.hero;Dungeon.hero=new Hero();
        oldUpgrades=Statistics.upgradesUsed;oldSneaks=Statistics.sneakAttacks;
        oldThrown=Statistics.thrownAttacks;
        oldLog=GLog.update;GLog.update=new Signal<>();
        GLog.update.add(s->{messages.add(s);return false;});
    }
    @After public void restore() throws Exception {
        global.set(null,oldGlobal);dirty.set(null,oldDirty);
        Dungeon.hero=oldHero;Game.version=oldVersion;Dungeon.customSeedText=oldSeed;
        Statistics.upgradesUsed=oldUpgrades;Statistics.sneakAttacks=oldSneaks;
        Statistics.thrownAttacks=oldThrown;GLog.update=oldLog;
    }
    private void meetAllRequirements(boolean identified) {
        Statistics.upgradesUsed=1;Statistics.sneakAttacks=10;Statistics.thrownAttacks=10;
        Shortsword weapon=new Shortsword();weapon.level(2);
        weapon.levelKnown=identified;weapon.cursedKnown=identified;
        Dungeon.hero.STR=20;Dungeon.hero.belongings.weapon=weapon;
        Badges.validateMageUnlock();Badges.validateRogueUnlock();Badges.validateHuntressUnlock();
        Badges.validateDuelistUnlock();Badges.validateClericUnlock();
    }
    @Test public void freshAlphaPlayersKeepAllSixClassesWithoutUnlockAnnouncements() {
        assertEquals(6,HeroClass.values().length);
        for(HeroClass cls:HeroClass.values())assertTrue(cls.name(),cls.isUnlocked());
        meetAllRequirements(true);
        assertTrue("no log notification or badge popup path",messages.isEmpty());
        for(Badges.Badge badge:UNLOCKS)assertTrue("progression retained: "+badge,Badges.isUnlocked(badge));
        for(HeroClass cls:HeroClass.values())assertTrue(cls.isUnlocked());
    }
    @Test public void unidentifiedWeaponAndReloadedProgressRemainQuiet() throws Exception {
        meetAllRequirements(false);
        for(Badges.Badge badge:UNLOCKS)assertTrue(badge.name(),Badges.isUnlocked(badge));
        Bundle saved=new Bundle();Badges.store(saved,new HashSet<>(Badges.allUnlocked()));
        global.set(null,Badges.restore(saved));
        meetAllRequirements(true);meetAllRequirements(false);
        assertTrue(messages.isEmpty());
        for(HeroClass cls:HeroClass.values())assertTrue(cls.isUnlocked());
    }
    @Test public void futureLockedPolicyAndUnrelatedBadgesRemainAvailable() throws Exception {
        Game.version="4.0.8-apd";
        assertTrue(HeroClass.WARRIOR.isUnlocked());
        for(Badges.Badge badge:UNLOCKS) {
            HeroClass cls=Badges.classForUnlockBadge(badge);
            assertNotNull(cls);assertFalse(cls.isUnlocked());
            Badges.unlock(badge);assertTrue(cls.isUnlocked());
        }
        assertNull(Badges.classForUnlockBadge(Badges.Badge.MONSTERS_SLAIN_1));
        assertFalse(Badges.isUnlocked(Badges.Badge.MONSTERS_SLAIN_1));
    }
    @Test public void seededRunsStillDoNotAwardUnlockBadges() {
        Dungeon.customSeedText="test seed";meetAllRequirements(true);
        for(Badges.Badge badge:UNLOCKS)assertFalse(Badges.isUnlocked(badge));
        assertTrue(messages.isEmpty());
    }
    @Test public void actuallyLockedClassesStillAnnounceTheirOriginalUnlocks() {
        Game.version="4.0.8-apd";
        meetAllRequirements(true);
        assertEquals(5,messages.stream().filter(s->s.startsWith(GLog.HIGHLIGHT)).count());
        for(Badges.Badge badge:UNLOCKS)assertTrue(Badges.isUnlocked(badge));
        for(HeroClass cls:HeroClass.values())assertTrue(cls.isUnlocked());
    }
}
