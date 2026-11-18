package com.shatteredpixel.shatteredpixeldungeon.apd;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

/** First collection of progression resources, kept separate from inventory quantity and scoring. */
public final class ProgressionPickups {
    private static final String RUN = "apd_progression_pickups_v1";
    private static final String ITEM = "apd_progression_origins_v1";
    private static int[] upgrades = new int[5], strength = new int[5];
    private static boolean[] strengthPairs = new boolean[10], knownPairs = new boolean[10];
    private static boolean complete;
    private ProgressionPickups() {}

    /** Sparse on disk; buckets0..25 are ordinary and26..51 guaranteed strength origins. */
    public static final class Origins {
        private final int[] pending = new int[52];
    }
    public static boolean tracked(Item item) {
        return item instanceof ScrollOfUpgrade || item instanceof PotionOfStrength;
    }
    public static int region(int depth) { return depth>=1 && depth<=25 ? (depth-1)/5 : -1; }
    public static int pair(int depth) {
        int r=region(depth), local=(depth-1)%5;
        return r<0 || local>=4 ? -1 : r*2+local/2;
    }
    public static void reset(boolean fullHistory) {
        upgrades=new int[5]; strength=new int[5]; strengthPairs=new boolean[10];
        knownPairs=new boolean[10]; complete=fullHistory;
    }
    public static int count(boolean potion,int region) { return (potion?strength:upgrades)[region]; }
    public static boolean collectedPair(int slot) { return strengthPairs[slot]; }
    public static boolean knownPair(int slot) { return complete || knownPairs[slot]; }

    public static void mark(Item item,int depth,boolean guaranteedStrength) {
        if (!tracked(item) || item.apdProgressionOrigins!=null || item.apdProgressionLegacy
                || region(depth)<0 || item.quantity()<=0) return;
        Origins origin=item.apdProgressionOrigins=new Origins();
        boolean guaranteed=guaranteedStrength && item instanceof PotionOfStrength && pair(depth)>=0;
        origin.pending[depth+(guaranteed?26:0)]=item.quantity();
        if(guaranteed) knownPairs[pair(depth)]=true;
    }
    public static void prepare(Item item) { mark(item,Dungeon.depth,false); }
    public static void merge(Item into,Item from) {
        if(!tracked(into) || from.apdProgressionOrigins==null) return;
        if(into.apdProgressionOrigins==null) into.apdProgressionOrigins=new Origins();
        for(int i=0;i<52;i++){
            into.apdProgressionOrigins.pending[i]=add(into.apdProgressionOrigins.pending[i],from.apdProgressionOrigins.pending[i]);
            from.apdProgressionOrigins.pending[i]=0;
        }
    }
    public static void split(Item from,Item piece,int amount) {
        if(!tracked(from) || from.apdProgressionOrigins==null) return;
        piece.apdProgressionOrigins=new Origins();
        // Choose physical units, never duplicate the serialized source's pending origins.
        int remaining=amount;
        for(int i=51;i>=0;i--){
            int taken=Math.min(remaining,from.apdProgressionOrigins.pending[i]);
            from.apdProgressionOrigins.pending[i]-=taken;
            piece.apdProgressionOrigins.pending[i]=taken; remaining-=taken;
        }
    }
    public static void collected(Item item,Bag container) {
        if(Dungeon.hero==null || (container!=Dungeon.hero.belongings.backpack
                && !Dungeon.hero.belongings.backpack.contains(container))) return;
        claim(item);
    }
    /** Invoked after successful collection only; package-independent for runtime regression fixtures. */
    public static void claim(Item item) {
        if(!tracked(item) || item.apdProgressionOrigins==null) return;
        int[] counts=item instanceof PotionOfStrength?strength:upgrades;
        for(int i=0;i<52;i++){
            int n=item.apdProgressionOrigins.pending[i],depth=i%26,r=region(depth);
            if(n>0 && r>=0){
                counts[r]=add(counts[r],n);
                if(i>=26 && item instanceof PotionOfStrength && pair(depth)>=0){
                    strengthPairs[pair(depth)]=true; knownPairs[pair(depth)]=true;
                }
            }
            item.apdProgressionOrigins.pending[i]=0;
        }
    }
    private static int add(int a,int b) { return (int)Math.min(Integer.MAX_VALUE,(long)a+b); }

