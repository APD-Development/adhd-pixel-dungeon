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

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

public class WndInfoItem extends Window {
	
	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	//only one WndInfoItem can appear at a time
	private static WndInfoItem INSTANCE;
    private Item displayedItem;

	public WndInfoItem( Heap heap ) {

		super();

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		if (heap.type == Heap.Type.HEAP) {
			fillFields( heap.peek() );

		} else {
			fillFields( heap );

		}
	}
	
	public WndInfoItem( Item item ) {
		super();

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;
		
		fillFields( item );
	}

	@Override
	public void hide() {
		super.hide();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}

	private void fillFields(Heap heap ) {
		
		IconTitle titlebar = new IconTitle( heap );
		titlebar.color( TITLE_COLOR );
		
		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( heap.info(), 6 );

		layoutFields(titlebar, txtInfo);
	}
	
	private void fillFields( Item item ) {
        displayedItem=item;
		
		int color = TITLE_COLOR;
        // Use the same public, read-only value as the item title and inventory slot.
        // Wand.level() performs gameplay curse-infusion cleanup and must not run just to color text.
        int visibleLevel = item.visiblyUpgraded();
		if (item.levelKnown && visibleLevel > 0) {
			color = ItemSlot.UPGRADED;
		} else if (item.levelKnown && visibleLevel < 0) {
			color = ItemSlot.DEGRADED;
		}

		IconTitle titlebar = new IconTitle( item );
		titlebar.color( color );
		
        String visibleInfo=item.info();
		RenderedTextBlock txtInfo = PixelScene.renderTextBlock( visibleInfo, 6 );
		
		layoutFields(titlebar, txtInfo);
	}

	private void layoutFields(IconTitle title, RenderedTextBlock info){
		int width = WIDTH_MIN;

		info.maxWidth(width);

		//window can go out of the screen on landscape, so widen it as appropriate
		while (PixelScene.landscape()
				&& info.height() > 100
				&& width < WIDTH_MAX){
			width += 20;
			info.maxWidth(width);
		}

		//leaves some space to add the journal button in WndUseItem. This is messy I know.
		if (this instanceof WndUseItem){
			title.setRect( 0, 0, width-16, 0 );
		} else {
			title.setRect( 0, 0, width, 0 );
		}
		add( title );

        float bottom=com.shatteredpixel.shatteredpixeldungeon.ui.MechanicsInfoPanel.add(this,info,title.left(),title.bottom()+GAP,width,this instanceof WndUseItem?72:0,()->com.shatteredpixel.shatteredpixeldungeon.apd.mechanics.MechanicsDetails.itemCalculations(displayedItem));
        resize(width,(int)(bottom+2));
	}
}
