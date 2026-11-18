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

package com.shatteredpixel.shatteredpixeldungeon.items.spells;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfMetamorphosis;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** APD: a Scroll of Metamorphosis refined at the alchemy table. Instead of one random
 * talent per class, it offers every other class's talent in the chosen tier.
 * Points transfer exactly like the scroll. Cancelling keeps the spell. */
public class GreaterMetamorphosis extends Spell {

	{
		image = ItemSpriteSheet.UNSTABLE_SPELL;
		talentFactor = 2f;
	}

	@Override
	protected void onCast(Hero hero) {
		curItem = this;
		curUser = hero;
		GameScene.show(new ScrollOfMetamorphosis.WndMetamorphChoose());
	}

	/** Called once a replacement has actually been applied. */
	public void consume(Hero hero) {
		detach(hero.belongings.backpack);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public int energyVal() {
		return 12 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ScrollOfMetamorphosis.class};
			inQuantity = new int[]{1};
			cost = 12;
			output = GreaterMetamorphosis.class;
			outQuantity = 1;
		}
	}
}
