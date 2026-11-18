package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.RunTurnBrowser;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** In-game UIMSD turn browser for a recorded run, also usable after importing history. */
public final class WndRunTurnBrowser extends Window {
    private final RunTurnBrowser model;
    private final RenderedTextBlock text;
    private final ScrollPane pane;
    private final int width;
    public WndRunTurnBrowser(RunTurnBrowser data){
        super();
        model=data;
        width=WndUimsdList.windowWidth();
        RenderedTextBlock title=PixelScene.renderTextBlock("Run history - turn by turn",9);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(width-4);
        title.setPos(2,2);
        add(title);

        Component body=new Component();
        text=PixelScene.renderTextBlock("",6);
        text.maxWidth(width-6);
        body.add(text);
        pane=new ScrollPane(body);
        float top=title.bottom()+3;
        float footer=40;
        float available=Math.max(35,WindowGeometry.maxPane(PixelScene.uiCamera.height,top,footer));
        resize(width,(int)(top+available+footer));
        add(pane);
        pane.setRect(0,top,width,available);

        float y=top+available+2;
        RedButton previous=new RedButton("Previous",6){@Override protected void onClick(){model.previous();refresh();}};
        RedButton next=new RedButton("Next",6){@Override protected void onClick(){model.next();refresh();}};
        previous.setRect(1,y,(width-4)/2f,16);
        next.setRect(previous.right()+2,y,(width-4)/2f,16);
        add(previous);add(next);
        RedButton step=new RedButton("Step: "+model.step(),6){@Override protected void onClick(){chooseStep();}};
        RedButton go=new RedButton("Go to turn",6){@Override protected void onClick(){chooseTurn();}};
        step.setRect(1,y+18,(width-4)/2f,16);
        go.setRect(step.right()+2,y+18,(width-4)/2f,16);
        add(step);add(go);
        refresh();
    }
    private void refresh(){
        text.text(model.summary());
        text.setPos(3,2);
        pane.content().setSize(width,Math.max(30,text.bottom()+4));
        pane.scrollTo(0,0);
    }
    private void chooseStep(){
        java.util.List<WndUimsdList.Entry> choices=new java.util.ArrayList<>();
        for(long n:new long[]{1,10,100,1000}) choices.add(WndUimsdList.Entry.item(n+" turn(s)").current(n==model.step()));
        choices.add(WndUimsdList.Entry.item("Custom amount..."));
        ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Turns per press",
                "Choose how far Previous and Next move.",choices,(index,checked)->{
            if(index>=0&&index<4){model.step(new long[]{1,10,100,1000}[index]);reopen();}
            else if(index==4) numberPrompt(true);
        }));
    }
    private void chooseTurn(){numberPrompt(false);}
    private void numberPrompt(boolean forStep){
        ShatteredPixelDungeon.scene().addToFront(new WndTextInput(forStep?"Custom step":"Go to turn",
                "Positive whole number",forStep?Long.toString(model.step()):Long.toString(model.turn()),19,false,"OK","Cancel"){
            @Override public void onSelect(boolean confirmed,String value){
                if(!confirmed)return;
                try {
                    long n=Long.parseLong(value.trim());
                    if(n<1)throw new NumberFormatException();
                    if(forStep)model.step(n);else model.go(n);
                    reopen();
                } catch (NumberFormatException invalid){
                    ShatteredPixelDungeon.scene().addToFront(new WndMessage("Enter a positive whole number."));
                }
            }
        });
    }
    private void reopen(){
        hide();
        ShatteredPixelDungeon.scene().addToFront(new WndRunTurnBrowser(model));
    }
    @Override public Runnable restoreOnSceneReset(){return ()->ShatteredPixelDungeon.scene().addToFront(new WndRunTurnBrowser(model));}
}
