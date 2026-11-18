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
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts;
import java.util.*;

/** Empirical generation comparison, explicitly not a theoretical RNG probability or scoring modifier. */
public final class RunLuck {
    private RunLuck(){}
    public static final int MIN_PEERS=10;
    public static final String[] METRICS={CustomRuleFacts.GENERATED_UPGRADES,CustomRuleFacts.GENERATED_EQUIPMENT,"gen.items","gen.mobs"};
    public static final String[] LABELS={"Generated upgrade scrolls","Generated equipment","Generated items","Generated enemies"};
    public static final class Metric {
        public final String key,label;public final Long observed;public final int n,below,tied,excluded;
        public final double mean,min,max,percentile;public final List<Row> evidence;
        Metric(QueryResult r,Row subject,List<Row> peers,int i){
            key=METRICS[i];label=LABELS[i];Long v=ResearchData.number(r,subject,key);
            observed=ResearchData.exact(r,subject,key) && v!=null && v>=0 ? v:null;
            int less=0,equal=0;double sum=0,lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;List<Row> used=new ArrayList<>();
            for(Row peer:peers){Long x=ResearchData.number(r,peer,key);if(x==null || x<0 || !ResearchData.exact(r,peer,key))continue;
                used.add(peer);sum+=x;lo=Math.min(lo,x);hi=Math.max(hi,x);if(observed!=null){if(x<observed)less++;if(x.equals(observed))equal++;}}
            n=used.size();excluded=peers.size()-n;below=less;tied=equal;mean=n==0 ? Double.NaN:sum/n;min=n==0 ? Double.NaN:lo;max=n==0 ? Double.NaN:hi;
            percentile=observed!=null && n>=MIN_PEERS ? 100d*(below+.5*tied)/n:Double.NaN;evidence=Collections.unmodifiableList(used);
        }
    }
    public static final class Result {
        public final Row subject;public final String unavailable;public final List<Row> peers;public final List<Metric> metrics;public final Query query;
        Result(QueryResult r,Row subject,String unavailable,List<Row> peers){
            this.subject=subject;this.unavailable=unavailable;this.peers=Collections.unmodifiableList(new ArrayList<>(peers));query=r.query;
            List<Metric> out=new ArrayList<>();if(unavailable.isEmpty())for(int i=0;i<METRICS.length;i++)out.add(new Metric(r,subject,peers,i));metrics=Collections.unmodifiableList(out);
        }
    }
    private static String eligibility(QueryResult r,Row row){
        if(!row.finished || row.imported || row.duplicate)return "Needs a finished recorded run, not an imported or repeated ranking.";
        if(row.ruleset==null || row.ruleset.isEmpty() || row.ruleset.equals(Row.RULESET_MIXED) || row.telemetry.isEmpty())return "Exact rules or recording version are unknown.";
        for(String k:new String[]{"run.apdVersion","run.heroClass",CustomRuleFacts.IDENTITY,"gen.levels","result.deepest","run.seedType","run.seed"})
            if(!ResearchData.exact(r,row,k))return "Missing or hidden exact evidence: "+k+". Generation and seed details require Show hidden information for finished runs.";
        Long levels=ResearchData.number(r,row,"gen.levels"),kind=ResearchData.number(r,row,"run.seedType");
        if(levels==null || levels<=0 || kind==null || kind!=0 || ResearchData.number(r,row,"run.seed")==null)return "Needs a random-seed run with a positive recorded generated-level count. Daily/custom seeds are not independent reference samples.";
        return "";
    }
    public static Result build(QueryResult r,Row subject){
        if(r==null || !Datasets.RUNS.equals(r.def.id) || r.isGrouped() || !r.problems.isEmpty() || subject==null)return null;
        List<Row> rows=ResearchData.distinct(r);if(!rows.contains(subject))return null;
        String why=eligibility(r,subject);List<Row> peers=new ArrayList<>();
        if(!why.isEmpty())return new Result(r,subject,why,peers);
        Set<Long> seeds=new HashSet<>();seeds.add(ResearchData.number(r,subject,"run.seed"));
        for(Row peer:rows){
            if(peer==subject || !eligibility(r,peer).isEmpty() || !Objects.equals(peer.ruleset,subject.ruleset) || !peer.telemetry.equals(subject.telemetry))continue;
            boolean same=true;
            for(String k:new String[]{"run.apdVersion","run.heroClass",CustomRuleFacts.IDENTITY,"gen.levels","result.deepest"})
                if(!Objects.equals(ResearchData.text(r,subject,k),ResearchData.text(r,peer,k))){same=false;break;}
            if(same && seeds.add(ResearchData.number(r,peer,"run.seed")))peers.add(peer);
        }
        return new Result(r,subject,"",peers);
    }
}
