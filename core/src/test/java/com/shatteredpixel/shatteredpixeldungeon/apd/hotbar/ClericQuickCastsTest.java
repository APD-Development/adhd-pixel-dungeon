package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class ClericQuickCastsTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private Preferences oldPrefs;
    private Hero oldHero, hero;
    private File prefsFile;
    private TestTome tome;
    private static class TestTome extends HolyTome { int currentCharge() { return charge; } }
    @Before public void setup() throws Exception {
        HeadlessTestSupport.start();
        Field prefs=GameSettings.class.getDeclaredField("prefs"); prefs.setAccessible(true);
        oldPrefs=(Preferences)prefs.get(null);
        prefsFile=new File(temp.newFolder(),"preferences.xml");
        GameSettings.set(new HeadlessPreferences(new FileHandle(prefsFile)));
        oldHero=Dungeon.hero; hero=new Hero(); hero.heroClass=HeroClass.CLERIC;
        Talent.initClassTalents(hero); hero.ready=true; Dungeon.hero=hero;
        tome=new TestTome(); hero.belongings.artifact=tome;
    }
    @After public void cleanup() { GameSettings.set(oldPrefs); Dungeon.hero=oldHero; }

    @Test public void whiteQuickCastSlotsAreOnlyShownForCleric() {
        for(HeroClass type:HeroClass.values()) { hero.heroClass=type;assertEquals(type==HeroClass.CLERIC?SPDSettings.apdClericQuickSlots():0,com.shatteredpixel.shatteredpixeldungeon.ui.ClericQuickCastTag.count()); }
    }
    @Test public void countDefaultsToOneAndClampsToFour() {
        assertEquals(1,SPDSettings.apdClericQuickSlots());
        SPDSettings.apdClericQuickSlots(99); assertEquals(4,SPDSettings.apdClericQuickSlots());
        SPDSettings.apdClericQuickSlots(-1); assertEquals(1,SPDSettings.apdClericQuickSlots());
    }
    @Test public void fourAssignmentsReplaceAndRemoveIndependently() {
        SPDSettings.apdClericQuickSlots(4);
        ClericQuickCasts.assign(0,GuidingLight.INSTANCE,tome);
        ClericQuickCasts.assign(1,HolyWeapon.INSTANCE,tome);
        ClericQuickCasts.assign(2,HolyWard.INSTANCE,tome);
        ClericQuickCasts.assign(3,Sunray.INSTANCE,tome);
        ClericQuickCasts.assign(1,HolyWard.INSTANCE,tome);
        ClericQuickCasts.assign(2,null,tome);
        assertSame(GuidingLight.INSTANCE,ClericQuickCasts.saved(0,tome));
        assertSame(HolyWard.INSTANCE,ClericQuickCasts.saved(1,tome));
        assertNull(ClericQuickCasts.saved(2,tome));
        assertSame(Sunray.INSTANCE,ClericQuickCasts.saved(3,tome));
    }
    @Test public void preferencesSurviveRestartAndNewRun() {
        SPDSettings.apdClericQuickSlots(4);
        ClericQuickCasts.assign(2,HolyWeapon.INSTANCE,tome);
        GameSettings.set(new HeadlessPreferences(new FileHandle(prefsFile)));
        assertEquals(4,SPDSettings.apdClericQuickSlots());
        assertSame(HolyWeapon.INSTANCE,ClericQuickCasts.saved(2,new HolyTome()));
    }
    @Test public void decreasingCountPreservesHiddenAssignments() {
        SPDSettings.apdClericQuickSlots(4); ClericQuickCasts.assign(3,HolyWard.INSTANCE,tome);
        SPDSettings.apdClericQuickSlots(1); assertNull(ClericQuickCasts.available(hero,tome,3));
        SPDSettings.apdClericQuickSlots(4); assertSame(HolyWard.INSTANCE,ClericQuickCasts.available(hero,tome,3));
    }
    @Test public void lockedTalentSpellIsRetainedUntilUnlocked() {
        ClericQuickCasts.assign(0,Sunray.INSTANCE,tome);
        assertNull(ClericQuickCasts.available(hero,tome,0));
        float charge=tome.currentCharge(); assertFalse(ClericQuickCasts.cast(hero,tome,0));
        assertEquals(charge,tome.currentCharge(),0);
        hero.talents.get(1).put(Talent.SUNRAY,1);
        assertSame(Sunray.INSTANCE,ClericQuickCasts.available(hero,tome,0));
        assertSame(Sunray.INSTANCE,ClericQuickCasts.saved(0,new HolyTome()));
    }
    @Test public void unknownFuturePreferenceIsNotInstantiatedOrDeleted() {
        SPDSettings.apdClericQuickSpell(0,"future.community.Spell");
        assertNull(ClericQuickCasts.saved(0,tome));
        assertEquals("future.community.Spell",SPDSettings.apdClericQuickSpell(0));
    }
    @Test public void olderSingleQuickSpellBundleStaysCompatible() {
        tome.setQuickSpell(GuidingLight.INSTANCE);
        Bundle bundle=new Bundle(); tome.storeInBundle(bundle);
        HolyTome restored=new HolyTome(); restored.restoreFromBundle(bundle);
        assertSame(GuidingLight.INSTANCE,restored.quickSpell());
        assertSame(GuidingLight.INSTANCE,ClericQuickCasts.saved(0,restored));
        ClericQuickCasts.assign(0,null,restored);
        assertNull(ClericQuickCasts.saved(0,restored)); assertNull(restored.quickSpell());
    }
    @Test public void assigningDoesNotCastSpendTurnOrConsumeRng() {
        float charge=tome.currentCharge(), cooldown=hero.cooldown(); int hp=hero.HP;
        Random.pushGenerator(101); long expected;
        try { expected=Random.Long(); } finally { Random.popGenerator(); }
        Random.pushGenerator(101);
        try { ClericQuickCasts.assign(0,HolyWeapon.INSTANCE,tome); assertEquals(expected,Random.Long()); }
        finally { Random.popGenerator(); }
        assertEquals(charge,tome.currentCharge(),0); assertEquals(cooldown,hero.cooldown(),0);
        assertEquals(hp,hero.HP); assertTrue(hero.ready);
        assertNull(hero.buff(HolyWeapon.HolyWepBuff.class));
    }
    @Test public void unavailableCasterDoesNotSpendResources() {
        ClericQuickCasts.assign(0,HolyWeapon.INSTANCE,tome); float charge=tome.currentCharge();
        hero.ready=false; assertFalse(ClericQuickCasts.cast(hero,tome,0)); hero.ready=true;
        assertFalse(ClericQuickCasts.cast(new Hero(),tome,0));
        hero.belongings.artifact=null; assertFalse(ClericQuickCasts.cast(hero,tome,0));
        assertEquals(charge,tome.currentCharge(),0);
    }
    @Test public void cursedTomeAndInsufficientChargeRejectNormalCast() {
        ClericQuickCasts.assign(0,HolyWeapon.INSTANCE,tome); float charge=tome.currentCharge();
        tome.cursed=true; assertFalse(ClericQuickCasts.cast(hero,tome,0));
        assertEquals(charge,tome.currentCharge(),0); tome.cursed=false;
        tome.spendCharge(charge); assertFalse(ClericQuickCasts.cast(hero,tome,0));
        assertEquals(0,tome.currentCharge(),0);
    }
    @Test public void restoredFirstPreferenceDoesNotUnlockAnUnavailableSpell() {
        ClericQuickCasts.assign(0,Sunray.INSTANCE,tome);
        HolyTome fresh=new HolyTome(); ClericQuickCasts.restoreFirst(hero,fresh);
        assertNull(fresh.quickSpell()); assertSame(Sunray.INSTANCE,ClericQuickCasts.saved(0,fresh));
    }
    @Test(expected=IllegalArgumentException.class) public void fifthPositionIsRejected() {
        ClericQuickCasts.assign(4,GuidingLight.INSTANCE,tome);
    }
}
