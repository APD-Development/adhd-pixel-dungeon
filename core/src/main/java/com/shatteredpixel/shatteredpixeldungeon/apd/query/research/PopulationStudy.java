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
import java.util.*;

/** Descriptive comparisons of explicit queries. Inference is refused for overlapping/clustered samples. */
public final class PopulationStudy {
    private PopulationStudy(){}
    public static final int MIN_SAMPLE=20;
    public static final String WIN_RATE="@winRate";
    public static final class Sample {
        public final Query query;public final String column;
        public final int matching,n,excluded,wins;public final double mean,sd,min,max,median;
        public final List<Row> contributors;public final Set<String> runIds;
        public final boolean independent,homogeneous;public final String version;
        private Sample(QueryResult r,String column){
            query=r.query;this.column=column;matching=r.rows.size();
            List<Row> rows=new ArrayList<>();List<Double> values=new ArrayList<>();Set<String> units=new HashSet<>();Set<List<String>> strata=new HashSet<>();
            double avg=0,m2=0,lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;boolean onePerRun=true;int winCount=0;
            for(Row row:ResearchData.distinct(r)){
                String source=WIN_RATE.equals(column) ? "result.outcome":column;
                Long v=ResearchData.number(r,row,source);
                if(v==null || !ResearchData.exact(r,row,source))continue;
                if(WIN_RATE.equals(column) && (!row.finished || v<0 || v>2))continue;
                double x=WIN_RATE.equals(column) ? (v==1 ? 100d:0d):v.doubleValue();if(WIN_RATE.equals(column)&&v==1)winCount++;rows.add(row);values.add(x);int count=rows.size();double delta=x-avg;avg+=delta/count;m2+=delta*(x-avg);lo=Math.min(lo,x);hi=Math.max(hi,x);
                if(!row.finished || row.runId.isEmpty() || !units.add(row.runId))onePerRun=false;
                String version=ResearchData.text(r,row,"run.apdVersion");
                if(!ResearchData.exact(r,row,"run.apdVersion"))version=null;
                if(row.ruleset==null || row.ruleset.isEmpty() || Row.RULESET_MIXED.equals(row.ruleset) || version==null || version.isEmpty())strata.add(Arrays.asList("unknown",row.id));
                else strata.add(Arrays.asList("known",row.ruleset,version));
            }
            Collections.sort(values);n=rows.size();excluded=matching-n;wins=winCount;
            mean=n==0 ? Double.NaN:avg;sd=n<2 ? Double.NaN:Math.sqrt(Math.max(0,m2/(n-1)));
            min=n==0 ? Double.NaN:lo;max=n==0 ? Double.NaN:hi;
            median=n==0 ? Double.NaN:(values.get((n-1)/2)/2+values.get(n/2)/2);
            contributors=Collections.unmodifiableList(rows);runIds=Collections.unmodifiableSet(units);independent=onePerRun;
            homogeneous=n>0 && strata.size()==1 && strata.iterator().next().get(0).equals("known");
            version=homogeneous ? strata.iterator().next().get(2):"";
        }
    }
    public static final class Comparison {
        public final Sample a,b;public final int overlappingRuns;public final double difference,low,high;public final boolean intervalKnown;public final String note;
        private Comparison(Sample a,Sample b){
            this.a=a;this.b=b;Set<String> overlap=new HashSet<>(a.runIds);overlap.retainAll(b.runIds);overlappingRuns=overlap.size();
            difference=a.mean-b.mean;
            intervalKnown=a.n>=MIN_SAMPLE && b.n>=MIN_SAMPLE && a.independent && b.independent && overlappingRuns==0
                    && a.homogeneous && b.homogeneous && sameStratum(a,b);
            double va=a.sd*a.sd/a.n,vb=b.sd*b.sd/b.n;
            double df=(va+vb)==0 ? Double.POSITIVE_INFINITY:(va+vb)*(va+vb)/(va*va/(a.n-1)+vb*vb/(b.n-1));
            double margin=intervalKnown ? critical(df)*Math.sqrt(va+vb):Double.NaN;
            if(intervalKnown && WIN_RATE.equals(a.column)){
                double[] x=Discoveries.wilson(a.wins,a.n,2.241402727604947),y=Discoveries.wilson(b.wins,b.n,2.241402727604947);
                low=100*(x[0]-y[1]);high=100*(x[1]-y[0]);
            }else{low=difference-margin;high=difference+margin;}
            note=intervalKnown ? (WIN_RATE.equals(a.column) ? "Conservative approximate 95% Wilson difference interval; observational association, not causation.":"Approximate 95% Welch interval; observational association, not causation. Sampling assumptions may not fit self-selected runs."):
                    "Descriptive comparison only. An interval requires at least 20 exact values per side, one observation per run, no overlapping runs and the same known version/rules. Unknown and uncertain values are excluded, not zero.";
        }
    }
    public static Sample sample(QueryResult r,String column){
        if(r==null || r.isGrouped() || !r.problems.isEmpty())return null;
        ColumnDef c=r.def.column(WIN_RATE.equals(column) ? "result.outcome":column);
        if(c==null || (!WIN_RATE.equals(column) && c.type!=ColumnDef.Type.NUM) || c.isInternal() && !r.query.showInternal)return null;
        return new Sample(r,column);
    }
    public static Comparison compare(Sample a,Sample b){return a==null || b==null || !a.query.dataset.equals(b.query.dataset) || !a.column.equals(b.column) ? null:new Comparison(a,b);}
    private static boolean sameStratum(Sample a,Sample b){
        if(a.contributors.isEmpty() || b.contributors.isEmpty())return false;
        Row x=a.contributors.get(0),y=b.contributors.get(0);
        // Metadata is compared from the selected result's guarded samples in sample().
        return Objects.equals(x.ruleset,y.ruleset) && stratumVersion(a).equals(stratumVersion(b));
    }
    private static String stratumVersion(Sample a){
        // Homogeneity is established from disclosure-safe values; cache it instead of reading raw cells here.
        return a.version;
    }
    private static double critical(double df){
        if(df>=120)return 1.980;if(df>=60)return 2.001;if(df>=40)return 2.022;if(df>=30)return 2.043;
        if(df>=20)return 2.086;if(df>=10)return 2.228;return 12.707;
    }
}
