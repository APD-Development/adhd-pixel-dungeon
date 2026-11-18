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

/** Explicit per-run measurements, never a guessed personality, quality rating or luck score. */
public final class RunDna {
    private RunDna(){}
    public static final class Metric {
        public final String id,label,numerator,denominator,definition;
        public final boolean known;public final double value;public final int state;
        public final String shown,detail;
        Metric(String id,String label,String n,String d,double factor,QueryResult r,Row row){
            this.id=id;this.label=label;numerator=n;denominator=d;
            definition=ResearchData.value(factor)+" x "+n+" / "+d;
            Long a=ResearchData.number(r,row,n),b=ResearchData.number(r,row,d);
            int ap=a==null ? 0:row.prov(r.def.indexOf(n)),bp=b==null ? 0:row.prov(r.def.indexOf(d));
            known=a!=null && b!=null && a>=0 && b>0 && ap>=Fact.PROV_RECORDED && ap<=Fact.PROV_INCOMPLETE && bp==Fact.PROV_RECORDED && (!(id.equals("accuracy") || id.equals("incoming")) || a<=b);
            value=known ? factor*((double)a/(double)b):Double.NaN;
            state=!known ? CellText.UNKNOWN:ap==Fact.PROV_INCOMPLETE ? CellText.LOWER:
                    ap==Fact.PROV_INFERRED ? CellText.INFERRED:ap==Fact.PROV_LEGACY ? CellText.LEGACY:CellText.EXACT;
            shown=!known ? "?":(state==CellText.LOWER ? ">=":state==CellText.INFERRED ? "~":"")+ResearchData.value(value);
            detail=definition+". Numerator: "+ResearchData.label(r,row,n)+"; denominator: "+ResearchData.label(r,row,d)
                    +(!known ? ". Requires a positive, exactly recorded denominator and nonnegative known numerator.":". "+CellFormatter.explain(state));
        }
    }
    public static final class Profile {
        public final Row row;public final Query evidence;public final List<Metric> metrics;
        private Profile(QueryResult r,Row row){
            this.row=row;evidence=ResearchData.evidence(r,row);
            List<Metric> out=new ArrayList<>();
            add(out,r,row,"tempo","Attacks per 100 turns","combat.heroAttacks","result.turns",100);
            add(out,r,row,"accuracy","Hero hit rate %","combat.heroHits","combat.heroAttacks",100);
            add(out,r,row,"incoming","Incoming hit rate %","combat.hitsOnHero","combat.attacksOnHero",100);
            add(out,r,row,"offense","Damage dealt per 100 turns","combat.damageDealt","result.turns",100);
            add(out,r,row,"pressure","Damage taken per 100 turns","combat.damageTaken","result.turns",100);
            add(out,r,row,"healing","Healing per 100 turns","combat.healed","result.turns",100);
            add(out,r,row,"kills","Kills per 100 turns","combat.heroKills","result.turns",100);
            add(out,r,row,"gear","Equip actions per 100 turns","items.equipEvents","result.turns",100);
            add(out,r,row,"pace","Turns per entered level","result.turns","journey.levelsEntered",1);
            add(out,r,row,"acquisition","Items acquired per entered level","items.acquired","journey.levelsEntered",1);
            metrics=Collections.unmodifiableList(out);
        }
    }
    private static void add(List<Metric> out,QueryResult r,Row row,String id,String label,String n,String d,double factor){out.add(new Metric(id,label,n,d,factor,r,row));}
    public static Profile build(QueryResult r,Row row){
        if(r==null || !r.def.id.equals(Datasets.RUNS) || r.isGrouped() || !r.problems.isEmpty() || row==null)return null;
        for(Row allowed:ResearchData.distinct(r))if(allowed==row)return new Profile(r,row);
        return null;
    }
    public static List<String> compare(Profile a,Profile b){
        List<String> out=new ArrayList<>();
        for(int i=0;i<a.metrics.size();i++){
            Metric x=a.metrics.get(i),y=b.metrics.get(i);
            out.add(x.label+": "+x.shown+" vs "+y.shown+"; A - B: "+
                    (x.state==CellText.EXACT && y.state==CellText.EXACT ? ResearchData.value(x.value-y.value):"? (uncertain input)"));
        }
        return Collections.unmodifiableList(out);
    }
}
