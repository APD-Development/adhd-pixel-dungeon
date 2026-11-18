package com.shatteredpixel.shatteredpixeldungeon.apd.hotbar;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;

/** Saved UI preferences only. Spell effects and costs remain in the original spell implementations. */
public final class ClericQuickCasts {
    public static final int MAX_SLOTS = 4;
    private static final String EMPTY = "-";
    private ClericQuickCasts() {}

    public static ClericSpell saved(int slot, HolyTome tome) {
        String type = SPDSettings.apdClericQuickSpell(slot);
        // An older run's single quick spell remains usable until this preference is explicitly edited.
        if (type.isEmpty()) return slot == 0 && tome != null ? tome.quickSpell() : null;
        for (ClericSpell spell : ClericSpell.getAllSpells()) {
            if (spell.getClass().getName().equals(type)) return spell;
        }
        return null; // Unknown/future preferences are preserved, never reflectively instantiated.
    }

    public static boolean unlocked(Hero hero, ClericSpell spell) {
        if (hero == null || spell == null) return false;
        for (int tier = 1; tier <= Talent.MAX_TALENT_TIERS; tier++) {
            if (ClericSpell.getSpellList(hero, tier).contains(spell)) return true;
        }
        return false;
    }

    public static ClericSpell available(Hero hero, HolyTome tome, int slot) {
        if (slot >= SPDSettings.apdClericQuickSlots()) return null;
        ClericSpell spell = saved(slot, tome);
        return unlocked(hero, spell) ? spell : null;
    }

    public static void assign(int slot, ClericSpell spell, HolyTome tome) {
        if (spell != null && !ClericSpell.getAllSpells().contains(spell))
            throw new IllegalArgumentException("Unknown Cleric spell");
        SPDSettings.apdClericQuickSpell(slot, spell == null ? EMPTY : spell.getClass().getName());
        if (slot == 0 && tome != null) tome.setQuickSpellExact(unlocked(Dungeon.hero, spell) ? spell : null);
    }

    public static void restoreFirst(Hero hero, HolyTome tome) {
        if (!SPDSettings.apdClericQuickSpell(0).isEmpty())
            tome.setQuickSpellExact(available(hero, tome, 0));
    }

    public static boolean cast(Hero hero, HolyTome tome, int slot) {
        if (hero == null || hero != Dungeon.hero || !hero.isAlive() || !hero.ready
                || tome == null || !hero.belongings.contains(tome)) return false;
        ClericSpell spell = available(hero, tome, slot);
        return spell != null && tome.castSavedSpell(hero, spell);
    }
}
