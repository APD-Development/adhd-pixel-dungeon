package com.shatteredpixel.shatteredpixeldungeon.apd.trophy;

import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

/** A one-pixel colored outline that follows an image every frame (works for animated sprites). */
public class Silhouette extends Group {

	private static final int[][] OFFSETS = {{-1,0},{1,0},{0,-1},{0,1}};

	private final Image target;
	private final Image[] copies = new Image[OFFSETS.length];
	private final float r, g, b;

	public Silhouette(Image target, int color){
		this.target = target;
		r = ((color >> 16) & 0xFF) / 255f;
		g = ((color >> 8) & 0xFF) / 255f;
		b = (color & 0xFF) / 255f;
		for (int i = 0; i < copies.length; i++){
			copies[i] = new Image();
			add(copies[i]);
		}
		sync();
	}

	@Override
	public void update(){
		super.update();
		if (target == null || target.parent == null || !target.alive){
			killAndErase();
			return;
		}
		sync();
	}

	private void sync(){
		if (target == null || target.texture == null) { visible = false; return; }
		visible = target.visible && com.shatteredpixel.shatteredpixeldungeon.SPDSettings.trophyOutlines();
		float px = target.scale.x;
		for (int i = 0; i < copies.length; i++){
			Image c = copies[i];
			c.flipHorizontal = target.flipHorizontal;
			c.flipVertical = target.flipVertical;
			c.copy(target);
			c.rm = c.gm = c.bm = 0f;
			c.ra = r; c.ga = g; c.ba = b;
			c.am = target.am; c.aa = target.aa;
			c.origin.set(target.origin);
			c.angle = target.angle;
			c.x = target.x + OFFSETS[i][0] * px;
			c.y = target.y + OFFSETS[i][1] * target.scale.y;
		}
	}
}
