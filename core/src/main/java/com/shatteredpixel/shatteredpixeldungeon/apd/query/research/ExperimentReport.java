package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import java.util.*;

/** The same calculation/evidence document drives the phone view and all existing exports. */
public final class ExperimentReport {
    private ExperimentReport(){}
    public static ResearchDocument build(SavedExperiments.Experiment e,SavedExperiments.Evaluation result){
        ResearchDocument d=new ResearchDocument(e.name);
        d.notes.add("Question: "+e.question);
        d.notes.add("The question is your label. The actual calculation uses the saved metric and exact population definitions shown below; no extra conditions are inferred from its wording.");
        d.notes.add("Saved definitions, recalculated from the currently loaded history each time you view results. Future matching records join automatically when history is reloaded. This is an observational comparison, not a randomized experiment or proof of causation.");
        if(!result.problem.isEmpty()){d.notes.add(result.problem);return d;}
        ResearchDocument study=ResearchDocument.study(result.a,result.b,result.comparison);
        d.notes.addAll(study.notes);d.cards.addAll(study.cards);
        definition(d,"Population A definition",result.a);
        definition(d,"Population B definition",result.b);
        conditions(d,"Population A conditions",result.a,result.comparison.a);
        conditions(d,"Population B conditions",result.b,result.comparison.b);
        return d;
    }
    private static void definition(ResearchDocument d,String label,QueryResult r){
        Query q=r.query;List<String> lines=new ArrayList<>();
        lines.add("Dataset: "+q.dataset+"; exact stored query: "+QueryCodec.encode(q));
        lines.add("Rules scope: "+q.scope.rules+" "+q.scope.ruleset+". Hidden-information choice: "+q.showInternal+".");
        for(Filter f:q.filters)lines.add(StatusText.describe(r.def,f));
        if(q.filters.isEmpty())lines.add("No column filters.");
        d.cards.add(new ResearchDocument.Card(label,r.rows.size()+" matching records",CellText.EXACT,String.join("\n",lines),
                new ResearchDocument.Evidence("Matching records (includes excluded metric values)",r,r.rows)));
    }
    private static void conditions(ResearchDocument d,String title,QueryResult r,PopulationStudy.Sample sample){
        List<String> lines=new ArrayList<>();Map<String,Integer> rules=new TreeMap<>();int unknownRules=0;
        for(Row row:sample.contributors){if(row.ruleset==null || row.ruleset.isEmpty() || Row.RULESET_MIXED.equals(row.ruleset))unknownRules++;else rules.merge(row.ruleset,1,Integer::sum);}
        lines.add("Exact rules identities: "+summary(rules)+"; unknown/mixed="+unknownRules);
        for(String key:new String[]{"run.apdVersion","run.heroClass","final.subClass","run.difficulty","run.difficultyBalance","run.challenges",com.shatteredpixel.shatteredpixeldungeon.apd.stats.CustomRuleFacts.IDENTITY}){
            ColumnDef col=r.def.column(key);if(col==null)continue;
            Map<String,Integer> values=new TreeMap<>();int unknown=0;
            for(Row row:sample.contributors){if(!ResearchData.exact(r,row,key)){unknown++;continue;}String value=ResearchData.label(r,row,key);values.merge(value,1,Integer::sum);}
            lines.add(col.label+": "+summary(values)+"; unknown/uncertain="+unknown);
        }
        lines.add("These are recorded conditions of exact metric contributors, not proof that all relevant conditions match. Equipment, route, player choices and unrecorded facts can differ. Compare both sides; unknowns are not assumed equal.");
        d.cards.add(new ResearchDocument.Card(title,sample.n+" exact metric contributors",sample.n==0?CellText.UNKNOWN:CellText.EXACT,String.join("\n",lines),new ResearchDocument.Evidence("Condition source records",r,sample.contributors)));
    }
    private static String summary(Map<String,Integer> values){
        if(values.isEmpty())return "none recorded";
        List<String> out=new ArrayList<>();int i=0;for(Map.Entry<String,Integer> e:values.entrySet()){if(i++==6)break;out.add((e.getKey().isEmpty()?"(empty)":e.getKey())+"="+e.getValue());}
        if(values.size()>6)out.add("+"+(values.size()-6)+" more values; inspect sources");return String.join(", ",out);
    }
}
