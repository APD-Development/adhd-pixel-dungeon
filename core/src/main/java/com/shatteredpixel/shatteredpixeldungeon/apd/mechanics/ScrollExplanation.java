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
package com.shatteredpixel.shatteredpixeldungeon.apd.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;

/** Known scroll calculations without reading hidden target HP, applying buffs or generating loot. */
public final class ScrollExplanation {
    private ScrollExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    /** Publicly apparent creatures only: a lower bound, never the hidden cast target count. */
    public static int visibleCreatureCount(){
        if(Dungeon.level==null||Dungeon.level.heroFOV==null||Dungeon.level.mobs==null)return -1;
        int count=0;for(Mob mob:Dungeon.level.mobs){int cell=mob.pos;if(cell>=0&&cell<Dungeon.level.heroFOV.length&&Dungeon.level.heroFOV[cell]&&mob.invisible<=0
            &&!(mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic&&mob.alignment==com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.NEUTRAL))count++;}
        return count;
    }
    public static String details(Scroll scroll,Hero hero){
        if(!scroll.isKnown())return "";
        String text;
        switch(scroll.getClass().getSimpleName()){
            case "ScrollOfRetribution":
                float power=hero==null||hero.HT<=0?0:com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution.powerForHealth(hero.HP,hero.HT);
                return "_Damage_\nHits creatures in your sight, including allies. Base damage is 10% of the target's maximum HP, plus a share of its current HP that grows as your health falls. That share reaches 90% near one-tenth of your maximum health. "
                    +(hero==null||hero.HT<=0?"":"At your current health, it adds _"+n(power*22.5f)+"%_ of the target's current HP. ")
                    +"The target's defenses still apply.\n\n_Formula_\nPower = min(4, 4.45 × (your maximum HP − your current HP) / your maximum HP). Base damage = round(target maximum HP / 10 + target current HP × power × 0.225).\n\n_Aftereffects_\nSurvivors are blinded for "+n(Blindness.DURATION)+" turns. You are blinded for "+n(Blindness.DURATION)+" turns and weakened for "+n(Weakness.DURATION)+" turns, before resistance.";
            case "ScrollOfPsionicBlast":
                int count=visibleCreatureCount();int self=hero==null||count<0?0:com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast.baseSelfDamage(hero.HT,count);
                return "_Blast damage_\nHits creatures in your sight, including allies. Base damage = round((target maximum HP + target current HP) / 2). The target's defenses still apply.\n\n_Risk to you_\nBase damage to you = max(0, round(your maximum HP × 0.5 × 0.9^N)). N counts all creatures caught in the blast, including allies. "
                    +(hero==null||count<0?"A current self-damage estimate needs an active run. ":"Based on _"+count+" creatures you can see_, this is _at most "+self+" base damage_ to you. Additional affected creatures can reduce it further. ")
                    +"Your defenses can reduce the health lost. This can kill you.\n\n_Aftereffects_\nSurvivors are blinded for "+n(Blindness.DURATION)+" turns. If you survive, you are blinded for "+n(Blindness.DURATION)+" turns and weakened for "+n(Weakness.DURATION*5)+" turns, before resistance.";
            case "ScrollOfRecharging":text="Offers Recharging for "+n(Recharging.DURATION)+" game-time units through the existing additive scheduled-duration path. Eligible wand-charger updates add up to 0.25 partial charge from Recharging, limited by its remaining duration and the wand's capacity. Duration is not that many whole charges or an instant refill of every wand. Charging rates and capacity remain item-specific.";break;
            case "ScrollOfMysticalEnergy":text="Offers 30 game-time units of Artifact Recharging. Its resource setter preserves a longer existing duration rather than adding another full duration. The supported effect permits Horn recharge for this source. Equipped supported artifacts convert recharge power using their own rates, curse rules and capacities; the duration is not a universal count of whole artifact charges.";break;
            case "ScrollOfAntiMagic":text="Offers Magic Immunity for "+n(MagicImmune.DURATION)+" game-time units through its existing scheduled-duration path. It blocks supported magical effects and also suppresses supported beneficial magic and item bonuses. It is not immunity to every damage type or permission to use all magical items while immune. Existing application and expiry rules restore the supported paths afterward.";break;
            case "ScrollOfForesight":text="Offers Foresight for "+n(Foresight.DURATION)+" game-time units. Its normal Search footprint uses the rounded distance input of "+Foresight.DISTANCE+" tiles and its existing mapping/discovery path. That radius is separate from duration. Eligibility, terrain and the effect's own checks remain in force; inspection does not map terrain or reveal a secret.";break;
            case "ScrollOfLullaby":text="Offers Drowsy for "+n(Drowsy.DURATION)+" game-time units to the hero and the supported visible creatures, including allies. Drowsy then uses its existing sleep checks and interruption rules. This is not a guarantee of immediate sleep or a fixed duration of helplessness; damage, immunity and state can affect the transition.";break;
            case "ScrollOfTerror":text="Offers Terror for "+n(Terror.DURATION)+" game-time units to eligible visible non-allied creatures, with the reader recorded as the feared character. Resistance, immunity, movement and interruption rules still apply. Duration is not a guaranteed number of retreating steps or extra damage.";break;
            case "ScrollOfDread":text="Eligible visible non-allied creatures are offered Dread with the reader as the feared character. Its flee/escape rules govern the outcome rather than a fixed per-hit execution percentage. An immune target instead receives the existing Terror offer of "+n(Terror.DURATION)+" units, still subject to its own immunity and resistance. Inspection does not decide whether any unseen target can escape.";break;
            case "ScrollOfRage":text="Calls the floor's supported monsters toward the reader's position. Eligible visible non-allied creatures are also offered Amok for 5 game-time units. Beckoning and Amok are separate effects: beckoning does not reveal every monster, and Amok uses its existing target priorities rather than guaranteeing that every enemy attacks another enemy.";break;
            case "ScrollOfMirrorImage":text="Attempts to spawn 2 mirror images in eligible nearby cells. Occupancy and terrain can leave fewer than two valid placements. Images use their existing copied combat and survival rules; this is not two permanent inventory-controlled allies, extra maximum HP for the hero or a guarantee of two immediate attacks.";break;
            case "ScrollOfPrismaticImage":text="If a supported Prismatic Image already exists on the floor or in the supported stasis slot, it is healed to its own maximum HP. Otherwise the scroll offers Prismatic Guard with maximum HP 10 + floor(2.5 × hero level). "+(hero==null?"":"Current new-guard offer: "+PrismaticGuard.maxHP(hero)+" HP. ")+"That is the image's health resource, not healing or maximum HP added to the hero. Existing spawn, summoning and ally rules remain.";break;
            case "ScrollOfDivination":text="Reveals up to 4 currently unknown potion, scroll and ring types, using the existing category weights and availability rules. It does not identify four specific carried items' upgrade levels, curses or enchantments. Fewer than four unknown types yields fewer reveals; inspection does not draw a type or alter knowledge.";break;
            case "ScrollOfIdentify":text="Identifies the chosen eligible item under its existing selection rules. This reveals that item's supported unknown properties, rather than permanently boosting its statistics or identifying every matching item in the backpack. It does not choose or identify anything merely by opening this description.";break;
            case "ScrollOfMagicMapping":text="Maps discoverable terrain on the current floor and runs its supported secret-terrain discovery path, including eligible hidden doors and traps. Mapping is not normal wall-search probability or a guarantee that an unmapped, undiscoverable area is empty. It does not identify carried equipment or transport the hero.";break;
            case "ScrollOfUpgrade":text="Applies one normal upgrade step to the chosen eligible item. Each item converts that step into its own damage, protection, charge or ring effects. Strength requirements follow their rounded thresholds rather than always falling by one. Curse removal, glyph/enchantment preservation or loss, hardening and applicable talents keep their actual separate rules; an upgrade is not a promise that every existing property survives or every curse is revealed.";break;
            case "ScrollOfEnchantment":text="Offers three supported enchantment or glyph choices for an eligible weapon or armor. The first uses the common pool, the second the uncommon pool and the third the existing general pool with the earlier choices excluded. The third is not guaranteed rare. Existing-property exclusions and the selected item's rules remain. Inspection does not generate choices, advance randomness or disclose an unknown existing curse.";break;
            case "ScrollOfRemoveCurse":text="Removes supported curses through the existing item-selection and container rules. Cleansing a curse is separate from identifying hidden upgrades or generating a new enchantment. Some effects tied to an item or a challenge keep their own restrictions; inspection does not remove, identify or unequip anything.";break;
            case "ScrollOfTransmutation":text="Transforms an eligible selected item through its category's existing replacement rules. Eligible replacements, artifact histories, level handling and property transfer depend on that category, rather than one universal damage or upgrade bonus. This is not a guarantee of a particular replacement. Inspection never chooses the result or changes the original item.";break;
            case "ScrollOfMetamorphosis":text="Replaces the selected eligible talent with one offered by the existing cross-class selection rules. Rank, tier, point transfer and exclusions remain governed by that process. Alternate-class talent effects can differ from their native-class effects; their own calculation panels describe the current context. Inspection does not alter the talent map or choose a replacement.";break;
            case "ScrollOfTeleportation":text="Attempts a legal destination under the floor's teleportation rules. Reachability, cells, restrictions and alternate target behavior remain in force; it is not movement through any selected wall or guaranteed access to a locked room. No destination is rolled or revealed by inspection.";break;
            case "ScrollOfPassage":text="Uses the existing return-floor formula max(1, current floor − 1 − ((current floor − 2) modulo 5)), with its ordinary inter-floor restrictions and arrival path. "+(hero==null?"":"For the current dungeon floor this selects floor "+Math.max(1,Dungeon.depth-1-(Dungeon.depth-2)%5)+" before those restrictions. ")+"It is a floor transition, not a free search, automatic shop sale or arbitrary teleport destination.";break;
            case "ScrollOfGathering":text="Collects supported loose ground loot into one ordinary pile beside the current floor's downstairs. Containers, shop stock, carried gear, keys, quest items and Lost Backpack follow the existing exclusions. It must find a safe destination and does not enlarge inventory, sell loot or grant free item pickup. Inspection does not enumerate unseen loot or move any item.";break;
            case "ScrollOfForwarding":text="Sends the supported loose-ground loot to one ordinary pile outside the next region shop's entrance. The current region is skipped; late casts route to the Imp shop on floor 20. "+(hero==null?"":com.shatteredpixel.shatteredpixeldungeon.apd.loot.LootScrollRules.destinationDepth(Dungeon.depth)<0?"The current floor has no supported main-dungeon destination. ":"The normal main-dungeon routing rule selects floor "+com.shatteredpixel.shatteredpixeldungeon.apd.loot.LootScrollRules.destinationDepth(Dungeon.depth)+" before existing validity checks. ")+"Undelivered shipments retain their existing save-safe waiting and placement rules. Nothing is automatically sold. It leaves containers, shop stock, keys, quest items and Lost Backpacks in place. Unlike Gathering, it cannot send armed bombs between floors. Inspection does not generate a future floor, examine hidden shop contents or transfer loot.";break;
            case "ScrollOfSirensSong":text="The selected eligible non-allied monster is offered enthrallment under its existing immunity rules; an immune selected target gets the supported Charm offer instead. Other supported visible non-allied monsters are offered Charm for "+n(Charm.DURATION)+" game-time units with the reader as the protected character. Charm is not a universal alliance or a guarantee of harmlessness to everyone; damage and resistance keep their normal effects.";break;
            case "ScrollOfChallenge":text="Creates the existing terrain-bounded challenge arena for 100 resource-time units, using its area expansion and in-arena rules. At its supported incoming-damage stage, damage is multiplied by 0.67 before later rounding and defenses. Character attacks apply that stage in normal attack processing; non-character damage uses its separate hero-damage path, preventing the normal double application. Hunger is paused while the effect applies. Leaving the recorded area ends it on its next position check. Area distance is 1 on floors 5, 10 and 20, otherwise 3 or 2 under the existing visible-area sizing rule. It is not a permanent maximum-health change or a free way to clear a whole floor. Inspection does not create an arena, map hidden cells or spend its timer.";break;
            default:return "";
        }
        return "_Known scroll effect_\n"+text+" Normal reading and target-selection costs remain unchanged; this explanation does not read or consume the scroll.";
    }
}
