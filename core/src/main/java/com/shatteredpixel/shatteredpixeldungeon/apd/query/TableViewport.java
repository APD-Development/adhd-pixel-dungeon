package com.shatteredpixel.shatteredpixeldungeon.apd.query;

/** Transient presentation state. Never changes queries, saved views, or history. */
public final class TableViewport {
    private final String anchor, sorting;
    private final float offset, x, y;
    private final boolean grouped;

    private TableViewport(String anchor, String sorting, float offset, float x, float y, boolean grouped) {
        this.anchor=anchor; this.sorting=sorting; this.offset=offset; this.x=x; this.y=y;
        this.grouped=grouped;
    }

    private static String sorting(ResultTable table) {
        StringBuilder s=new StringBuilder();
        java.util.SortedMap<Integer,String> keys=new java.util.TreeMap<>();
        for(ResultTable.Header h:table.headers) if(h.sortDirection!=0)
            keys.put(h.sortRank,key(h));
        for(String key:keys.values())s.append(key);
        return s.toString();
    }

    private static String key(ResultTable.Header h){return h.id+":"+h.sortDirection+":"+h.sortRank+";";}

    public static TableViewport capture(ResultTable table, float x, float y) {
        if(table==null)return null;
        TableGeometry g=TableGeometry.of(table);
        String anchor=""; float offset=0;
        for(int i=1;i<table.headers.size();i++) {
            float left=g.lefts[i]-g.firstWidth();
            if(x>=left && x<left+g.widths[i]) {anchor=table.headers.get(i).id;offset=x-left;break;}
        }
        return new TableViewport(anchor,sorting(table),offset,Math.max(0,x),Math.max(0,y),table.grouped);
    }

    /** Width and height are the scrolling body, excluding frozen header/column. */
    public float[] position(ResultTable table, float width, float height) {
        if(table.grouped!=grouped)return new float[]{0,0};
        TableGeometry g=TableGeometry.of(table);
        float sx=x;
        for(int i=1;i<table.headers.size();i++) if(table.headers.get(i).id.equals(anchor)) {
            sx=g.lefts[i]-g.firstWidth()+Math.min(offset,Math.max(0,g.widths[i]-1));break;
        }
        boolean changed=!sorting.equals(sorting(table));
        if(changed) {
            int focus=-1,rank=Integer.MAX_VALUE;
            for(int i=0;i<table.headers.size();i++){
                ResultTable.Header h=table.headers.get(i);
                if(h.sortDirection!=0 && h.sortRank<rank && !(";"+sorting).contains(";"+key(h))){
                    focus=i;rank=h.sortRank;
                }
            }
            if(focus>0){
                float left=g.lefts[focus]-g.firstWidth(), right=left+g.widths[focus];
                if(left<sx)sx=left;
                else if(right>sx+width)sx=g.widths[focus]>width?left:right-width;
            }
        }
        return new float[]{clamp(sx,g.scrollingWidth()-width),
                clamp(changed?0:y,g.bodyHeight()-height)};
    }

    private static float clamp(float value,float max) {return Math.max(0,Math.min(value,Math.max(0,max)));}

    public static String heading(ResultTable.Header h) {
        // Put the indicator first so long names cannot truncate it away.
        return (h.sortDirection==0?"":h.sortDirection>0?"^ ":"v ")+h.label
                +(h.sortRank>1?" ("+h.sortRank+")":"");
    }
}
