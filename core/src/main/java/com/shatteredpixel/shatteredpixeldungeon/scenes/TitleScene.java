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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.apd.TitleGeometry;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndAPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats;
import com.watabou.glwrap.Blending;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.RectF;

//APD: the title/home screen uses the APD art in assets (apd_logo, apd_logo_glow, apd_flame, apd_button,
//apd_bg_port, apd_bg_land). Portrait stacks the logo over the buttons, landscape puts them side by side.
public class TitleScene extends PixelScene {

	//art dimensions, see docs/APD-TITLE-SCREEN.md
	private static final int LOGO_W = TitleGeometry.LOGO_W;
	private static final int LOGO_H = TitleGeometry.LOGO_H;
	private static final int FLAME_SIZE = 47;
	private static final int FLAME_BASE_X = 23; //flame base is bottom-centre of the 47x47 cell
	private static final int FLAME_BASE_Y = 46;

	//brazier bowl anchors inside apd_logo.png, from the asset notes. Tweak these if the flames look off.
	private static final int LEFT_FLAME_X = 10;
	private static final int RIGHT_FLAME_X = 129;
	private static final int FLAME_Y = 67;

	private static final int BG_COLOR = 0xFF08110D;

	private static final int MAIN_GAP = TitleGeometry.MAIN_GAP;
	private static final int BOTTOM_H = TitleGeometry.BOTTOM_H;
	//APD: the full-width APD Settings row sits between the 2x3 grid and the News/Changes/About row
	private static final int APD_ROW_H = TitleGeometry.APD_ROW_H;

	//APD: joke annotation pointing at the UIMSD button
	private static final String ANNOTATION_TEXT = "";

	private Image logo;
	private Image glow;
	private APDFlame leftFlame;
	private APDFlame rightFlame;

	private APDMenuButton btnPlay;
	private APDMenuButton btnStatistics;
	private APDMenuButton btnBadges;
	private APDMenuButton btnJournal;
	private APDMenuButton btnOptions;
	private APDMenuButton btnQuit;
	private APDMenuButton btnNews;
	private APDMenuButton btnChanges;
	private APDMenuButton btnAbout;
	private APDMenuButton btnAPDSettings;
	private APDMenuButton btnCommunity;
	private APDAnnotation annotation;

	private BitmapText version;
	private IconButton btnFade;
	private TitleGeometry.Layout titleLayout;
	private ExitButton btnExit;

