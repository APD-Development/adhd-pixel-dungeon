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
package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.backends.headless.HeadlessPreferences;
import com.watabou.utils.GameSettings;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;

public class SearchedWallAppearanceTest {
    @Rule public org.junit.rules.TemporaryFolder temporary=new org.junit.rules.TemporaryFolder();
    private Preferences previous;private File file;
    @Before public void setup()throws Exception{
        HeadlessTestSupport.start();Field f=GameSettings.class.getDeclaredField("prefs");f.setAccessible(true);previous=(Preferences)f.get(null);
        file=new File(temporary.getRoot(),"appearance.xml");GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
    }
    @After public void restore(){GameSettings.set(previous);}
    @Test public void existingPlayersKeepTheOriginalGreenByDefault(){assertEquals(0,SPDSettings.searchedWallColor());assertEquals(0x93DFAB,SearchedWallAppearance.color(0).rgb);}
    @Test public void everyColorIsSavedAndSurvivesPreferenceReload(){
        java.util.Set<Integer> colors=new java.util.HashSet<>();
        for(SearchedWallAppearance.Color color:SearchedWallAppearance.Color.values()){
            SPDSettings.searchedWallColor(color.ordinal());GameSettings.set(new HeadlessPreferences(new FileHandle(file)));
            assertEquals(color.ordinal(),SPDSettings.searchedWallColor());assertTrue(colors.add(color.rgb));assertTrue(color.rgb>0&&color.rgb<=0xFFFFFF);
        }
    }
    @Test public void invalidPreferencesSafelyFallBackToGreen(){
        for(int index:new int[]{-1,8,Integer.MAX_VALUE}){SPDSettings.searchedWallColor(index);assertEquals(0,SPDSettings.searchedWallColor());}
        GameSettings.put("apd_searched_wall_color",999);assertEquals(0,SPDSettings.searchedWallColor());
    }
    @Test public void markerAtlasIsNeutralSoAllPaletteColorsRenderFaithfully()throws Exception{
        java.nio.file.Path root=java.nio.file.Paths.get(System.getProperty("apd.game.src")).toAbsolutePath();
        while(root!=null&&!java.nio.file.Files.isRegularFile(root.resolve("core/src/main/assets/interfaces/apd_searched_wall.png")))root=root.getParent();
        assertNotNull("marker source asset exists",root);
        java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(root.resolve("core/src/main/assets/interfaces/apd_searched_wall.png").toFile());
        assertEquals(256,image.getWidth());assertEquals(16,image.getHeight());int white=0,outline=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int pixel=image.getRGB(x,y);if((pixel>>>24)==0)continue;
            int rgb=pixel&0xFFFFFF;assertTrue("neutral glyph/outline",rgb==0||rgb==0xFFFFFF);if(rgb==0)outline++;else white++;
        }
        assertTrue(white>0);assertTrue(outline>0);
    }
}
