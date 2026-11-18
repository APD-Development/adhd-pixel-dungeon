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


package com.shatteredpixel.shatteredpixeldungeon.apd;

//Title screen arithmetic with no game or graphics classes in it, so it can be tested on any screen size.
//TitleScene uses these numbers for the vertical layout of the button grid and for the UIMSD annotation arrow.
public final class TitleGeometry {

	private TitleGeometry(){}

    /** Keep background pixels uniform and cover the viewport independently of UI scale. */
    public static float backgroundCoverScale(float width,float height,float imageWidth,float imageHeight,float zoom){
        if(!(width>0&&height>0&&imageWidth>0&&imageHeight>0&&zoom>0)
                ||!Float.isFinite(width)||!Float.isFinite(height)||!Float.isFinite(imageWidth)||!Float.isFinite(imageHeight)||!Float.isFinite(zoom))throw new IllegalArgumentException("Invalid background dimensions");
        return (float)Math.ceil(Math.max(width*zoom/imageWidth,height*zoom/imageHeight))/zoom;
    }

	public static final int LOGO_W = 139;
	public static final int LOGO_H = 100;
	public static final int MAIN_GAP = 3;
	public static final int FADE_SIZE = 16, FOOTER_GAP = 4, FOOTER_H = FADE_SIZE + FOOTER_GAP;
	// Make the small footer icon easier to touch without moving the artwork.
	public static final int FADE_TOUCH_SIDE_PADDING = 8;
	public static final int FADE_TOUCH_TOP_PADDING = FOOTER_GAP;
	public static final int BOTTOM_H = 20;
	//the full-width APD Settings row sits between the 2x3 grid and the News/Changes/About row
	public static final int APD_ROW_H = BOTTOM_H + MAIN_GAP;

	//--- the annotation arrow: a 5 wide shaft, then a head that goes 15, 13, 11 ... 1 wide ---
	public static final int SHAFT_W = 5;
	public static final int SHAFT_H = 5;
	public static final int HEAD_ROWS = 8;
	public static final int HEAD_W = 15;
	public static final int ARROW_H = SHAFT_H + HEAD_ROWS;
	public static final int GLOW = 2;  //the glow reaches this many px past the 1px outline
	public static final int GAP = 3;   //between the tip's glow and the button
	public static final int BOB = 3;   //the arrow only bobs upwards, so it never reaches the button

	public interface TextWidth { float measure(String text); }

