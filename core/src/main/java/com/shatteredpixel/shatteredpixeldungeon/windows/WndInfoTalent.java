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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Callback;

public class WndInfoTalent extends Window {

	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	public WndInfoTalent(Talent talent, int points, TalentButtonCallback buttonCallback){
		super();

		int width = WIDTH_MIN;

		IconTitle titlebar = new IconTitle();

		titlebar.icon( new TalentIcon( talent ) );
		String title = Messages.titleCase(talent.title());
		if (points > 0){
			title += " +" + points;
		}
		titlebar.label( title, Window.TITLE_COLOR );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		boolean metaDesc = (buttonCallback != null && buttonCallback.metamorphDesc()) ||
				(Dungeon.hero != null && Dungeon.hero.metamorphedTalents.containsValue(talent));

		String description = talent.desc(metaDesc);
        if (buttonCallback != null && !buttonCallback.extraInfo().isEmpty())
            description = buttonCallback.extraInfo() + "\n\n" + description;
        RenderedTextBlock txtInfo = PixelScene.renderTextBlock(description, 6);
		txtInfo.maxWidth(width);
		txtInfo.setPos(titlebar.left(), titlebar.bottom() + 2*GAP);


		while (PixelScene.landscape()
				&& txtInfo.height() > 120
				&& width < WIDTH_MAX){
			width += 20;
			txtInfo.maxWidth(width);
		}
		titlebar.setRect( 0, 0, width, 0 );
        final String visibleDescription=description;
        float descriptionBottom=com.shatteredpixel.shatteredpixeldungeon.ui.MechanicsInfoPanel.add(this,txtInfo,titlebar.left(),titlebar.bottom()+2*GAP,width,buttonCallback==null?0:22,()->com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.MechanicsDetails.talent(talent,points,visibleDescription,metaDesc));
        resize(width,(int)(descriptionBottom+GAP));

		if (buttonCallback != null) {
			RedButton button = new RedButton( buttonCallback.prompt() ) {
				@Override
				protected void onClick() {
					super.onClick();
					hide();
					buttonCallback.call();
				}
			};
			button.enable(buttonCallback.enabled());
            button.icon(Icons.get(Icons.TALENT));
			button.setRect(0, descriptionBottom + 2*GAP, width, 18);
			add(button);
			resize( width, (int)button.bottom()+1 );
		}

	}

	public static abstract class TalentButtonCallback implements Callback {

		public abstract String prompt();

        public boolean enabled() { return true; }

        public String extraInfo() { return ""; }

		public boolean metamorphDesc(){
			return false;
		}

	}

}