	@Override
	public void create() {
		
		super.create();

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;
		
		int fullW = Camera.main.width;
		int fullH = Camera.main.height;

		RectF insets = getCommonInsets();

		//flat fill for any space the background plate doesn't cover, the plate edges fade to this colour
		add( new ColorBlock( fullW, fullH, BG_COLOR ) );

		Image plate = new Image( landscape() ? "splashes/title/apd_bg_land.png" : "splashes/title/apd_bg_port.png" );
		plate.scale.set(TitleGeometry.backgroundCoverScale(fullW,fullH,plate.width(),plate.height(),Camera.main.zoom));
		plate.x = (fullW - plate.width()) / 2f;
		plate.y = (fullH - plate.height()) / 2f;
		align( plate );
		add( plate );

		int w = (int)(fullW - insets.left - insets.right);
		int h = (int)(fullH - insets.top - insets.bottom);

		logo = new Image( "interfaces/apd_logo.png" );
		add( logo );

		glow = new Image( "interfaces/apd_logo_glow.png" ){
			private float time = 0;
			@Override
			public void update() {
				super.update();
				am = Math.max(0f, (float)Math.sin( time += Game.elapsed ));
				am = Math.min(am, logo.am);
				if (time >= 1.5f*Math.PI) time = 0;
			}
			@Override
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		add( glow );

		leftFlame = new APDFlame( false );
		rightFlame = new APDFlame( true );
		add( leftFlame );
		add( rightFlame );

		btnPlay = new APDMenuButton( "Play", Icons.ENTER, true ){
			@Override
			protected void onClick() {
				super.onClick();
				if (GamesInProgress.checkAll().size() == 0){
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
				} else {
					ShatteredPixelDungeon.switchNoFade( StartScene.class );
				}
			}

			@Override
			protected boolean onLongClick() {
				//making it easier to start runs quickly while debugging
				if (DeviceCompat.isDebug()) {
					GamesInProgress.selectedClass = null;
					GamesInProgress.curSlot = 1;
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
					return true;
				}
				return super.onLongClick();
			}
		};
		add( btnPlay );

		//UIMSD (Ultimate Infinite Mega Statistics Database) replaces Rankings and opens the UIMSD screen
		btnStatistics = new APDMenuButton( "UIMSD", Icons.STATS, true ){
			@Override
			protected void onClick() {
				super.onClick();
				UimsdScene.open( TitleScene.class );
			}
		};
		add( btnStatistics );
		Dungeon.daily = Dungeon.dailyReplay = false;

		btnBadges = new APDMenuButton( "Badges", Icons.BADGES, true ){
			@Override
			protected void onClick() {
				super.onClick();
				JournalScene.lastIDX = 0; //badges tab
				ShatteredPixelDungeon.switchNoFade( JournalScene.class );
			}
		};
		add( btnBadges );

		btnJournal = new APDMenuButton( "Journal", Icons.JOURNAL, true ){
			@Override
			protected void onClick() {
				super.onClick();
				if (JournalScene.lastIDX == 0) JournalScene.lastIDX = 1; //catalog tab, badges has its own button
				ShatteredPixelDungeon.switchNoFade( JournalScene.class );
			}
		};
		add( btnJournal );

		btnOptions = new APDMenuButton( "Options", Icons.PREFS, true ){
			@Override
			protected void onClick() {
				super.onClick();
				ShatteredPixelDungeon.scene().add(new WndSettings());
			}
		};
		add( btnOptions );

		final boolean androidTopScores = DeviceCompat.isAndroid();
		btnQuit = new APDMenuButton( androidTopScores ? "Top Scores" : "Quit", androidTopScores ? Icons.RANKINGS : Icons.EXIT, true ){
			@Override
			protected void onClick() {
				super.onClick();
				if (androidTopScores) {
					ShatteredPixelDungeon.switchNoFade( RankingsScene.class );
				} else {
					Game.instance.finish();
				}
			}
		};
		//iOS apps don't quit themselves
		if (DeviceCompat.isiOS()) btnQuit.visible = btnQuit.active = false;
		add( btnQuit );

		//APD: APD-specific settings have their own entry, separate from Options
		btnAPDSettings = new APDMenuButton( "APD Settings", Icons.PREFS, false ){
			@Override
			protected void onClick() {
				super.onClick();
				ShatteredPixelDungeon.scene().add(new WndAPDSettings());
			}
		};
		add( btnAPDSettings );

		btnCommunity = new APDMenuButton( "Community", Icons.NEWS, false ){
			@Override
			protected void onClick() {
				super.onClick();
				CommunityScene.open();
			}
		};
		add( btnCommunity );

		//APD: News and Changes open the offline APD screens, not the stock internet/upstream ones
		btnNews = new APDMenuButton( "News", Icons.NEWS, false ){
			@Override
			protected void onClick() {
				super.onClick();
				APDTextScene.page = APDTextScene.Page.NEWS;
				APDTextScene.backTo = TitleScene.class;
				ShatteredPixelDungeon.switchNoFade( APDTextScene.class );
			}
		};
		add( btnNews );

		btnChanges = new APDMenuButton( "Changes", Icons.CHANGES, false ){
			@Override
			protected void onClick() {
				super.onClick();
				ChangesScene.changesSelected = 0; ShatteredPixelDungeon.switchNoFade( ChangesScene.class );
			}
		};
		add( btnChanges );

		btnAbout = new APDMenuButton( "About", Icons.INFO, false ){
			@Override
			protected void onClick() {
				super.onClick();
				AboutScene.section = AboutScene.Section.MENU; ShatteredPixelDungeon.switchScene( AboutScene.class );
			}
		};
		add( btnAbout );

		version = new BitmapText( "v4.0.9", pixelFont);
		version.measure();
		version.hardlight( 0x8C938C );
		add( version );

		//*** layout ***

		float logoX, logoY;
		float gridLeft, gridWidth, gridTop;

		String annotationText = com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.titleAnnotation( ANNOTATION_TEXT );
		annotation = new APDAnnotation( annotationText, Math.min( 150, landscape() ? Math.min(LOGO_W, w - Math.min(150, w-8) - 12) : w - 8 ) );
		annotation.visible = !annotationText.isEmpty();
		add( annotation );
		add(new com.watabou.noosa.PointerArea(0, 0, 0, 0){
			@Override public void update(){
				super.update();
				float left = Float.MAX_VALUE, top = Float.MAX_VALUE, right = 0, bottom = 0;
				for (BitmapText line : annotation.lines){
					left = Math.min(left, line.x); top = Math.min(top, line.y);
					right = Math.max(right, line.x + line.width()); bottom = Math.max(bottom, line.y + line.height());
				}
				x = left; y = top; width = Math.max(0, right-left); height = Math.max(0, bottom-top);
				active = annotation.visible;
			}
			@Override protected void onClick(com.watabou.input.PointerEvent event){
				com.shatteredpixel.shatteredpixeldungeon.windows.UimsdGate.open(TitleScene.class);
			}
		});
		float textH = annotation.visible ? annotation.textHeight() : -1;

		if (landscape()){
			//logo on the left, button grid on the right
			TitleGeometry.Layout l = TitleGeometry.landscape( insets.left, insets.top, w, h, version.height(), textH );
			titleLayout = l;
			logoX = l.logoX;
			logoY = l.logoY;
			gridLeft = l.gridLeft;
			gridWidth = l.gridWidth;
			gridTop = l.gridTop;
			layoutGrid( gridLeft, gridTop, gridWidth, l.mainH );
		} else {
			TitleGeometry.Layout l = TitleGeometry.portrait( insets.left, insets.top, w, h, version.height(), textH );
			gridWidth = l.gridWidth;
			gridLeft = l.gridLeft;
			titleLayout = l;
			logoX = l.logoX;
			logoY = l.logoY;
			gridTop = l.gridTop;
			layoutGrid( gridLeft, gridTop, gridWidth, l.mainH );
		}

		float artScale = titleLayout.logoScale;
		logo.scale.set(artScale); glow.scale.set(artScale);
		leftFlame.scale.set(artScale); rightFlame.scale.set(artScale);
		logo.x = logoX;
		logo.y = logoY;
		align( logo );
		glow.x = logo.x;
		glow.y = logo.y;

		leftFlame.x = logo.x + (LEFT_FLAME_X - FLAME_BASE_X)*artScale;
		leftFlame.y = logo.y + (FLAME_Y - FLAME_BASE_Y)*artScale;
		rightFlame.x = logo.x + (RIGHT_FLAME_X - FLAME_BASE_X)*artScale;
		rightFlame.y = logo.y + (FLAME_Y - FLAME_BASE_Y)*artScale;
		align( leftFlame );
		align( rightFlame );

		version.x = logo.x + (LOGO_W*artScale - version.width()) / 2f;
		version.y = titleLayout.versionY;
		align( version );

		btnFade = new IconButton(Icons.CHEVRON.get()){
			@Override
			protected void layout() {
				super.layout();
				// Expand only the hit area into the reserved footer space. Keep the
				// 16px icon, existing Hide UI effect and all menu geometry intact.
				if (hotArea != null) {
					hotArea.x = x - TitleGeometry.FADE_TOUCH_SIDE_PADDING;
					hotArea.y = y - TitleGeometry.FADE_TOUCH_TOP_PADDING;
					hotArea.width = width + 2 * TitleGeometry.FADE_TOUCH_SIDE_PADDING;
					hotArea.height = height + TitleGeometry.FADE_TOUCH_TOP_PADDING;
				}
			}
			@Override
			protected void onClick() {
				enable(false);
				parent.add(new Tweener(parent, 0.5f) {
					@Override
					protected void updateValues(float progress) {
						if (!btnFade.active) {
							uiAlpha = 1 - progress;
							updateFade();
						}
					}
				});
			}
		};
		btnFade.icon().originToCenter();
		btnFade.icon().angle = 180f;
		btnFade.setRect(titleLayout.fadeX, titleLayout.fadeY, TitleGeometry.FADE_SIZE, TitleGeometry.FADE_SIZE);
		add(btnFade);

		PointerArea fadeResetter = new PointerArea(0, 0, Camera.main.width, Camera.main.height){
			@Override
			public boolean onSignal(PointerEvent event) {
				if (event != null && event.type == PointerEvent.Type.UP && !btnPlay.active){
					parent.add(new Tweener(parent, 0.5f) {
						@Override
						protected void updateValues(float progress) {
							uiAlpha = progress;
							updateFade();
							if (progress >= 1){
								btnFade.enable(true);
							}
						}
					});
				}
				return false;
			}
		};
		add(fadeResetter);
		// The full-screen restore listener must not swallow a footer-button press.
		btnFade.givePointerPriority();

		if (DeviceCompat.isDesktop()) {
			btnExit = new ExitButton();
			btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
			add( btnExit );
		}

		Badges.loadGlobal();
		if (Badges.isUnlocked(Badges.Badge.VICTORY) && !SPDSettings.victoryNagged()) {
			SPDSettings.victoryNagged(true);
			add(new WndVictoryCongrats());
		}

		fadeIn();
		// Prepare the exact UIMSD Home in the background while the title is open.
		// When it is ready, tapping UIMSD does not begin a new history load.
		UimsdScene.prewarmTitle();
	}

	//2 columns x 3 rows, then a row of three smaller buttons
	private void layoutGrid( float left, float top, float width, float mainH ){
		float colW = TitleGeometry.colWidth( width );

		APDMenuButton[] main = { btnPlay, btnStatistics, btnBadges, btnJournal, btnOptions, btnQuit };
		for (int i = 0; i < main.length; i++){
			int col = i % 2, row = i / 2;
			main[i].setRect( left + col*(colW + MAIN_GAP), top + row*(mainH + MAIN_GAP), colW, mainH );
			align( main[i] );
		}

		//the arrow points down at the UIMSD button (top row, right column), the text sits above it in the space reserved over the grid
		if (titleLayout.annotationBesideGrid) annotation.placeTextOnly(titleLayout.annotationLeft, titleLayout.annotationTop);
		else if (titleLayout.annotationCompact) annotation.placeTextOnly(left, top-titleLayout.annotationH);
		else annotation.place( left, width, TitleGeometry.colWidth( width ) + MAIN_GAP, top );

		float apdY = top + 3*(mainH + MAIN_GAP);
		btnAPDSettings.setRect( left, apdY, (width - MAIN_GAP)/2f, BOTTOM_H );
		btnCommunity.setRect( left + (width + MAIN_GAP)/2f, apdY, (width - MAIN_GAP)/2f, BOTTOM_H );
		align( btnCommunity );
		align( btnAPDSettings );

		float bottomY = apdY + APD_ROW_H;
		float smallW = (float)Math.floor((width - 2*MAIN_GAP) / 3f);
		APDMenuButton[] bottom = { btnNews, btnChanges, btnAbout };
		for (int i = 0; i < bottom.length; i++){
			bottom[i].setRect( left + i*(smallW + MAIN_GAP), bottomY, smallW, BOTTOM_H );
			align( bottom[i] );
		}
	}

	private float uiAlpha;

	public void updateFade() {
		float alpha = GameMath.gate(0f, uiAlpha, 1f);

		logo.am = alpha;
		leftFlame.am = alpha;
		rightFlame.am = alpha;
		//glow handles its own alpha

		APDMenuButton[] all = { btnPlay, btnStatistics, btnBadges, btnJournal, btnOptions, btnQuit,
				btnAPDSettings, btnCommunity, btnNews, btnChanges, btnAbout };
		for (APDMenuButton b : all){
			//Quit is hidden on iOS, enable() would make it tappable again
			if (b == btnQuit && DeviceCompat.isiOS()) continue;
			b.enable(alpha != 0);
			b.alpha(alpha);
		}

		annotation.alpha(alpha);
		version.alpha(alpha);
		btnFade.icon().alpha(alpha);
		if (btnExit != null){
			btnExit.enable(alpha != 0);
			btnExit.icon().alpha(alpha);
		}

	}

	//animated green flame, 24 frames of 47x47 in a 5x5 sheet. The second flame is mirrored and offset.
	private static class APDFlame extends MovieClip {

		APDFlame( boolean second ){
			texture( "effects/apd_flame.png" );
			TextureFilm frames = new TextureFilm( texture, FLAME_SIZE, FLAME_SIZE );
			MovieClip.Animation anim = new MovieClip.Animation( 24, true );
			anim.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 );
			play( anim );

			if (second){
				flipHorizontal = true;
				curFrame = 12;
				frame( curAnim.frames[curFrame] );
			}
		}
	}

