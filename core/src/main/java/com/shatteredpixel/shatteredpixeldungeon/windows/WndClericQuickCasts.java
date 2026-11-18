package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.ClericQuickCasts;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import java.util.ArrayList;
import java.util.List;

public final class WndClericQuickCasts {
    private WndClericQuickCasts() {}
    private static void show(com.shatteredpixel.shatteredpixeldungeon.ui.Window window) {
        ShatteredPixelDungeon.scene().addToFront(window);
    }
    public static void settings() {
        List<WndUimsdList.Entry> entries = new ArrayList<>();
        for (int i=1; i<=ClericQuickCasts.MAX_SLOTS; i++)
            entries.add(WndUimsdList.Entry.item(i + (i==1 ? " spell button" : " spell buttons")
                    + (SPDSettings.apdClericQuickSlots()==i ? " (selected)" : "")));
        show(WndUimsdList.menu("Cleric quick-cast buttons",
                "Saved spell buttons stack upward above the inventory, starting with spell 1 at the bottom. Long-press a spell to choose its position; long-press a saved button to replace or remove it. Reducing the count keeps hidden preferences for later runs.",
                entries, (index, checked) -> { SPDSettings.apdClericQuickSlots(index+1); GameScene.updateTags=true; })
                .restoreWith(WndClericQuickCasts::settings));
    }
    private static HolyTome currentTome() {
        return Dungeon.hero==null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready
                ? null : Dungeon.hero.belongings.getItem(HolyTome.class);
    }
    public static void assign(ClericSpell spell) {
        HolyTome tome=currentTome();
        if (tome==null || !ClericQuickCasts.unlocked(Dungeon.hero,spell)) return;
        List<WndUimsdList.Entry> entries=new ArrayList<>();
        for(int i=0;i<SPDSettings.apdClericQuickSlots();i++) {
            ClericSpell saved=ClericQuickCasts.saved(i,tome);
            entries.add(WndUimsdList.Entry.item("Spell " + (i+1) + ": " + (saved==null ? "Empty" : saved.name())));
        }
        show(WndUimsdList.menu("Save " + spell.name(), "Choose its position. This never casts the spell.",
                entries, (slot, checked) -> {
                    if (currentTome()==tome && ClericQuickCasts.unlocked(Dungeon.hero,spell)) {
                        ClericQuickCasts.assign(slot,spell,tome); GameScene.updateTags=true;
                    }
                }).restoreWith(() -> assign(spell)));
    }
    public static void edit(int slot) {
        HolyTome tome=currentTome();
        if(tome==null || slot>=SPDSettings.apdClericQuickSlots()) return;
        List<ClericSpell> spells=new ArrayList<>();
        List<WndUimsdList.Entry> entries=new ArrayList<>();
        entries.add(WndUimsdList.Entry.item("Remove saved spell"));
        for(int tier=1;tier<=Talent.MAX_TALENT_TIERS;tier++) {
            for(ClericSpell spell:ClericSpell.getSpellList(Dungeon.hero,tier)) {
                spells.add(spell); entries.add(WndUimsdList.Entry.item(spell.name()));
            }
        }
        show(WndUimsdList.menu("Spell " + (slot+1), "Choose a replacement or remove this preference. No spell is cast.",
                entries, (choice, checked) -> {
                    if(currentTome()!=tome) return;
                    ClericSpell spell=choice==0 ? null : spells.get(choice-1);
                    if(spell==null || ClericQuickCasts.unlocked(Dungeon.hero,spell)) {
                        ClericQuickCasts.assign(slot,spell,tome); GameScene.updateTags=true;
                    }
                }).restoreWith(() -> edit(slot)));
    }
}
