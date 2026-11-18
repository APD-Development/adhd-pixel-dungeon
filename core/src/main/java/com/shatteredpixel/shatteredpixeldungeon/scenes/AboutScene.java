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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

public class AboutScene extends PixelScene {

	//APD (Nov 18, Chris): About opens on a two-button menu; APD and the original credits each have their own page.
	public enum Section { MENU, APD, CREDITS }
	public static Section section = Section.MENU;

	private static void show(Section s){
		section = s;
		ShatteredPixelDungeon.switchNoFade(AboutScene.class);
	}

	@Override
	public void create() {
		super.create();

		final float colWidth = landscape() ? 120 : 130;
		final float fullWidth = colWidth * (landscape() ? 2 : 1);

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground( w, h );
		add( BG );

		//darkens the arches
		add(new ColorBlock(w, h, 0x44000000));

		ScrollPane list = new ScrollPane( new Component() );
		add( list );

		Component content = list.content();
		content.clear();

		//*** ADHD Pixel Dungeon ***

		final int APD_COLOR = 0x44DD88; //differs from the stock SHPX, Pixel Dungeon and libGDX colors
		CreditsBlock apd = new CreditsBlock(true, APD_COLOR,
				"ADHD Pixel Dungeon", null,
				"Modified by Chris Charlton (2026)\n" +
				"Version " + com.watabou.noosa.Game.version + " (build " + com.watabou.noosa.Game.versionCode + ")\n" +
				"An unofficial modified version of Shattered Pixel Dungeon by Evan Debenham, based on Pixel Dungeon by Oleg Dolya (Watabou). APD is not affiliated with or endorsed by Evan Debenham or Shattered Pixel.\n" +
				(com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdAdultHumor()
                    ? "Founded on the principle that shielding should have fucking worked."
                    : "Founded on the principle that shielding should have worked."), null, null);
		apd.setRect((w - colWidth)/2f, insets.top + 6, colWidth, 0);
		content.add(apd);
		float infoTop = apd.bottom() + 4;
		final float infoWidth = Math.min(w - 12, 220);
		RedButton info0 = new RedButton("Why APD Exists", 6){
			@Override protected void onClick(){
				AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText("Why APD Exists", (com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdAdultHumor()
                        ? "APD began with the shielding mechanic: shielding should have fucking worked."
                        : "APD began with the shielding mechanic: shielding should have worked.") + "\n\nAfter years playing Shattered Pixel Dungeon, I wanted to expand the experience. APD builds on SPD's accomplishments, rather than diminishing them."));
			}
		};
		info0.multiline = true;
		info0.setRect((w - infoWidth)/2f + 0 * (infoWidth + 2)/2f, infoTop + 0 * 22, (infoWidth - 2)/2f, 20);
		content.add(info0);
		RedButton info1 = new RedButton("Features & Vision", 6){
			@Override protected void onClick(){
				AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText("Features & Vision", "Available: UIMSD statistics, filtering and comparisons, plus gameplay information and player settings.\n\nPlanned: additional characters, items, mechanics and challenges; deeper statistics and meaningful replayability.\n\nAPD will have strictly no pay-to-win mechanics."));
			}
		};
		info1.multiline = true;
		info1.setRect((w - infoWidth)/2f + 1 * (infoWidth + 2)/2f, infoTop + 0 * 22, (infoWidth - 2)/2f, 20);
		content.add(info1);
		RedButton info2 = new RedButton("Community & Voting", 6){
			@Override protected void onClick(){
				AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText("Community & Voting", "Available now: an offline Community preview.\n\nPlanned: player-created submissions and threaded discussions in Month 1; qualification voting selecting the top 16 in Month 2; weekly elimination rounds in Month 3. Comments are not votes.\n\nFuture ambitions include run reviews and peer feedback, community mods and content, and testing winning ideas during development. Some content remains exclusively creator-controlled. Online interaction is not available yet."));
			}
		};
		info2.multiline = true;
		info2.setRect((w - infoWidth)/2f + 0 * (infoWidth + 2)/2f, infoTop + 1 * 22, (infoWidth - 2)/2f, 20);
		content.add(info2);
		RedButton info3 = new RedButton("Developer's Note", 6){
			@Override protected void onClick(){
				AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText("Developer's Note", "I've spent years playing Shattered Pixel Dungeon, and APD is my attempt to build the experience I've always wanted.\n\nI'm one person developing a game for people who love Pixel Dungeon as much as I do.\n\nMy goal is for players to never run out of content they actually WANT to play.\n\nAnd if the community says an idea is terrible, I might implement two."));
			}
		};
		info3.multiline = true;
		info3.setRect((w - infoWidth)/2f + 1 * (infoWidth + 2)/2f, infoTop + 1 * 22, (infoWidth - 2)/2f, 20);
		content.add(info3);
		RedButton info4 = new RedButton("Development Roadmap", 6){
			@Override protected void onClick(){
				AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText("Development Roadmap", "Priorities: bug fixes, reliability and save protection; statistics and UIMSD improvements; additional gameplay content; Community features; future modding support.\n\nThese are development goals, not promises of availability or release dates."));
			}
		};
		info4.multiline = true;
		info4.setRect((w - infoWidth)/2f + 0 * (infoWidth + 2)/2f, infoTop + 2 * 22, (infoWidth - 2)/2f, 20);
		content.add(info4);
		RedButton info5 = new RedButton("Source & Licenses", 6){
			@Override protected void onClick(){
                AboutScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions(
                        "Source & Licenses", com.shatteredpixel.shatteredpixeldungeon.apd.GplLicense.NOTICE,
                        "GPLv3 (offline)", "APD source") {
                    @Override protected void onSelect(int index) {
                        if (index == 0) com.shatteredpixel.shatteredpixeldungeon.windows.WndGplLicense.show();
                        else ShatteredPixelDungeon.platform.openURI(com.shatteredpixel.shatteredpixeldungeon.apd.GplLicense.SOURCE_URL);
                    }
                });
			}
		};
		info5.multiline = true;
		info5.setRect((w - infoWidth)/2f + 1 * (infoWidth + 2)/2f, infoTop + 2 * 22, (infoWidth - 2)/2f, 20);
		content.add(info5);
		addLine(infoTop + 70, content);
		final float creditsTop = infoTop + 78;

		//*** Shattered Pixel Dungeon Credits ***

		CreditsBlock shpx = new CreditsBlock(true, Window.SHPX_COLOR,
				"Shattered Pixel Dungeon",
				null,
				"Developed by: _Evan Debenham_\nBased on Pixel Dungeon's open source",
				"ShatteredPixel.com",
				"https://ShatteredPixel.com");
		if (landscape()){
			shpx.setRect((w - fullWidth)/2f - 6, creditsTop + 20, 120, 0);
		} else {
			shpx.setRect((w - fullWidth)/2f, creditsTop, 120, 0);
		}
		content.add(shpx);

		CreditsBlock aleks = new CreditsBlock(false, Window.SHPX_COLOR,
				"Splash & Dungeon Art:",
				Icons.ALEKS.get(),
				"Aleksandar Komitov",
				"alekskomitov.com",
				"https://www.alekskomitov.com/");
		aleks.setSize(colWidth/2f, 0);
		if (landscape()){
			aleks.setPos(shpx.right(), creditsTop);
		} else {
			aleks.setPos(w/2f - colWidth/2f, shpx.bottom()+6);
		}
		content.add(aleks);

		CreditsBlock lumine = new CreditsBlock(false, Window.SHPX_COLOR,
				"Composer:",
				Icons.LUMINE.get(),
				"Lumine Haaristo",
				"youtube.com/@Lumine...",
				"https://www.youtube.com/@LumineThomasHaaristo");
		lumine.setRect(aleks.right(), aleks.top(), colWidth/2f, 0);
		content.add(lumine);

		CreditsBlock pumpkin = new CreditsBlock(false, Window.SHPX_COLOR,
				"Item Pixel Art:",
				Icons.PUMPKINVOLT.get(),
				"PumpkinVolt",
				null,
				null);
		pumpkin.setSize(colWidth/2f, 0);
		pumpkin.setRect(aleks.left(), aleks.bottom()+6, colWidth/2f, 0);
		content.add(pumpkin);

		CreditsBlock celesti = new CreditsBlock(false, Window.SHPX_COLOR,
				"Sound Effects:",
				Icons.CELESTI.get(),
				"Celesti",
				"celesti-whispers.itch.io",
				"https://celesti-whispers.itch.io/");
		celesti.setRect(pumpkin.right(), pumpkin.top(), colWidth/2f, 0);
		content.add(celesti);

		CreditsBlock alastair = new CreditsBlock(false, Window.SHPX_COLOR,
				"Additional Pixel Art:",
				Icons.ALASTAIR.get(),
				"Alastair Braun",
				null,
				null);
		alastair.setSize(colWidth/2f, 0);
		alastair.setRect(pumpkin.left(), celesti.bottom()+6, colWidth/2f, 0);
		content.add(alastair);

		//*** Pixel Dungeon Credits ***

		final int WATA_COLOR = 0x55AAFF;
		CreditsBlock wata = new CreditsBlock(true, WATA_COLOR,
				"Pixel Dungeon",
				Icons.WATA.get(),
				"Developed by: _Watabou_\nInspired by Brian Walker's Brogue",
				"watabou.itch.io",
				"https://watabou.itch.io/");
		if (landscape()){
			wata.setRect(shpx.left(), alastair.bottom() + 8, colWidth, 0);
		} else {
			wata.setRect(shpx.left(), alastair.bottom() + 8, colWidth, 0);
		}
		content.add(wata);

		addLine(wata.top() - 4, content);

		CreditsBlock cube = new CreditsBlock(false, WATA_COLOR,
				"Music:",
				Icons.CUBE_CODE.get(),
				"Cube Code",
				null,
				null);
		cube.setSize(colWidth/2f, 0);
		if (landscape()){
			cube.setPos(wata.right() + colWidth/4f, wata.top() + (wata.height() - cube.height())/2f);
		} else {
			cube.setPos(aleks.left() + colWidth/4f, wata.bottom()+6);
		}
		content.add(cube);

		//*** libGDX Credits ***

		final int GDX_COLOR = 0xE44D3C;
		CreditsBlock gdx = new CreditsBlock(true,
				GDX_COLOR,
				"libGDX",
				Icons.LIBGDX.get(),
				"APD is powered by _libGDX_!",
				"libgdx.com",
				"https://libgdx.com/");
		if (landscape()){
			gdx.setRect(wata.left(), wata.bottom() + 8, colWidth, 0);
		} else {
			gdx.setRect(wata.left(), cube.bottom() + 8, colWidth, 0);
		}
		content.add(gdx);

		addLine(gdx.top() - 4, content);

		CreditsBlock arcnor = new CreditsBlock(false, GDX_COLOR,
				"Pixel Dungeon GDX:",
				Icons.ARCNOR.get(),
				"Edu García",
				"gamedev.place/@arcnor",
				"https://mastodon.gamedev.place/@arcnor");
		arcnor.setSize(colWidth/2f, 0);
		if (landscape()){
			arcnor.setPos(gdx.right(), gdx.top() + (gdx.height() - arcnor.height())/2f);
		} else {
			arcnor.setPos(aleks.left(), gdx.bottom()+6);
		}
		content.add(arcnor);

		CreditsBlock purigro = new CreditsBlock(false, GDX_COLOR,
				"Shattered GDX Help:",
				Icons.PURIGRO.get(),
				"Kevin MacMartin",
				"github.com/prurigro",
				"https://github.com/prurigro/");
		purigro.setRect(arcnor.right()+2, arcnor.top(), colWidth/2f, 0);
		content.add(purigro);

		//*** Transifex Credits ***

		CreditsBlock transifex = new CreditsBlock(true,
				Window.TITLE_COLOR,
				null,
				null,
				"Inherited Shattered Pixel Dungeon translations were contributed by volunteers through Transifex. Thank you to the original translators! APD-specific text may remain in English.",
				null,
				null);
		transifex.setRect((Camera.main.width - colWidth)/2f, purigro.bottom() + 12, colWidth, 0);
		content.add(transifex);

		addLine(transifex.top() - 4, content);

		addLine(transifex.bottom() + 4, content);

		//*** Freesound Credits ***

		CreditsBlock freesound = new CreditsBlock(true,
				Window.TITLE_COLOR,
				null,
				null,
				"APD inherits the following sound samples from Shattered Pixel Dungeon from _freesound.org_:\n\n" +

				"Creative Commons Attribution License:\n" +
				"_SFX ATTACK SWORD 001.wav_ by _JoelAudio_\n" +
				"_Pack: Slingshots and Longbows_ by _saturdaysoundguy_\n" +
				"_Cracking/Crunching, A.wav_ by _InspectorJ_\n" +
				"_Extracting a sword.mp3_ by _Taira Komori_\n" +
				"_Pack: Uni Sound Library_ by _timmy h123_\n\n" +

				"Creative Commons Zero License:\n" +
				"_Pack: Movie Foley: Swords_ by _Black Snow_\n" +
				"_machine gun shot 2.flac_ by _qubodup_\n" +
				"_m240h machine gun burst 4.flac_ by _qubodup_\n" +
				"_Pack: Onomatopoeia_ by _Adam N_\n" +
				"_Pack: Watermelon_ by _lolamadeus_\n" +
				"_metal chain_ by _Mediapaja2009_\n" +
				"_Pack: Sword Clashes Pack_ by _JohnBuhr_\n" +
				"_Pack: Metal Clangs and Pings_ by _wilhellboy_\n" +
				"_Pack: Stabbing Stomachs & Crushing Skulls_ by _TheFilmLook_\n" +
				"_Sheep bleating_ by _zachrau_\n" +
				"_Lemon,Juicy,Squeeze,Fruit.wav_ by _Filipe Chagas_\n" +
				"_Lemon,Squeeze,Squishy,Fruit.wav_ by _Filipe Chagas_",
				"freesound.org",
				"https://www.freesound.org");
		freesound.setRect(transifex.left()-10, transifex.bottom() + 8, colWidth+20, 0);
		content.add(freesound);

		content.setSize( fullWidth, freesound.bottom()+10 + insets.bottom );

		if (section == Section.MENU){
			list.visible = list.active = false;
			com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock title = renderTextBlock("About Game", 9);
			title.hardlight(Window.TITLE_COLOR);
			title.setPos((w - title.width())/2f, insets.top + 8);
			add(title);
			float bw = Math.min(colWidth, w - 20);
			RedButton toApd = new RedButton("ADHD Pixel Dungeon"){
				@Override protected void onClick(){ show(Section.APD); }
			};
			toApd.setRect((w - bw)/2f, h/2f - 26, bw, 22);
			add(toApd);
			RedButton toCredits = new RedButton("Shattered & Pixel Dungeon credits"){
				@Override protected void onClick(){ show(Section.CREDITS); }
			};
			toCredits.multiline = true;
			toCredits.setRect((w - bw)/2f, h/2f + 4, bw, 22);
			add(toCredits);
			addExit();
			return;
		}

		//keep only this page's part of the scroll content
		final float cut = creditsTop - 8;
		for (com.watabou.noosa.Gizmo g : content.snapshot()){
			if (g == null) continue;
			float top = g instanceof Component ? ((Component) g).top() : g instanceof com.watabou.noosa.Visual ? ((com.watabou.noosa.Visual) g).y : 0;
			boolean apdPart = top < cut;
			if (apdPart != (section == Section.APD)) {
				content.remove(g);
			} else if (section == Section.CREDITS) {
				if (g instanceof Component) ((Component) g).setPos(((Component) g).left(), top - creditsTop + insets.top + 4);
				else if (g instanceof com.watabou.noosa.Visual) ((com.watabou.noosa.Visual) g).y = top - creditsTop + insets.top + 4;
			}
		}
		if (section == Section.CREDITS){
			content.setSize(fullWidth, freesound.bottom() + 10 + insets.bottom);
			list.setRect(0, 0, w, h);
			list.allowDragFromButtons();
			list.scrollTo(0, 0);
			addExit();
			return;
		}
		content.setSize(fullWidth, cut + insets.bottom);

		//APD: News and Changes buttons in a bar along the bottom, the credits scroll above it
		final float apdBarHeight = 22 + insets.bottom;
		final float apdBtnWidth = Math.min(colWidth, (w - 6) / 2f - 1);

		add(new ColorBlock(w, apdBarHeight, 0xFF000000){
			{ y = h - apdBarHeight; }
		});

		RedButton btnApdNews = new RedButton("APD News"){
			@Override
			protected void onClick() {
				APDTextScene.page = APDTextScene.Page.NEWS;
				APDTextScene.backTo = AboutScene.class;
				ShatteredPixelDungeon.switchScene(APDTextScene.class);
			}
		};
		btnApdNews.setRect(w/2f - apdBtnWidth - 1, h - apdBarHeight + 1, apdBtnWidth, 20);
		add(btnApdNews);

		RedButton btnApdChanges = new RedButton("APD Changes"){
			@Override
			protected void onClick() {
				ChangesScene.changesSelected = 0; ShatteredPixelDungeon.switchScene( ChangesScene.class );
			}
		};
		btnApdChanges.setRect(w/2f + 1, btnApdNews.top(), apdBtnWidth, 20);
		add(btnApdChanges);

		list.setRect( 0, 0, w, h - apdBarHeight );
		list.allowDragFromButtons();
		list.scrollTo(0, 0);

		addExit();
	}

	private void addExit(){
		ExitButton btnExit = new ExitButton(){
			@Override protected void onClick(){ onBackPressed(); }
		};
		int ofs = PixelScene.landscape() ? 0 : 4;
		btnExit.setPos( Camera.main.width - btnExit.width() - ofs, ofs );
		add( btnExit );
	}
	
	@Override
	protected void onBackPressed() {
		// A Back return must not hide an otherwise ready title behind the
		// standard one-second scene-entry fade. A genuine scene switch is still
		// required, but the title can be visible on its first rendered frame.
		if (section != Section.MENU){ show(Section.MENU); return; }
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}

	private void addLine( float y, Group content ){
		ColorBlock line = new ColorBlock(Camera.main.width, 1, 0xFF333333);
		line.y = y;
		content.add(line);
	}

	private static class CreditsBlock extends Component {

		boolean large;
		RenderedTextBlock title;
		Image avatar;
		Flare flare;
		RenderedTextBlock body;

		RenderedTextBlock link;
		ColorBlock linkUnderline;
		PointerArea linkButton;

		//many elements can be null, but body is assumed to have content.
		private CreditsBlock(boolean large, int highlight, String title, Image avatar, String body, String linkText, String linkUrl){
			super();

			this.large = large;

			if (title != null) {
				this.title = PixelScene.renderTextBlock(title, large ? 8 : 6);
				if (highlight != -1) this.title.hardlight(highlight);
				add(this.title);
			}

			if (avatar != null){
				this.avatar = avatar;
				add(this.avatar);
			}

			if (large && highlight != -1 && this.avatar != null){
				this.flare = new Flare( 7, 24 ).color( highlight, true ).show(this.avatar, 0);
				this.flare.angularSpeed = 20;
			}

			this.body = PixelScene.renderTextBlock(body, 6);
			if (highlight != -1) this.body.setHightlighting(true, highlight);
			if (large) this.body.align(RenderedTextBlock.CENTER_ALIGN);
			add(this.body);

			if (linkText != null && linkUrl != null){

				int color = 0xFFFFFFFF;
				if (highlight != -1) color = 0xFF000000 | highlight;
				this.linkUnderline = new ColorBlock(1, 1, color);
				add(this.linkUnderline);

				this.link = PixelScene.renderTextBlock(linkText, 6);
				if (highlight != -1) this.link.hardlight(highlight);
				add(this.link);

				linkButton = new PointerArea(0, 0, 0, 0){
					@Override
					protected void onClick( PointerEvent event ) {
						ShatteredPixelDungeon.platform.openURI( linkUrl );
					}
				};
				add(linkButton);
			}

		}

		@Override
		protected void layout() {
			super.layout();

			float topY = top();

			if (title != null){
				title.maxWidth((int)width());
				title.setPos( x + (width() - title.width())/2f, topY);
				topY += title.height() + (large ? 2 : 1);
			}

			if (large){

				if (avatar != null){
					avatar.x = x + (width()-avatar.width())/2f;
					avatar.y = topY;
					PixelScene.align(avatar);
					if (flare != null){
						flare.point(avatar.center());
					}
					topY = avatar.y + avatar.height() + 2;
				}

				body.maxWidth((int)width());
				body.setPos( x + (width() - body.width())/2f, topY);
				topY += body.height() + 2;

			} else {

				if (avatar != null){
					avatar.x = x;
					body.maxWidth((int)(width() - avatar.width - 1));

					float fullAvHeight = Math.max(avatar.height(), 16);
					if (fullAvHeight > body.height()){
						avatar.y = topY + (fullAvHeight - avatar.height())/2f;
						PixelScene.align(avatar);
						body.setPos( avatar.x + avatar.width() + 1, topY + (fullAvHeight - body.height())/2f);
						topY += fullAvHeight + 1;
					} else {
						avatar.y = topY + (body.height() - fullAvHeight)/2f;
						PixelScene.align(avatar);
						body.setPos( avatar.x + avatar.width() + 1, topY);
						topY += body.height() + 2;
					}

				} else {
					topY += 1;
					body.maxWidth((int)width());
					body.setPos( x, topY);
					topY += body.height()+2;
				}

			}

			if (link != null){
				if (large) topY += 1;
				link.maxWidth((int)width());
				link.setPos( x + (width() - link.width())/2f, topY);
				topY += link.height() + 2;

				linkButton.x = link.left()-1;
				linkButton.y = link.top()-1;
				linkButton.width = link.width()+2;
				linkButton.height = link.height()+2;

				linkUnderline.size(link.width(), PixelScene.align(0.49f));
				linkUnderline.x = link.left();
				linkUnderline.y = link.bottom()+1;

			}

			topY -= 2;

			height = Math.max(height, topY - top());
		}
	}
}