	//Measured by the actual title font in-game; tests can provide deterministic metrics.
	public static String[] annotationLines(String text, float maxWidth, TextWidth width){
		if (text == null || text.isEmpty()) return new String[]{""};
		if (width.measure(text) <= maxWidth) return new String[]{text};
		java.util.List<String> lines = new java.util.ArrayList<>();
		String line = "";
		for (String word : text.trim().split("\\s+")){
			String candidate = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && width.measure(candidate) > maxWidth){
				lines.add(line);
				line = "";
			}
			//Very long single tokens wrap too; normal instructional words stay whole.
			while (word.length() > 1 && width.measure(word) > maxWidth){
				int end = 1;
				while (end < word.length() && width.measure(word.substring(0, end + 1)) <= maxWidth) end++;
				lines.add(word.substring(0, end));
				word = word.substring(end);
			}
			line = line.isEmpty() ? word : line + " " + word;
		}
		lines.add(line);
		//Avoid stranding the final word when the previous line can lend it a word without overflowing.
		if (lines.size() > 1 && line.indexOf(' ') < 0){
			int previous = lines.size() - 2;
			String before = lines.get(previous);
			int split = before.lastIndexOf(' ');
			if (split > 0 && width.measure(before.substring(split + 1) + " " + line) <= maxWidth){
				lines.set(previous, before.substring(0, split));
				lines.set(previous + 1, before.substring(split + 1) + " " + line);
			}
		}
		return lines.toArray(new String[0]);
	}

	//height of each of the three main button rows, given the vertical space available for the whole grid
	public static float mainHeight( float availableForGrid ){
		float h = (availableForGrid - APD_ROW_H - BOTTOM_H - 3*MAIN_GAP) / 3f;
		return Math.max(24, Math.min(36, (float)Math.floor(h)));
	}

	//the main rows keep their normal height when the screen has room for the annotation, otherwise they give up the space it needs
	public static float mainHeightWith( float availableForGrid, float annotationH ){
		float normal = mainHeight( availableForGrid );
		float used = 3*normal + 3*MAIN_GAP + APD_ROW_H + BOTTOM_H;
		if (availableForGrid - used >= annotationH) return normal;
		return mainHeight( availableForGrid - annotationH );
	}

	//the annotation strip: text, the arrow with its outline and glow, the gap above the button, and room for the bob
	public static float annotationHeight( float textHeight ){
		if (textHeight < 0) return 0; //no progression prompt: no arrow or reserved strip
		return (float)Math.ceil( textHeight + 2 + (2 + ARROW_H + 2*GLOW) + GAP + BOB );
	}

	public static float gridHeight( float annotationH, float mainH ){
		return annotationH + 3*mainH + 3*MAIN_GAP + APD_ROW_H + BOTTOM_H;
	}

	//column width of the 2x3 grid, and where the right column starts
	public static float colWidth( float gridWidth ){
		return (float)Math.floor((gridWidth - MAIN_GAP) / 2f);
	}

	//the vertical layout result for one orientation (x positions of the logo are not needed here)
	public static final class Layout {
		public float logoX, logoY, logoScale = 1, gridLeft, gridWidth, gridTop, mainH, annotationH;
		public float fadeX, fadeY, annotationLeft, annotationTop;
		public boolean annotationBesideGrid, annotationCompact;
		//where the version text sits: just under the logo
		public float versionY;
	}

	//w/h: usable screen size, left/top: its insets. versionH: height of the version text. textH: annotation text height.
    public static Layout landscape(float left0,float top0,float w,float h,float versionH,float textH){
        Layout r=new Layout(); float usable=h-FOOTER_H, versionGap=versionH+2;
        r.gridWidth=Math.min(150,w-8);
        float logoColumn=Math.min(LOGO_W,Math.max(0,w-r.gridWidth-12));
        float left=left0+(w-logoColumn-8-r.gridWidth)/2f;
        r.annotationH=annotationHeight(textH);
        r.annotationBesideGrid=gridHeight(r.annotationH,24)>usable-8;
        if(r.annotationBesideGrid)r.annotationH=0;
        r.mainH=mainHeightWith(usable-8,r.annotationH);
        float gridH=gridHeight(r.annotationH,r.mainH);
        float noteH=r.annotationBesideGrid && textH>=0 ? textH+4:0;
        r.logoScale=Math.min(1,Math.min(logoColumn/LOGO_W,Math.max(0,usable-versionGap-noteH-8)/LOGO_H));
        r.logoX=left+(logoColumn-LOGO_W*r.logoScale)/2;
        r.logoY=top0+Math.max(0,(usable-LOGO_H*r.logoScale-versionGap-noteH)/2);
        r.gridLeft=left+logoColumn+8;
        r.gridTop=top0+(usable-gridH)/2+r.annotationH;
        r.versionY=r.logoY+LOGO_H*r.logoScale-1;
        r.annotationLeft=left; r.annotationTop=r.versionY+versionH+3;
        footer(r,left0,top0,w,h); return r;
    }
    public static Layout portrait(float left0,float top0,float w,float h,float versionH,float textH){
        Layout r=new Layout(); float usable=h-FOOTER_H, versionGap=versionH+2, topPad=6;
        r.gridWidth=Math.min(w-8,150); r.gridLeft=left0+(w-r.gridWidth)/2;
        r.annotationH=annotationHeight(textH);
        r.annotationCompact=gridHeight(r.annotationH,24)+versionGap+4+topPad>usable;
        if(r.annotationCompact)r.annotationH=textH<0?0:(float)Math.ceil(textH+2);
        r.mainH=mainHeightWith(usable-topPad-LOGO_H-versionGap-10,r.annotationH);
        float gridH=gridHeight(r.annotationH,r.mainH);
        r.logoScale=Math.min(1,Math.min((w-8)/LOGO_W,Math.max(0,usable-topPad-versionGap-4-gridH)/LOGO_H));
        float content=LOGO_H*r.logoScale+versionGap+4+gridH;
        r.logoY=top0+Math.max(topPad,(usable-content)*0.35f);
        r.logoX=left0+(w-LOGO_W*r.logoScale)/2;
        r.gridTop=r.logoY+LOGO_H*r.logoScale+versionGap+4+r.annotationH;
        r.versionY=r.logoY+LOGO_H*r.logoScale-1;
        footer(r,left0,top0,w,h); return r;
    }
    private static void footer(Layout r,float left,float top,float w,float h){
        r.fadeX=left+(w-FADE_SIZE)/2; r.fadeY=top+h-FADE_SIZE;
    }
}
