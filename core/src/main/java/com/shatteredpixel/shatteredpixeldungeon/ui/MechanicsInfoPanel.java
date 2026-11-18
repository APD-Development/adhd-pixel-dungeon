/*
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
package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import java.util.function.Supplier;

/** Bounded description plus opt-in calculations, without moving the owning window's action buttons. */
public final class MechanicsInfoPanel {
    private MechanicsInfoPanel(){}
    // ScrollPane cameras use integer pixel heights; leave room below the last glyph.
    static int paddedHeight(float textHeight){return (int)Math.ceil(textHeight)+2;}
    public static float add(Window window,RenderedTextBlock body,float x,float top,int width,float reserved,Supplier<String> details){
        String plain=body.text();String initial=details==null?"":details.get();boolean hasDetails=!initial.isEmpty();
        com.watabou.noosa.ui.Component content=new com.watabou.noosa.ui.Component();
        body.setPos(0,0);body.maxWidth(width);content.add(body);content.setSize(width,paddedHeight(body.height()));
        ScrollPane scroll=new DescriptionScrollPane(content);window.add(scroll);
        float max=Math.max(36,com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry.maxPane(PixelScene.uiCamera.height,0,0)-top-reserved-(hasDetails?20:0));
        float height=Math.max(24,Math.min(Math.max(content.height(),hasDetails?96:0),(float)Math.floor(max)));scroll.setRect(x,top,width,height);scroll.allowDragFromButtons();
        float bottom=top+height;
        if(hasDetails){
            final int textWidth=width;
            RedButton expand=new RedButton("Show calculations",6){
                boolean expanded;
                @Override protected void onClick(){expanded=!expanded;text(expanded?"Hide calculations":"Show calculations");
                    body.text(expanded?plain+"\n\n_Calculations and explanation_\n"+details.get():plain,textWidth);
                    content.setSize(textWidth,paddedHeight(body.height()));scroll.scrollTo(0,0);
                }
            };
            expand.setRect(x,bottom+2,width,16);window.add(expand);bottom=expand.bottom();
        }
        return bottom;
    }
    /** Window.resize/offset move its camera after this pane was laid out. */
    private static final class DescriptionScrollPane extends ScrollPane {
        DescriptionScrollPane(com.watabou.noosa.ui.Component content){super(content);}
        private void alignViewport(){
            com.watabou.noosa.Camera parentCamera=camera();
            com.watabou.utils.Point origin=parentCamera.cameraToScreen(left(),top());
            if(content.camera.x!=origin.x||content.camera.y!=origin.y||content.camera.zoom!=parentCamera.zoom){
                content.camera.zoom=parentCamera.zoom;
                layout();
            }
        }
        @Override public synchronized void update(){alignViewport();super.update();}
        @Override public void draw(){alignViewport();super.draw();}
    }
}
