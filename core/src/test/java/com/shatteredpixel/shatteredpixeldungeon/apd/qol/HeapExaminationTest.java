package com.shatteredpixel.shatteredpixeldungeon.apd.qol;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Piles are inspected without picking up, reordering, identifying or simulating turns. */
public class HeapExaminationTest {
    @Before public void init() { HeadlessTestSupport.start(); }

    private static Heap pile(Item... items) {
        Heap h=new Heap();
        h.type=Heap.Type.HEAP;
        h.seen=true;
        for(Item item:items)h.items.add(item);
        return h;
    }

    @Test public void twoItemPileIsTopFirstAndEachReferenceIsSelectable() {
        Item top=new Food(), second=new Food();
        Heap h=pile(top,second);
        List<Item> items=HeapExamination.snapshot(h);
        assertEquals(2,items.size());
        assertSame(h.peek(),items.get(0));
        assertSame(top,HeapExamination.select(items,0));
        assertSame(second,HeapExamination.select(items,1));
        assertNull(HeapExamination.select(items,-1));
        assertNull(HeapExamination.select(items,2));
    }

    @Test public void onlyOpenVisiblePilesWithMoreThanOneItemQualify() {
        Heap h=pile(new Food());
        assertFalse(HeapExamination.hasVisiblePile(h));
        assertTrue(HeapExamination.snapshot(h).isEmpty());
        h.items.add(new Food());
        assertTrue(HeapExamination.hasVisiblePile(h));
        h.seen=false;
        assertFalse(HeapExamination.hasVisiblePile(h));
        h.seen=true;
        for(Heap.Type type:new Heap.Type[]{Heap.Type.CHEST,Heap.Type.LOCKED_CHEST,
                Heap.Type.CRYSTAL_CHEST,Heap.Type.TOMB,Heap.Type.SKELETON,Heap.Type.REMAINS}) {
            h.type=type;
            assertFalse("Do not expose sealed container: "+type,HeapExamination.hasVisiblePile(h));
        }
        h.type=Heap.Type.FOR_SALE;
        assertTrue(HeapExamination.hasVisiblePile(h));
    }

    @Test public void quantitiesAreVisibleButNothingIsMergedOrConsumed() {
        Item stack=new Food().quantity(9),single=new Food();
        Heap h=pile(stack,single);
        assertTrue(HeapExamination.visibleLabel(stack).endsWith("x9"));
        assertFalse(HeapExamination.visibleLabel(single).endsWith("x1"));
        List<Item> list=HeapExamination.snapshot(h);
        assertEquals(2,h.size());
        assertEquals(9,list.get(0).quantity());
        assertEquals(1,list.get(1).quantity());
    }

    @Test public void unknownPotionAndScrollLabelsNeverUseHiddenTrueNames() {
        PotionOfHealing potion=new PotionOfHealing(){
            @Override public boolean isKnown(){return false;}
            @Override public String name(){return "amber potion";}
        };
        ScrollOfUpgrade scroll=new ScrollOfUpgrade(){
            @Override public boolean isKnown(){return false;}
            @Override public String name(){return "KAUNAN scroll";}
        };
        List<Item> items=HeapExamination.snapshot(pile(potion,scroll));
        String a=HeapExamination.visibleLabel(items.get(0)).toLowerCase();
        String b=HeapExamination.visibleLabel(items.get(1)).toLowerCase();
        assertTrue(a.contains("amber"));
        assertTrue(b.contains("kaunan"));
        assertFalse(a.contains("healing"));
        assertFalse(b.contains("upgrade"));
        assertFalse(potion.isKnown());
        assertFalse(scroll.isKnown());
    }

    @Test public void threeHundredItemsMaintainStableOrderAndSnapshot() {
        Heap heap=pile(new Food());
        ArrayList<Item> originals=new ArrayList<>(heap.items);
        for(int i=0;i<300;i++) {
            Item item=new Food().quantity(i+1);
            heap.items.add(item);
            originals.add(item);
        }
        List<Item> snapshot=HeapExamination.snapshot(heap);
        assertEquals(301,snapshot.size());
        for(int i=0;i<301;i++)assertSame(originals.get(i),snapshot.get(i));
        heap.items.removeLast();
        assertEquals("Snapshot is not a mutable view of live heap",301,snapshot.size());
    }

    @Test public void inspectionDoesNotChangeGameplayState() {
        Item first=new Food().quantity(3),second=new Food().quantity(5);
        first.cursed=true;
        first.cursedKnown=false;
        first.levelKnown=false;
        Heap h=pile(first,second);
        List<Item> entries=HeapExamination.snapshot(h);
        for(int i=0;i<entries.size();i++) {
            assertNotNull(HeapExamination.visibleLabel(HeapExamination.select(entries,i)));
        }
        assertSame(first,h.peek());
        assertEquals(2,h.size());
        assertEquals(3,first.quantity());
        assertEquals(5,second.quantity());
        assertTrue(first.cursed);
        assertFalse(first.cursedKnown);
        assertFalse(first.levelKnown);
    }
}