	//Deliberately out of place: hot pink text and a big, bright, outlined arrow with a pulsing glow, bobbing upwards, pointing
	//down at the UIMSD button. The text wraps by measured words within the grid. It is not interactive
	//and sits in its own strip above the grid.
	private static class APDAnnotation extends Component {

		private static final int COLOR = 0xFF4FD8;       //text
		private static final int SHADOW = 0xFF000000;
		private static final int ARROW_COLOR = 0xFF2EC8; //brighter, more saturated than the text
		private static final int ARROW_LIGHT = 0xFFA8F0; //highlight on top of the head and down the shaft
		private static final int GLOW_COLOR = 0xFF2EC8;
		//arrow shape and spacing are in TitleGeometry so they can be tested
		private static final int SHAFT_W = TitleGeometry.SHAFT_W;
		private static final int SHAFT_H = TitleGeometry.SHAFT_H;
		private static final int HEAD_ROWS = TitleGeometry.HEAD_ROWS;
		private static final int HEAD_W = TitleGeometry.HEAD_W;
		private static final int ARROW_H = TitleGeometry.ARROW_H;
		private static final int GLOW = TitleGeometry.GLOW;
		private static final int GAP = TitleGeometry.GAP;
		private static final int BOB = TitleGeometry.BOB;

		private final BitmapText[] lines;
		private final BitmapText[] shadows;
		private final float textH;
		//one entry per arrow row: shaft first, then the head rows
		private final int[] rowW = new int[1 + HEAD_ROWS];
		private final int[] rowH = new int[1 + HEAD_ROWS];
		private final ColorBlock[] glow = new ColorBlock[1 + HEAD_ROWS];
		private final ColorBlock[] outline = new ColorBlock[1 + HEAD_ROWS];
		private final ColorBlock[] body = new ColorBlock[1 + HEAD_ROWS];
		private final ColorBlock highlightTop;
		private final ColorBlock highlightShaft;
		private float arrowX, arrowY;
		private float time;

