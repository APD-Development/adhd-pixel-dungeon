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


package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import java.util.*;

/** Shared disclosure and identity boundary for read-only research. No log IO or scope widening. */
public final class ResearchData {
    private ResearchData(){}
    public static Query rows(Query q){return q.withGroup(null).withMinSample(0);}
    public static QueryResult select(Catalog catalog,Query query,String rules){
        if(catalog==null || query==null)return null;
        Dataset d=catalog.dataset(query.dataset);if(d==null)return null;
        QueryResult r=QueryEngine.run(d,rows(query),rules);
        return r.problems.isEmpty() ? r : null; // Never silently drop an unsupported stored filter.
    }
    public static boolean open(QueryResult r,Row row,String key){
        ColumnDef c=r==null ? null:r.def.column(key);
        return c!=null && c.index()<row.size() && QueryEngine.open(r.query,row,c) && row.isKnown(c.index());
    }
    public static boolean exact(QueryResult r,Row row,String key){
        return open(r,row,key) && row.prov(r.def.indexOf(key))==Fact.PROV_RECORDED;
    }
    public static Long number(QueryResult r,Row row,String key){
        if(!open(r,row,key))return null;
        int i=r.def.indexOf(key);return row.kind(i)==Row.K_NUM ? row.num(i):null;
    }
    public static String text(QueryResult r,Row row,String key){
        return open(r,row,key) ? row.asText(r.def.indexOf(key)):null;
    }
    public static String label(QueryResult r,Row row,String key){
        ColumnDef c=r.def.column(key);return c==null ? "?":CellFormatter.cell(r.query,row,c).text;
    }
    public static List<Row> distinct(QueryResult r){
        if(r==null || r.isGrouped())return Collections.emptyList();
        List<Row> rows=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Row row:r.rows){
            if(row.duplicate)continue;
            String key=r.def.id.equals(Datasets.RUNS) ? runIdentity(row):row.id;
            if(!key.isEmpty() && seen.add(key))rows.add(row);
        }
        return Collections.unmodifiableList(rows);
    }
    public static String runIdentity(Row r){return r.runId.isEmpty() ? "row:"+r.id:"run:"+r.runId;}
    public static Query evidence(QueryResult r,Row row){
        String key=r.def.id.equals(Datasets.RUNS) ? "run.id":null;
        if(key==null || !open(r,row,key))return null;
        return rows(r.query).addFilter(Filter.text(key,Op.GROUP_TEXT,row.asText(r.def.indexOf(key))));
    }
    public static String value(double n){return Double.isFinite(n) ? String.format(Locale.ROOT,"%.2f",n):"?";}
}
