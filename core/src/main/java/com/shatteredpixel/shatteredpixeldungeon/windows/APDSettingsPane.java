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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoData;
import com.shatteredpixel.shatteredpixeldungeon.apd.demo.DemoGenerator;
import com.shatteredpixel.shatteredpixeldungeon.apd.archive.StorageMeter;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.ApdRecorder;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.HistoryTransfer;
import com.shatteredpixel.shatteredpixeldungeon.apd.recorder.Ruleset;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.SelfTest;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;
import com.watabou.utils.FileUtils;

//APD: shared settings pane opened directly from the title screen and in-game menu.
//Category navigation changes presentation only; preference keys and callbacks stay shared.
public class APDSettingsPane extends Component {

	private static final int GAP = 4;
	//the slider moves in steps of 10%, the stored setting is the percent itself (0-80)
	private static final int STEP = 10;

	private RenderedTextBlock title;
	private OptionSlider optExotic;
	private RenderedTextBlock info;
	private CheckBox chkRecord;
    private CheckBox chkDotOverlay;
    private CheckBox chkTrophyOutlines;
	private CheckBox chkAdult;
    private CheckBox chkTileVisibility;
    private RedButton btnSearchedWallColor;
    private CheckBox chkStairNavigation;
    private CheckBox chkAutoEnterStairs;
    private CheckBox chkRatKingSearch;
	private RenderedTextBlock status;
	private RenderedTextBlock storage;
	private RedButton btnClericQuickCasts;
    private RedButton btnWaitingInterruptions,btnAutoEatTrigger;
    private CheckBox chkAutoEatWaiting;
    private CheckBox chkSingleTapSearch;
    private CheckBox chkAdaptiveSearchChoice;
    private CheckBox chkContinueTrapDiscovery;
    private RedButton btnPlaybackSpeed;
    private RedButton btnWaterskinSlot;
    private RedButton btnHealingConfirmation;
    private RedButton btnCustomRules;
    private RedButton btnCurrentCustomRules;
	private RedButton btnTest;
	private RedButton btnExportHistory;
	private RedButton btnImportHistory;
	private RedButton btnImport;
	private RedButton btnUimsd;
	//About / Developer: the Cleric grudge's page (null while the feature is switched off)
	private RedButton btnGrudge;
	//INDEV builds only (null in other builds): synthetic demo data for testing UIMSD, see DemoData
	private RedButton btnDemoGenerate;
	private RedButton btnDemoRemove;
	private RedButton btnDemoView;

    private ApdSettingsSection section;
    private java.util.List<SettingRow> rows;
    private java.util.List<SettingButton> categories;
    private SettingButton btnSections,btnHotbarLayout,btnHotbarCustom;
    private CheckBox chkFoodInHealing;
    private RenderedTextBlock sectionInfo;
    private boolean diagnosticsLoaded;
    private RedButton btnDiagnosticReport;
    private CheckBox chkExtendedBugLogging;
    public interface Navigation { void changed(ApdSettingsSection previous); }
    private Navigation navigation;
    public void navigation(Navigation value){navigation=value;}
    public ApdSettingsSection section(){return section;}
    public void showSection(ApdSettingsSection next){
        ApdSettingsSection previous=section;section=next;layout();
        if(navigation!=null)navigation.changed(previous);
    }
    /** Also supports direct links to a setting without changing its value. */
    public void reveal(Component control){for(SettingRow r:rows)if(r.control==control){if(section!=r.section)showSection(r.section);return;}}
    private final class SettingRow {
        final ApdSettingsSection section;final Component control;final String note;
        RenderedTextBlock description;
        SettingRow(ApdSettingsSection section,Component control,String note){
            this.section=section;this.control=control;this.note=note;
        }
        RenderedTextBlock description(){
            if(description==null&&!note.isEmpty()){
                description=PixelScene.renderTextBlock(note,6);
                description.hardlight(0xBBBBBB);
                APDSettingsPane.this.add(description);
            }
            return description;
        }
    }
    private void row(ApdSettingsSection section,Component control,String note){if(control!=null)rows.add(new SettingRow(section,control,note));}
    private static class SettingButton extends RedButton {
        SettingButton(String label){this(label,7);}
        SettingButton(String label,int ignored){super(label,7);remove(bg);bg=Chrome.get(Chrome.Type.GREY_BUTTON);addToBack(bg);multiline=true;}
    }
    private static class SettingCheckBox extends CheckBox {
        SettingCheckBox(String label){super(label);remove(bg);bg=Chrome.get(Chrome.Type.GREY_BUTTON);addToBack(bg);
            remove(text);text=PixelScene.renderTextBlock(label,7);add(text);}
    }