		APDAnnotation( String text, float maxWidth ){
			BitmapText probe = new BitmapText(text, pixelFont);
			String[] parts = TitleGeometry.annotationLines(text, maxWidth - 1, value -> {
				probe.text(value);
				probe.measure();
				return probe.width();
			});
			lines = new BitmapText[parts.length];
			shadows = new BitmapText[parts.length];
			float lh = 0;
			for (int i = 0; i < parts.length; i++){
				shadows[i] = new BitmapText( parts[i], pixelFont );
				shadows[i].hardlight( SHADOW );
				shadows[i].measure();
				add( shadows[i] );
				lines[i] = new BitmapText( parts[i], pixelFont );
				lines[i].hardlight( COLOR );
				lines[i].measure();
				add( lines[i] );
				lh = lines[i].height();
			}
			textH = lh * parts.length + 1;

			rowW[0] = SHAFT_W;
			rowH[0] = SHAFT_H;
			for (int i = 0; i < HEAD_ROWS; i++){
				rowW[1 + i] = HEAD_W - 2*i;
				rowH[1 + i] = 1;
			}
			//layers, back to front: glow, black outline, pink body, highlights
			for (int i = 0; i < rowW.length; i++){
				glow[i] = new ColorBlock( rowW[i] + 2*(1 + GLOW), rowH[i] + 2*(1 + GLOW), GLOW_COLOR );
				add( glow[i] );
			}
			for (int i = 0; i < rowW.length; i++){
				outline[i] = new ColorBlock( rowW[i] + 2, rowH[i] + 2, SHADOW );
				add( outline[i] );
			}
			for (int i = 0; i < rowW.length; i++){
				body[i] = new ColorBlock( rowW[i], rowH[i], 0xFF000000 | ARROW_COLOR );
				add( body[i] );
			}
			highlightTop = new ColorBlock( HEAD_W - 2, 1, 0xFF000000 | ARROW_LIGHT );
			add( highlightTop );
			highlightShaft = new ColorBlock( 1, SHAFT_H, 0xFF000000 | ARROW_LIGHT );
			add( highlightShaft );
			for (ColorBlock b : glow) b.am = 0.25f;
			setSize( maxWidth, height() );
		}

