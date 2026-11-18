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
package com.shatteredpixel.shatteredpixeldungeon.apd.healing;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.replay.ReplayContext;
import com.watabou.utils.Bundle;

/** One-time owner-requested adoption of the selected chance when continuing an older run. */
public final class HealingConversionUpgrade {
    public static final String KEY="apd_healing_conversion_version";
    public static final int VERSION=2;
    private HealingConversionUpgrade() {}
    public static void store(Bundle save){save.put(KEY,VERSION);}
    public static void applyToLoadedRun(Bundle save){
        if(save.getInt(KEY)>=VERSION || ReplayContext.active() || !Dungeon.apdExoticKnown)return;
        int before=Dungeon.apdExotic;
        Dungeon.apdExotic=SPDSettings.apdExoticSetting();
        com.watabou.utils.SupportDiagnostics.event("healing.conversion.upgrade","previous="+before+";selected="+Dungeon.apdExotic+";version="+VERSION);
    }
}
