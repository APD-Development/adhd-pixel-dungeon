package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WornDartTrap;
import com.watabou.utils.SparseArray;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class GroundLootMovementTest {
    private SewerLevel floor() {
        HeadlessTestSupport.start(); SewerLevel l = new SewerLevel(); l.setSize(9,9);
        l.heaps = new SparseArray<>(); l.traps = new SparseArray<>(); l.plants = new SparseArray<>();
        l.blobs = new HashMap<>(); l.mobs = new HashSet<>(); l.transitions = new ArrayList<>();
        Arrays.fill(l.passable,true); return l;
    }
    private Heap heap(Level l,int cell,Heap.Type type,Item...items) {
        Heap h = new Heap(); h.pos=cell; h.type=type; Collections.addAll(h.items,items); l.heaps.put(cell,h); return h;
    }
    @Test public void missingExitNeverFallsBackToEntrance() {
        SewerLevel l=floor(); l.transitions.add(new LevelTransition(l,40,LevelTransition.Type.REGULAR_ENTRANCE));
        assertEquals(-1,LootPlacement.besideDownstairs(l));
    }
    @Test public void exitPlacementSkipsHazardsContainersAndMapWrapping() {
        SewerLevel l=floor(); l.transitions.add(new LevelTransition(l,44,LevelTransition.Type.REGULAR_EXIT));
        l.pit[53]=true;heap(l,43,Heap.Type.CHEST,new Pasty());l.traps.put(35,new WornDartTrap());
        l.passable[52]=false;l.passable[34]=false;
        assertEquals(-1,LootPlacement.besideDownstairs(l));
        l.passable[43]=true;l.heaps.remove(43);assertEquals(43,LootPlacement.besideDownstairs(l));
    }
    @Test public void onePilePreservesEveryObjectAndPropertiesWithoutMerging() {
        SewerLevel l=floor(); List<Item> originals=new ArrayList<>();
        for(int i=0;i<100;i++){Dagger d=new Dagger();d.upgrade(i%4);d.cursed=i%2==0;d.apdUid="item-"+i;
            originals.add(d);}
        // Retain every stack even when several share the same source heap.
        l.heaps.clear();for(Item item:originals){Heap h=l.heaps.get(10+originals.indexOf(item)%5);
            if(h==null)h=heap(l,10+originals.indexOf(item)%5,Heap.Type.HEAP);h.items.add(item);}
        Heap chest=heap(l,60,Heap.Type.CHEST,new Pasty());
        List<Item> moved=GroundLootMover.remove(l,GroundLootPlan.capture(l,i->true));
        Heap result=GroundLootMover.place(l,40,moved); assertEquals(100,result.items.size());assertSame(chest,l.heaps.get(60));
        Set<Item> identities=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());identities.addAll(result.items);
        assertEquals(100,identities.size());
        for(int i=0;i<100;i++){Item item=originals.get(i);assertTrue(identities.contains(item));assertEquals("item-"+i,item.apdUid);
            assertEquals(i%4,item.level());assertEquals(i%2==0,item.cursed);}
    }
    @Test public void gatheringExistingDestinationDoesNotDuplicateIt() {
        SewerLevel l=floor();Pasty a=new Pasty(),b=new Pasty();a.quantity(3);b.quantity(4);
        heap(l,10,Heap.Type.HEAP,a);heap(l,40,Heap.Type.HEAP,b);
        Heap pile=GroundLootMover.place(l,40,GroundLootMover.remove(l,GroundLootPlan.capture(l,i->true)));
        assertEquals(2,pile.items.size());assertEquals(7,pile.items.stream().mapToInt(Item::quantity).sum());
    }
    @Test public void stillOwnedItemIsRejectedBeforeDestinationChanges() {
        SewerLevel l=floor();Pasty p=new Pasty();heap(l,10,Heap.Type.HEAP,p);
        try{GroundLootMover.place(l,40,Collections.singletonList(p));fail();}catch(IllegalStateException expected){}
        assertNull(l.heaps.get(40));assertSame(p,l.heaps.get(10).items.getFirst());
    }
    @Test public void progressionAndBackpackPolicyPreservesTheirObjects() {
        assertFalse(LootTransfers.eligible(new LostBackpack()));
        assertFalse(LootTransfers.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey()));
        assertFalse(LootTransfers.eligible(new com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust()));
        assertTrue(LootTransfers.eligible(new Pasty()));
    }

    @Test public void crossContainerAliasesAndWrongFloorPlansNeverRemoveItems() {
        SewerLevel first=floor(),second=floor();Pasty item=new Pasty();
        Heap original=heap(first,10,Heap.Type.HEAP,item);
        GroundLootPlan plan=GroundLootPlan.capture(first,i->true);
        try{GroundLootMover.remove(second,plan);fail();}catch(IllegalStateException expected){}
        assertSame(item,original.items.getFirst());
        heap(first,11,Heap.Type.CHEST,item);
        try{GroundLootPlan.capture(first,i->true);fail();}catch(IllegalStateException expected){}
        assertSame(item,original.items.getFirst());assertEquals(2,first.heaps.size);
    }
}