		//the strip this takes above the grid: text, the arrow with its outline and glow, the gap above the button, and the bob
		@Override
		public float height() {
			return TitleGeometry.annotationHeight( textH );
		}

		float textHeight() {
			return textH;
		}

		//gridLeft/gridWidth: the grid's span. rightColOffset: where the right column starts. gridTop: top of the buttons.
		void place( float gridLeft, float gridWidth, float rightColOffset, float gridTop ){
			float colW = gridWidth - rightColOffset;
			float centre = gridLeft + rightColOffset + colW / 2f;
			arrowX = (float)Math.floor( centre - HEAD_W / 2f );
			//rest position: the tip, its outline and its glow all end GAP px above the button
			arrowY = gridTop - GAP - GLOW - 1 - ARROW_H;
			float textRight = gridLeft + gridWidth - 1;
			float top = gridTop - height();
			for (int i = 0; i < lines.length; i++){
				float x = Math.max( gridLeft, textRight - lines[i].width() );
				float y = top + i * lines[i].height();
				lines[i].x = (float)Math.floor( x );
				lines[i].y = (float)Math.floor( y );
				shadows[i].x = lines[i].x + 1;
				shadows[i].y = lines[i].y + 1;
			}
			positionArrow( 0 );
		}

        // In a short landscape window the same clickable guidance moves beside the grid.
        void placeTextOnly(float left,float top){
            for(int i=0;i<lines.length;i++){
                lines[i].x=left;lines[i].y=top+i*lines[i].height();
                shadows[i].x=left+1;shadows[i].y=lines[i].y+1;
            }
            for(ColorBlock b:glow)b.visible=false;
            for(ColorBlock b:outline)b.visible=false;
            for(ColorBlock b:body)b.visible=false;
            highlightTop.visible=highlightShaft.visible=false;
        }

