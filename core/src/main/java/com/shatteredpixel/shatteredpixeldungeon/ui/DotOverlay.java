package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.DotForecast;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

/** Compact, live, collapsible in-game DoT panel. No game actions, prompts or actor updates. */
public final class DotOverlay extends Component {
    private final StyledButton toggle;
    private final RenderedTextBlock text;
    private final ScrollPane pane;
    private final NinePatch backdrop;
    private boolean expanded;
    private Char focused;
    private String previous="";
    private int effects;
    private float panelHeight;

    public DotOverlay(){
        super();
        toggle=new StyledButton(Chrome.Type.TOAST_TR,"DoT",6){
            @Override protected void onClick(){expanded=!expanded;rebuild(true);}
        };
        add(toggle);
        Component content=new Component();
        text=PixelScene.renderTextBlock("",6);
        content.add(text);
        pane=new ScrollPane(content);
        add(pane);
        backdrop=Chrome.get(Chrome.Type.WINDOW);
        addToBack(backdrop);
    }
    public void focus(Char enemy){
        focused=enemy;
        expanded=true;
        rebuild(true);
    }
    @Override public void update(){
        super.update();
        if(!com.shatteredpixel.shatteredpixeldungeon.SPDSettings.dotOverlayEnabled()){
            visible=false;active=false;return;
        }
        rebuild(false);
    }
    private void rebuild(boolean force){
        Char who=focused;
        if(who!=null && who!=Dungeon.hero){
            if(Dungeon.level==null || !who.isAlive() || who.pos<0
                    || who.pos>=Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[who.pos])focused=who=null;
        }
        if(who==null)who=Dungeon.hero;
        if(who==null){visible=false;active=false;return;}
        StringBuilder sb=new StringBuilder();
        int count=0;
        for(Buff buff:who.buffs()){
            DotForecast d=DotForecast.of(buff);
            if(d==null)continue;
            if(count++>0)sb.append("\n\n");
            sb.append(d.details());
        }
        effects=count;
        visible=active=count>0;
        if(count==0)return;
        String info=sb.toString();
        if(!force && info.equals(previous))return;
        previous=info;
        toggle.text((who==Dungeon.hero?"Hero ":"Enemy ")+"DoT: "+count+" "+(expanded?"[-]":"[+]"));
        text.text(info);
        layout();
    }
    @Override protected void layout(){
        super.layout();
        if(toggle==null || pane==null || text==null || backdrop==null)return;
        toggle.setRect(x,y,width,14);
        pane.visible=pane.active=expanded;
        backdrop.visible=expanded;
        if(expanded){
            float maxH=Math.max(35,Math.min(108f,PixelScene.uiCamera.height/3f));
            panelHeight=maxH;
            text.maxWidth((int)Math.max(1,width-8));
            text.setPos(3,3);
            pane.content().setSize(width,Math.max(20,text.bottom()+5));
            pane.setRect(x,y+15,width,panelHeight);
            backdrop.x=x-1;
            backdrop.y=y+14;
            backdrop.size(width+2,panelHeight+2);
        } else {
            panelHeight=0;
        }
    }
}
