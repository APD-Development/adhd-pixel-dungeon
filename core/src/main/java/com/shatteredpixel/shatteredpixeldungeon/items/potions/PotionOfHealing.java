/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.Arrays;

public class PotionOfHealing extends Potion {

	{
		icon = ItemSpriteSheet.Icons.POTION_HEALING;

		bones = true;
	}
	
    @Override
    public void execute(final Hero hero, final String action) {
        if (AC_DRINK.equals(action) && com.shatteredpixel.shatteredpixeldungeon.apd.healing.HealingConfirmation.required(
                isKnown(), com.shatteredpixel.shatteredpixeldungeon.SPDSettings.healingConfirmationThreshold(), hero.HP, hero.HT)) {
            requestHealingConfirmation(hero, () -> PotionOfHealing.super.execute(hero, action));
            return;
        }
        super.execute(hero, action);
    }

    protected void requestHealingConfirmation(Hero hero, final Runnable confirmed) {
        com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.show(
                new com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions(
                        new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(this), name(),
                        "Use this healing potion at your current HP?", "USE HEALING ITEM", "CANCEL") {
                    @Override protected void onSelect(int index) { if (index == 0) confirmed.run(); }
                });
    }

	@Override
	public void apply( Hero hero ) {
		identify();
		cure( hero );
		heal( hero );
	}

	public static void heal( Char ch ){
		if (ch == Dungeon.hero && Dungeon.isChallenged(Challenges.NO_HEALING)){
			pharmacophobiaProc(Dungeon.hero);
		} else {
			//starts out healing 30 hp, equalizes with hero health total at level 11
			Healing healing = Buff.affect(ch, Healing.class);
			healing.setHeal((int) (0.8f * ch.HT + 14), 0.25f, 0, true);
			if (ch == Dungeon.hero){
				GLog.p( Messages.get(PotionOfHealing.class, "heal") );
			}
		}
	}

	public static void pharmacophobiaProc( Hero hero ){
		// harms the hero for ~40% of their max HP in poison
		Buff.affect( hero, Poison.class).set(4 + hero.lvl/2);
	}
	
	public static void cure( Char ch ) {
		Buff.detach( ch, Poison.class );
		Buff.detach( ch, Cripple.class );
		Buff.detach( ch, Weakness.class );
		Buff.detach( ch, Vulnerable.class );
		Buff.detach( ch, Bleeding.class );
		Buff.detach( ch, Blindness.class );
		Buff.detach( ch, Drowsy.class );
		Buff.detach( ch, Slow.class );
		Buff.detach( ch, Vertigo.class);
	}


	// Uncollected enemy-drop provenance follows a potion through floor piles and save/load.
	// It is deliberately separate from inventory quantity and from the permanent per-run counts.
	private static final String APD_ENEMY_DROP_REGIONS = "apd_healing_enemy_drop_regions_v1";
	private int[] apdUncollectedEnemyDrops = new int[5];

	/** Called by a hostile mob at the actual drop site, never by ordinary floor/shop generation. */
	public void apdMarkEnemyDrop(int depth) {
		int region = Statistics.enemyHealingRegionForDepth(depth);
		if (region < 0) return;
		int pending = 0;
		for (int count : apdUncollectedEnemyDrops) pending += count;
		if (quantity() > pending) apdUncollectedEnemyDrops[region] += quantity() - pending;
	}

	/** Merge loot origins as well as quantities, including mixed-origin floor piles. */
	@Override
	public Item merge(Item other) {
		int oldQuantity = other.quantity();
		Item result = super.merge(other);
		if (result == this && oldQuantity > 0 && other.quantity() == 0 && other instanceof PotionOfHealing) {
			int[] origins = ((PotionOfHealing)other).apdUncollectedEnemyDrops;
			for (int i = 0; i < 5; i++) apdUncollectedEnemyDrops[i] += origins[i];
		}
		return result;
	}

	/** Splitting a still-uncollected stack must not duplicate its enemy origins. */
	@Override
	public Item split(int amount) {
		Item separated = super.split(amount);
		if (separated instanceof PotionOfHealing) {
			PotionOfHealing piece = (PotionOfHealing)separated;
			Arrays.fill(piece.apdUncollectedEnemyDrops, 0);
			int remaining = amount;
			for (int i = 0; i < 5; i++) {
				int taken = Math.min(remaining, apdUncollectedEnemyDrops[i]);
				piece.apdUncollectedEnemyDrops[i] = taken;
				apdUncollectedEnemyDrops[i] -= taken;
				remaining -= taken;
			}
		}
		return separated;
	}

	/** Counts only once, only after the inventory accepted this pickup. */
	public void apdClaimCollectedEnemyDrops() {
		Statistics.recordEnemyHealingPickup(apdUncollectedEnemyDrops);
		Arrays.fill(apdUncollectedEnemyDrops, 0);
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		// super may merge this stack into the bandolier before returning.
		int[] acceptedOrigins = apdUncollectedEnemyDrops.clone();
		if (!super.doPickUp(hero, pos)) return false;
		Statistics.recordEnemyHealingPickup(acceptedOrigins);
		Arrays.fill(apdUncollectedEnemyDrops, 0);
		PotionOfHealing carried = hero.belongings.getItem(PotionOfHealing.class);
		if (carried != null) Arrays.fill(carried.apdUncollectedEnemyDrops, 0);
		return true;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		for (int n : apdUncollectedEnemyDrops) {
			if (n > 0) {
				bundle.put(APD_ENEMY_DROP_REGIONS, apdUncollectedEnemyDrops);
				break;
			}
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		apdUncollectedEnemyDrops = new int[5];
		if (bundle.contains(APD_ENEMY_DROP_REGIONS)) {
			int[] stored = bundle.getIntArray(APD_ENEMY_DROP_REGIONS);
			if (stored != null && stored.length == 5) {
				int remaining = Math.max(0, quantity());
				for (int i = 0; i < 5; i++) {
					apdUncollectedEnemyDrops[i] = Math.min(remaining, Math.max(0, stored[i]));
					remaining -= apdUncollectedEnemyDrops[i];
				}
			}
		}
	}

	@Override
	public void reset() {
		super.reset();
		// Potion's constructor calls reset before subclass field initialization.
		if (apdUncollectedEnemyDrops == null) apdUncollectedEnemyDrops = new int[5];
		else Arrays.fill(apdUncollectedEnemyDrops, 0);
	}

    @Override
    public String desc() {
        if (isKnown() && Dungeon.hero != null && Dungeon.isChallenged(Challenges.NO_HEALING)) {
            return Messages.get(this, "desc_pharmacophobia");
        }
        if (isKnown() && Dungeon.hero != null && com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.disabled()) {
            return Messages.get(this, "desc_disabled_healing");
        }
        return super.desc();
    }

	@Override
	public String info() {
		String description = super.info();
		if (!isKnown() || Dungeon.hero == null) return description;
		StringBuilder out = new StringBuilder(description);
		out.append("\n\n_").append(Messages.get(this, "enemy_regions")).append("_");
		if (!Statistics.enemyHealingPotionsRecorded) {
			out.append("\n").append(Messages.get(this, "enemy_regions_legacy"));
		}
		for (int region = 0; region < 5; region++) {
			out.append("\n").append(Messages.get(this, "enemy_region_" + region))
				.append(": ").append(Statistics.enemyHealingPotionsByRegion[region]);
		}
		return out.toString();
	}

	@Override
	public int value() {
		return isKnown() ? 30 * quantity : super.value();
	}
}
