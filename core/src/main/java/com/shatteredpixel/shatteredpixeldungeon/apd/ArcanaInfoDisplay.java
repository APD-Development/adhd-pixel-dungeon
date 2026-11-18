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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfArcana;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Read-only Arcana contribution. Never call genericProcChanceMultiplier here:
 * weapon calculations can detach one-use combat buffs. Other bonuses are deliberately excluded.
 */
public final class ArcanaInfoDisplay {
    private ArcanaInfoDisplay() {}

    private static float visibleMultiplier(Hero hero) {
        if (hero == null || hero.buff(MagicImmune.class) != null) return Float.NaN;
        boolean equipped = false;
        int bonus = 0;
        for (Item item : new Item[]{hero.belongings.ring(), hero.belongings.misc()}) {
            // Hide for any unknown ring, so suppression itself cannot reveal its type.
            if (item instanceof Ring && !item.isIdentified()) return Float.NaN;
            if (item instanceof RingOfArcana) {
                // Suppress combined totals if ANY contributing ring has hidden identity/level/curse.
                equipped = true;
                bonus += ((RingOfArcana)item).soloBuffedBonus();
            }
        }
        // Core Ring.getBuffedBonus uses Spirit Form only when the regular bonus is zero.
        // Its ring() getter updates the cached item level, so preview through the read-only accessor.
        if (equipped && bonus == 0) {
            com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff spirit =
                    hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class);
            if (spirit != null) bonus = spirit.previewRingBonus(hero, RingOfArcana.class);
        }
        return equipped ? (float)Math.pow(1.175f, bonus) : Float.NaN;
    }

    public static boolean hasKnownArcana(Hero hero){
        float total=knownArcanaPower(hero);
        return Float.isFinite(visibleMultiplier(hero))||(Float.isFinite(total)&&total!=1f);
    }

    /** Read-only public-value preview; unknown rings are never identified by their numerical effect. */
    public static float knownArcanaPower(Hero hero){
        if(hero==null||hero.buff(MagicImmune.class)!=null)return Float.NaN;
        for(Item item:new Item[]{hero.belongings.ring(),hero.belongings.misc()})
            if(item instanceof Ring&&!item.isIdentified())return Float.NaN;
        com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff spirit=hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm.SpiritFormBuff.class);
        if(spirit!=null&&!spirit.previewRingIdentified())return Float.NaN;
        float known=visibleMultiplier(hero);
        if(!Float.isNaN(known))return known;
        return spirit==null?1f:(float)Math.pow(1.175f,spirit.previewRingBonus(hero,RingOfArcana.class));
    }

    private static String number(float value) {
        return Messages.decimalFormat(Math.abs(value) >= 1000000 ? "0.##E0" : "0.##", value);
    }
    private static String text(String key, Object... values) {
        return Messages.get(ArcanaInfoDisplay.class, key, values);
    }
    private static String power(float multi) {
        return text(multi>=1?"power":"power_decrease", number(100f*Math.abs(multi-1f)));
    }
    private static String metric(String key, float before, float after) {
        return text(key, number(before), number(after));
    }
    private static String chance(float base, float multi) {
        // Probability saturates at 100%; excess is effect strength, never a >100% chance.
        float before=100f*Math.max(0,Math.min(1,base)),after=100f*Math.max(0,Math.min(1,base*multi));
        return text("chance",number(before),number(after));
    }


    private static String effect(String name) {
        return " " + text("effect_" + name.toLowerCase(java.util.Locale.ROOT));
    }
    private static String weaponChance(float base,float ringPower,Hero hero){
        float before=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.EnchantmentExplanation.knownPowerWithoutArcana(hero);
        float after=com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.EnchantmentExplanation.knownPower(hero);
        return Float.isFinite(before)&&Float.isFinite(after)&&before>0?chance(base*before,after/before):chance(base,ringPower);
    }
    private static String weaponStrength(String name, Weapon weapon, float baseChance, float multi) {
        float strength=Math.max(1f,baseChance*multi);
        float level=Math.max(0,weapon.buffedLvl());
        switch(name) {
            case "Chilling": return " " + text("chill_strength",number(3),number(3*strength),number(6),number(6*strength));
            case "Blocking": return " " + text("blocking_strength",number(Math.round(2+weapon.buffedLvl())),number(Math.round((2+weapon.buffedLvl())*strength)));
            case "Shocking": return " " + text("shock_strength",number(50),number(50*strength));
            case "Venomous": return " " + text("venom_strength",number(3+level/2f),number((3+level/2f)*strength));
            case "Eldritch": return " " + text("eldritch_strength",number(5),number(5*strength));
            case "Elastic": return " " + text("elastic_strength",number(2),number(Math.round(2*strength)));
            case "Blooming": return " " + text("bloom_strength",number(1+.1f*level),number((1+.1f*level)*strength));
            default: return "";
        }
    }
    private static String armorStrength(String name, Armor armor, float baseChance, float multi) {
        float strength=Math.max(1f,baseChance*multi);
        float level=Math.max(0,armor.buffedLvl());
        switch(name) {
            case "Potential": return " " + text("potential_strength",number(1),number(strength));
            case "Entanglement": return " " + text("entangle_strength",number(Math.round(5+2*level)),number(Math.round((5+2*level)*strength)));
            case "Repulsion": return " " + text("repulse_strength",number(2),number(Math.round(2*strength)));
            case "Affection": {
                float d=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm.DURATION;
                return " " + text("affection_strength",number(Math.round(d)),number(Math.round(d*strength)));
            }
            case "Thorns": return " " + text("thorns_strength",number(Math.round(4+level)),number(Math.round((4+level)*strength)));
            default: return "";
        }
    }

    public static String weapon(Weapon weapon) {
        Hero hero = Dungeon.hero;
        float multi = visibleMultiplier(hero);
        if (Float.isNaN(multi) || weapon == null || weapon.enchantment == null
                || (weapon.enchantment.curse() && !weapon.cursedKnown)
                || !weapon.hasEnchant(weapon.enchantment.getClass(), hero)) return "";
        if (!Float.isFinite(multi)) return "\n\n" + text("unavailable");
        String result = power(multi);
        String name = weapon.enchantment.getClass().getSimpleName();
        String pkg = weapon.enchantment.getClass().getPackage().getName();
        if (!pkg.equals("com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments")
                && !pkg.equals("com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses")) {
            return "\n\n" + text("custom", number(multi));
        }
        if (name.equals("Kinetic")) return "\n\n" + result + " " + text("kinetic") + effect(name);
        if (name.equals("Crystal")) return "\n\n" + metric("magic", 25, 25*multi) + effect(name);
        if (name.equals("Projecting")) {
            if (weapon instanceof MissileWeapon) {
                // MissileWeapon.throwPos uses four times enchantment power
                // for its wall-penetrating target radius, not melee reach.
                return "\n\n" + metric("missile_reach", 4, Math.round(4f*multi)) + effect(name);
            }
            int base=weapon.RCH;
            int normal=Projecting.additionalReach(1f,base);
            int arcana=Projecting.additionalReach(multi,base);
            String details="\n\n" + metric("reach",normal,arcana)
                    + " " + text("total_reach",base+arcana);
            if (normal == arcana) details += " " + text("reach_rounding");
            if (weapon instanceof RunicBlade && weapon.levelKnown && hero.heroClass == HeroClass.DUELIST) {
                float slash=((RunicBlade)weapon).runicSlashEnchantBoost();
                // Use the same read-only combat power authority for all other active
                // modifiers, including Berserk, without consuming any one-use buff.
                float otherPower=Weapon.Enchantment.genericProcChanceMultiplierReadOnly(hero);
                RunicBlade.RunicSlashTracker active=hero.buff(RunicBlade.RunicSlashTracker.class);
                if (active!=null) otherPower-=active.boost;
                int slashBonus=Projecting.additionalReach(otherPower+slash,base);
                details += " " + text("runic_reach",number(100f*slash),base+slashBonus);
            }
            return details + effect(name);
        }
        if (name.equals("Unstable")) return "\n\n" + result + " " + text("unstable") + effect(name);
        if (name.equals("Explosive")) return "\n\n" + result + " " + text("explosive") + effect(name);
        if (name.equals("Vampiric")) {
            float base = hero.HT > 0 ? 0.05f + 0.25f*(hero.HT-hero.HP)/(float)hero.HT : 0.05f;
            return "\n\n" + result + " " + weaponChance(base, multi, hero) + " " + text("current_hp") + effect(name);
        }
        if (!weapon.levelKnown) return "\n\n" + result + " " + text("unknown_level");
        float level = Math.max(0, weapon.buffedLvl()), base;
        switch (name) {
            case "Blazing": case "Blooming": base=(level+1)/(level+3); break;
            case "Chilling": base=(level+1)/(level+4); break;
            case "Shocking": case "Venomous": base=1/3f; break;
            case "Blocking": case "Lucky": base=(level+4)/(level+40); break;
            case "Eldritch": case "Elastic": base=(level+1)/(level+5); break;
            case "Vorpal": case "Wayward": base=1/4f; break;
            case "Corrupting": base=(level+5)/(level+25); break;
            case "Grim": return "\n\n" + result + " " + text("grim") + effect(name);
            case "Annoying": base=1/20f; break;
            case "Displacing": base=1/12f; break;
            case "Dazzling": case "Friendly": base=1/10f; break;
            case "Polarized": base=1/2f; break;
            case "Pressurized": case "Wondrous": base=1/8f; break;
            case "Sacrificial": return "\n\n" + result + " " + text("sacrificial") + effect(name);
            default: return "\n\n" + text("custom", number(multi));
        }
        return "\n\n" + result + " " + weaponChance(base, multi, hero) + weaponStrength(name,weapon,base,multi) + effect(name);
    }

    public static String armor(Armor armor) {
        Hero hero = Dungeon.hero;
        float multi = visibleMultiplier(hero);
        if (Float.isNaN(multi) || armor == null || armor.glyph == null
                || (armor.glyph.curse() && !armor.cursedKnown)
                || !armor.hasGlyph(armor.glyph.getClass(), hero)) return "";
        if (!Float.isFinite(multi)) return "\n\n" + text("unavailable");
        String name=armor.glyph.getClass().getSimpleName(), result=power(multi);
        String pkg=armor.glyph.getClass().getPackage().getName();
        if (!pkg.equals("com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs")
                && !pkg.equals("com.shatteredpixel.shatteredpixeldungeon.items.armor.curses")) {
            return "\n\n" + text("custom", number(multi));
        }
        if (name.equals("Brimstone")) return "\n\n" + (armor.levelKnown&&armor.buffedLvl()<0?text("inactive"):text("brimstone") + effect(name));
        if (!armor.levelKnown) return "\n\n" + result + " " + text("unknown_level");
        float actualLevel=armor.buffedLvl(), level=Math.max(0, actualLevel), base;
        if (actualLevel < 0 && (name.equals("Obfuscation") || name.equals("Swiftness")
                || name.equals("Flow") || name.equals("Camouflage") || name.equals("AntiMagic")
                || name.equals("Bulk"))) return "\n\n" + text("inactive");
        switch (name) {
            case "Obfuscation": return "\n\n" + com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.ObfuscationExplanation.summary(armor,hero);
            case "Swiftness": return "\n\n" + metric("speed", 1.2f+0.04f*actualLevel, (1.2f+0.04f*actualLevel)*multi) + " " + text("swiftness") + effect(name);
            case "Flow": return "\n\n" + metric("speed", 2+0.5f*actualLevel, (2+0.5f*actualLevel)*multi) + " " + text("water") + effect(name);
            case "Camouflage": return "\n\n" + metric("turns", Math.round(3+actualLevel/2f), Math.round((3+actualLevel/2f)*multi)) + effect(name);
            case "AntiMagic": return "\n\n" + text("magic_dr", number(Math.round(actualLevel)), number(Math.round(3+1.5f*actualLevel)), number(Math.round(actualLevel*multi)), number(Math.round((3+1.5f*actualLevel)*multi))) + effect(name);
            case "Stone": return "\n\n" + result + " " + text("stone") + effect(name);
            case "Bulk": return "\n\n" + metric("speed", 1/3f, multi/3f) + " " + text("door") + effect(name);
            case "Viscosity": return "\n\n" + result + " " + text("viscosity", number((level+1)/(level+6)*multi)) + effect(name);
            case "Potential": base=(level+1)/(level+6); break;
            case "Entanglement": base=1/4f; break;
            case "Repulsion": base=(level+1)/(level+5); break;
            case "Affection": base=(level+3)/(level+20); break;
            case "Thorns": base=(level+2)/(level+12); break;
            case "AntiEntropy": case "Stench": base=1/8f; break;
            case "Corrosion": base=1/10f; break;
            case "Displacement": case "Multiplicity": case "Overgrowth": base=1/20f; break;
            case "Metabolism": base=1/6f; break;
            default: return "\n\n" + text("custom", number(multi));
        }
        return "\n\n" + result + " " + chance(base, multi) + armorStrength(name,armor,base,multi) + effect(name);
    }
}
