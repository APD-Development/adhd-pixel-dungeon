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

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdGuide;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdTutorial;

import java.util.ArrayList;
import java.util.List;

//APD: the UIMSD Guide: the interactive tour first, then every topic. A topic can be read, and (for most) shown on the real screen by
//starting the tour at the step that teaches it.
public final class WndUimsdGuide {

	private WndUimsdGuide(){}

	public interface Starter {
		//start the interactive tour at this step (0 for the beginning)
		void startTour( int stepIndex );
	}

	//the Guide stays open under a topic, so Back from a topic returns to the list of topics
	private static void keepGuide( WndUimsdList w ){
		w.keepOpen();
		ShatteredPixelDungeon.scene().addToFront(w);
	}

	public static void show( final Starter starter ){
		final List<UimsdGuide.Topic> topics = UimsdGuide.topics();
		List<WndUimsdList.Entry> entries = new ArrayList<>();
		entries.add(WndUimsdList.Entry.heading("Take the tour"));
		entries.add(WndUimsdList.Entry.item("Interactive tour", "about two minutes, on the real screens (sample data if you have no runs)"));
		String last = null;
		for (UimsdGuide.Topic t : topics){
			if (!t.section.equals(last)){
				entries.add(WndUimsdList.Entry.heading(t.section));
				last = t.section;
			}
			entries.add(WndUimsdList.Entry.item(t.title));
		}
		keepGuide(WndUimsdList.menu("Guide", "Read a topic, or have it shown on the real screen.", entries, new WndUimsdList.Picker(){
			@Override
			public void onPick( int i, boolean checked ){
				if (i == 0){
					WndUimsdList.closeAll();
					starter.startTour(0);
					return;
				}
				final UimsdGuide.Topic topic = topics.get(i - 1);
				final int step = UimsdTutorial.indexOf(topic.tourStep);
				ShatteredPixelDungeon.scene().addToFront(new WndUimsdTopic(topic, step >= 0, new Runnable(){
					@Override
					public void run(){
						WndUimsdList.closeAll();
						starter.startTour(step);
					}
				}));
			}
		}).restoreWith(() -> show(starter)));
	}
}
