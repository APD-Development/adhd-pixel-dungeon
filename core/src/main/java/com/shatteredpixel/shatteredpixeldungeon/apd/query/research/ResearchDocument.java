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
import com.shatteredpixel.shatteredpixeldungeon.apd.report.Report;
import java.util.*;

/** One inspectable snapshot used by both native UI and all existing report formats. */
public final class ResearchDocument {
    public static final class Evidence {
        public final String label;public final QueryResult source;public final List<Row> rows;
        public Evidence(String label,QueryResult source,List<Row> rows){this.label=label;this.source=source;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));}
    }
    public static final class Card {
        public final String label,value,detail;public final int state;public final List<Evidence> evidence;
        public Card(String label,String value,int state,String detail,Evidence... evidence){this.label=label;this.value=value;this.state=state;this.detail=detail;this.evidence=Collections.unmodifiableList(Arrays.asList(evidence));}
    }
    public final String title;public final List<String> notes=new ArrayList<>();public final List<Card> cards=new ArrayList<>();
    public ResearchDocument(String title){this.title=title;}
    public static ResearchDocument dna(QueryResult source,RunDna.Profile profile){
        ResearchDocument d=new ResearchDocument("Run DNA");
        d.notes.add("A measured fingerprint, not a player rating. Rates use the recorded denominators; unknown values are not zero. Compare runs to see differences in the same defined metrics.");
        for(RunDna.Metric m:profile.metrics)d.cards.add(new Card(m.label,m.shown,m.state,m.detail,new Evidence("Source run",source,Arrays.asList(profile.row))));
        return d;
    }
    public static ResearchDocument dnaComparison(QueryResult source,RunDna.Profile a,RunDna.Profile b){
        ResearchDocument d=new ResearchDocument("Compare Run DNA");d.notes.add("A minus B uses exact measurements only. This is a descriptive fingerprint, not a causal explanation of either outcome.");
        for(int i=0;i<a.metrics.size();i++){
            RunDna.Metric x=a.metrics.get(i),y=b.metrics.get(i);boolean exact=x.state==CellText.EXACT && y.state==CellText.EXACT;
            d.cards.add(new Card(x.label,exact ? ResearchData.value(x.value-y.value):"?",exact ? CellText.EXACT:CellText.UNKNOWN,
                    "A: "+x.shown+"; B: "+y.shown+". "+x.detail+"\nB evidence: "+y.detail,
                    new Evidence("Run A",source,Arrays.asList(a.row)),new Evidence("Run B",source,Arrays.asList(b.row))));
        }
        return d;
    }
    public static ResearchDocument luck(QueryResult source,RunLuck.Result result){
        ResearchDocument d=new ResearchDocument("Luck laboratory");
        d.notes.add("Empirical generation comparison, not a theoretical RNG probability or an overall luck score. No score is changed. Only random-seed finished runs with matching class, APD version, exact custom settings, rules, recording version, generated-level count and deepest floor enter the reference. Different routes can still differ.");
        if(!result.unavailable.isEmpty()){d.notes.add(result.unavailable);return d;}
        d.notes.add(result.peers.size()+" comparable unique-seed peers in your current filtered history; the selected run and repeated seeds are excluded. At least "+RunLuck.MIN_PEERS+" exact peer values are required for a percentile.");
        for(RunLuck.Metric m:result.metrics)d.cards.add(new Card(m.label,m.observed==null ? "?":Long.toString(m.observed),m.observed==null ? CellText.UNKNOWN:CellText.EXACT,
                "Reference n="+m.n+"; unknown/uncertain peer values excluded="+m.excluded+"; mean="+ResearchData.value(m.mean)+"; range="+ResearchData.value(m.min)+" to "+ResearchData.value(m.max)+". Empirical mid-rank percentile="+ResearchData.value(m.percentile)+"; below="+m.below+", tied="+m.tied+". A percentile describes this reference history, not the chance the RNG generated the result.",
                new Evidence("Selected run",source,Arrays.asList(result.subject)),new Evidence("Comparable measured peers",source,m.evidence)));
        return d;
    }
    public static ResearchDocument study(QueryResult ar,QueryResult br,PopulationStudy.Comparison c){
        ResearchDocument d=new ResearchDocument("Statistical investigation lab");d.notes.add(c.note);
        d.notes.add("Population A: "+ar.def.title+", "+ar.rows.size()+" matching rows. Population B: "+br.def.title+", "+br.rows.size()+" matching rows. Grouping and display paging are not population filters. Original filters, rules and hidden-information choices are retained.");
        String label=PopulationStudy.WIN_RATE.equals(c.a.column) ? "Win rate (%)":ar.def.column(c.a.column).label;
        double divisor=1;
        if(!PopulationStudy.WIN_RATE.equals(c.a.column)){
            ColumnDef.Format format=ar.def.column(c.a.column).format;if(format==ColumnDef.Format.HUNDREDTHS)divisor=100;if(format==ColumnDef.Format.THOUSANDTHS)divisor=1000;
        }
        d.cards.add(new Card(label+": A minus B",ResearchData.value(c.difference/divisor),Double.isFinite(c.difference) ? CellText.EXACT:CellText.UNKNOWN,
                "A: n="+c.a.n+", mean="+ResearchData.value(c.a.mean/divisor)+", median="+ResearchData.value(c.a.median/divisor)+", SD="+ResearchData.value(c.a.sd/divisor)+", excluded="+c.a.excluded+".\nB: n="+c.b.n+", mean="+ResearchData.value(c.b.mean/divisor)+", median="+ResearchData.value(c.b.median/divisor)+", SD="+ResearchData.value(c.b.sd/divisor)+", excluded="+c.b.excluded+".\nOverlapping runs="+c.overlappingRuns+". Approximate 95% difference interval="+(c.intervalKnown ? ResearchData.value(c.low/divisor)+" to "+ResearchData.value(c.high/divisor):"? (requirements not met)")+". "+c.note,
                new Evidence("Population A contributors",ar,c.a.contributors),new Evidence("Population B contributors",br,c.b.contributors)));
        return d;
    }
    /** Connected evidence/export presentation for the fail-closed personal-progress model. */
    public static ResearchDocument progress(QueryResult earlierRows,QueryResult recentRows,ComparableProgress.Result result){
        ResearchDocument d=new ResearchDocument("Comparable personal progress");
        if(result==null){d.notes.add("The comparison is unavailable. No answer was invented.");return d;}
        d.notes.add(result.interpretation());
        if(!result.available())return d;
        d.notes.add("Only exact metric contributors establish dates and matching conditions. Cohort definitions also retain every matching row so excluded unknown or uncertain metric values remain inspectable.");
        PopulationStudy.Comparison c=result.comparison;ColumnDef metric=recentRows.def.column(c.a.column);
        String label=PopulationStudy.WIN_RATE.equals(c.a.column) ? "Win rate (%)":metric.label;
        double divisor=1;if(!PopulationStudy.WIN_RATE.equals(c.a.column)){if(metric.format==ColumnDef.Format.HUNDREDTHS)divisor=100;if(metric.format==ColumnDef.Format.THOUSANDTHS)divisor=1000;}
        String dates="Earlier: "+CellFormatter.date(result.earlierFirstMillis)+" through "+CellFormatter.date(result.earlierLastMillis)
                +"; recent: "+CellFormatter.date(result.recentFirstMillis)+" through "+CellFormatter.date(result.recentLastMillis)+".";
        String interval=c.intervalKnown ? ResearchData.value(c.low/divisor)+" to "+ResearchData.value(c.high/divisor):"? (requirements not met)";
        d.cards.add(new Card(label+": recent minus earlier",ResearchData.value(c.difference/divisor),Double.isFinite(c.difference)?CellText.EXACT:CellText.UNKNOWN,
                dates+"\nRecent: n="+c.a.n+", mean="+ResearchData.value(c.a.mean/divisor)+", median="+ResearchData.value(c.a.median/divisor)+", SD="+ResearchData.value(c.a.sd/divisor)+", excluded="+c.a.excluded+".\nEarlier: n="+c.b.n+", mean="+ResearchData.value(c.b.mean/divisor)+", median="+ResearchData.value(c.b.median/divisor)+", SD="+ResearchData.value(c.b.sd/divisor)+", excluded="+c.b.excluded+".\nApproximate 95% difference interval="+interval+". "+c.note,
                new Evidence("Recent exact metric contributors",recentRows,result.recent.contributors),new Evidence("Earlier exact metric contributors",earlierRows,result.earlier.contributors)));
        List<String> conditions=new ArrayList<>();conditions.add("Rules identity: "+result.recent.contributors.get(0).ruleset);
        for(String key:new String[]{com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.APD_VERSION,com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.HERO_CLASS,com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.DIFFICULTY,com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.DIFFICULTY_BALANCE,com.shatteredpixel.shatteredpixeldungeon.apd.stats.FactKeys.CHALLENGES,com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.IDENTITY}){
            ColumnDef column=recentRows.def.column(key);conditions.add(column.label+": "+ResearchData.label(recentRows,result.recent.contributors.get(0),key));
        }
        conditions.add("These recorded conditions match exactly across all metric contributors. Equipment, route, player choices and unrecorded facts can still differ.");
        d.cards.add(new Card("Comparable recorded conditions",result.conditions.size()+" exact matches",CellText.EXACT,String.join("\n",conditions),
                new Evidence("Recent condition records",recentRows,result.recent.contributors),new Evidence("Earlier condition records",earlierRows,result.earlier.contributors)));
        progressDefinition(d,"Earlier cohort definition",earlierRows,result.earlier,dates);
        progressDefinition(d,"Recent cohort definition",recentRows,result.recent,dates);
        return d;
    }
    private static void progressDefinition(ResearchDocument d,String title,QueryResult result,PopulationStudy.Sample sample,String dates){
        Query q=result.query;List<String> lines=new ArrayList<>();lines.add("Exact query: "+QueryCodec.encode(q));
        lines.add("Rules scope: "+q.scope.rules+" "+q.scope.ruleset+". Hidden-information choice: "+q.showInternal+".");
        for(Filter filter:q.filters)lines.add(StatusText.describe(result.def,filter));if(q.filters.isEmpty())lines.add("No column filters.");
        lines.add(dates);lines.add(sample.n+" exact metric contributors; "+sample.excluded+" matching rows excluded from the metric. Excluded values are not zero.");
        d.cards.add(new Card(title,result.rows.size()+" matching runs",CellText.EXACT,String.join("\n",lines),new Evidence("All matching cohort rows",result,result.rows)));
    }
    public static ResearchDocument discoveries(Discoveries.Result result){
        ResearchDocument d=new ResearchDocument("Automatic discoveries");
        d.notes.add("Exploratory recorded win-rate contrasts, not causes or predictions. Requires at least 20 known outcomes per group, the same exact rules and APD version, and non-overlapping multiplicity-adjusted Wilson bands. Wins include ascensions; recorded deaths and abandonment remain distinct non-wins. Imported, unfinished, duplicate and uncertain-outcome records do not contribute.");
        d.notes.add(result.eligible+" eligible finished runs; "+result.excluded+" excluded rows; "+result.comparisons+" comparisons screened. "+result.omitted+" entries omitted by display/group limits. No finding is fabricated when data are insufficient.");
        if(result.findings.isEmpty())d.notes.add("No supported contrasts found in this filtered history. This does not prove that groups are equal.");
        for(Discoveries.Finding f:result.findings)d.cards.add(new Card(f.label,ResearchData.value(f.difference)+" pp",CellText.EXACT,
                "A won "+f.a.wins+" / "+f.a.rows.size()+"; B won "+f.b.wins+" / "+f.b.rows.size()+". Version: "+f.version+"; rules: "+f.rules+". Adjusted Wilson bands: A "+ResearchData.value(100*f.aLow)+" to "+ResearchData.value(100*f.aHigh)+"%; B "+ResearchData.value(100*f.bLow)+" to "+ResearchData.value(100*f.bHigh)+"%. Sample selection, gear, route and player experience can explain associations. Inspect both groups before drawing conclusions.",
                new Evidence("Group A contributors",result.source,f.a.rows),new Evidence("Group B contributors",result.source,f.b.rows)));
        return d;
    }
    public Report report(long time,String version,boolean synthetic){
        Report report=new Report(title,time,version);report.synthetic=synthetic;report.about.add("A read-only snapshot of the selected recorded history.");
        Report.Facts facts=new Report.Facts(Report.Provenance.RECORDED);
        for(String note:notes)facts.add("Method and limits",new Report.Cell(note,CellText.EXACT),"");
        for(Card c:cards){facts.add(c.label,new Report.Cell(c.value,c.state),c.detail);}
        report.add(facts);
        for(Card c:cards)for(Evidence evidence:c.evidence){
            Report.Table table=new Report.Table((c.label+" / "+evidence.label).replace('\n',' ').replace('\r',' '),Report.Provenance.RECORDED);table.headers.add("Record");table.headers.add("Run");
            int count=Math.min(200,evidence.rows.size());for(int i=0;i<count;i++){Row row=evidence.rows.get(i);table.rows.add(new Report.Cell[]{new Report.Cell(row.id,CellText.EXACT),new Report.Cell(row.runId.isEmpty()?"?":row.runId,row.runId.isEmpty()?CellText.UNKNOWN:CellText.EXACT)});}
            table.omitted=evidence.rows.size()-count;report.add(table);
        }
        return report;
    }
}
