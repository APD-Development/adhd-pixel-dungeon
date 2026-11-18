package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.*;
import com.watabou.utils.Random;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class NormalWarriorPreparationTest {
    private Hero old,hero;
    @Before public void start() {
        HeadlessTestSupport.start(); old=Dungeon.hero;
        Dungeon.hero=hero=new Hero(); hero.heroClass=HeroClass.WARRIOR; hero.STR=12;
        Talent.initClassTalents(hero);
        hero.belongings.weapon=(MeleeWeapon)new WornShortsword().identify(false);
        hero.belongings.armor=(Armor)new ClothArmor().identify();
    }
    @After public void restore() { Dungeon.hero=old; }
    @Test public void noTalentBeforeLevelUpAndNeverOverfillsATalent() {
        assertNull(NormalWarriorPreparation.talent(hero));
        hero.lvl=2; assertEquals(Talent.IRON_WILL,NormalWarriorPreparation.talent(hero));
        hero.talents.get(0).put(Talent.IRON_WILL,2);
        hero.lvl=4; assertEquals(Talent.HEARTY_MEAL,NormalWarriorPreparation.talent(hero));
        hero.lvl=2; assertNull(NormalWarriorPreparation.talent(hero));
    }
    @Test public void hiddenEquipmentAndHiddenUpgradesNeverDriveSelection() {
        Sword hidden=new Sword(); hidden.level(20); hidden.levelKnown=false; hidden.cursedKnown=false;
        hero.belongings.backpack.items.add(hidden);
        PlateArmor hiddenArmor=new PlateArmor(); hiddenArmor.level(20); hiddenArmor.levelKnown=false;
        hero.belongings.backpack.items.add(hiddenArmor);
        assertNull(NormalWarriorPreparation.weapon(hero));
        assertNull(NormalWarriorPreparation.armor(hero));
    }
    @Test public void knownUsableUpgradesAreSelectedButHeavyAndCursedItemsAreNot() {
        Sword sword=new Sword(); sword.identify(); hero.belongings.backpack.items.add(sword);
        assertNull(NormalWarriorPreparation.weapon(hero));
        hero.STR=20; assertSame(sword,NormalWarriorPreparation.weapon(hero));
        sword.cursed=true; assertNull(NormalWarriorPreparation.weapon(hero));
        LeatherArmor armor=new LeatherArmor(); armor.identify(); hero.belongings.backpack.items.add(armor);
        assertSame(armor,NormalWarriorPreparation.armor(hero));
        armor.cursed=true; assertNull(NormalWarriorPreparation.armor(hero));
    }
    @Test public void equippedCursesPreventImpossibleReplacement() {
        Sword sword=new Sword(); sword.identify(); hero.STR=20;
        hero.belongings.backpack.items.add(sword); hero.belongings.weapon.cursed=true;
        assertNull(NormalWarriorPreparation.weapon(hero));
        LeatherArmor armor=new LeatherArmor(); armor.identify(); hero.belongings.backpack.items.add(armor);
        hero.belongings.armor.cursed=true; assertNull(NormalWarriorPreparation.armor(hero));
    }
    @Test public void throwablesRespectActualKnownStrengthAndRetainInventory() {
        ThrowingSpike spike=new ThrowingSpike(); spike.identify();
        hero.belongings.backpack.items.add(spike);
        hero.STR=1; assertNull(NormalWarriorPreparation.throwable(hero));
        hero.STR=20; assertSame(spike,NormalWarriorPreparation.throwable(hero));
        assertTrue(hero.belongings.backpack.items.contains(spike));
    }
    @Test public void selectionDoesNotSpendTurnsConsumeItemsHealOrRollRng() {
        hero.lvl=2; int hp=hero.HP, count=hero.belongings.backpack.items.size(); float time=Actor.now();
        Random.pushGenerator(76202); int expected;
        try { expected=Random.Int(); } finally { Random.popGenerator(); }
        Random.pushGenerator(76202);
        try {
            NormalWarriorPreparation.talent(hero); NormalWarriorPreparation.weapon(hero);
            NormalWarriorPreparation.armor(hero); NormalWarriorPreparation.throwable(hero);
            assertEquals(expected,Random.Int());
        } finally { Random.popGenerator(); }
        assertEquals(hp,hero.HP); assertEquals(count,hero.belongings.backpack.items.size());
        assertEquals(time,Actor.now(),0f); assertEquals(0,hero.pointsInTalent(Talent.IRON_WILL));
    }
}
