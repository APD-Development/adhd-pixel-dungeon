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
package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/** Compact native settings: no extra full-width explanation or checkbox rows on the parent tab. */
public class WndGrassDewSettings extends Window {
    public WndGrassDewSettings() {
        super();
        int width=Math.max(92,Math.min(160,(int)PixelScene.uiCamera.width-16));
        RenderedTextBlock title=PixelScene.renderTextBlock(Messages.get(this,"title"),9);
        title.hardlight(TITLE_COLOR);title.maxWidth(width);add(title);
        com.watabou.noosa.ui.Component content=new com.watabou.noosa.ui.Component();
        com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane scroll=new com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane(content);
        add(scroll);
        RenderedTextBlock description=PixelScene.renderTextBlock(Messages.get(this,"desc"),6);
        description.maxWidth(width);content.add(description);
        final CheckBox withEnemies=new CheckBox(Messages.get(this,"with_enemies")){
            @Override protected void onClick(){super.onClick();SPDSettings.autoCollectDewWithEnemies(checked());}
        };
        Runnable updateEnemyControl=()->withEnemies.enable(SPDSettings.autoCollectGrassDew());
        CheckBox dew=new CheckBox(Messages.get(this,"enabled")){
            @Override protected void onClick(){super.onClick();SPDSettings.autoCollectGrassDew(checked());updateEnemyControl.run();}
        };
        dew.checked(SPDSettings.autoCollectGrassDew());
        withEnemies.checked(SPDSettings.autoCollectDewWithEnemies());updateEnemyControl.run();
        float y=description.bottom()+4;
        for(CheckBox control:new CheckBox[]{dew,withEnemies}){
            control.setRect(0,y,width,16);content.add(control);y=control.bottom()+2;
        }
        content.setSize(width,y-2);
        float top=title.bottom()+4;
        float max=Math.max(36,com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry.maxPane(PixelScene.uiCamera.height,0,0)-top);
        float height=com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry.paneHeight(content.height(),max);
        resize(width,(int)Math.ceil(top+height));scroll.setRect(0,top,width,height);scroll.allowDragFromButtons();
    }
}
