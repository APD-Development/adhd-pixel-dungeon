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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndGame;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndJournal;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndStory;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;

public class MenuPane extends Component {

	private Image bg;

	private Image depthIcon;
	private BitmapText depthText;
	private Button depthButton;

	private Image challengeIcon;
	private BitmapText challengeText;
	private Button challengeButton;

	private JournalButton btnJournal;
	private MenuButton btnMenu;

	private Toolbar.PickedUpItem pickedUp;

	private BitmapText version;
	//APD Developer build: real-world clock under the version, left of the depth icon
	private BitmapText clock;
	private int clockMinute = -1;
	//APD Developer build: this frame's time in ms, updated every frame
	private BitmapText frameTime;
	//APD: hunger number for everyone. Alpha: where the Developer clock sits. Developer: under the frame time.
	private BitmapText hungerText;
	private int hungerShown = Integer.MIN_VALUE;
	private int frameShownMs = -1;
	private NinePatch versionOverflowBG;

	private DangerIndicator danger;

	public static final int WIDTH = 31;

	@Override
	protected void createChildren() {
		super.createChildren();

		bg = new Image(Assets.Interfaces.MENU, 1, 0, 31, 21);
		add(bg);

		versionOverflowBG = new NinePatch(bg.texture, 1, 22, 6, 8, 3, 0, 2, 0);
		add(versionOverflowBG);

		version = new BitmapText( "v" + Game.version , PixelScene.pixelFont);
		version.hardlight( 0xCACFC2 );
		add(version);

		hungerText = new BitmapText( "", PixelScene.pixelFont );
		hungerText.hardlight( 0xCACFC2 );
		add(hungerText);
		if (com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.testBuild()) {
			clock = new BitmapText( "", PixelScene.pixelFont );
			clock.hardlight( 0xCACFC2 );
			add(clock);
			frameTime = new BitmapText( "", PixelScene.pixelFont );
			frameTime.hardlight( 0xCACFC2 );
			add(frameTime);
			updateClock();
		}

		depthIcon = Icons.get(Dungeon.level.feeling);
		add(depthIcon);

		depthText = new BitmapText( Integer.toString( Dungeon.depth ), PixelScene.pixelFont);
		depthText.hardlight( 0xCACFC2 );
		depthText.measure();
		add( depthText );

		depthButton = new Button(){
			@Override
			protected String hoverText() {
				if (Dungeon.level.feeling != Level.Feeling.NONE){
					return Dungeon.level.feeling.desc();
				} else {
					return null;
				}
			}

			@Override
			protected void onClick() {
				super.onClick();

				if (Dungeon.level.feeling == Level.Feeling.NONE){
					GameScene.show(new WndJournal());
				} else {
					GameScene.show(new WndTitledMessage(Icons.getLarge(Dungeon.level.feeling),
							Messages.titleCase(Dungeon.level.feeling.title()),
							Dungeon.level.feeling.desc()));
				}
			}
		};
		add(depthButton);

		if (Challenges.activeChallenges() > 0){
			challengeIcon = Icons.get(Icons.CHAL_COUNT);
			add(challengeIcon);

			challengeText = new BitmapText( Integer.toString( Challenges.activeChallenges() ), PixelScene.pixelFont);
			challengeText.hardlight( 0xCACFC2 );
			challengeText.measure();
			add( challengeText );

			challengeButton = new Button(){
				@Override
				protected void onClick() {
					GameScene.show(new WndChallenges(Dungeon.challenges, false));
				}

				@Override
				protected String hoverText() {
					return Messages.get(WndChallenges.class, "title");
				}
			};
			add(challengeButton);
		}

		btnJournal = new JournalButton();
		add( btnJournal );

		btnMenu = new MenuButton();
		add( btnMenu );

		danger = new DangerIndicator();
		add( danger );

		add( pickedUp = new Toolbar.PickedUpItem());
	}

