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
import org.junit.Test;
import static org.junit.Assert.*;
public class DescriptionViewportTest {
 @Test public void integerCameraCannotClipTheLastLineOrItsBottomPadding(){
  for(float height:new float[]{0,5,24,24.1f,39.5f,61.9f,120,300.2f}){
   int content=MechanicsInfoPanel.paddedHeight(height);
   assertTrue(content>=height+2);
   assertEquals(0,(float)content%1,0);
   for(int viewport:new int[]{24,36,64,120}){
    float scroll=Math.max(0,content-viewport);
    assertTrue("Last glyph fits after scrolling to bottom",height-scroll<=viewport-2);
   }
  }
 }
}
