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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.GenerationEvents;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ParchmentScrap;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Original GENERATION support, never a measurement of the item's rolled or current upgrade level. */
public final class GenerationUpgradeRanges {
    private GenerationUpgradeRanges() { }
    private static final String SAVE_KEY = "apd_item_upgrade_range_v1";
    public static final class Range {
        public final int min, max;
        public Range(int min, int max) { this.min = min; this.max = max; }
        @Override public boolean equals(Object other) {
            return other instanceof Range && min == ((Range) other).min && max == ((Range) other).max;
        }
        @Override public int hashCode() { return 31 * min + max; }
    }
    private static final Set<String> BONUS_ROOMS = new HashSet<>(Arrays.asList(
            "PoolRoom", "TrapsRoom", "SentryRoom", "SecretMazeRoom"));
    private static final Set<String> CURSED_BONUS_ROOMS = new HashSet<>(Arrays.asList("CryptRoom", "SacrificeRoom"));

    public static void set(Item item, int min, int max) {
        if (ItemOrigins.eligible(item)) item.apdGenerationRange = new Range(min, max);
    }
    public static void baseline(Item item) {
        if (item instanceof Artifact) set(item, 0, 0);
        else if (item instanceof Weapon || item instanceof Armor || item instanceof Ring || item instanceof Wand) set(item, 0, 2);
    }
    /** Called OUTSIDE the actual optional-upgrade branch, on every prize. */
    public static void optionalBonus(Item item) { if (item instanceof Weapon || item instanceof Armor) set(item, 0, 3); }
    /** Never inspect the prize's curse or level: only the independent generation condition. */
    public static void curseConditionalBonus(Item item) {
        if (item instanceof Weapon || item instanceof Armor) set(item, ParchmentScrap.curseChanceMultiplier() == 0 ? 1 : 0, 3);
    }
    /** A level-zero-only upgrade cannot turn +2 into +3. Called on ALL candidates. */
    public static void minimumBonus(Item item, boolean guaranteed) {
        if (item instanceof Artifact || item.apdGenerationRange == null) return;
        Range range = item.apdGenerationRange;
        set(item, guaranteed ? Math.max(1, range.min) : range.min, Math.max(1, range.max));
    }
    /** Whole-room union includes prize reuse and equipment-rule extras, without consulting which hidden roll occurred. */
    public static void finalRoomSupport(Item item, ItemOrigins.Origin origin) {
        if (!ItemOrigins.eligible(item) || origin == null || item.apdFoundOrigin != null
                || origin.source == GenerationEvents.C_REMAINS) return;
        if (origin.source == GenerationEvents.C_FOR_SALE) { set(item, 0, 0); return; }
        if (BONUS_ROOMS.contains(origin.room)) { optionalBonus(item); return; }
        if (CURSED_BONUS_ROOMS.contains(origin.room) && (item instanceof Weapon || item instanceof Armor)) {
            boolean extrasPossible = com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomCombatHooks.percentage(
                    com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.EQUIPMENT) > 100;
            set(item, ParchmentScrap.curseChanceMultiplier() == 0 && !extrasPossible ? 1 : 0, 3);
        }
    }
    public static void finalMobSupport(Item item, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob) {
        if (item == null || item.apdFoundOrigin != null) return;
        if (mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime
                || mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollTrickster) {
            boolean extrasPossible = com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomCombatHooks.percentage(
                    com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules.Slider.EQUIPMENT) > 100;
            set(item, 0, extrasPossible ? 2 : 0);
        }
    }
    public static void impReward(Item item) {
        if (item instanceof Artifact) { item.apdGenerationRange = null; return; }
        // Tier is visible base equipment identity, never an upgrade-level accessor.
        boolean tierFour = item instanceof MeleeWeapon && ((MeleeWeapon) item).tier == 4
                || item instanceof MissileWeapon && ((MissileWeapon) item).tier == 4;
        set(item, tierFour ? 3 : 2, tierFour ? 5 : 4);
    }
    public static void merge(Item survivor, Item incoming) {
        Range a = survivor.apdGenerationRange, b = incoming.apdGenerationRange;
        survivor.apdGenerationRange = a == null || b == null ? null : new Range(Math.min(a.min, b.min), Math.max(a.max, b.max));
    }
    public static void store(Item item, Bundle bundle) {
        Range r = item.apdGenerationRange;
        if (r == null) return;
        bundle.put(SAVE_KEY, new int[]{r.min, r.max});
    }
    public static void restore(Item item, Bundle bundle) {
        item.apdGenerationRange = null;
        if (!ItemOrigins.eligible(item) || !bundle.contains(SAVE_KEY)) return;
        int[] values = bundle.getIntArray(SAVE_KEY);
        if (values != null && values.length == 2 && values[0] <= values[1] && values[0] >= -100 && values[1] <= 100)
            item.apdGenerationRange = new Range(values[0], values[1]);
    }
    /** Rule projection only. Deliberately does not read trueClass, trueLevel, curses or enchantments. */
    public static String historicalRule(GenerationEvents.ItemGenerated source) {
        String owner = source.owner == null ? "" : source.owner;
        if (source.container == GenerationEvents.C_REMAINS) return ""; // prior player alterations are unbounded
        if (source.container == GenerationEvents.C_FOR_SALE) return "shop";
        if (source.container == GenerationEvents.C_QUEST_REWARD) {
            if (owner.endsWith(".Ghost") || owner.equals("Ghost")) return "ghost";
            if (owner.endsWith(".Wandmaker") || owner.equals("Wandmaker")) return "wandmaker";
            if (owner.endsWith(".Blacksmith") || owner.equals("Blacksmith")) return "blacksmith";
            if (owner.endsWith(".Imp") || owner.equals("Imp")) return "imp";
            return "";
        }
        if (source.container == GenerationEvents.C_MOB_LOOT) {
            if (owner.endsWith("TormentedSpirit")) return "normal";
            if (owner.endsWith("Slime") || owner.endsWith(".GnollTrickster") || owner.equals("GnollTrickster")) return "zero_drop_unknown_context";
            return source.drawKind != GenerationEvents.DRAW_NONE ? "normal" : "";
        }
        if (source.container == GenerationEvents.C_STATUE) return "normal";
        if (source.container == GenerationEvents.C_MIMIC) return owner.endsWith("EbonyMimic") ? "ebony" : "normal";
        if (source.container == GenerationEvents.C_BONUS_DROP) return source.drawKind != GenerationEvents.DRAW_NONE ? "normal" : "";
        if (BONUS_ROOMS.contains(source.room) || CURSED_BONUS_ROOMS.contains(source.room)) return "room_bonus_unknown_context";
        return source.drawKind != GenerationEvents.DRAW_NONE ? "normal" : "";
    }
    /** Restore only reliable source support, not the recorded hidden roll. Unrecorded conditions use a union. */
    public static void recover(Item item, ItemOrigins.Origin origin) {
        if (item.apdGenerationRange != null || !ItemOrigins.eligible(item) || origin == null) return;
        switch (origin.generationRule) {
            case "normal": case "zero_drop_unknown_context": baseline(item); break;
            case "shop": case "fixed_zero": set(item, 0, 0); break;
            case "ghost": case "blacksmith":
                if (item instanceof Weapon || item instanceof Armor) set(item, 0, 3);
                break;
            case "wandmaker": if (item instanceof Wand) set(item, 1, 3); break;
            case "imp": impReward(item); break;
            case "ebony": baseline(item); minimumBonus(item, true); break;
            case "room_bonus_unknown_context":
                // Old logs cannot distinguish a prize from equipment-rule extras or recover old Parchment level.
                // Union all legitimate branches rather than using the actual hidden roll to choose one.
                if (item instanceof Weapon || item instanceof Armor) set(item, 0, 3);
                else baseline(item);
                break;
            default: break;
        }
    }
    public static String describe(Item item) {
        if (!ItemOrigins.eligible(item) || item.isIdentified() || item.apdFoundOrigin == null
                || item.apdFoundOrigin.source == ItemOrigins.PICKUP || item.apdFoundOrigin.source == ItemOrigins.CRAFTED
                || item.apdGenerationRange == null) return "";
        Range range = item.apdGenerationRange;
        return "\n" + Messages.get(GenerationUpgradeRanges.class, "possible", signed(range.min), signed(range.max))
                + "\n" + Messages.get(GenerationUpgradeRanges.class, "generation_only");
    }
    private static String signed(int value) { return value >= 0 ? "+" + value : Integer.toString(value); }
}
