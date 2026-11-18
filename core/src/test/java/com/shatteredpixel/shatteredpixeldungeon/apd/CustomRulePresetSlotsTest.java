/* ADHD Pixel Dungeon; Copyright (C) 2026 Chris Charlton
 * SPDX-License-Identifier: GPL-3.0-or-later */
package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRulePresets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Six independent, optionally named next-run presets, plus fixed difficulty shortcuts. */
public class CustomRulePresetSlotsTest {
    @Test public void sixClassNamesAreDefaultsAndCanBeRenamedWithoutAffectingOthers(){
        HeadlessTestSupport.start();
        assertEquals(6,SPDSettings.APD_CUSTOM_RULE_SLOT_COUNT);
        String[] names={"Warrior","Mage","Rogue","Huntress","Duelist","Cleric"};
        for(int i=1;i<=names.length;i++)assertEquals(names[i-1],SPDSettings.apdCustomRuleSlotName(i));
        SPDSettings.apdCustomRuleSlotName(2,"My mage challenge");
        assertEquals("My mage challenge",SPDSettings.apdCustomRuleSlotName(2));
        assertEquals("Warrior",SPDSettings.apdCustomRuleSlotName(1));
        SPDSettings.apdCustomRuleSlotName(2,"");
        assertEquals("Mage",SPDSettings.apdCustomRuleSlotName(2));
    }
    @Test public void allSixSlotsStoreTheirOwnCompleteSliderConfigurations(){
        HeadlessTestSupport.start();
        for(int slot=1;slot<=6;slot++){
            assertFalse(SPDSettings.apdCustomRuleSlotOccupied(slot));
            assertNull(SPDSettings.apdCustomRuleSlot(slot));
            CustomDungeonRules rules=CustomDungeonRules.defaults().with(CustomDungeonRules.Slider.UPGRADE_SCROLLS,slot);
            SPDSettings.apdCustomRuleSlot(slot,rules);
            assertTrue(SPDSettings.apdCustomRuleSlotOccupied(slot));
        }
        for(int slot=1;slot<=6;slot++)
            assertEquals(slot,SPDSettings.apdCustomRuleSlot(slot).get(CustomDungeonRules.Slider.UPGRADE_SCROLLS));
        assertEquals(15,SPDSettings.apdCustomRules().get(CustomDungeonRules.Slider.UPGRADE_SCROLLS));
    }
    @Test public void presetsRespectEasyHardAndOriginalDefaultForEverySlider(){
        CustomDungeonRules easy=CustomRulePresets.apply(CustomRulePresets.Preset.EASIEST);
        CustomDungeonRules hard=CustomRulePresets.apply(CustomRulePresets.Preset.HARDEST);
        CustomDungeonRules def=CustomRulePresets.apply(CustomRulePresets.Preset.DEFAULT);
        assertEquals(18,CustomDungeonRules.Slider.values().length);
        for(CustomDungeonRules.Slider slider:CustomDungeonRules.Slider.values()){
            assertTrue(slider.valid(easy.get(slider)));
            assertTrue(slider.valid(hard.get(slider)));
            assertEquals(slider.defaultValue(),def.get(slider));
            assertEquals(slider.max,CustomRulePresets.apply(CustomRulePresets.Preset.MAXIMUM).get(slider));
            assertEquals(slider.min,CustomRulePresets.apply(CustomRulePresets.Preset.MINIMUM).get(slider));
        }
        assertEquals(CustomDungeonRules.Slider.ENEMY_HP.min,easy.get(CustomDungeonRules.Slider.ENEMY_HP));
        assertEquals(CustomDungeonRules.Slider.ENEMY_HP.max,hard.get(CustomDungeonRules.Slider.ENEMY_HP));
        assertEquals(CustomDungeonRules.Slider.PLAYER_HP.max,easy.get(CustomDungeonRules.Slider.PLAYER_HP));
        assertEquals(CustomDungeonRules.Slider.PLAYER_HP.min,hard.get(CustomDungeonRules.Slider.PLAYER_HP));
    }
}
