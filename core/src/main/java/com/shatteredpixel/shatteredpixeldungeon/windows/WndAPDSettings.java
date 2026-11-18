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

import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

//APD: the shared APD Settings window reached from the title screen and in-game menu.
public class WndAPDSettings extends Window {

    private final Component content;
    private final ScrollPane scroll;
    private final APDSettingsPane pane;
    private final int paneWidth;
    private final java.util.Map<com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection,Float> positions=new java.util.HashMap<>();
    public WndAPDSettings(){this(null);}
    public WndAPDSettings(com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection section){
        super();
        paneWidth=Math.max(72,Math.min(180,(int)PixelScene.uiCamera.width-16));
        content=new Component();
        // Keep the scrolling area's pointer listener older than every control.
        scroll=new ScrollPane(content);
        pane=new APDSettingsPane();content.add(pane);pane.showSection(section);
        pane.navigation(previous->{
            positions.put(previous,scroll.content().camera().scroll.y);
            reflow();Float saved=positions.get(pane.section());scroll.scrollTo(0,saved==null?0:saved);
        });
        // ScrollPane.layout needs the window camera; attach it before the first reflow.
        add(scroll);reflow();scroll.allowDragFromButtons();
    }
    private void reflow(){
        pane.setSize(paneWidth,0);
        float contentHeight=(float)Math.ceil(pane.height());content.setSize(paneWidth,contentHeight);
        float height=WindowGeometry.paneHeight(contentHeight,WindowGeometry.maxPane(PixelScene.uiCamera.height,0,0));
        resize(paneWidth,(int)height);scroll.setRect(0,0,paneWidth,height);
    }
    @Override public void onBackPressed(){if(pane.section()!=null)pane.showSection(null);else super.onBackPressed();}
    @Override public Runnable restoreOnSceneReset(){
        com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection section=pane.section();
        float offset=scroll.content().camera().scroll.y;
        return ()->{WndAPDSettings restored=new WndAPDSettings(section);restored.scroll.scrollTo(0,offset);
            com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene().addToFront(restored);};
    }
}
