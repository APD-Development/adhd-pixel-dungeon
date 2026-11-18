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

/** Deterministic exploratory contrasts with minimum samples and multiplicity-adjusted Wilson bands. */
public final class Discoveries {
    private Discoveries(){}
    public static final int MIN_SAMPLE=20,MAX_FINDINGS=12,MAX_GROUPS=24;
    private static final String[] KEYS={"run.heroClass","final.subClass"};
    public static final class Group {
        public final String value,label;public final List<Row> rows=new ArrayList<>();public int wins;
        Group(String value,String label){this.value=value;this.label=label;}
    }
    public static final class Finding {
        public final String key,version,rules,label;public final Group a,b;public final double difference,aLow,aHigh,bLow,bHigh;
        Finding(String key,String version,String rules,Group a,Group b,double z){
            this.key=key;this.version=version;this.rules=rules;this.a=a;this.b=b;
            difference=100d*((double)a.wins/a.rows.size()-(double)b.wins/b.rows.size());
            double[] x=wilson(a.wins,a.rows.size(),z),y=wilson(b.wins,b.rows.size(),z);aLow=x[0];aHigh=x[1];bLow=y[0];bHigh=y[1];
            label=a.label+" vs "+b.label+": "+ResearchData.value(difference)+" percentage points";
        }
        public boolean separated(){return aLow>bHigh || bLow>aHigh;}
    }
    public static final class Result {
        public final QueryResult source;public final int eligible,excluded,comparisons,omitted;public final List<Finding> findings;
        Result(QueryResult source,int eligible,int comparisons,List<Finding> findings,int omitted){this.source=source;this.eligible=eligible;excluded=source.rows.size()-eligible;this.comparisons=comparisons;this.findings=Collections.unmodifiableList(findings);this.omitted=omitted;}
    }
    public static Result build(QueryResult r){
        if(r==null || r.isGrouped() || !r.def.id.equals(Datasets.RUNS) || !r.problems.isEmpty())return null;
        Map<List<String>,Map<String,Group>> strata=new LinkedHashMap<>();int eligible=0;
        for(Row row:ResearchData.distinct(r)){
            Long outcome=ResearchData.number(r,row,"result.outcome");
            if(!row.finished || row.imported || row.ruleset==null || row.ruleset.isEmpty() || Row.RULESET_MIXED.equals(row.ruleset)
                    || outcome==null || outcome<0 || outcome>2 || !ResearchData.exact(r,row,"result.outcome") || !ResearchData.exact(r,row,"run.apdVersion"))continue;
            String version=ResearchData.text(r,row,"run.apdVersion");if(version==null || version.isEmpty())continue;
            eligible++;
            for(String key:KEYS){
                if(!ResearchData.exact(r,row,key))continue;String value=ResearchData.text(r,row,key);if(value==null || value.isEmpty())continue;
                List<String> stratum=Arrays.asList(key,version,row.ruleset);
                Map<String,Group> groups=strata.computeIfAbsent(stratum,k->new TreeMap<>());
                Group g=groups.computeIfAbsent(value,k->new Group(value,ResearchData.label(r,row,key)));g.rows.add(row);if(outcome==1)g.wins++;
            }
        }
        List<Finding> candidates=new ArrayList<>();int omitted=0;
        for(Map.Entry<List<String>,Map<String,Group>> e:strata.entrySet()){
            List<Group> groups=new ArrayList<>();for(Group g:e.getValue().values())if(g.rows.size()>=MIN_SAMPLE)groups.add(g);
            if(groups.size()>MAX_GROUPS){omitted+=groups.size()-MAX_GROUPS;groups=new ArrayList<>(groups.subList(0,MAX_GROUPS));}
            List<String> identity=e.getKey();
            for(int i=0;i<groups.size();i++)for(int j=i+1;j<groups.size();j++)candidates.add(new Finding(identity.get(0),identity.get(1),identity.get(2),groups.get(i),groups.get(j),1.96));
        }
        int tests=candidates.size();double z=normalQuantile(1-.05/(4*Math.max(1,tests)));List<Finding> out=new ArrayList<>();
        for(Finding c:candidates){Finding f=new Finding(c.key,c.version,c.rules,c.a,c.b,z);if(Math.abs(f.difference)>=10 && f.separated())out.add(f);}
        out.sort((a,b)->{int c=Double.compare(Math.abs(b.difference),Math.abs(a.difference));return c!=0?c:(a.key+a.version+a.rules+a.label).compareTo(b.key+b.version+b.rules+b.label);});
        if(out.size()>MAX_FINDINGS){omitted+=out.size()-MAX_FINDINGS;out=new ArrayList<>(out.subList(0,MAX_FINDINGS));}
        return new Result(r,eligible,tests,out,omitted);
    }
    public static double[] wilson(int successes,int n,double z){
        if(n<=0 || successes<0 || successes>n || !Double.isFinite(z) || z<=0)return new double[]{Double.NaN,Double.NaN};
        double p=(double)successes/n,z2=z*z,den=1+z2/n,centre=(p+z2/(2*n))/den;
        double margin=z*Math.sqrt(p*(1-p)/n+z2/(4d*n*n))/den;
        return new double[]{Math.max(0,centre-margin),Math.min(1,centre+margin)};
    }
    // Acklam's rational approximation to the inverse normal CDF. Used only for exploratory bands.
    static double normalQuantile(double p){
        double[] a={-39.6968302866538,220.946098424521,-275.928510446969,138.357751867269,-30.6647980661472,2.50662827745924};
        double[] b={-54.4760987982241,161.585836858041,-155.698979859887,66.8013118877197,-13.2806815528857};
        double[] c={-.00778489400243029,-.322396458041136,-2.40075827716184,-2.54973253934373,4.37466414146497,2.93816398269878};
        double[] d={.00778469570904146,.32246712907004,2.445134137143,3.75440866190742};
        p=Math.max(1e-15,Math.min(1-1e-15,p));
        if(p<.02425){double q=Math.sqrt(-2*Math.log(p));return (((((c[0]*q+c[1])*q+c[2])*q+c[3])*q+c[4])*q+c[5])/((((d[0]*q+d[1])*q+d[2])*q+d[3])*q+1);}
        if(p>1-.02425)return -normalQuantile(1-p);
        double q=p-.5,r=q*q;return (((((a[0]*r+a[1])*r+a[2])*r+a[3])*r+a[4])*r+a[5])*q/(((((b[0]*r+b[1])*r+b[2])*r+b[3])*r+b[4])*r+1);
    }
}
