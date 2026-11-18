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

package com.shatteredpixel.shatteredpixeldungeon.windows;

//The warning and choices remain the owner's wording. Both the explanation and choices
//wrap and scroll on narrow screens; Back still means "I'LL PLAY 10 RUNS".
public class WndDemoUnlock extends WndUimsdChoices {
    public interface Choice {
        void confusion();
        void playRuns();
    }
    private final Choice choice;

    public WndDemoUnlock(Choice choice){
        super(UimsdGate.WARNING_TITLE, UimsdGate.WARNING_TEXT, UimsdGate.CHOICE_CONFUSION, UimsdGate.CHOICE_PLAY);
        this.choice = choice;
    }

    @Override protected void onSelect(int index){
        if (index == 0) choice.confusion();
        else choice.playRuns();
    }

    @Override public void onBackPressed(){
        hide();
        choice.playRuns();
    }
    @Override public Runnable restoreOnSceneReset(){
        final Choice saved = choice;
        return () -> com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene().addToFront(new WndDemoUnlock(saved));
    }

}
