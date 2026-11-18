package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.util.*;
public class ResearchFixture {
    public final DatasetDef def;
    public final List<Row> rows=new ArrayList<>();
    public ResearchFixture(){DatasetDef found=null;for(DatasetDef d:Datasets.defs())if(d.id.equals(Datasets.RUNS))found=d;def=found;if(def==null)throw new AssertionError();}
    public Row add(String id,String kind,int outcome,long score){
        Row r=new Row(id,null,"run-"+id,def.size());r.finished=true;r.ruleset="rules";r.telemetry="1.12";
        text(r,"run.id",r.runId);text(r,"run.heroClass",kind);text(r,"run.apdVersion","version-1");text(r,CustomRuleFacts.IDENTITY,"default");
        num(r,"result.outcome",outcome);num(r,"result.score",score);num(r,"result.turns",100);num(r,"result.deepest",5);num(r,"journey.levelsEntered",5);
        num(r,"combat.heroAttacks",20);num(r,"combat.heroHits",10);num(r,"combat.attacksOnHero",20);num(r,"combat.hitsOnHero",5);
        num(r,"combat.damageDealt",200);num(r,"combat.damageTaken",100);num(r,"combat.healed",50);num(r,"combat.heroKills",5);num(r,"items.equipEvents",2);num(r,"items.acquired",10);
        num(r,"gen.levels",5);num(r,"run.seedType",0);num(r,"run.seed",rows.size()+1);num(r,CustomRuleFacts.GENERATED_UPGRADES,3);num(r,CustomRuleFacts.GENERATED_EQUIPMENT,2);num(r,"gen.items",20);num(r,"gen.mobs",30);
        rows.add(r);return r;
    }
    public void num(Row r,String key,long value){int i=def.indexOf(key);if(i<0)throw new AssertionError(key);r.setNum(i,value,Fact.PROV_RECORDED);}
    public void text(Row r,String key,String value){int i=def.indexOf(key);if(i<0)throw new AssertionError(key);r.setText(i,value,Fact.PROV_RECORDED);}
    public Query query(){return Query.of(Datasets.RUNS).withScope(new Scope(Scope.Rules.ALL,"",true,true,true,true)).withShowInternal(true);}
    public Catalog catalog(){List<Dataset.RunInfo> runs=new ArrayList<>();for(Row row:rows)runs.add(new Dataset.RunInfo(row.runId,row.finished,row.ruleset,true,row.duplicate));Catalog c=new Catalog();c.synthetic=true;c.add(new Dataset(def,rows,runs));c.add(new ViewDef(Datasets.RUNS,"Runs","Recorded test history",query()));return c;}
    public QueryResult result(){return result(query());}
    public QueryResult result(Query q){return ResearchData.select(catalog(),q,"rules");}
    public Query forClass(String name){return query().addFilter(Filter.text("run.heroClass",Op.GROUP_TEXT,name));}
}
