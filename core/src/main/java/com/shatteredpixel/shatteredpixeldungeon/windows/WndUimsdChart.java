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

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.ui.Component;
import java.util.ArrayList;
import java.util.List;

/** Charts only the authorized grouped result, retaining its order, scope and knowledge marks. */
public class WndUimsdChart extends Window {
    private final UimsdModel model;
    private final QueryResult source;
    private final UimsdChart.Series series;
    private final ScrollPane pane;
    private final List<float[]> spans=new ArrayList<>();

    public static void show(UimsdModel model){
        if(model.chartMeasures().isEmpty()){
            ShatteredPixelDungeon.scene().addToFront(new WndUimsdText("Charts",
                    "Open Options > Group by to compare groups, such as runs per class. Then choose Charts. Your filters and rules still apply."));
        }else ShatteredPixelDungeon.scene().addToFront(new WndUimsdChart(model,UimsdChart.COUNT));
    }

    public WndUimsdChart(UimsdModel model,String measure){
        this.model=model;
        source=model.result();
        UimsdChart.Series selected=UimsdChart.build(source,measure,UimsdChart.MAX_POINTS);
        series=selected==null ? UimsdChart.build(source,UimsdChart.COUNT,UimsdChart.MAX_POINTS) : selected;
        int width=WndUimsdList.windowWidth();
        RenderedTextBlock title=text(this,"Chart",9,UimsdStyle.TITLE,2,2,width-4);
        float pos=title.bottom()+3;
        StyledButton measures=new StyledButton(Chrome.Type.GREY_BUTTON_TR,
                "Choose measure...",7){
            @Override protected void onClick(){pickMeasure();}
        };
        measures.setRect(2,pos,width-4,20);add(measures);pos+=23;
        Component content=new Component();
        float y=2;
        if(series!=null) y=text(content,series.measure.label,8,UimsdStyle.HEADING,4,y,width-8).bottom()+4;
        if(series==null || series.points.isEmpty()){
            y=text(content,"No matching groups. Close this chart to adjust the table filters or grouping.",7,UimsdStyle.MUTED,4,y,width-8).bottom()+4;
        }else{
            y=text(content,"Same filters and group order as the table. Tap a group for details. Bars start at zero; ? has no bar. Marks keep their table meaning.",6,UimsdStyle.MUTED,4,y,width-8).bottom()+5;
            for(UimsdChart.Point point:series.points){
                float top=y;
                y=text(content,point.label,7,UimsdTable.color(point.labelState),4,y,width-8).bottom()+2;
                int color=UimsdTable.color(point.state);
                if(point.small) color=UimsdTable.dim(color);
                y=text(content,point.shown+(point.small ? "  (small sample)" : ""),7,color,4,y,width-8).bottom()+2;
                float barWidth=width-10;
                ColorBlock track=new ColorBlock(barWidth,5,UimsdStyle.RULE);track.x=4;track.y=y;content.add(track);
                ChartBar bar=point.known ? ChartBar.of(series.min,series.max,point.value) : null;
                if(bar!=null){
                    if(bar.end>bar.start){
                        ColorBlock fill=new ColorBlock((float)((bar.end-bar.start)*barWidth),5,0xFF000000|color);
                        fill.x=4+(float)(bar.start*barWidth);fill.y=y;content.add(fill);
                    }
                    ColorBlock zero=new ColorBlock(1,7,0xFF999999);
                    zero.x=4+(float)(bar.zero*(barWidth-1));zero.y=y-1;content.add(zero);
                }
                y+=9;
                y=text(content,point.detail,6,UimsdStyle.FAINT,4,y,width-8).bottom()+7;
                spans.add(new float[]{top,y-top});
            }
            if(series.omitted>0) y=text(content,"Showing the first "+series.points.size()+" of "+series.sourceGroups+" groups in table order. "+series.omitted+" more are in the table; no combined Other group is created.",6,UimsdStyle.WARN,4,y,width-8).bottom()+4;
        }
        content.setSize(width,y);
        pane=new ScrollPane(content){
            @Override public void onClick(float x,float y){
                if(x<2 || x>=width-2 || series==null)return;
                for(int i=0;i<spans.size();i++){
                    float[] span=spans.get(i);
                    if(y>=span[0] && y<span[0]+span[1]){
                        // Never apply a stale group's identity to a changed query.
                        if(model.result()!=source){hide();show(model);return;}
                        ResultTable table=ResultTable.build(source,0,UimsdChart.MAX_POINTS);
                        hide();WndUimsdRow.show(model,table.lines.get(i));return;
                    }
                }
            }
        };
        float h=WindowGeometry.paneHeight(y,WindowGeometry.maxPane(PixelScene.uiCamera.height,pos,2));
        resize(width,(int)(pos+h+2));add(pane);pane.setRect(0,pos,width,h);
    }

    private static RenderedTextBlock text(Group into,String value,int size,int color,float x,float y,int width){
        RenderedTextBlock t=PixelScene.renderTextBlock(value,size);t.setHightlighting(false);t.hardlight(color);
        t.maxWidth(width);t.setPos(x,y);into.add(t);return t;
    }

    private void pickMeasure(){
        List<WndUimsdList.Entry> entries=new ArrayList<>();
        final List<UimsdChart.Measure> choices=model.chartMeasures();
        for(UimsdChart.Measure measure:choices) entries.add(WndUimsdList.Entry.item(measure.label).current(series!=null && measure.id.equals(series.measure.id)));
        hide();
        ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu("Chart measure","Uses only the aggregates available in this table.",entries,
                (i,checked)->ShatteredPixelDungeon.scene().addToFront(new WndUimsdChart(model,choices.get(i).id)))
                .restoreWith(()->{
                    WndUimsdChart restored=new WndUimsdChart(model,series==null ? UimsdChart.COUNT : series.measure.id);
                    ShatteredPixelDungeon.scene().addToFront(restored);restored.pickMeasure();
                }));
    }

    @Override public Runnable restoreOnSceneReset(){
        String measure=series==null ? UimsdChart.COUNT : series.measure.id;
        float y=pane.content().camera().scroll.y;
        return ()->{WndUimsdChart restored=new WndUimsdChart(model,measure);
            restored.pane.scrollTo(0,y);ShatteredPixelDungeon.scene().addToFront(restored);};
    }
}
