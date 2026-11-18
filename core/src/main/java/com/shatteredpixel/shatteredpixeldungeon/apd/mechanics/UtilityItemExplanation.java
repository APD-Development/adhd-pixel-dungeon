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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.*;

/** Plain units for utility resources; never repairs, revives, blesses, collects or inscribes. */
public final class UtilityItemExplanation {
    private UtilityItemExplanation(){}
    private static String n(float value){return ObfuscationExplanation.number(value);}
    private static String glyphSelection(){
        float[] weights=com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.typeChances;
        int[] sizes={com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.common.length,
                com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.uncommon.length,
                com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor.Glyph.rare.length};
        String[] names={"common","uncommon","rare"};float total=0;
        for(float weight:weights)total+=weight;
        StringBuilder text=new StringBuilder("The ordinary glyph selector first chooses a rarity pool, then chooses equally among that pool's eligible glyphs. Current pool odds: ");
        for(int i=0;i<3;i++){
            if(i>0)text.append("; ");
            text.append(names[i]).append(" ").append(n(100*weights[i]/total)).append("%");
            if(sizes[i]>0)text.append(" (with no exclusions, ").append(n(100*weights[i]/total/sizes[i])).append("% per glyph)");
        }
        return text+". Inscribing excludes the selected armor's existing glyph, so eligible alternatives in that same pool share its chance. If an entire selected pool is excluded, the selector uses its existing unrestricted fallback. These are selection odds, not a glyph's chance to trigger in combat; Arcana affects the resulting glyph's own effect, not these rarity weights.";
    }
    private static String holsterDetails(MagicalHolster holster){
        StringBuilder text=new StringBuilder("\n_Normal thrown-weapon wear_\nFor weapons using the normal thrown durability rule, the holster increases the budget of uses before wear calculations by ")
                .append(n(100*(MagicalHolster.HOLSTER_DURABILITY_FACTOR-1))).append("% (× ").append(n(MagicalHolster.HOLSTER_DURABILITY_FACTOR))
                .append("). Rounding, existing wear, rings, talents and the weapon's own limits determine the actual whole throws left. Special dart types can use different wear rules; this does not multiply damage by the same percentage.\n\n_Wand charging_\nStored wands normally refill faster. The benefit depends on the number of charges currently missing. The ordinary recharge curve changes from ")
                .append(n(Wand.normalRechargeScaleFactor())).append(" to ").append(n(MagicalHolster.HOLSTER_SCALE_FACTOR))
                .append(". Base interval = ").append(n(Wand.baseRechargeDelay())).append(" + ").append(n(Wand.scalingRechargeDelay()))
                .append(" × curve^(missing charges) game-time units per baseline charge. Missing charges means maximum charges minus current charges. As these wands get fuller, their baseline charging slows. Each refill changes the missing-charge count and therefore the rate. Existing partial progress can finish the next charge sooner; this interval is not a countdown to the next charge or the time to refill the entire wand. Energy and other charging effects apply separately. Locked-floor regeneration rules can pause base gain; Recharging adds its own fractional gain when the charger runs. Magic immunity skips the scheduled recharge step. Direct charge gifts retain their own rules.");
        for(Item stored:holster.items){
            if(!(stored instanceof Wand))continue;
            Wand wand=(Wand)stored;
            if(!wand.levelKnown||!wand.cursedKnown||!wand.curChargeKnown)continue;
            int missing=Math.max(0,wand.maxCharges-wand.curCharges);
            text.append("\n").append(wand.name()).append(": ");
            if(missing==0)text.append("full or overcharged; the scheduled charger adds no further charge.");
            else text.append(missing).append(" missing charges; ordinary → holstered baseline interval ")
                    .append(n(Wand.baselineRechargeTurns(missing,Wand.normalRechargeScaleFactor()))).append(" → ")
                    .append(n(Wand.baselineRechargeTurns(missing,MagicalHolster.HOLSTER_SCALE_FACTOR)))
                    .append(" game-time units per baseline charge, before Energy and other effects.");
        }
        return text.append(" Unidentified wand calculations are withheld; inspection does not charge a wand or spend game time.").toString();
    }
    public static String details(Item item,Hero hero){
        if(item instanceof TengusMask)return "_Choosing a subclass_\nConfirming a subclass consumes the mask and "+n(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.TICK)+" game-time unit before time-changing effects, then unlocks that subclass and its talent choices. Opening or inspecting the choice does not select a subclass. The available choices come from your hero class; the choice is not an extra equipment slot or an ordinary item upgrade. Existing learned talents and the subclass's own effects keep their normal rules. You can review each subclass in its choice screen before confirming.";
        if(item instanceof KingsCrown)return "_Transforming worn armor_\nConfirming an armor ability consumes the crown and "+n(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.TICK)+" game-time unit before time-changing effects. You need armor worn to open the crown's normal choice. Its class-armor conversion carries over the armor's permanent upgrade level, tier, augmentation, glyph, seal, mastery benefit and curse state; it does not simply add an upgrade level. An unknown upgrade level stays unknown. The converted armor's curse status becomes known through the existing conversion rule.\n\n_Starting ability resource_\nThe transformed armor starts with "+n(com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.initialUpgradeCharge())+" charge points on the normal 100-point armor meter. These points pay for armor abilities when used. Your chosen ability has its own charge cost, turn cost and talent effects, explained in its choice screen and armor details. Merely inspecting the crown does not convert or identify armor, spend charges, or choose an ability.";

        if(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.keys.Key)return KeyExplanation.details((com.shatteredpixel.shatteredpixeldungeon.items.keys.Key)item);
        if(item instanceof Bag){Bag bag=(Bag)item;String text="_Container slots_\nCurrent capacity: "+bag.capacity()+" item slots. A compatible stack generally occupies one slot regardless of its quantity; nested-container and secondary-weapon rules keep their normal accounting. Capacity is not a weight limit, gold value or a guarantee that every item type belongs in this container. Inventory capacity and automatic pickup rules are unchanged.";
            if(item instanceof MagicalHolster)text+=holsterDetails((MagicalHolster)item);
            return text;}
        if(item instanceof Honeypot)return "_Bee and broken pot_\nA supported shatter can create one bee at the impact cell. If that cell is occupied, it considers the four side-adjacent non-solid empty cells; when none is eligible, the pot is returned instead of guaranteeing a spawn. The bee uses the existing scaling-floor spawn rules. It is not automatically a harmless ally. A ground pot's bee normally looks for eligible targets within three tiles of the pot, counting a diagonal step as one tile. Carrying its matching broken pot can make the holder its target. Throwing into a pit follows the ordinary falling-item branch. Inspection does not choose a spawn cell, create a bee or advance randomness.";
        if(item instanceof Honeypot.ShatteredPot)return "_The bee's tracked home_\nThis is an existing bee's broken pot, not another unused summon. Picking it up updates matching hostile bees to track the holder; dropping, throwing or moving it updates their recorded pot location. A stack updates at most its quantity of matching bees. Destroying it clears those matching home records. A holder can become the bee's target; carrying the pot does not guarantee friendship or protection. Inspection does not move the pot, change a bee's target or reveal unseen bee locations.";
        if(item instanceof BrokenSeal){
            String text="_Seal protection and upgrades_\nFor normal Warrior sealed-armor protection, the capacity is 3 + 2 × armor tier + Iron Will rank shield points. Armor upgrade level is not part of that capacity formula. A separate non-Warrior Iron Will rule uses the talent's rank as its capacity. Capacity is protection available to the existing activation/cooldown system, not permanent HP or a continuously full shield. The shield's own status describes its current reserve and cooldown. One ordinary upgrade can reside on the seal; affixing an upgraded seal adds one armor level and detaching removes that transferred level. Existing eligibility, curses and glyph-transfer choices still apply.";
            if(hero!=null&&hero.heroClass==com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.WARRIOR&&hero.belongings.armor!=null)text+=" If affixed to your currently worn armor, its normal Warrior capacity would be "+(3+2*hero.belongings.armor.tier+hero.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.IRON_WILL))+" shield points. This conditional preview does not attach or activate it.";
            else if(hero!=null&&hero.hasTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.IRON_WILL))text+=" Your alternate Iron Will capacity is "+hero.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.IRON_WILL)+" shield points under that separate rule.";
            return text;
        }
        if(item instanceof Ankh){
            Ankh ankh=(Ankh)item;
            String text="_Blessing and revival_\nThis Ankh is "+(ankh.isBlessed()?"blessed. ":"unblessed. ")+"Blessing empties a full "+Waterskin.capacity()+"-drop Waterskin and has a base cost of one game-time unit before time-changing effects. On death, an available blessed Ankh is used before an ordinary one. It restores you in place with one quarter of maximum HP, rounded down, and clears the harmful conditions that a healing potion clears. It grants temporary Invulnerability with a base duration of "+n(Invulnerability.DURATION)+" game-time units before duration modifiers; longer existing protection is kept. The run's Healing percentage does not multiply it, including at 0% Healing. ";
            if(hero!=null)text+="At current maximum HP, the blessed revival value is "+Ankh.blessedReviveHP(hero.HT)+" HP. ";
            return text+="\n\n_Ordinary resurrection and recovery_\nAn ordinary Ankh opens a resurrection choice with two item slots for choosing what stays usable. Resurrection starts with full health and a base three game-time units of Invisibility, while most inventory must be recovered from the Lost Backpack. On a normal floor the pack is left at the death location and you return elsewhere. A locked floor is regenerated, with the recovery pack placed on that new floor. Picking up the pack restores access to the existing inventory; it does not create a second set of items.\n\n_Calculation_\nBlessed revival HP = floor(maximum HP / 4). Floor means round down to a whole HP point. This revival assigns its own health value instead of applying an ordinary heal, so changing the Healing percentage does not scale that quarter-HP result.";
        }
        if(item instanceof Waterskin||item instanceof Dewdrop)return DewExplanation.details(item,hero);
        if(item instanceof Torch)return "_Torch time and sight_\nLighting consumes one Torch and has a base cost of "+n(Torch.TIME_TO_LIGHT)+" game-time unit before time-changing effects. It adds "+n(Light.DURATION)+" game-time units to Light's remaining duration before duration modifiers. Repeated torches extend that time rather than adding their sight distances. Light sets base view distance to the greater of the floor's normal view distance and "+Light.DISTANCE+" tiles. Farsight, trinkets, blindness, terrain and other sight rules can still change what is seen. Walls can still block sight, and light does not reveal hidden doors merely because they are in range.";
        if(item instanceof LostBackpack)return "_Recovering lost inventory_\nPicking up this pack ends the Lost Inventory restriction and restores access to your existing items. It reactivates eligible worn equipment and wand charging under the normal recovery rules; items already kept usable are not activated a second time. The base pickup time is "+n(item.pickupDelay())+" game-time unit before time-changing effects. This recovery restores the inventory you already had, rather than providing another storage container or duplicating its items.";
        if(item instanceof ArcaneResin){
            String talent=hero==null?"Your personal Preservation bonus requires a loaded character; no rank is assumed. ":"Your current extra resin yield from Preservation is "+(hero.heroClass==com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE?0:hero.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.WAND_PRESERVATION))+" resin units. ";
            return "_Applying resin_\nAn identified wand below permanent level +"+ArcaneResin.applicationLevelLimit()+" costs current permanent level + 1 resin units to gain one resin bonus and one current charge, followed by its normal capacity update. The level check and cost include existing resin and curse-infusion bonuses; temporary casting bonuses are separate. A normal upgrade or staff imbuing can remove resin under its existing rules.\n\n_Converting a wand into resin_\nConversion consumes the selected wand and "+ArcaneResin.Recipe.energyCost()+" alchemy energy. It requires known, uncursed status. Base yield = 2 × (permanent wand level excluding resin bonuses + 1) resin units. A non-Mage's Wand Preservation adds its trained rank in extra units; the Mage version affects staff imbuing instead. "+talent+"An unknown wand level has no exact preview yield; its true yield is resolved when the wand is consumed. Merely previewing the recipe or inspecting resin does not alter, identify, charge or consume a wand.";
        }
        if(item instanceof LiquidMetal)return MetalExplanation.details();
        if(item instanceof Stylus)return "_Inscribing armor_\nA supported use consumes one Stylus and "+n(Stylus.inscriptionTime())+" game-time units to apply a glyph. The selected armor must have known curse status and must not be cursed or bear a curse glyph; a rejected selection does not consume the Stylus. "+glyphSelection()+" Inscription does not raise armor upgrade level. Inspection does not select armor, roll a glyph, consume time or reveal hidden armor properties.";
        if(item instanceof EnergyCrystal)return "_Alchemy energy units_\nThis pile's quantity is "+item.quantity()+" alchemy energy units. The supported pickup adds that quantity to the run's energy balance through its normal pickup action; it is not gold, weapon charge or experience. Recipes consume their stated energy separately, and inspection does not collect the pile.";
        if(item instanceof Gold)return "_Gold units_\nThis pile's quantity is "+item.quantity()+" gold. Supported pickup adds that quantity to the run's gold balance through its normal pickup action. It is not alchemy energy, an automatic shop sale or a change to prices; inspection does not collect or spend it.";
        return "";
    }
}
