/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdCombat;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

public class TornPage extends RemainsItem {

	{
		image = ItemSpriteSheet.TORN_PAGE;
	}

    @Override public String desc(){
        if(Dungeon.hero==null)return Messages.get(this,"desc","10% of your maximum health, rounded");
        return Messages.get(this,"desc_current",com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.preview(Dungeon.hero,Math.round(Dungeon.hero.HT/10f)));
    }

	@Override
	protected void doEffect(Hero hero) {
		int toHeal = Math.round(hero.HT/10f);
		int apdHpBefore = hero.HP;
		hero.HP = Math.min(hero.HP + com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomHealingHooks.amount(hero, toHeal), hero.HT);
		ApdCombat.onHeal( hero, apdHpBefore, ApdCombat.HEAL_GEAR_TORN_PAGE, toHeal );
		hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(toHeal), FloatingText.HEALING );
		Sample.INSTANCE.play( Assets.Sounds.READ );
	}

}

