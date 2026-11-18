package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArcanaInfoDisplayTest {
    private Hero previous;
    private Sword sword;
    private ClothArmor armor;
    @Before public void start() {
        HeadlessTestSupport.start(); Messages.setup(Languages.ENGLISH);
        com.shatteredpixel.shatteredpixeldungeon.journal.Notes.reset();
        previous=Dungeon.hero; Dungeon.hero=new Hero(); Dungeon.hero.HP=10; Dungeon.hero.HT=20;
        sword=new Sword(); sword.enchant(new Blazing()); sword.levelKnown=true;
        armor=new ClothArmor(); armor.inscribe(new Flow()); armor.levelKnown=true;
    }
    @After public void finish() { Dungeon.hero=previous; }
    private RingOfArcana ring(int level) {
        RingOfArcana r=new RingOfArcana(); r.anonymize(); r.level(level);
        r.levelKnown=true; r.cursedKnown=true; return r;
    }
    private void equip(int level) { Dungeon.hero.belongings.ring=ring(level); }
    @Test public void absentAndBackpackRingsShowNothing() {
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
        Dungeon.hero.belongings.backpack.items.add(ring(8));
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
    }
    @Test public void unknownIdentityLevelOrCurseNeverShowsValues() {
        Dungeon.hero.belongings.ring=new RingOfArcana();
        assertEquals("",ArcanaInfoDisplay.weapon(sword));
        equip(8); Dungeon.hero.belongings.ring.levelKnown=false;
        assertEquals("",ArcanaInfoDisplay.weapon(sword));
        Dungeon.hero.belongings.ring.levelKnown=true; Dungeon.hero.belongings.ring.cursedKnown=false;
        assertEquals("",ArcanaInfoDisplay.armor(armor));
    }
    @Test public void unknownSecondRingSuppressesCombinedTotals() {
        equip(2); Dungeon.hero.belongings.misc=ring(8); Dungeon.hero.belongings.misc.levelKnown=false;
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
    }
    @Test public void unknownOtherRingCannotBeDistinguishedFromUnknownArcana() {
        equip(2); Dungeon.hero.belongings.misc=new com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste();
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
    }
    @Test public void inactivePassiveGlyphDoesNotClaimABoost() {
        equip(0); armor.level(-1);
        assertTrue(ArcanaInfoDisplay.armor(armor).contains("does nothing"));
    }
    @Test public void identifiedRingShowsCalculatedChanceAndMovementEffect() {
        equip(0);
        String w=ArcanaInfoDisplay.weapon(sword), a=ArcanaInfoDisplay.armor(armor);
        assertTrue(w,w.contains("+17.5%_ power")); assertTrue(w,w.contains("_39.17%_"));
        assertTrue(a,a.contains("_2.35x_ normal")); assertTrue(a,a.contains("Only while in water"));
    }
    @Test public void combinedAndEnhancedRingsUseTheirKnownEffectiveLevels() {
        equip(0); Dungeon.hero.belongings.misc=ring(0);
        assertTrue(ArcanaInfoDisplay.weapon(sword).contains("+38.06%_ power"));
        Buff.affect(Dungeon.hero,EnhancedRings.class);
        assertTrue(ArcanaInfoDisplay.weapon(sword).contains("+90.61%_ power"));
    }
    @Test public void unknownEquipmentUpgradesDoNotLeak() {
        equip(2); sword.levelKnown=false; armor.levelKnown=false;
        String w=ArcanaInfoDisplay.weapon(sword),a=ArcanaInfoDisplay.armor(armor);
        sword.level(9); armor.level(9);
        assertEquals(w,ArcanaInfoDisplay.weapon(sword)); assertEquals(a,ArcanaInfoDisplay.armor(armor));
        assertTrue(w.contains("Identify this item")); assertTrue(a.contains("Identify this item"));
    }
    @Test public void descriptionsAreWiredForMeleeMissileAndArmor() {
        equip(0); assertTrue(sword.info(),sword.info().contains("39.17%"));
        HeavyBoomerang missile=new HeavyBoomerang(); missile.enchant(new Blazing());missile.levelKnown=true;
        assertTrue(missile.info(),missile.info().contains("39.17%")); assertTrue(armor.info().contains("Move"));
        Dungeon.hero.belongings.ring=null;
        assertTrue(sword.info(),sword.info().contains("33.33%")); assertTrue(armor.info(),armor.info().contains("2×"));
    }
    @Test public void magicImmunityAndMissingEnchantmentsShowNothing() {
        equip(0); Buff.affect(Dungeon.hero,MagicImmune.class);
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
        Buff.detach(Dungeon.hero,MagicImmune.class); sword.enchant(null); armor.inscribe(null);
        assertEquals("",ArcanaInfoDisplay.weapon(sword)); assertEquals("",ArcanaInfoDisplay.armor(armor));
    }
    @Test public void descriptionsLeaveRngCombatBuffsAndResourcesUntouched() {
        equip(2); Buff.affect(Dungeon.hero,RunicBlade.RunicSlashTracker.class).boost=4;
        int hp=Dungeon.hero.HP,qty=sword.quantity(); float time=Dungeon.hero.cooldown();
        Random.pushGenerator(7737); int expected; try {expected=Random.Int();} finally {Random.popGenerator();}
        Random.pushGenerator(7737);
        try {
            for(int i=0;i<100;i++){ArcanaInfoDisplay.weapon(sword);ArcanaInfoDisplay.armor(armor);}
            assertEquals(expected,Random.Int());
        } finally {Random.popGenerator();}
        assertEquals(4,Dungeon.hero.buff(RunicBlade.RunicSlashTracker.class).boost,0);
        assertEquals(hp,Dungeon.hero.HP); assertEquals(qty,sword.quantity()); assertEquals(time,Dungeon.hero.cooldown(),0);
    }
    @Test public void highPowerChancesSaturateButKineticStillShowsScaling() {
        equip(36); sword.level(12);
        String s=ArcanaInfoDisplay.weapon(sword); assertTrue(s,s.contains("100%")); assertFalse(s,s.contains("33826"));
        sword.enchant(new Kinetic()); assertTrue(ArcanaInfoDisplay.weapon(sword).contains("overkill damage"));
    }
    @Test public void passiveAndHpDependentDescriptionsStayCompactAndQualified() {
        equip(2); sword.enchant(new Vampiric());
        assertTrue(ArcanaInfoDisplay.weapon(sword).contains("current HP"));
        armor.inscribe(new Stone()); assertTrue(ArcanaInfoDisplay.armor(armor).contains("Arcana raises the evasion"));
        armor.inscribe(new Brimstone()); assertTrue(ArcanaInfoDisplay.armor(armor).contains("already immune to fire"));
        armor.inscribe(new Viscosity()); assertTrue(ArcanaInfoDisplay.armor(armor).contains("part of it is prevented"));
        assertTrue(ArcanaInfoDisplay.armor(armor).length()<250);
    }
    @Test public void everyBuiltInEnchantmentGlyphAndCurseHasResolvableText() throws Exception {
        equip(3);
        for (Class<?>[] types : new Class<?>[][]{Weapon.Enchantment.common,Weapon.Enchantment.uncommon,Weapon.Enchantment.rare,Weapon.Enchantment.curses}) {
            for(Class<?> type:types){
                sword.enchantment=(Weapon.Enchantment)type.getDeclaredConstructor().newInstance(); sword.cursedKnown=true;
                String text=ArcanaInfoDisplay.weapon(sword);
                assertFalse(type.getName(),text.isEmpty()); assertFalse(text,text.contains(Messages.NO_TEXT_FOUND));
                assertTrue(type.getName()+" missing effect label",text.contains("Arcana"));
                assertTrue(type.getName()+" missing effect label",text.contains("Arcana"));
                assertTrue(text,text.length()<600);
            }
        }
        for (Class<?>[] types : new Class<?>[][]{Armor.Glyph.common,Armor.Glyph.uncommon,Armor.Glyph.rare,Armor.Glyph.curses}) {
            for(Class<?> type:types){
                armor.glyph=(Armor.Glyph)type.getDeclaredConstructor().newInstance(); armor.cursedKnown=true;
                String text=ArcanaInfoDisplay.armor(armor);
                assertFalse(type.getName(),text.isEmpty()); assertFalse(text,text.contains(Messages.NO_TEXT_FOUND));
                assertTrue(text,text.length()<600);
            }
        }
    }
    @Test public void projectingSeparatesArcanaAndRunicSlashWithoutCorruptGlyphs() {
        equip(0);
        RunicBlade blade=new RunicBlade(); blade.level(6); blade.levelKnown=true;
        blade.enchant(new Projecting());
        Dungeon.hero.heroClass=com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.DUELIST;
        String s=ArcanaInfoDisplay.weapon(blade);
        assertTrue(s,s.contains("Extra melee reach: _1_ tiles"));
        assertTrue(s,s.contains("Runic Slash adds another _+600%_ power"));
        assertTrue(s,s.contains("for _8_ tiles of total reach"));
        assertTrue(s,s.contains("round up to another tile"));
        assertFalse(s,s.contains("\uFFFD"));
        assertFalse(s,s.contains("→"));
    }
    @Test public void missileProjectingDoesNotClaimAddedMeleeReach() {
        equip(0);
        HeavyBoomerang boomerang=new HeavyBoomerang();boomerang.enchant(new Projecting());
        String s=ArcanaInfoDisplay.weapon(boomerang);
        assertTrue(s,s.contains("Wall-piercing distance: _5_ tiles"));
        assertFalse(s,s.contains("Extra melee reach"));
    }
    @Test public void effectDescriptionsExposeCalculatedStrengthWithoutACombatProc(){
        equip(8);
        sword.level(6);sword.enchant(new Blocking());
        String block=ArcanaInfoDisplay.weapon(sword);
        assertTrue(block,block.contains("Shield per hit:"));
        sword.enchant(new Chilling());
        String chill=ArcanaInfoDisplay.weapon(sword);
        assertTrue(chill,chill.contains("Chill per hit:"));
        armor.level(6);armor.inscribe(new Thorns());
        String thorns=ArcanaInfoDisplay.armor(armor);
        assertTrue(thorns,thorns.contains("Bleeding:"));
    }
    @Test public void identifiedArcanaInMiscSlotAlsoWorks() {
        Dungeon.hero.belongings.misc=ring(0);
        assertTrue(ArcanaInfoDisplay.weapon(sword).contains("+17.5%_ power"));
    }
    @Test public void armorAndWeaponDescriptionWorkAcrossSaveRoundTrip() {
        equip(2); sword.level(3); armor.level(3);
        String a=ArcanaInfoDisplay.armor(armor),s=ArcanaInfoDisplay.weapon(sword);
        Bundle b=new Bundle(); b.put("armor",armor); b.put("weapon",sword);
        Armor restored=(Armor)b.get("armor"); Weapon restoredWeapon=(Weapon)b.get("weapon");
        assertEquals(a,ArcanaInfoDisplay.armor(restored)); assertEquals(s,ArcanaInfoDisplay.weapon(restoredWeapon));
    }
}
