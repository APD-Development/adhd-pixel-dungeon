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
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.ChangeIcons;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import java.util.*;

/** Native list presentation of the existing Home model; no calculations or persistence here. */
public class UimsdHomePane extends ScrollPane {
 public interface Listener {void onLink(String viewId);}
 private Listener listener;private UimsdHome home;private float builtWidth=-1;
 private final List<float[]> boxes=new ArrayList<>();
 private final List<String> boxViews=new ArrayList<>();
 private final List<float[]> infoBoxes=new ArrayList<>();
 private final List<String[]> infoText=new ArrayList<>();
 public UimsdHomePane(){super(new UimsdContent());}
 public void setListener(Listener l){listener=l;}
 public void setHome(UimsdHome h){home=h;builtWidth=-1;layout();}
 @Override protected void layout(){super.layout();if(home!=null&&width>0&&builtWidth!=width){rebuild();builtWidth=width;}}
 public float[] linkBox(String id){for(int i=0;i<boxes.size();i++)if(boxViews.get(i).equals(id))return boxes.get(i);return null;}
 public float contentHeight(){return content.height();}
 private boolean inside(float[] b,float x,float y){return x>=b[0]&&x<=b[0]+b[2]&&y>=b[1]&&y<=b[1]+b[3];}
 @Override public void onClick(float x,float y){
  for(int i=0;i<infoBoxes.size();i++)if(inside(infoBoxes.get(i),x,y)){
   String[] text=infoText.get(i);ShatteredPixelDungeon.scene().addToFront(new WndUimsdText(text[0],text[1],false));return;
  }
  if(listener!=null)for(int i=0;i<boxes.size();i++)if(inside(boxes.get(i),x,y)){listener.onLink(boxViews.get(i));return;}
 }
 private RenderedTextBlock text(String value,int size,int color,float max,float x,float y){
  RenderedTextBlock t=PixelScene.renderTextBlock(size);
  int limit=Math.max(1,(int)max);
  // Set wrapping before text: ordinary labels and values need one build,
  // not an unbounded build followed by a second build for maxWidth.
  t.setHightlighting(false);t.text(value,limit);
  if(t.width()>limit){
   final RenderedTextBlock probe=PixelScene.renderTextBlock(size);probe.setHightlighting(false);
   try {value=com.shatteredpixel.shatteredpixeldungeon.apd.query.LongWordWrap.wrap(value,Math.max(1,(int)max),part->{probe.text(part);return probe.width();});}
   finally {probe.destroy();}
   t.text(value,limit);
  }
  t.hardlight(color);t.setPos(x,y);content.add(t);return t;
 }
 private void link(float x,float y,float w,float h,String id){boxes.add(new float[]{x,y,w,h});boxViews.add(id);}
 private void info(float x,float y,float w,float h,String title,String value){infoBoxes.add(new float[]{x,y,w,h});infoText.add(new String[]{title,value});}
 private Image icon(String id){
  switch(id){
   case "gear":return ChangeIcons.V061_INVENTORY.get();
   case "enemies":return ChangeIcons.PD_RAT.get();
   case "characters":return ChangeIcons.V035_WARRIOR_CLOTH.get();
   case "ask":return Icons.INFO.get();
   case "navigate":return Icons.JOURNAL.get();
   case "difficulty":return Icons.CHALLENGE_GREY.get();
   case "recent":return Icons.CALENDAR.get();
   case "records":return Icons.BADGES.get();
   default:return Icons.RANKINGS.get();
  }
 }
 private void rebuild(){
  content.clear();boxes.clear();boxViews.clear();infoBoxes.clear();infoText.clear();
  float w=width,y=1;
  for(UimsdHome.Card card:home.cards){
   Image sprite=icon(card.id);sprite.x=2;sprite.y=y;content.add(sprite);
   RenderedTextBlock title=text(card.title,7,UimsdStyle.TITLE,w-24,22,y+2);
   y+=Math.max(sprite.height(),title.height()+4)+2;
   for(UimsdHome.Line line:card.lines){
    if(card.id.equals("ask") && line.heading)continue;
    if(line.heading){RenderedTextBlock section=text(line.label,5,UimsdStyle.MUTED,w-4,2,y);y=section.bottom()+2;continue;}
    if(card.id.equals("ask")){
     StringBuilder help=new StringBuilder();
     for(UimsdHome.Line note:card.lines)if(note.heading)help.append(note.label).append("\n\n");
     help.append(line.detail);info(0,Math.max(0,y-20),w,18,card.title,help.toString());continue;
    }
    String outcome=card.id.equals("runs")?(line.label.equals("Won")?"1":line.label.equals("Died")?"0":line.label.equals("Abandoned")?"2":null):null;
    float detailW=outcome!=null&&!line.detail.isEmpty()?18:0;
    float labelW=(w-6)*0.45f,valueW=w-6-labelW-2-detailW;
    RenderedTextBlock label=text(line.label,6,UimsdStyle.TEXT,labelW,2,y);
    RenderedTextBlock value=text(line.value,7,UimsdTable.color(line.state),valueW,4+labelW,y);
    //Numeric values align at the right edge; long compound values retain wrapping.
    value.setPos(w-2-detailW-value.width(),y);
    float rowH=Math.max(16,Math.max(label.height(),value.height())+3);
    if(outcome!=null){
     link(0,y,w-detailW,rowH,"!outcome:"+outcome);
     if(detailW>0){text("?",6,UimsdStyle.MUTED,16,w-16,y+3);info(w-18,y,18,rowH,line.label,line.value+"\n\n"+line.detail);}
    }
    else if(!line.detail.isEmpty())info(0,y,w,rowH,line.label,line.value+"\n\n"+line.detail);
    y+=rowH;
   }
   for(UimsdHome.Link l:card.links){
    NinePatch button=Chrome.get(Chrome.Type.GREY_BUTTON_TR);
    RenderedTextBlock label=text(l.label,6,UimsdStyle.TEXT,w-10,5,y+4);
    float rowH=Math.max(18,label.height()+8);button.size(w,rowH);button.x=0;button.y=y;
    content.addToBack(button);link(0,y,w,rowH,l.viewId);y+=rowH+2;
   }
   ColorBlock divider=new ColorBlock(w,1,0xFF333333);divider.y=y;content.add(divider);y+=4;
  }
  content.setSize(w,y);
 }
}


