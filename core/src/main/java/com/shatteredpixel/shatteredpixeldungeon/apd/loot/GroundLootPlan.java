package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import java.util.*;
import java.util.function.Predicate;

/**
 * Read-only capture for the gathering scroll pair.
 * No pickup, drop, merging, RNG, identification, terrain or recorder callbacks.
 * A persistence transaction must validate this snapshot before removing items.
 */
public final class GroundLootPlan {
    public static final class Source {
        public final int cell;
        public final Heap heap;
        public final List<Item> items;
        private final int[] quantities;
        private Source(Heap heap,List<Item> items) {
            this.heap=heap;this.cell=heap.pos;
            this.items=Collections.unmodifiableList(items);
            quantities=new int[items.size()];
            for(int i=0;i<items.size();i++)quantities[i]=items.get(i).quantity();
        }
    }
    public final List<Source> sources;
    private final Level level;
    private final Predicate<Item> eligible;
    private GroundLootPlan(Level level,Predicate<Item> eligible,List<Source> sources) {
        this.level=level;this.eligible=eligible;
        this.sources=Collections.unmodifiableList(sources);
    }
    public static GroundLootPlan capture(Level level,Predicate<Item> eligible) {
        Objects.requireNonNull(level);Objects.requireNonNull(eligible);
        List<Heap> heaps=new ArrayList<>(level.heaps.valueList());
        heaps.sort(Comparator.comparingInt(h->h.pos));
        List<Source> sources=new ArrayList<>();
        Set<Item> seen=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        for(Heap heap:heaps) {
            List<Item> items=new ArrayList<>();
            for(Item item:heap.items) {
                if(item==null || item.quantity()<=0 || !seen.add(item))
                    throw new IllegalStateException("Invalid or aliased ground item");
                if(heap.type==Heap.Type.HEAP && eligible.test(item))items.add(item);
            }
            if(!items.isEmpty())sources.add(new Source(heap,items));
        }
        return new GroundLootPlan(level,eligible,sources);
    }
    public int stacks() {
        int count=0;for(Source source:sources)count+=source.items.size();return count;
    }
    /** Recheck immediately before committing; a changed source must not be moved. */
    public boolean stillValid() {
        for(Source source:sources) {
            Heap current=level.heaps.get(source.cell);
            if(current!=source.heap || current.pos!=source.cell || current.type!=Heap.Type.HEAP)return false;
            for(int i=0;i<source.items.size();i++) {
                Item item=source.items.get(i);
                boolean identical=false;for(Item currentItem:current.items)if(currentItem==item){identical=true;break;}
                if(!identical || item.quantity()!=source.quantities[i] || !eligible.test(item))return false;
            }
        }
        return true;
    }

    boolean belongsTo(Level candidate) { return level == candidate; }
}

