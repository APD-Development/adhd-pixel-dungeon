package com.shatteredpixel.shatteredpixeldungeon.apd.loot;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import java.util.*;

/** Full-fidelity pending shipment payload. Persistence/commit ownership is separate. */
public final class LootShipment implements Bundlable {
    private String id;
    private int sourceDepth,sourceBranch,destinationDepth;
    private List<Item> items;
    public LootShipment() {} // Bundle restoration
    public LootShipment(String id,int sourceDepth,int sourceBranch,int destinationDepth,List<Item> items) {
        this.id=id;this.sourceDepth=sourceDepth;this.sourceBranch=sourceBranch;
        this.destinationDepth=destinationDepth;this.items=new ArrayList<>(items);validate();
    }
    public String id(){return id;}
    public int sourceDepth(){return sourceDepth;}
    public int sourceBranch(){return sourceBranch;}
    public int destinationDepth(){return destinationDepth;}
    public List<Item> items(){return Collections.unmodifiableList(items);}
    private long total() {
        long sum=0;for(Item item:items)sum=Math.addExact(sum,item.quantity());return sum;
    }
    private void validate() {
        if(id==null || id.isEmpty() || sourceDepth<1 || sourceBranch<0
                || destinationDepth!=LootScrollRules.destinationDepth(sourceDepth) || destinationDepth<1 || items==null || items.isEmpty())
            throw new IllegalArgumentException("Invalid shipment metadata");
        Set<Item> unique=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        for(Item item:items)if(item==null || item.quantity()<=0 || !unique.add(item))
            throw new IllegalArgumentException("Invalid shipment item");
        total();
    }
    @Override public void storeInBundle(Bundle bundle) {
        validate();
        bundle.put("version",1);bundle.put("id",id);bundle.put("source_depth",sourceDepth);
        bundle.put("source_branch",sourceBranch);bundle.put("destination_depth",destinationDepth);
        bundle.put("stacks",items.size());bundle.put("quantity",total());
        bundle.put("items",items);
    }
    @Override public void restoreFromBundle(Bundle bundle) {
        for(String key:new String[]{"version","id","source_depth","source_branch","destination_depth","stacks","quantity","items"})
            if(!bundle.contains(key))throw new IllegalArgumentException("Incomplete shipment: "+key);
        if(bundle.getInt("version")!=1)throw new IllegalArgumentException("Unsupported shipment version");
        id=bundle.getString("id");sourceDepth=bundle.getInt("source_depth");
        sourceBranch=bundle.getInt("source_branch");destinationDepth=bundle.getInt("destination_depth");
        List<Item> restored=new ArrayList<>();
        for(Bundlable value:bundle.getCollection("items")) {
            if(!(value instanceof Item))throw new IllegalArgumentException("Unknown shipment item");
            restored.add((Item)value);
        }
        items=restored;validate();
        if(items.size()!=bundle.getInt("stacks") || total()!=bundle.getLong("quantity"))
            throw new IllegalArgumentException("Incomplete shipment contents");
    }
}
