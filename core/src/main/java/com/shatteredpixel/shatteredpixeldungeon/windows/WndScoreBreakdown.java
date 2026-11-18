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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Group;
import com.watabou.noosa.ui.Component;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;

import java.text.NumberFormat;

public class WndScoreBreakdown extends Window {

	private static final int WIDTH			= 115;

	private int GAP	= 4;

	public WndScoreBreakdown(){this(null,Statistics.totalScore);}

	public WndScoreBreakdown(com.watabou.utils.Bundle recordedGameData){this(recordedGameData,Statistics.totalScore);}

	public WndScoreBreakdown(com.watabou.utils.Bundle recordedGameData,long originalScore){

		Component content=new Component();
		IconTitle title = new IconTitle( Icons.get(Icons.INFO), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 16);
		content.add(title);

		float pos = title.bottom()+2;

		NumberFormat num = NumberFormat.getInstance(Messages.locale());
		if (Dungeon.initialVersion > ShatteredPixelDungeon.v1_2_3) {
			pos = statSlot(content, Messages.get(this, "progress_title"),
					num.format(Statistics.progressScore), pos, Statistics.progressScore >= 50_000);
			pos = addInfo(content, Messages.get(this, "progress_desc"), pos);
			pos = statSlot(content, Messages.get(this, "treasure_title"),
					num.format(Statistics.treasureScore), pos, Statistics.treasureScore >= 20_000);
			pos = addInfo(content, Messages.get(this, "treasure_desc"), pos);
			pos = statSlot(content, Messages.get(this, "explore_title"),
					num.format(Statistics.exploreScore), pos, Statistics.exploreScore >= 20_000);
			pos = addInfo(content, Messages.get(this, "explore_desc"), pos);
			pos = statSlot(content, Messages.get(this, "bosses_title"),
					num.format(Statistics.totalBossScore), pos, Statistics.totalBossScore >= 15_000);
			pos = addInfo(content, Messages.get(this, "bosses_desc"), pos);
			pos = statSlot(content, Messages.get(this, "quests_title"),
					num.format(Statistics.totalQuestScore), pos, Statistics.totalQuestScore >= 10_000);
			pos = addInfo(content, Messages.get(this, "quests_desc"), pos);
		} else {
			pos = statSlot(content, Messages.get(this, "progress_title"),
					num.format(Statistics.progressScore), pos, Statistics.progressScore >= 78_000);
			pos = addInfo(content, Messages.get(this, "progress_desc"), pos);
			pos = statSlot(content, Messages.get(this, "treasure_title"),
					num.format(Statistics.treasureScore), pos, Statistics.treasureScore >= 30_000);
			pos = addInfo(content, Messages.get(this, "treasure_desc_old"), pos);
		}

		if (Statistics.winMultiplier > 1) {
			pos = statSlot(content, Messages.get(this, "win_multiplier"), Messages.decimalFormat("#.##", Statistics.winMultiplier) + "x", pos, false);
		}
		{
			pos = statSlot(content, Messages.get(this, "challenge_multiplier"), Messages.decimalFormat("#.##", Statistics.chalMultiplier) + "x", pos, false);
		}
        pos=statSlot(content,"Custom Rules Multiplier",com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScorePresentation.multiplier(recordedGameData),pos,false);
        String customNotice=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScorePresentation.notice(recordedGameData);
        if(!customNotice.isEmpty())pos=addInfo(content,customNotice,pos);
		pos = statSlot(content, "Original score", num.format(originalScore), pos, false);
        Long adjusted=com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScorePresentation.adjustedIdentityScore(recordedGameData,originalScore);
        String unavailable="Not applied".equals(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScorePresentation.multiplier(recordedGameData))?"Not applied":"?";
        pos=statSlot(content,"Custom-rules adjusted score",adjusted==null?unavailable:num.format(adjusted),pos,false);
        com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedReuseScore.Result reuse =
                com.shatteredpixel.shatteredpixeldungeon.apd.recorder.SeedReuseScore.restore(recordedGameData);
        if (reuse != null) {
            pos=statSlot(content,"Seed reuse retention",
                    Messages.decimalFormat("#.##",reuse.retentionBasisPoints/100f)+"%",pos,false);
            pos=statSlot(content,"Seed reuse deduction","-"+num.format(reuse.deduction),pos,false);
            pos=statSlot(content,"Final earned score",num.format(reuse.earned),pos,false);
            pos=addInfo(content,"Earlier local attempts: "+reuse.priorAttempts
                    +". Deepest earlier floor: "+(reuse.deepestBefore<0?"unknown":reuse.deepestBefore)
                    +". Current deepest floor: "+reuse.currentDepth+".",pos);
            if (reuse.known && reuse.priorAttempts > 0) {
                pos=addInfo(content,reuse.individualDepthsKnown
                        ? "Replay scoring: verified individual earlier depths."
                        : "Replay scoring: conservative fallback; individual earlier depths unavailable.",pos);
            }
        } else if (recordedGameData != null && recordedGameData.contains("custom_seed")
                && !recordedGameData.getString("custom_seed").isEmpty()) {
            // Only historical seeded records lack an earlier reuse-scoring snapshot.
            // Ordinary and daily games never used a custom seed and need no disclaimer.
            pos=addInfo(content,"This seeded run predates recorded seed-reuse scoring.",pos);
        }

		if (Dungeon.initialVersion <= ShatteredPixelDungeon.v1_2_3){
			pos = addInfo(content, Messages.get(this, "old_score_desc"), pos);
		}

		content.setSize(WIDTH,pos);
        ScrollPane pane=new ScrollPane(content);add(pane);
        int visibleHeight=Math.min((int)Math.ceil(pos),Math.max(32,(int)PixelScene.uiCamera.height-20));
        resize(WIDTH,visibleHeight);
        pane.setRect(0,0,WIDTH,visibleHeight);

	}

	private float statSlot(Group parent, String label, String value, float pos, boolean highlight ) {

		RenderedTextBlock txt = PixelScene.renderTextBlock( label, 7 );
		txt.maxWidth((int)(WIDTH*0.66f));
		if (highlight) txt.hardlight(Window.TITLE_COLOR);
		txt.setPos(0, pos);
		parent.add( txt );

		float labelHeight=txt.height();
		txt = PixelScene.renderTextBlock( value, 7 );
		if (highlight) txt.hardlight(Window.TITLE_COLOR);
		if(txt.width()>WIDTH*0.3f){pos+=labelHeight+1;labelHeight=0;txt.setPos(0,pos);}
        else txt.setPos(WIDTH-txt.width(), pos);
		PixelScene.align(txt);
		parent.add( txt );

		return pos + GAP + Math.max(labelHeight,txt.height());
	}

	private float addInfo(Group parent, String info, float pos){

		RenderedTextBlock txt = PixelScene.renderTextBlock( info, 5 );
		txt.maxWidth(WIDTH);
		txt.hardlight(0x999999);
		txt.setPos(0, pos-2);
		parent.add( txt );

		return pos - 2 + GAP + txt.height();

	}


}

