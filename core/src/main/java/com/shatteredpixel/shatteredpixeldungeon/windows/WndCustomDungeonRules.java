package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomDungeonRules;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRulePresets;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomScoreSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRulesPresentation;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** Native controls edit next-run preferences, never the locked active-run configuration. */
public class WndCustomDungeonRules extends Window {
    private CustomDungeonRules draft;
    private final boolean currentRun;
    private final CustomScoreSnapshot frozenScoring;
    private final RenderedTextBlock scoringNotice;
    private final ScrollPane scroll;
    private final RenderedTextBlock combined;
    private final OptionSlider[] sliderControls=new OptionSlider[CustomDungeonRules.Slider.values().length];
    private final RenderedTextBlock[] sliderCaptions=new RenderedTextBlock[CustomDungeonRules.Slider.values().length];
    public WndCustomDungeonRules() {this(false);}
    public WndCustomDungeonRules(final boolean currentRun) {
        super();
        this.currentRun=currentRun;
        frozenScoring=currentRun ? Dungeon.customScoring() : null;
        draft=currentRun ? Dungeon.customRules() : SPDSettings.apdCustomRules();
        int width=WindowGeometry.width(PixelScene.landscape(),PixelScene.uiCamera.width);
        RenderedTextBlock title=PixelScene.renderTextBlock("Custom Dungeon Rules",9);
        title.maxWidth(width);title.hardlight(TITLE_COLOR);title.setPos(0,0);add(title);
        RenderedTextBlock experimentalHeading=PixelScene.renderTextBlock(CustomRulesPresentation.EXPERIMENTAL_HEADING,6);
        experimentalHeading.maxWidth(width);experimentalHeading.hardlight(TITLE_COLOR);
        experimentalHeading.setPos(0,title.bottom()+4);add(experimentalHeading);
        combined=PixelScene.renderTextBlock("",6);combined.maxWidth(width);combined.hardlight(TITLE_COLOR);
        combined.setPos(0,experimentalHeading.bottom()+3);add(combined);updateCombined();
        float above=combined.bottom()+4;
        // Create the scroll touch area before controls, matching WndAPDSettings' touch ordering.
        Component content=new Component();scroll=new ScrollPane(content);add(scroll);
        // The full warning leads the same scroll area, so narrow viewports retain room for controls.
        scoringNotice=PixelScene.renderTextBlock(CustomRulesPresentation.noticeText(),6);
        scoringNotice.maxWidth(width);scoringNotice.setPos(0,0);content.add(scoringNotice);
        float y=scoringNotice.bottom()+4;
        RedButton scoringInfo=new RedButton(CustomRulesPresentation.SCORING_INFO_BUTTON,6) {
            @Override protected void onClick() {
                com.watabou.noosa.Game.scene().addToFront(new WndUimsdText(
                        CustomRulesPresentation.SCORING_INFO_TITLE,CustomRulesPresentation.SCORING_VISION));
            }
        };
        scoringInfo.setRect(0,y,width,18);content.add(scoringInfo);y=scoringInfo.bottom()+4;
        if(draft==null) {
            RenderedTextBlock unknown=PixelScene.renderTextBlock("This run's custom settings were not recorded.",6);
            unknown.maxWidth(width);unknown.setPos(0,y);content.add(unknown);y=unknown.bottom()+4;
        } else {
            String mode=currentRun ? "Current run — locked" : "Next run only.";
            if(currentRun&&(Dungeon.customScoring()==null||Dungeon.customScoring().recordVersion<2))
                mode+=" Original score kept; multipliers preview new runs.";
            RenderedTextBlock context=PixelScene.renderTextBlock(mode,6);
            context.maxWidth(width);context.setPos(0,y);content.add(context);y=context.bottom()+4;
            if(!currentRun){
                RedButton presets=new RedButton("Quick presets",6){
                    @Override protected void onClick(){choosePreset();}
                };
                presets.setRect(0,y,width,18);content.add(presets);y=presets.bottom()+3;
                RedButton load=new RedButton("Load saved settings",6){
                    @Override protected void onClick(){chooseSlot(false);}
                };
                load.setRect(0,y,width,18);content.add(load);y=load.bottom()+3;
                RedButton save=new RedButton("Save settings to slot",6){
                    @Override protected void onClick(){chooseSlot(true);}
                };
                save.setRect(0,y,width,18);content.add(save);y=save.bottom()+3;
                RedButton rename=new RedButton("Rename preset slot",6){
                    @Override protected void onClick(){chooseRenameSlot();}
                };
                rename.setRect(0,y,width,18);content.add(rename);y=rename.bottom()+5;
            }
            for(final CustomDungeonRules.Slider rule:CustomDungeonRules.Slider.values()) {
                final RenderedTextBlock caption=PixelScene.renderTextBlock(caption(rule),6);
                caption.maxWidth(width);caption.setPos(0,y);content.add(caption);sliderCaptions[rule.ordinal()]=caption;y+=22;
                OptionSlider slider=new OptionSlider("",CustomRulesPresentation.valueText(rule,rule.min),
                        CustomRulesPresentation.valueText(rule,rule.max),0,CustomRulesPresentation.positions(rule)) {
                    @Override protected void onChange() {
                        draft=draft.with(rule,CustomRulesPresentation.value(rule,getSelectedValue()));
                        caption.text(caption(rule));caption.maxWidth((int)width());updateCombined();
                    }
                };
                slider.scrollVerticallyWith(scroll);slider.liveChanges(true);
                slider.setRect(0,y,width,22);
                slider.setSelectedValue(CustomRulesPresentation.position(rule,draft.get(rule)));
                sliderControls[rule.ordinal()]=slider;
                slider.enable(!currentRun && CustomRulesPresentation.gameplayImplemented(rule));
                content.add(slider);y=slider.bottom()+4;
            }
        }
        content.setSize(width,y);
        float paneHeight=WindowGeometry.paneHeight(y,WindowGeometry.maxPane(PixelScene.uiCamera.height,above,26));
        resize(width,(int)Math.ceil(above+paneHeight+26));scroll.setRect(0,above,width,paneHeight);
        RedButton finish=new RedButton(currentRun?"Close":"Save for new runs",6) {
            @Override protected void onClick() {
                if(!currentRun && draft!=null)SPDSettings.apdCustomRules(draft);
                hide();
            }
        };
        finish.setRect(0,above+paneHeight+4,width,22);add(finish);
    }
    /** All changes remain in the next-run draft until Save for new runs is pressed. */
    private void applyDraft(CustomDungeonRules selected){
        if(currentRun||selected==null)return;
        draft=selected;
        for(CustomDungeonRules.Slider rule:CustomDungeonRules.Slider.values()){
            int index=rule.ordinal();
            OptionSlider control=sliderControls[index];
            RenderedTextBlock label=sliderCaptions[index];
            if(control!=null)control.setSelectedValue(CustomRulesPresentation.position(rule,draft.get(rule)));
            if(label!=null){label.text(caption(rule));label.maxWidth((int)width);}
        }
        updateCombined();
    }
    private void choosePreset(){
        if(currentRun)return;
        final CustomRulePresets.Preset[] presets=CustomRulePresets.Preset.values();
        String[] options=new String[presets.length];
        for(int i=0;i<presets.length;i++)options[i]=presets[i].label;
        com.watabou.noosa.Game.scene().addToFront(new WndUimsdChoices(
                "Apply preset","Apply to the next-run draft. Individual sliders can still be adjusted.",options){
            @Override protected void onSelect(int index){
                if(index>=0&&index<presets.length)applyDraft(CustomRulePresets.apply(presets[index]));
            }
        });
    }
    private void chooseSlot(final boolean saving){
        if(currentRun)return;
        String[] options=new String[SPDSettings.APD_CUSTOM_RULE_SLOT_COUNT];
        for(int i=0;i<options.length;i++){
            boolean occupied=SPDSettings.apdCustomRuleSlotOccupied(i+1);
            options[i]="Slot "+(i+1)+": "+SPDSettings.apdCustomRuleSlotName(i+1)+(occupied?" (saved)":" (empty)");
        }
        com.watabou.noosa.Game.scene().addToFront(new WndUimsdChoices(
                saving?"Save settings":"Load settings",
                saving?"Save the current slider draft to one slot. Existing saved settings in that slot will be replaced."
                      :"Load a stored slider configuration into the next-run draft. Empty slots cannot be loaded.",options){
            @Override protected void onSelect(int index){
                if(index<0||index>=SPDSettings.APD_CUSTOM_RULE_SLOT_COUNT)return;
                int slot=index+1;
                // The scrollable choices widget deliberately allows every row to be
                // reached. Empty saved slots are inert and must not reset a draft.
                if(!saving&&!SPDSettings.apdCustomRuleSlotOccupied(slot)){
                    com.watabou.noosa.Game.scene().addToFront(
                            new WndMessage("Slot "+slot+" has no saved settings yet."));
                    return;
                }
                if(saving){
                    if(SPDSettings.apdCustomRuleSlotOccupied(slot)){
                        com.watabou.noosa.Game.scene().addToFront(new WndOptions(
                                "Overwrite slot "+slot+"?","This replaces the settings stored in this slot.",
                                "Overwrite","Cancel"){
                            @Override protected void onSelect(int choice){
                                if(choice==0)SPDSettings.apdCustomRuleSlot(slot,draft);
                            }
                        });
                    }else SPDSettings.apdCustomRuleSlot(slot,draft);
                }else applyDraft(SPDSettings.apdCustomRuleSlot(slot));
            }
        });
    }
    private void chooseRenameSlot(){
        if(currentRun)return;
        String[] options=new String[SPDSettings.APD_CUSTOM_RULE_SLOT_COUNT];
        for(int i=0;i<options.length;i++)
            options[i]="Slot "+(i+1)+": "+SPDSettings.apdCustomRuleSlotName(i+1);
        com.watabou.noosa.Game.scene().addToFront(new WndUimsdChoices(
                "Rename preset","Choose a slot to rename. This does not change its saved slider values.",options){
            @Override protected void onSelect(int index){
                if(index<0||index>=SPDSettings.APD_CUSTOM_RULE_SLOT_COUNT)return;
                final int slot=index+1;
                com.watabou.noosa.Game.scene().addToFront(new WndTextInput(
                        "Rename slot "+slot,
                        "Up to 24 characters. Leave blank to restore the class name.",
                        SPDSettings.apdCustomRuleSlotName(slot),24,false,"Save","Cancel"){
                    @Override public void onSelect(boolean confirmed,String text){
                        if(confirmed)SPDSettings.apdCustomRuleSlotName(slot,text);
                    }
                });
            }
        });
    }
    private void updateCombined(){
        String factor=draft==null?"Not recorded":
                currentRun && frozenScoring!=null && frozenScoring.status==CustomScoreSnapshot.Status.EXPERIMENTAL
                    ? com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRuleMultipliers.format(new java.math.BigDecimal(frozenScoring.multiplierExact))
                    : com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRuleMultipliers.format(com.shatteredpixel.shatteredpixeldungeon.apd.difficulty.CustomRuleMultipliers.combined(draft));
        combined.text("Combined Custom Rules Multiplier\n"+factor);
    }
    private String caption(CustomDungeonRules.Slider rule) {
        String factor=currentRun && frozenScoring!=null && frozenScoring.status==CustomScoreSnapshot.Status.EXPERIMENTAL
                ? CustomRulesPresentation.multiplierTextForVersion(rule,draft.get(rule),frozenScoring.calculationVersion)
                : CustomRulesPresentation.multiplierText(rule,draft.get(rule));
        return CustomRulesPresentation.label(rule)+": "+CustomRulesPresentation.valueText(rule,draft.get(rule))
            +" · "+factor;
    }
}
