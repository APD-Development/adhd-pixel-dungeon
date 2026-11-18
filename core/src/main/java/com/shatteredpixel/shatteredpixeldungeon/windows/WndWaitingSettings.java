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
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;

/** Independent saved interruption preferences; no difficulty/scoring or auto-eating side effects. */
public class WndWaitingSettings extends Window {
    public WndWaitingSettings(){
        int width=Math.max(92,Math.min(170,(int)PixelScene.uiCamera.width-16));
        RenderedTextBlock title=PixelScene.renderTextBlock("Waiting interruptions",9);title.hardlight(TITLE_COLOR);title.maxWidth(width);add(title);
        com.watabou.noosa.ui.Component content=new com.watabou.noosa.ui.Component();ScrollPane scroll=new ScrollPane(content);add(scroll);
        RenderedTextBlock description=PixelScene.renderTextBlock("Choose when continuous waiting stops. Enemy sightings, other damage and manual cancellation still work normally.",6);description.maxWidth(width);content.add(description);
        CheckBox full=new CheckBox("Stop at full health"){@Override protected void onClick(){super.onClick();SPDSettings.waitStopFullHealth(checked());}};
        CheckBox starving=new CheckBox("Stop on becoming starving"){@Override protected void onClick(){super.onClick();SPDSettings.waitStopStarving(checked());}};
        CheckBox critical=new CheckBox("Stop at critical health"){@Override protected void onClick(){super.onClick();SPDSettings.waitStopCritical(checked());}};
        full.checked(SPDSettings.waitStopFullHealth());starving.checked(SPDSettings.waitStopStarving());critical.checked(SPDSettings.waitStopCritical());
        float y=description.bottom()+4;
        for(CheckBox check:new CheckBox[]{full,starving,critical}){check.setRect(0,y,width,18);content.add(check);y=check.bottom()+2;}
        RedButton threshold=new RedButton("Critical HP: "+SPDSettings.waitCriticalPercent()+"%"){
            @Override protected void onClick(){
                final RedButton button=this;
                com.watabou.noosa.Game.scene().addToFront(new WndOptions("Critical health","Stops continuous waiting at or below this HP percentage when enabled.","10%","20%","30%","40%","50%"){
                    @Override protected void onSelect(int index){SPDSettings.waitCriticalPercent((index+1)*10);button.text("Critical HP: "+SPDSettings.waitCriticalPercent()+"%");}
                });
            }
        };
        threshold.setRect(0,y,width,20);content.add(threshold);y=threshold.bottom()+3;
        RedButton none=new RedButton("Disable these three stops"){
            @Override protected void onClick(){SPDSettings.waitStopFullHealth(false);SPDSettings.waitStopStarving(false);SPDSettings.waitStopCritical(false);full.checked(false);starving.checked(false);critical.checked(false);}
        };
        none.setRect(0,y,width,20);content.add(none);y=none.bottom()+3;
        RenderedTextBlock caution=PixelScene.renderTextBlock("With these stops disabled, waiting can continue through starvation and dangerously low HP. Auto-Eat is configured separately.",6);caution.maxWidth(width);caution.setPos(0,y);content.add(caution);content.setSize(width,caution.bottom());
        float top=title.bottom()+4;float max=Math.max(36,com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry.maxPane(PixelScene.uiCamera.height,0,0)-top);
        float height=com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry.paneHeight(content.height(),max);
        resize(width,(int)Math.ceil(top+height));scroll.setRect(0,top,width,height);scroll.allowDragFromButtons();
    }
}
