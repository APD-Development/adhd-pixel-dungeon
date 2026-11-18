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

package com.shatteredpixel.shatteredpixeldungeon.apd.humor;

// Is this a quiet moment? The caller fills in what the game is doing; any one of these being true means "not now". The humor code never
// looks at the game itself, so it cannot disturb it.
public final class Safety {

	public static final Safety CALM = new Safety( false, false, false, false, false, false, false );

	public final boolean inCombat, hostileInView, windowOpen, bossFight, dialogue, saveOrLoad, heroBusy;

	public Safety( boolean inCombat, boolean hostileInView, boolean windowOpen, boolean bossFight, boolean dialogue, boolean saveOrLoad, boolean heroBusy ){
		this.inCombat = inCombat;
		this.hostileInView = hostileInView;
		this.windowOpen = windowOpen;
		this.bossFight = bossFight;
		this.dialogue = dialogue;
		this.saveOrLoad = saveOrLoad;
		this.heroBusy = heroBusy;
	}

	public boolean ok(){
		return !(inCombat || hostileInView || windowOpen || bossFight || dialogue || saveOrLoad || heroBusy);
	}
}