	@Override
	protected void layout() {
		super.layout();

		bg.x = x;
		bg.y = y;

		version.scale.set(PixelScene.align(0.5f));
		version.measure();

		float rightMargin = DeviceCompat.isDesktop() ? 1 : 8;
		if (DeviceCompat.isDebug()) rightMargin = 1; //don't care about hiding 'indev'
		float overFlow = version.width()-(bg.width()-4-rightMargin);
		if (overFlow >= 1){
			version.x = x + 2 - overFlow;
			versionOverflowBG.size(overFlow+3, 8);
			versionOverflowBG.x = version.x-3;
			versionOverflowBG.y = y;
		} else {
			version.x = x + 3;
			versionOverflowBG.visible = false;
		}
		version.y = y + 3 - (version.baseLine()*version.scale.y)/2f;
		version.y -= .001f;
		PixelScene.align(version);

		btnMenu.setPos( x + WIDTH - btnMenu.width(), y );

		btnJournal.setPos( btnMenu.left() - btnJournal.width() + 2, y );

		depthIcon.x = btnJournal.left() - 7 + (7 - depthIcon.width())/2f - 0.1f;
		depthIcon.y = y+8;
		PixelScene.align(depthIcon);

		depthText.scale.set(PixelScene.align(0.67f));
		depthText.x = depthIcon.x + (depthIcon.width() - depthText.width())/2f;
		depthText.y = depthIcon.y + depthIcon.height();
		PixelScene.align(depthText);

		depthButton.setRect(depthIcon.x, depthIcon.y, depthIcon.width(), depthIcon.height() + depthText.height());

		if (challengeIcon != null){
			challengeIcon.x = btnJournal.left() - 14 + (7 - challengeIcon.width())/2f - 0.1f;
			challengeIcon.y = depthIcon.y;
			PixelScene.align(challengeIcon);

			challengeText.scale.set(PixelScene.align(0.67f));
			challengeText.x = challengeIcon.x + (challengeIcon.width() - challengeText.width())/2f;
			challengeText.y = challengeIcon.y + challengeIcon.height();
			PixelScene.align(challengeText);

			challengeButton.setRect(challengeIcon.x, challengeIcon.y, challengeIcon.width(), challengeIcon.height() + challengeText.height());
		}

		layoutClock();

		danger.setPos( x + WIDTH - danger.width(), y + bg.height + 1 );
		danger.setSize( camera.width - danger.width(), danger.height());
	}

	private void layoutClock(){
		float right = (challengeIcon != null ? challengeIcon.x : depthIcon.x) - 2;
		float nextY = depthIcon.y + 1;
		if (clock != null){
		clock.scale.set(PixelScene.align(0.67f));
		clock.measure();
		clock.x = right - clock.width();
		clock.y = nextY;
		PixelScene.align(clock);
		nextY = clock.y + clock.height() + 1;
		}
		if (frameTime != null){
			frameTime.scale.set(PixelScene.align(0.67f));
			frameTime.measure();
			frameTime.x = right - frameTime.width();
			frameTime.y = nextY;
			PixelScene.align(frameTime);
			nextY = frameTime.y + frameTime.height() + 1;
		}
		if (hungerText != null){
			hungerText.scale.set(PixelScene.align(0.67f));
			hungerText.measure();
			hungerText.x = right - hungerText.width();
			hungerText.y = nextY;
			PixelScene.align(hungerText);
		}
	}

	//only rebuilds the text when the minute changes
	private void updateClock(){
		java.util.Calendar now = java.util.Calendar.getInstance();
		int minute = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
		if (minute == clockMinute) return;
		clockMinute = minute;
		int hour = now.get(java.util.Calendar.HOUR);
		if (hour == 0) hour = 12;
		int m = now.get(java.util.Calendar.MINUTE);
		clock.text( hour + ":" + (m < 10 ? "0" : "") + m + (now.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM ? "am" : "pm") );
		if (depthIcon != null) layoutClock();
	}