	@Override
	protected void createChildren() {
        rows=new java.util.ArrayList<>();categories=new java.util.ArrayList<>();
		title = PixelScene.renderTextBlock( "APD Settings", 9 );
		title.hardlight( Window.TITLE_COLOR );
		add( title );

		optExotic = new OptionSlider( "Healing to Shielding chance (new runs)",
				"0%", SPDSettings.APD_EXOTIC_CHANCE_MAX + "%",
				0, SPDSettings.APD_EXOTIC_CHANCE_MAX / STEP ) {
			@Override
			protected void onChange() {
				SPDSettings.apdExoticChance( getSelectedValue() * STEP );
			}
		};
		optExotic.setSelectedValue( SPDSettings.apdExoticSetting() / STEP );
		add( optExotic );

		info = PixelScene.renderTextBlock(
				"Exact chance that a generated Potion of Healing becomes Shielding, including drops affected by Exotic Crystals. 0% prevents this conversion. Other potions and scrolls keep their normal behavior.", 6 );
		add( info );

		chkRecord = new SettingCheckBox( "Record run history" ) {
			@Override
			protected void onClick() {
				super.onClick();
				SPDSettings.apdRecording( checked() );
				ShatteredPixelDungeon.scene().addToFront( new WndMessage( "Takes effect the next time the game starts." ) );
			}
		};
		chkRecord.checked( SPDSettings.apdRecording() );
		add( chkRecord );
        chkDotOverlay=new SettingCheckBox("Damage-over-time panel"){
            @Override protected void onClick(){super.onClick();SPDSettings.dotOverlayEnabled(checked());}
        };
        chkDotOverlay.checked(SPDSettings.dotOverlayEnabled());
        add(chkDotOverlay);
        chkTrophyOutlines=new SettingCheckBox("Trophy outlines"){
            @Override protected void onClick(){super.onClick();SPDSettings.trophyOutlines(checked());}
        };
        chkTrophyOutlines.checked(SPDSettings.trophyOutlines());
        add(chkTrophyOutlines);

		//Unknown age leaves the choice usable; verified underage players receive Clean.
		chkAdult = new SettingCheckBox( "Adult humor (18+)" ) {
			@Override
			protected void onClick() {
				if (!com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.permitsAdult(
						com.shatteredpixel.shatteredpixeldungeon.apd.age.AgeEligibility.current())){
					checked(false);
					ShatteredPixelDungeon.scene().addToFront(new WndMessage(
							"Adult humor is unavailable for players verified as under 18."));
					return;
				}
				super.onClick();
				SPDSettings.apdAdultHumor( checked() );
			}
		};
		chkAdult.checked( SPDSettings.apdAdultHumor() );
		add( chkAdult );

        btnSearchedWallColor=new SettingButton(searchedWallColorText()){
            @Override protected void onClick(){
                java.util.List<WndUimsdList.Entry> choices=new java.util.ArrayList<>();
                for(com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.Color color:com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.Color.values())
                    choices.add(WndUimsdList.Entry.item(color.label+(color.ordinal()==SPDSettings.searchedWallColor()?" (current)":"")));
                ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Searched-wall checkmarks",
                        "Choose a color. Changes apply immediately to remembered marks. A mark means searched, not guaranteed free of secrets. Only plausible door positions are marked.",choices,
                        (index,checked)->{SPDSettings.searchedWallColor(index);btnSearchedWallColor.text(searchedWallColorText());}));
            }
        };add(btnSearchedWallColor);

        chkTileVisibility=new SettingCheckBox("Enhanced tile visibility"){
            @Override protected void onClick(){super.onClick();SPDSettings.enhancedTileVisibility(checked());}
        };
        chkTileVisibility.checked(SPDSettings.enhancedTileVisibility());add(chkTileVisibility);
        chkStairNavigation=new SettingCheckBox("Stair navigation shortcut"){
            @Override protected void onClick(){super.onClick();SPDSettings.stairNavigationShortcut(checked());
                if(checked()) ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                    "Tap the staircase icon beside the hotbar, above the side controls, and choose Stairs Up or Stairs Down. Only discovered stairs are available. Enable Auto-enter Stairs below to also change floors on arrival."));
            }
        };
        chkStairNavigation.checked(SPDSettings.stairNavigationShortcut());add(chkStairNavigation);
        chkAutoEnterStairs=new SettingCheckBox("Auto-enter stairs"){
            @Override protected void onClick(){super.onClick();SPDSettings.autoEnterStairs(checked());}
        };
        chkAutoEnterStairs.checked(SPDSettings.autoEnterStairs());add(chkAutoEnterStairs);

        btnWaitingInterruptions=new SettingButton("Waiting interruptions"){
            @Override protected void onClick(){ShatteredPixelDungeon.scene().addToFront(new WndWaitingSettings());}
        };add(btnWaitingInterruptions);
        chkAutoEatWaiting=new SettingCheckBox("Auto-Eat While Waiting"){
            @Override protected void onClick(){super.onClick();SPDSettings.autoEatWhileWaiting(checked());}
        };chkAutoEatWaiting.checked(SPDSettings.autoEatWhileWaiting());add(chkAutoEatWaiting);
        btnAutoEatTrigger=new SettingButton("Auto-Eat trigger: "+(SPDSettings.autoEatWhenHungry()?"Hungry":"Starving")){
            @Override protected void onClick(){
                ShatteredPixelDungeon.scene().addToFront(new WndOptions("Auto-Eat trigger","Only while continuously waiting. Uses ordinary rations or safely cooked meat, with normal eating time. Special food is left for manual use.","Hungry","Starving"){
                    @Override protected void onSelect(int index){SPDSettings.autoEatWhenHungry(index==0);btnAutoEatTrigger.text("Auto-Eat trigger: "+(SPDSettings.autoEatWhenHungry()?"Hungry":"Starving"));}
                });
            }
        };add(btnAutoEatTrigger);

        chkSingleTapSearch=new SettingCheckBox("Single-tap Search"){
            @Override protected void onClick(){super.onClick();SPDSettings.singleTapSearch(checked());}
        };
        chkSingleTapSearch.checked(SPDSettings.singleTapSearch());add(chkSingleTapSearch);
        chkAdaptiveSearchChoice=new SettingCheckBox("Adaptive Search choice"){
            @Override protected void onClick(){super.onClick();SPDSettings.adaptiveSearchChoice(checked());}
        };
        chkAdaptiveSearchChoice.checked(SPDSettings.adaptiveSearchChoice());add(chkAdaptiveSearchChoice);
        chkContinueTrapDiscovery=new SettingCheckBox("Continue when traps are revealed"){
            @Override protected void onClick(){super.onClick();SPDSettings.continueOnTrapDiscovery(checked());}
        };
        chkContinueTrapDiscovery.checked(SPDSettings.continueOnTrapDiscovery());add(chkContinueTrapDiscovery);


        btnPlaybackSpeed = new SettingButton("Game speed: "
                + com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.label(SPDSettings.playbackSpeed())) {
            @Override protected void onClick() {
                if (com.shatteredpixel.shatteredpixeldungeon.apd.ApdFeatures.extendedPlayback()) {
                    final java.util.List<WndUimsdList.Entry> choices = new java.util.ArrayList<>();
                    for (int i = 0; i < com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.choiceCount(); i++) {
                        choices.add(WndUimsdList.Entry.item(
                                com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.label(i))
                                .current(i == SPDSettings.playbackSpeed()));
                    }
                    ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Game speed",
                            "Visual playback only. Combat turns and turn costs remain unchanged.",
                            choices, (index, checked) -> {
                                if (!com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.allowed(index)) return;
                                SPDSettings.playbackSpeed(index);
                                btnPlaybackSpeed.text("Game speed: "
                                        + com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.label(SPDSettings.playbackSpeed()));
                            }));
                } else {
                    ShatteredPixelDungeon.scene().addToFront(new WndOptions("Game speed",
                            "Playback only. Turn costs and combat stay unchanged.", "0.5x", "1x", "2x") {
                        @Override protected void onSelect(int index) {
                            if (!com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.allowed(index)) return;
                            SPDSettings.playbackSpeed(index);
                            btnPlaybackSpeed.text("Game speed: "
                                    + com.shatteredpixel.shatteredpixeldungeon.apd.PlaybackGroup.label(SPDSettings.playbackSpeed()));
                        }
                    });
                }
            }
        };
        add(btnPlaybackSpeed);
        // This is intentionally on the shared APD settings pane: both ordinary
        // player builds and private developer builds receive the same control.
        btnWaterskinSlot = new SettingButton("Waterskin start: "
                + com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.WaterskinStartupSlot.label(
                        SPDSettings.waterskinStartingSlot()), 7) {
            @Override protected void onClick() {
                final java.util.List<WndUimsdList.Entry> choices = new java.util.ArrayList<>();
                int selected = SPDSettings.waterskinStartingSlot();
                for (int position = -1; position <= 6; position++) {
                    choices.add(WndUimsdList.Entry.item(
                            com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.WaterskinStartupSlot.label(position))
                            .current(position == selected));
                }
                ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(
                        "Waterskin Starting Hotbar Slot",
                        "New runs only. Default Slot 2. Resuming a saved run keeps every manual assignment. Category hotbars have separate layouts.",
                        choices, (index, checked) -> {
                            if (index < 0 || index > 7) return;
                            SPDSettings.waterskinStartingSlot(index - 1);
                            btnWaterskinSlot.text("Waterskin start: "
                                    + com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.WaterskinStartupSlot.label(
                                            SPDSettings.waterskinStartingSlot()));
                        }));
            }
        };
        add(btnWaterskinSlot);
        btnHealingConfirmation = new SettingButton("Healing confirmation") {
            @Override protected void onClick() { WndHealingConfirmation.settings(); }
        };
        add(btnHealingConfirmation);
        btnCustomRules = new SettingButton("Custom Dungeon Rules", 6) {
            @Override protected void onClick() {
                ShatteredPixelDungeon.scene().addToFront(new WndCustomDungeonRules());
            }
        };
        add(btnCustomRules);
        btnCurrentCustomRules = new SettingButton("View current run rules", 6) {
            @Override protected void onClick() {
                ShatteredPixelDungeon.scene().addToFront(new WndCustomDungeonRules(true));
            }
        };
        btnCurrentCustomRules.visible = Game.scene() instanceof GameScene;
        btnCurrentCustomRules.active = btnCurrentCustomRules.visible;
        add(btnCurrentCustomRules);
		btnClericQuickCasts = new SettingButton("Cleric quick-cast buttons") {
            @Override protected void onClick() { WndClericQuickCasts.settings(); }
        };
        add(btnClericQuickCasts);

		status = PixelScene.renderTextBlock( "", 6 );
		add( status );

		storage = PixelScene.renderTextBlock( "", 6 );
		add( storage );

		btnTest = new SettingButton( "Run self-test" ) {
			@Override
			protected void onClick() {
				runSelfTest( this );
			}
		};
		add( btnTest );

        chkExtendedBugLogging=new SettingCheckBox("Extended bug logging (OFF by default)") {
            @Override protected void onClick() {
                super.onClick();
                if (!checked()) {
                    SPDSettings.apdExtendedBugLogging(false);
                    ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                            "Extended logging is off. The locally stored action journal has been cleared. "
                            +"Standard bug reports still work."));
                    return;
                }
                // The checkbox is NOT enabled merely because it was tapped.
                checked(false);
                java.util.List<WndUimsdList.Entry> disclosures=new java.util.ArrayList<>();
                disclosures.add(WndUimsdList.Entry.warning("OFF BY DEFAULT. Turning this on begins collecting APD activity on this device until you turn it off."));
                disclosures.add(WndUimsdList.Entry.heading("Player input"));
                disclosures.add(WndUimsdList.Entry.note("Time and order of touches, taps, swipes, touch positions, pointer IDs, non-text key press/release and Back navigation. Typed text, passwords and keyboard contents are excluded."));
                disclosures.add(WndUimsdList.Entry.heading("In-game actions"));
                disclosures.add(WndUimsdList.Entry.note("Clicked game controls, available hero movement, attacks, interactions, settings changed and their numeric/boolean values. Text settings are redacted."));
                disclosures.add(WndUimsdList.Entry.heading("Game and device observations"));
                disclosures.add(WndUimsdList.Entry.note("Game screens opened, error classes, memory usage samples, performance milestones, when APD goes into the background, when it returns and wall-clock time away. These do not reveal which other app you used or prove why you left."));
                disclosures.add(WndUimsdList.Entry.heading("Retention and sharing"));
                disclosures.add(WndUimsdList.Entry.note("Collected locally in APD private storage, across play sessions and restarts, keeping the newest 8 MiB (older actions are dropped first). If recording fails, some actions will be missing. Extended bug reports include this history since the last successfully saved extended report; Standard remains the old report. Nothing is automatically sent or uploaded. Review the report before sharing. Turning this setting OFF stops collection and deletes the local action history. Existing game saves are not read for this journal."));
                disclosures.add(WndUimsdList.Entry.action("Enable Extended logging (I understand)"));
                disclosures.add(WndUimsdList.Entry.action("Keep logging OFF"));
                ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(
                        "Extended bug logging: disclosure", "",
                        disclosures, (index, checked) -> {
                            if(index==0){
                                SPDSettings.apdExtendedBugLogging(true);
                                chkExtendedBugLogging.checked(true);
                            } else {
                                chkExtendedBugLogging.checked(false);
                            }
                        }));
            }
        };
        chkExtendedBugLogging.checked(SPDSettings.apdExtendedBugLogging());
        add(chkExtendedBugLogging);
        row(ApdSettingsSection.ADVANCED,chkExtendedBugLogging,
                "OFF by default. Explicit opt-in records APD actions locally between extended reports; turn off to erase the journal.");

        final RedButton diagnosticReport=new SettingButton("Create bug report") {
            @Override protected void onClick() {
                final RedButton source=this;
                ShatteredPixelDungeon.scene().addToFront(new WndOptions(
                        "Bug report type",
                        "Standard keeps the original report. Extended requires logging to be turned ON "
                        +"in Advanced Settings after reading the full disclosure, and adds the chronological "
                        +"in-game action history since the last successfully saved extended report, "
                        +"plus time APD spent in the background. Review the saved file before sharing.",
                        "Standard", "Extended") {
                    @Override protected void onSelect(int index) {
                        if(index!=0 && index!=1)return;
                        if(index==1 && !SPDSettings.apdExtendedBugLogging()){
                            ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                                    "Extended bug logging is OFF. Enable it explicitly in Advanced Settings "
                                    +"and read the full warning before collecting an action history. "
                                    +"You can still choose Standard now."));
                            return;
                        }
                        final com.watabou.noosa.Game engine=Game.instance;
                        final com.watabou.noosa.Scene owner=Game.scene();
                        source.enable(false);
                        com.shatteredpixel.shatteredpixeldungeon.apd.diagnostics.DiagnosticReport.export(
                                message->Game.runOnRenderThread(()->{
                                    if(Game.instance!=engine || Game.scene()!=owner || source.parent==null)return;
                                    source.enable(true);owner.addToFront(new WndMessage(message));
                                }),index==1);
                    }
                });
            }
        };
        add(diagnosticReport);btnDiagnosticReport=diagnosticReport;
        row(ApdSettingsSection.ADVANCED,diagnosticReport,
                "Choose Standard (existing support report) or Extended (adds APD action chronology, touches, and time away). Review before sharing; no automatic upload.");

		btnImport = new SettingButton( "Import APD rankings" ) {
			@Override
			protected void onClick() {
				runImport( this );
			}
		};
		add( btnImport );

		btnExportHistory = new SettingButton( "Export history (ZIP)" ) {
			@Override
			protected void onClick() {
				runTransfer( this, "Export history (ZIP)", true );
			}
		};
		add( btnExportHistory );

		btnImportHistory = new SettingButton( "Import history (ZIP)" ) {
			@Override
			protected void onClick() {
				runTransfer( this, "Import history (ZIP)", false );
			}
		};
		add( btnImportHistory );

		btnUimsd = new SettingButton( "Open UIMSD (preview)" ) {
			@Override
			protected void onClick() {
				UimsdScene.open( ShatteredPixelDungeon.scene().getClass() == GameScene.class ? GameScene.class : TitleScene.class );
			}
		};
		add( btnUimsd );

		if (GrudgeHooks.available()) {
			btnGrudge = new SettingButton( "About / Developer: The Grudge" ) {
				@Override
				protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront( new WndClericGrudge() );
				}
			};
			add( btnGrudge );
		}

		if (DemoData.available()) {
			btnDemoGenerate = new SettingButton( "DEMO data: generate / reset" ) {
				@Override
				protected void onClick() {
					generateDemo( this );
				}
			};
			add( btnDemoGenerate );

			btnDemoRemove = new SettingButton( "DEMO data: remove all" ) {
				@Override
				protected void onClick() {
					DemoData.viewDemo( false );
					boolean gone = DemoData.delete( demoRoot() );
					btnDemoView.text( demoViewText() );
					ShatteredPixelDungeon.scene().addToFront( new WndMessage( gone
							? "All demo data removed. Your real history was not touched. UIMSD shows real data."
							: "Nothing was removed: that folder is not demo data." ) );
				}
			};
			add( btnDemoRemove );

			btnDemoView = new SettingButton( demoViewText() ) {
				@Override
				protected void onClick() {
					DemoData.viewDemo( !DemoData.viewingDemo() );
					text( demoViewText() );
				}
			};
			add( btnDemoView );
		}
        btnHotbarLayout=new SettingButton("Hotbar: "+com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.MODES[SPDSettings.apdHotbarMode()]){ @Override protected void onClick(){
            java.util.List<WndUimsdList.Entry> choices=new java.util.ArrayList<>();
            for(int i=0;i<com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.MODES.length;i++)choices.add(WndUimsdList.Entry.item(com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.MODES[i]).current(i==SPDSettings.apdHotbarMode()));
            ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Hotbar layout","Immediate. Classic item positions and saved custom layouts are kept when switching modes.",choices,(index,checked)->{
                SPDSettings.apdHotbarMode(index);btnHotbarLayout.text("Hotbar: "+com.shatteredpixel.shatteredpixeldungeon.apd.hotbar.CategoryHotbar.MODES[index]);com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.cancel();com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar.updateLayout();com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();
            }));
        }};add(btnHotbarLayout);
        btnHotbarCustom=new SettingButton("Configure custom hotbar"){ @Override protected void onClick(){WndCategoryHotbar.slots();}};add(btnHotbarCustom);
        chkFoodInHealing=new SettingCheckBox("Food in Healing"){@Override protected void onClick(){super.onClick();SPDSettings.apdFoodInHealing(checked());com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.refresh();}};
        chkFoodInHealing.checked(SPDSettings.apdFoodInHealing());add(chkFoodInHealing);
        chkRatKingSearch=new SettingCheckBox("Rat King Quick Search"){@Override protected void onClick(){super.onClick();SPDSettings.ratKingQuickSearch(checked());}};
        chkRatKingSearch.checked(SPDSettings.ratKingQuickSearch());add(chkRatKingSearch);
        row(ApdSettingsSection.GAMEPLAY,chkRatKingSearch,"Off by default. After Goo is defeated, Search can reveal only the Rat King's entrance on Floor 5. Pays the minimum ordinary walking-and-search time through normal turns. Other floors and ordinary Search remain unchanged when off. Temporary effects that prevent a reliable price must finish first.");
        row(ApdSettingsSection.GAMEPLAY,btnWaitingInterruptions,"Independent stops for full health, starvation onset and critical HP. Defaults preserve existing waiting; critical HP is opt-in. You may explicitly disable all three.");
        row(ApdSettingsSection.GAMEPLAY,chkAutoEatWaiting,"Off by default. While waiting, automatically eat available ordinary rations or safely cooked meat at the selected trigger. Pays normal eating time and consumes one food. If none is suitable, your interruption preferences apply.");
        row(ApdSettingsSection.GAMEPLAY,btnAutoEatTrigger,"Saved independently of Auto-Eat and interruption choices. Special, raw and unknown food is never automatically consumed.");
        row(ApdSettingsSection.GAMEPLAY,chkSingleTapSearch,"On: tap to search nearby; hold to examine a tile, enemy or item. Off: tap to examine; hold to search. A search costs the normal turns; examining is free.");
        row(ApdSettingsSection.VISIBILITY,chkAdaptiveSearchChoice,"On: choose ordinary Search first in an open room, and remembered Wall Search first when an eligible nearby wall needs checking. Off: always offer ordinary Search first. Both actions remain manually available. Does not search or spend turns automatically.");
        row(ApdSettingsSection.GAMEPLAY,chkContinueTrapDiscovery,"Off by default. On: revealing a hidden trap while moving keeps your destination and held movement. Normal pathfinding still avoids discovered hazards where it normally would. Stepping onto a trap, trap confirmation, enemies, damage, blocked routes and manual cancellation keep their normal behavior. Off: trap discovery can stop movement. Manual Search and other secret discoveries keep their normal interruptions.");
        row(ApdSettingsSection.GAMEPLAY,btnPlaybackSpeed,"Immediate visual playback speed. Turn costs and combat rules stay unchanged.");
        row(ApdSettingsSection.GAMEPLAY,btnHealingConfirmation,"Immediate. Choose which healing actions ask first, including low-health thresholds.");
        row(ApdSettingsSection.GAMEPLAY,chkAdult,"Immediate content preference. Off keeps humor clean; verified under-18 accounts cannot enable adult humor.");
        row(ApdSettingsSection.RULES,optExotic,"New runs only. Your current run keeps its recorded Healing-to-Shielding chance.");
        row(ApdSettingsSection.RULES,info,"");
        row(ApdSettingsSection.RULES,btnCustomRules,"New runs only. Configure the 18 experimental Custom Dungeon Rules. Existing runs keep their saved rules.");
        row(ApdSettingsSection.RULES,btnCurrentCustomRules,"Read only. Inspect the rules saved with the current run.");
        row(ApdSettingsSection.HOTBAR,btnHotbarLayout,"Immediate. Choose Classic SPD, category presets or Custom. Switching layouts retains the other layouts and Classic item assignments.");
        row(ApdSettingsSection.HOTBAR,btnHotbarCustom,"Immediate. Assign categories or known item types to six custom positions. Classic assignments are retained.");
        row(ApdSettingsSection.HOTBAR,chkFoodInHealing,"Immediate. Also offer food from a Healing category slot; normal food costs still apply.");
        row(ApdSettingsSection.HOTBAR,btnWaterskinSlot,"New runs only. This starting preference never rearranges a resumed run or changes manual hotbar assignments.");
        row(ApdSettingsSection.HOTBAR,btnClericQuickCasts,"Immediate control preference. Configure Cleric quick-cast buttons without changing spell costs.");
        row(ApdSettingsSection.VISIBILITY,btnSearchedWallColor,"Immediate display preference, saved between sessions. Choose a high-contrast checkmark color. Corner/end pieces are left unmarked. Checkmarks record searches, not proof that a wall is clear. Search chances stay unchanged.");
        row(ApdSettingsSection.VISIBILITY,chkTileVisibility,"Display preference. Make visible tiles easier to distinguish without revealing undiscovered terrain.");
        row(ApdSettingsSection.VISIBILITY,chkDotOverlay,"Immediate display preference. Show or hide the collapsible damage-over-time panel.");
        row(ApdSettingsSection.VISIBILITY,chkTrophyOutlines,"Immediate display preference, off by default. Show bronze, silver and gold outlines on enemies you've defeated enough times. The Bestiary always shows them.");
        row(ApdSettingsSection.VISIBILITY,chkStairNavigation,"Immediate shortcut to discovered stairs only. This does not reveal hidden stairs.");
        row(ApdSettingsSection.VISIBILITY,chkAutoEnterStairs,"Immediate. Automatically change floors when a stair-navigation path reaches the staircase.");
        row(ApdSettingsSection.HISTORY,btnUimsd,"Open your recorded statistics, queries and investigation tools.");
        row(ApdSettingsSection.HISTORY,chkRecord,"After restarting APD. Enable or disable recording; existing history is preserved.");
        row(ApdSettingsSection.HISTORY,btnExportHistory,"Save a ZIP backup of run history using the platform file chooser.");
        row(ApdSettingsSection.HISTORY,btnImportHistory,"Import a history ZIP through the platform file chooser. Existing records remain protected.");
        row(ApdSettingsSection.HISTORY,btnImport,"Import existing APD rankings. Unrecorded historical details stay unknown.");
        row(ApdSettingsSection.ADVANCED,status,"Recording diagnostics: version, log identity and event counts. These are information, not gameplay settings.");
        row(ApdSettingsSection.ADVANCED,storage,"Storage used by recorded history and caches.");
        row(ApdSettingsSection.ADVANCED,btnTest,"Run offline statistics, query and archive self-tests. This does not alter your saves.");
        row(ApdSettingsSection.ADVANCED,btnGrudge,"Open the existing Cleric lore and developer information controls.");
        row(ApdSettingsSection.ADVANCED,btnDemoGenerate,"Developer tool. Generate made-up demo history separately from real records.");
        row(ApdSettingsSection.ADVANCED,btnDemoRemove,"Developer tool. Remove demo history only; real run history is not deleted.");
        row(ApdSettingsSection.ADVANCED,btnDemoView,"Developer tool. Choose whether UIMSD displays real history or clearly marked demo data.");
        sectionInfo=PixelScene.renderTextBlock("",6);sectionInfo.hardlight(0xBBBBBB);add(sectionInfo);
        btnSections=new SettingButton("< All APD settings"){@Override protected void onClick(){showSection(null);}};add(btnSections);
        for(ApdSettingsSection choice:ApdSettingsSection.values()){
            SettingButton button=new SettingButton(choice.title){@Override protected void onClick(){showSection(choice);}};
            categories.add(button);add(button);
        }

	}

    private static String searchedWallColorText(){
        return "Searched-wall color: "+com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance.color(SPDSettings.searchedWallColor()).label;
    }

	private static String demoViewText() {
		return DemoData.viewingDemo() ? "UIMSD shows: DEMO data" : "UIMSD shows: REAL data";
	}

	private static java.io.File demoRoot() {
		java.io.File base = ApdRecorder.historyRoot();
		if (base == null) base = FileUtils.getFileHandle( "apd" ).file();
		return DemoData.demoRootFor( base );
	}

	private void generateDemo( final RedButton button ) {
		button.text( "Generating..." );
		button.enable( false );
		Thread t = new Thread( new Runnable() {
			@Override
			public void run() {
				String result;
				try {
					int runs = DemoGenerator.generate( demoRoot(), Ruleset.forNewRun(), System.currentTimeMillis() );
					DemoData.viewDemo( true );
					result = runs + " made-up runs written. UIMSD now shows DEMO data (the button below switches back to your real data). Your real history was not touched.";
				} catch (Throwable e) {
					result = "Demo data failed: " + e;
				}
				final String shown = result;
				Game.runOnRenderThread( new Callback() {
					@Override
					public void call() {
						button.text( "DEMO data: generate / reset" );
						button.enable( true );
						btnDemoView.text( demoViewText() );
						ShatteredPixelDungeon.scene().addToFront( new WndMessage( shown ) );
					}
				} );
			}
		} );
		t.start();
	}

	@Override
    protected void layout(){
        if(rows==null || width<=0)return;
        title.text(section==null ? "APD Settings":section.title);title.maxWidth((int)width);title.setPos(x+(width-title.width())/2,y);
        float pos=title.bottom()+GAP;
        sectionInfo.text(section==null ? "Choose a category. Each setting explains when it applies.":section.description);
        sectionInfo.maxWidth((int)width);sectionInfo.setPos(x,pos);pos=sectionInfo.bottom()+GAP;
        for(SettingRow r:rows){
            r.control.visible=r.control.active=false;
            if(r.description!=null)r.description.visible=r.description.active=false;
        }
        for(SettingButton button:categories)button.visible=button.active=false;
        btnSections.visible=btnSections.active=section!=null;
        if(section==null){
            btnDiagnosticReport.visible=btnDiagnosticReport.active=true;btnDiagnosticReport.setRect(x,pos,width,24);pos=btnDiagnosticReport.bottom()+GAP;
            for(SettingButton button:categories){button.visible=button.active=true;button.setRect(x,pos,width,24);pos=button.bottom()+GAP;}
        }else{
            btnSections.setRect(x,pos,width,20);pos=btnSections.bottom()+2*GAP;
            if(section==ApdSettingsSection.ADVANCED && !diagnosticsLoaded){
                status.text(ApdRecorder.status().summary());
                try{storage.text(HistoryTransfer.storage().summary());}catch(Throwable e){storage.text("History storage could not be measured: "+e.getMessage());}
                diagnosticsLoaded=true;
            }
            for(SettingRow r:rows){
                if(r.section!=section || r.control==btnCurrentCustomRules && !(Game.scene() instanceof GameScene))continue;
                r.control.visible=r.control.active=true;
                if(r.control instanceof RenderedTextBlock){RenderedTextBlock t=(RenderedTextBlock)r.control;t.maxWidth((int)width);t.setPos(x,pos);}
                else if(r.control==optExotic)r.control.setRect(x,pos,width,26);
                else r.control.setRect(x,pos,width,22);
                pos=r.control.bottom()+2;
                RenderedTextBlock description=r.description();
                if(description!=null){description.visible=description.active=true;description.maxWidth((int)width);description.setPos(x,pos);pos=description.bottom()+GAP+2;}
                else pos+=GAP;
            }
        }
        height=pos-y;
    }

	//export or import the whole history through the platform's file chooser; the result is shown in plain words
	private void runTransfer( final RedButton button, final String label, boolean export ) {
		button.enable( false );
		HistoryTransfer.Listener done = new HistoryTransfer.Listener() {
			@Override
			public void finished( final String message ) {
				Game.runOnRenderThread( new Callback() {
					@Override
					public void call() {
						button.enable( true );
						ShatteredPixelDungeon.scene().addToFront( new WndMessage( message ) );
					}
				} );
			}
		};
		if (export) HistoryTransfer.export( done );
		else HistoryTransfer.importHistory( done );
	}

	private static void runImport( final RedButton button ) {
		button.text( "Importing..." );
		button.enable( false );
		Thread t = new Thread( new Runnable() {
			@Override
			public void run() {
				String result;
				try {
					result = ApdRecorder.importApdRankings() + "\n\n" + ApdRecorder.historyOverview();
				} catch (Throwable e) {
					result = "Import failed: " + e;
				}
				final String shown = result;
				Game.runOnRenderThread( new Callback() {
					@Override
					public void call() {
						button.text( "Import APD rankings" );
						button.enable( true );
						ShatteredPixelDungeon.scene().addToFront( new WndMessage( shown ) );
					}
				} );
			}
		} );
		t.start();
	}

	private static void runSelfTest( final RedButton button ) {
		button.text( "Running..." );
		button.enable( false );
		Thread t = new Thread( new Runnable() {
			@Override
			public void run() {
				String result;
				try {
					java.io.File base = ApdRecorder.historyRoot();
					if (base == null) base = FileUtils.getFileHandle( "apd" ).file();
					java.io.File work = new java.io.File( base, "selftest-work" );
					work.mkdirs();
					SelfTest.Report report = SelfTest.run( work );
					com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdSelfTest.run( work, report );
					com.shatteredpixel.shatteredpixeldungeon.apd.archive.ArchiveSelfTest.run( work, report );
					result = report.text();
					work.delete();
				} catch (Throwable e) {
					result = "SELF-TEST FAILED\n" + e;
				}
				final String shown = result;
				Game.runOnRenderThread( new Callback() {
					@Override
					public void call() {
						button.text( "Run self-test" );
						button.enable( true );
						ShatteredPixelDungeon.scene().addToFront( new WndMessage( shown ) );
					}
				} );
			}
		} );
		t.start();
	}
}