    public static void storeItem(Item item,Bundle bundle) {
        if(!tracked(item)) return;
        if(item.apdProgressionOrigins==null){
            // Fresh enemy-held/reward items can be saved before their first placement.
            // Distinguish those from truly unrecorded items restored from an older APK.
            if(!item.apdProgressionLegacy)bundle.put(ITEM,new int[]{-1,0});
            return;
        }
        int size=0;for(int n:item.apdProgressionOrigins.pending)if(n>0)size+=2;
        int[] sparse=new int[size];int k=0;
        for(int i=0;i<52;i++)if(item.apdProgressionOrigins.pending[i]>0){
            sparse[k++]=i;sparse[k++]=item.apdProgressionOrigins.pending[i];
        }
        bundle.put(ITEM,sparse); // empty is meaningful: this stack was already accounted for
    }
    public static void restoreItem(Item item,Bundle bundle) {
        if(!tracked(item)) return;
        item.apdProgressionOrigins=null;item.apdProgressionLegacy=!bundle.contains(ITEM);
        if(item.apdProgressionLegacy)return;
        int[] sparse=bundle.getIntArray(ITEM);
        if(sparse!=null && sparse.length==2 && sparse[0]==-1 && sparse[1]==0)return;
        item.apdProgressionOrigins=new Origins();
        if(sparse==null || sparse.length%2!=0)return;
        int remaining=Math.max(0,item.quantity());
        for(int k=0;k<sparse.length;k+=2){
            int slot=sparse[k],n=sparse[k+1];
            if(slot<0 || slot>=52 || region(slot%26)<0 || n<0)continue;
            n=Math.min(n,remaining);
            item.apdProgressionOrigins.pending[slot]=add(item.apdProgressionOrigins.pending[slot],n);
            remaining-=n;
        }
    }
    public static void store(Bundle bundle) {
        Bundle state=new Bundle();state.put("upgrades",upgrades);state.put("strength",strength);
        state.put("pairs",strengthPairs);state.put("known",knownPairs);state.put("complete",complete);
        bundle.put(RUN,state);
    }
    public static void restore(Bundle bundle) {
        reset(false);if(!bundle.contains(RUN))return;
        Bundle state=bundle.getBundle(RUN);
        int[] u=state.getIntArray("upgrades"),s=state.getIntArray("strength");
        boolean[] p=state.getBooleanArray("pairs"),k=state.getBooleanArray("known");
        if(u==null || u.length!=5 || s==null || s.length!=5 || p==null || p.length!=10 || k==null || k.length!=10)return;
        for(int n:u)if(n<0)return;for(int n:s)if(n<0)return;
        upgrades=u.clone();strength=s.clone();strengthPairs=p.clone();knownPairs=k.clone();
        complete=state.getBoolean("complete");
    }
    public static String info(boolean potion) {
        Class<?> type=potion?PotionOfStrength.class:ScrollOfUpgrade.class;
        StringBuilder out=new StringBuilder("\n\n_").append(Messages.get(type,"collected_regions")).append("_");
        if(!complete)out.append("\n").append(Messages.get(PotionOfStrength.class,"pickup_legacy"));
        for(int r=0;r<5;r++){
            out.append("\n").append(Messages.get(PotionOfHealing.class,"enemy_region_"+r)).append(": ").append(count(potion,r));
            if(potion)for(int half=0;half<2;half++){
                int first=1+r*5+half*2,slot=r*2+half;
                String status=strengthPairs[slot]?"pair_collected":knownPair(slot)?"pair_missing":"pair_unknown";
                out.append("\n").append(Messages.get(PotionOfStrength.class,"pickup_pair",first,first+1,
                        Messages.get(PotionOfStrength.class,status)));
            }
        }
        return out.toString();
    }
}