	@Override
	public void update() {
		super.update();
		if (hungerText != null) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger h = Dungeon.hero == null ? null : Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger.class);
			int value = h == null ? -1 : h.hunger();
			if (value != hungerShown) {
				hungerShown = value;
				hungerText.text(com.shatteredpixel.shatteredpixeldungeon.apd.qol.HungerDisplay.corner(value));
				hungerText.hardlight(com.shatteredpixel.shatteredpixeldungeon.apd.qol.HungerDisplay.color(value));
				if (depthIcon != null) layoutClock();
			}
		}
		if (frameTime != null) {
			//every frame: this frame's time in ms; the text is only rebuilt when the number changes
			int ms = Math.round(com.badlogic.gdx.Gdx.graphics.getDeltaTime() * 1000f);
			if (ms != frameShownMs) {
				frameShownMs = ms;
				frameTime.text(ms + "ms");
				if (depthIcon != null) layoutClock();
			}
		}
		if (clock != null && Game.realTime / 1000 != lastClockCheck) {
			lastClockCheck = Game.realTime / 1000;
			updateClock();
		}
	}
	private long lastClockCheck = -1;

	public void pickup(Item item, int cell) {
		pickedUp.reset( item,
				cell,
				btnJournal.centerX(),
				btnJournal.centerY());
	}

	public void flashForPage( Document doc, String page ){
		btnJournal.flashingDoc = doc;
		btnJournal.flashingPage = page;
	}

	public void updateKeys(){
		btnJournal.updateKeyDisplay();
	}

	private static class JournalButton extends Button {

		private Image bg;
		private Image journalIcon;
		private KeyDisplay keyIcon;

		private Document flashingDoc = null;
		private String flashingPage = null;

		public JournalButton() {
			super();

			width = bg.width + 4;
			height = bg.height + 10;
		}

		@Override
		public GameAction keyAction() {
			return SPDAction.JOURNAL;
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = new Image( Assets.Interfaces.MENU_BTN, 2, 2, 13, 11 );
			add( bg );

			journalIcon = new Image( Assets.Interfaces.MENU_BTN, 31, 0, 11, 6);
			add( journalIcon );

			keyIcon = new KeyDisplay();
			add(keyIcon);
			updateKeyDisplay();
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x + 2;
			bg.y = y + 8;

			journalIcon.x = bg.x + (bg.width() - journalIcon.width())/2f;
			journalIcon.y = bg.y + (bg.height() - journalIcon.height())/2f;
			PixelScene.align(journalIcon);

			keyIcon.x = bg.x + 1;
			keyIcon.y = bg.y + 1;
			keyIcon.width = bg.width - 2;
			keyIcon.height = bg.height - 2;
			PixelScene.align(keyIcon);
		}

		private float time;

		@Override
		public void update() {
			super.update();

			if (flashingPage != null){
				journalIcon.am = (float)Math.abs(Math.cos( StatusPane.FLASH_RATE * (time += Game.elapsed) ));
				keyIcon.am = journalIcon.am;
				bg.brightness(0.5f + journalIcon.am);
				if (time >= Math.PI/StatusPane.FLASH_RATE) {
					time = 0;
				}
			}
		}

		public void updateKeyDisplay() {
			keyIcon.updateKeys();
			keyIcon.visible = keyIcon.keyCount() > 0;
			journalIcon.visible = !keyIcon.visible;
			if (keyIcon.keyCount() > 0) {
				bg.brightness(.8f - (Math.min(6, keyIcon.keyCount()) / 20f));
			} else {
				bg.resetColor();
			}
		}

		@Override
		protected void onPointerDown() {
			bg.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			if (keyIcon.keyCount() > 0) {
				bg.brightness(.8f - (Math.min(6, keyIcon.keyCount()) / 20f));
			} else {
				bg.resetColor();
			}
		}

		@Override
		protected void onClick() {
			time = 0;
			keyIcon.am = journalIcon.am = 1;
			if (flashingPage != null){
				if (flashingDoc == Document.ALCHEMY_GUIDE){
					WndJournal.last_index = 2;
					GameScene.show( new WndJournal() );
				} else if (flashingDoc.pageNames().contains(flashingPage)){
					if (flashingDoc == Document.ADVENTURERS_GUIDE){
						WndJournal.last_index = 1;
					} else if (flashingDoc.isLoreDoc()){
						WndJournal.last_index = 3;
						WndJournal.CatalogTab.currentItemIdx = 3;
					}
					GameScene.show( new WndStory( flashingDoc.pageSprite(flashingPage),
							flashingDoc.pageTitle(flashingPage),
							flashingDoc.pageBody(flashingPage) ){
						@Override
						public void hide() {
							super.hide();
							if (SPDSettings.intro()){
								GameScene.endIntro();
							}
						}
					});
					flashingDoc.readPage(flashingPage);
				} else {
					GameScene.show( new WndJournal() );
				}
				flashingPage = null;
			} else {
				GameScene.show( new WndJournal() );
			}
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(Messages.get(WndKeyBindings.class, "journal"));
		}
	}

	private static class MenuButton extends Button {

		private Image image;

		public MenuButton() {
			super();

			width = image.width + 4;
			height = image.height + 10;
		}

		@Override
		protected void createChildren() {
			super.createChildren();

			image = new Image( Assets.Interfaces.MENU_BTN, 17, 2, 12, 11 );
			add( image );
		}

		@Override
		protected void layout() {
			super.layout();

			image.x = x + 2;
			image.y = y + 8;
		}

		@Override
		protected void onPointerDown() {
			image.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			image.resetColor();
		}

		@Override
		protected void onClick() {
			GameScene.show( new WndGame() );
		}

		@Override
		public GameAction keyAction() {
			return GameAction.BACK;
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(Messages.get(WndKeyBindings.class, "menu"));
		}
	}
}
