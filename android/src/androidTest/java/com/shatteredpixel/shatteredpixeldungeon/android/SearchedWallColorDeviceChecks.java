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
package com.shatteredpixel.shatteredpixeldungeon.android;

import static org.junit.Assert.*;
import android.os.SystemClock;
import android.view.*;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.SearchedWallAppearance;
import com.shatteredpixel.shatteredpixeldungeon.apd.settings.ApdSettingsSection;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.SearchedWallTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.*;
import com.watabou.noosa.Game;
import com.watabou.utils.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/** Actual native settings taps, live layer tint, and unchanged search/turn state. */
final class SearchedWallColorDeviceChecks {
    private static void waitFor(String message,Callable<Boolean> check)throws Exception{
        long end=SystemClock.uptimeMillis()+60000;
        while(SystemClock.uptimeMillis()<end){if(check.call())return;Thread.sleep(100);}fail(message);
    }
    private static void frames()throws Exception{
        long start=com.badlogic.gdx.Gdx.graphics.getFrameId();waitFor("render frames",()->com.badlogic.gdx.Gdx.graphics.getFrameId()>=start+4);
    }
    private static void tap(Point point)throws Exception{
        android.app.Instrumentation in=InstrumentationRegistry.getInstrumentation();long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point.x,point.y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);in.sendPointerSync(event);event.recycle();Thread.sleep(100);
        }frames();
    }
    static void chooseColorThroughSettingsAndVerifyLiveLayer()throws Exception{
        int previous=SPDSettings.searchedWallColor();AtomicReference<WndAPDSettings> parent=new AtomicReference<>();
        final Bundle memory=new Bundle();final float[] cooldown={0};
        try{
            TopScoresDeviceTest.render(()->{
                Dungeon.level.searchedWalls.store(memory);cooldown[0]=Dungeon.hero.cooldown();SPDSettings.searchedWallColor(SearchedWallAppearance.Color.WHITE.ordinal());
                WndAPDSettings w=new WndAPDSettings(ApdSettingsSection.VISIBILITY);GameScene.show(w);parent.set(w);
            });frames();AtomicReference<Point> at=new AtomicReference<>();
            TopScoresDeviceTest.render(()->{try{
                for(RedButton b:TopScoresDeviceTest.find(parent.get(),RedButton.class))if(b.text().startsWith("Searched-wall color:")){
                    at.set(b.camera().cameraToScreen(b.left()+b.width()/2,b.top()+b.height()/2));break;
                }assertNotNull("real color preference button",at.get());
            }catch(Exception e){throw new RuntimeException(e);}});tap(at.get());
            waitFor("palette opens",()->!TopScoresDeviceTest.find(Game.scene(),WndUimsdList.class).isEmpty());frames();at.set(null);
            TopScoresDeviceTest.render(()->{try{
                WndUimsdList picker=TopScoresDeviceTest.find(Game.scene(),WndUimsdList.class).get(0);
                for(RenderedTextBlock label:TopScoresDeviceTest.find(picker,RenderedTextBlock.class))if(label.text().equals("Magenta")){
                    at.set(label.camera().cameraToScreen(label.left()+label.width()/2,label.top()+label.height()/2));break;
                }assertNotNull("visible Magenta choice",at.get());
            }catch(Exception e){throw new RuntimeException(e);}});tap(at.get());
            waitFor("actual palette choice saved",()->SPDSettings.searchedWallColor()==SearchedWallAppearance.Color.MAGENTA.ordinal());
            TopScoresDeviceTest.render(()->{try{
                SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);int rgb=SearchedWallAppearance.Color.MAGENTA.rgb;
                assertEquals(((rgb>>16)&255)/255f,layer.rm,.00001f);assertEquals(((rgb>>8)&255)/255f,layer.gm,.00001f);assertEquals((rgb&255)/255f,layer.bm,.00001f);
                Bundle after=new Bundle();Dungeon.level.searchedWalls.store(after);assertEquals(memory.toString(),after.toString());
                assertEquals("appearance changes spend no turn",cooldown[0],Dungeon.hero.cooldown(),0f);parent.get().hide();parent.set(null);
            }catch(Exception e){throw new RuntimeException(e);}});frames();
            final int[] bounds=new int[4];
            TopScoresDeviceTest.render(()->{try{
                SearchedWallTilemap layer=TopScoresDeviceTest.find(Game.scene(),SearchedWallTilemap.class).get(0);
                int w=Dungeon.level.width(),p=Dungeon.hero.pos-w;
                Point a=layer.camera().cameraToScreen((p%w)*16,(p/w)*16),b=layer.camera().cameraToScreen((p%w+1)*16,(p/w+1)*16);
                bounds[0]=a.x;bounds[1]=a.y;bounds[2]=b.x;bounds[3]=b.y;
            }catch(Exception e){throw new RuntimeException(e);}});
            android.graphics.Bitmap pixels=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(pixels);
            try{
                int magenta=0;
                for(int y=Math.max(0,bounds[1]);y<Math.min(pixels.getHeight(),bounds[3]);y++)for(int x=Math.max(0,bounds[0]);x<Math.min(pixels.getWidth(),bounds[2]);x++){
                    int rgb=pixels.getPixel(x,y),r=android.graphics.Color.red(rgb),g=android.graphics.Color.green(rgb),b=android.graphics.Color.blue(rgb);
                    if(r>180&&g<150&&b>120)magenta++;
                }
                assertTrue("actual wall pixels must use Magenta, not a shader-ignored white glyph",magenta>0);
            }finally{pixels.recycle();}
            TopScoresDeviceTest.screenshot("qol-searched-wall-color.png");
        }finally{TopScoresDeviceTest.render(()->{if(parent.get()!=null)parent.get().hide();SPDSettings.searchedWallColor(previous);});}
    }
}
