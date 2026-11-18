package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

/** A short visual marker for an already known tile; separate from combat warning cells. */
public final class StairHighlight extends Image {
    public final int cell;
    private float remaining=1.5f;
    public static boolean known(int cell){return Dungeon.level!=null && cell>=0 && cell<Dungeon.level.length()
            && (Dungeon.level.visited[cell] || Dungeon.level.mapped[cell]);}
    public static void showKnown(int cell){if(known(cell) && Game.scene() instanceof GameScene)GameScene.effectOverFog(new StairHighlight(cell));}
    public StairHighlight(int cell){
        super(Icons.get(Icons.TARGET));this.cell=cell;
        point(DungeonTilemap.tileToWorld(cell));hardlight(0xFFE080);
    }
    @Override public void update(){super.update();remaining-=Game.elapsed;if(remaining<=0)killAndErase();else alpha(Math.min(1f,remaining));}
}