		private void positionArrow( float lift ){
			float y = arrowY - lift;
			float rowY = y;
			for (int i = 0; i < rowW.length; i++){
				float x = arrowX + (HEAD_W - rowW[i]) / 2f;
				body[i].x = (float)Math.floor( x );
				body[i].y = (float)Math.floor( rowY );
				outline[i].x = body[i].x - 1;
				outline[i].y = body[i].y - 1;
				glow[i].x = body[i].x - 1 - GLOW;
				glow[i].y = body[i].y - 1 - GLOW;
				rowY += rowH[i];
			}
			highlightTop.x = body[1].x + 1;
			highlightTop.y = body[1].y;
			highlightShaft.x = body[0].x + 1;
			highlightShaft.y = body[0].y;
		}

		void alpha( float value ){
			for (BitmapText t : lines) t.alpha( value );
			for (BitmapText t : shadows) t.alpha( value );
			for (ColorBlock b : outline) b.am = value;
			for (ColorBlock b : body) b.am = value;
			highlightTop.am = value;
			highlightShaft.am = value;
			fade = value;
		}

		private float fade = 1f;

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			positionArrow( Math.round( Math.abs( (float)Math.sin( time * 4f ) ) * BOB ) );
			//the glow pulses (each of its overlapping pieces is faint, together they stack into a soft halo) (and fades out with the rest of the menu)
			float pulse = 0.10f + 0.10f * (0.5f + 0.5f * (float)Math.sin( time * 6f ));
			for (ColorBlock b : glow) b.am = pulse * fade;
		}
	}

	//stone button using apd_button.png as a 9-patch (8px margins). Main buttons stack the icon above the
	//label, the bottom row puts the icon beside it.
	private static class APDMenuButton extends StyledButton {

		private final boolean stacked;

		APDMenuButton( String label, Icons icon, boolean stacked ){
			super( Chrome.Type.GREY_BUTTON_TR, label, 6 );
			this.stacked = stacked;

			remove( bg );
			bg = new NinePatch( "interfaces/apd_button.png", 0, 0, 24, 18, 8 );
			addToBack( bg );

			icon( Icons.get( icon ) );
		}

		@Override
		protected void layout() {
			super.layout();

			if (stacked && icon != null && text != null){
				float contentH = icon.height() + 2 + text.height();
				float top = y + (height - contentH) / 2f;

				icon.x = x + (width - icon.width()) / 2f;
				icon.y = top;
				PixelScene.align( icon );

				text.setPos( x + (width - text.width()) / 2f, top + icon.height() + 2 );
				PixelScene.align( text );
			}
		}
	}
}
