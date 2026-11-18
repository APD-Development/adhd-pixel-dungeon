package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

/**
 * APD: once you hold the Amulet, every floor you enter shows its full layout, so you can see
 * the rooms you missed on the way up. Map only: secret doors stay hidden and nothing is identified.
 */
public final class AmuletReveal {
	private AmuletReveal(){}

	public static boolean applies(Hero hero){
		return hero != null && hero.belongings != null && hero.belongings.getItem(Amulet.class) != null;
	}

	/** Marks every discoverable cell as mapped. Returns how many cells were newly mapped. */
	public static int reveal(Level level){
		if (level == null || level.mapped == null || level.discoverable == null) return 0;
		int n = Math.min(level.mapped.length, level.discoverable.length), added = 0;
		for (int i = 0; i < n; i++){
			if (level.discoverable[i] && !level.mapped[i]){
				level.mapped[i] = true;
				added++;
			}
		}
		return added;
	}
}
