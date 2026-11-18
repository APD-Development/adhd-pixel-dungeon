package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.qol.HeapExamination;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.WindowGeometry;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/** Open visible pile inspection. Labels, icons and individual item info use normal SPD identification. */
public final class WndInfoPile extends Window {
    private static final int MARGIN = 2;
    private static final float GAP = 2f;
    private static final float MIN_ROW = 25f;

    private final List<Item> items;
    private final ScrollPane scroll;
    private final float[] tops;
    private final int width;

    public WndInfoPile(Heap heap) {
        this(HeapExamination.snapshot(heap));
    }

    private WndInfoPile(List<Item> snapshot) {
        super();
        items = new ArrayList<>(snapshot);
        width = WindowGeometry.width(PixelScene.landscape(), PixelScene.uiCamera.width);
        RenderedTextBlock title = PixelScene.renderTextBlock("Items on tile (" + items.size() + ")", 9);
        title.hardlight(TITLE_COLOR);
        title.maxWidth(width - 2*MARGIN);
        title.setPos(MARGIN, MARGIN);
        add(title);
        float y = title.bottom() + 2*GAP;

        // Large heaps use one clipped, scrollable pane. Sprites are the SAME appearance
        // shown by the regular item-info window, so unidentified items stay unidentified.
        Component body = new Component();
        tops = new float[items.size()+1];
        float contentY=0f;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            RenderedTextBlock name = PixelScene.renderTextBlock(HeapExamination.visibleLabel(item),7);
            name.maxWidth(Math.max(1,width-25));
            float h=Math.max(MIN_ROW,name.height()+2*GAP+4f);
            tops[i]=contentY;
            NinePatch backdrop = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
            backdrop.size(width - 2*MARGIN,h-1);
            backdrop.x=MARGIN;
            backdrop.y=contentY;
            body.add(backdrop);
            ItemSprite sprite = new ItemSprite(item);
            sprite.x=MARGIN+3f;
            sprite.y=contentY+(h-sprite.height())/2f;
            body.add(sprite);
            name.setPos(MARGIN+22f,contentY+(h-name.height())/2f);
            body.add(name);
            contentY+=h+GAP;
        }
        tops[items.size()]=contentY;
        body.setSize(width,contentY);

        scroll = new ScrollPane(body) {
            @Override public void onClick(float x, float y) {
                if (x < MARGIN || x >= width - MARGIN) return;
                int index=WindowGeometry.rowAt(tops,y);
                Item selected=HeapExamination.select(items,index);
                if (selected != null) {
                    ShatteredPixelDungeon.scene().addToFront(new WndInfoItem(selected));
                }
            }
        };
        // Any item description opens above the list. Closing it returns to the
        // same scroll position without recreating or touching the dungeon.
        float height=WindowGeometry.paneHeight(contentY,
                WindowGeometry.maxPane(PixelScene.uiCamera.height,y,0));
        resize(width,(int)Math.ceil(y+height));
        add(scroll);
        scroll.setRect(0,y,width,height);
    }

    @Override public Runnable restoreOnSceneReset() {
        final List<Item> saved=new ArrayList<>(items);
        final float scrollY=scroll.content().camera.scroll.y;
        return () -> {
            WndInfoPile restored=new WndInfoPile(saved);
            restored.scroll.scrollTo(0,scrollY);
            ShatteredPixelDungeon.scene().addToFront(restored);
        };
    }
}
