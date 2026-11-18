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


package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.research.*;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.watabou.noosa.Game;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Native front end for four read-only research capabilities and their common evidence browser. */
public final class WndUimsdResearch {
    private WndUimsdResearch(){}
    private static final Map<Catalog,ResearchPins> sessionPins=Collections.synchronizedMap(new WeakHashMap<>());
    private static void tell(String title,String message){ShatteredPixelDungeon.scene().addToFront(new WndUimsdText(title,message,false));}
    private static void menu(UimsdModel m,String title,String note,List<WndUimsdList.Entry> entries,List<Runnable> actions,Runnable restore){
        Catalog catalog=m.catalog();Query query=m.query();
        Runnable checkedRestore=()->{if(m.catalog()==catalog && m.query().equals(query))restore.run();else tell(title,"The query or history changed. Open Research lab again for current results.");};
        ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(catalog.synthetic ? "Demo data: "+title:title,note,entries,(i,checked)->{
            if(m.catalog()==catalog && m.query().equals(query))actions.get(i).run();else tell(title,"The query or history changed. Open Research lab again.");
        }).restoreWith(checkedRestore));
    }
    private static void add(List<WndUimsdList.Entry> entries,List<Runnable> actions,String label,String detail,Runnable action){entries.add(WndUimsdList.Entry.item(label,detail));actions.add(action);}
    public static void show(UimsdModel m){
        if(m.catalog()==null){tell("Research lab","History is not ready yet.");return;}
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        add(entries,actions,"Run DNA","Defined combat, healing, equipment and exploration rates; compare two run fingerprints.",()->runs(m,r->chooseRun(m,r,"Run DNA",row->document(m,ResearchDocument.dna(r,RunDna.build(r,row))))));
        add(entries,actions,"Compare Run DNA","Choose run A and run B; inspect both original runs behind every difference.",()->runs(m,r->chooseRun(m,r,"Run A",a->chooseRun(m,r,"Run B",b->document(m,ResearchDocument.dnaComparison(r,RunDna.build(r,a),RunDna.build(r,b)))))));
        add(entries,actions,ComparableProgressEntry.TITLE,"Use persistent population A as the earlier cohort and B as the recent cohort; choose a metric and inspect the exact definitions and contributors.",()->progressMenu(m));
        add(entries,actions,"Luck laboratory","Compare recorded generated loot and enemies against matching finished, random-seed runs. No scoring changes.",()->runs(m,r->chooseRun(m,r,"Luck laboratory",row->presentLuck(m,r,row))));
        add(entries,actions,"Automatic discoveries","Find exploratory win-rate contrasts with sufficient matched data, then inspect the contributors.",()->runs(m,r->work(m,"Automatic discoveries",()->ResearchDocument.discoveries(Discoveries.build(r)),d->document(m,d))));
        add(entries,actions,"Statistical investigation lab","Capture filtered populations A and B, choose a metric, compare samples and inspect evidence.",()->studyMenu(m));
        add(entries,actions,"Saved experiments","Named questions and population definitions; results recalculate from the latest loaded history.",()->savedExperiments(m));
        add(entries,actions,"Damage after equipping","Choose an equipment occurrence and a turn window, then inspect observed damage and its source records.",()->equipmentWindows(m));
        add(entries,actions,"Healing after damage","Choose recorded damage to the hero and inspect exact HP healing in a following turn window.",()->damageRecoveryWindows(m));
        menu(m,"Research lab","These tools read recorded history. Unknowns stay unknown. Demo results are made up and are always identified as demo data.",entries,actions,()->show(m));
    }
    public static boolean canEquipmentWindow(QueryResult r){return r!=null && !r.isGrouped() && EventDatasets.EQUIPMENT.equals(r.def.id);}
    public static void equipmentWindows(UimsdModel m){
        if(!EventDatasets.EQUIPMENT.equals(m.query().dataset)){
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"Open Equipment","Set equipment and run conditions there, then choose Damage after equipping. Conditions from another table cannot be translated implicitly.",()->UimsdScene.showResearchEvidence(m,Query.of(EventDatasets.EQUIPMENT).withScope(m.query().scope).withShowInternal(m.query().showInternal)));
            menu(m,"Damage after equipping","This question starts with one recorded equipment occurrence.",entries,actions,()->equipmentWindows(m));return;
        }
        Catalog cat=m.catalog();Query query=m.query();String rules=m.currentRuleset();
        work(m,"Recorded equipment",()->ResearchData.select(cat,query,rules),r->{
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            int n=0;for(Row row:ResearchData.distinct(r)){
                if(n++>=80)break;
                add(entries,actions,ResearchData.label(r,row,"equip.item")+" / turn "+ResearchData.label(r,row,"equip.start"),"Occurrence "+row.id,()->equipmentWindow(m,r,row));
            }
            if(entries.isEmpty())entries.add(WndUimsdList.Entry.warning("No recorded equipment matches. Older runs may not have equipment observations."));
            menu(m,"Choose equipment occurrence","First 80 matching occurrences, in the current sort order. Narrow the table filters to find another. No equip occurrence is invented.",entries,actions,()->equipmentWindows(m));
        });
    }
    public static void equipmentWindow(UimsdModel m,QueryResult anchors,Row anchor){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(int turns:new int[]{1,5,10,20,50})add(entries,actions,"Next "+turns+" turns","Strictly after the equip turn, through equip turn + "+turns+" inclusive. Includes shield absorption.",()->{
            Catalog cat=m.catalog();String rules=m.currentRuleset();work(m,"Damage after equipping",()->EquipmentWindow.answer(cat,anchors,anchor,turns,rules),d->document(m,d));
        });
        menu(m,"Damage after equipping","Choose the time window. This measures observed damage after an event, not whether the equipment caused it.",entries,actions,()->equipmentWindow(m,anchors,anchor));
    }
    public static boolean canDamageRecoveryWindow(QueryResult r,Row row){
        return r!=null && row!=null && !r.isGrouped() && EventDatasets.DAMAGE.equals(r.def.id)
                && ResearchData.exact(r,row,"damage.targetKind")
                && Objects.equals(ResearchData.number(r,row,"damage.targetKind"),(long)com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents.KIND_HERO);
    }
    public static void damageRecoveryWindows(UimsdModel m){
        if(!EventDatasets.DAMAGE.equals(m.query().dataset)){
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"Open Damage","Set damage and run conditions there, then choose Healing after damage. Conditions from another table cannot be translated implicitly.",()->UimsdScene.showResearchEvidence(m,Query.of(EventDatasets.DAMAGE).withScope(m.query().scope).withShowInternal(m.query().showInternal)));
            menu(m,"Healing after damage","This question starts with one exact damage occurrence against the hero.",entries,actions,()->damageRecoveryWindows(m));return;
        }
        Catalog cat=m.catalog();Query query=m.query();String rules=m.currentRuleset();
        Query heroQuery=query.addFilter(Filter.num("damage.targetKind",Op.EQ,com.shatteredpixel.shatteredpixeldungeon.apd.stats.CombatEvents.KIND_HERO));
        work(m,"Recorded hero damage",()->ResearchData.select(cat,heroQuery,rules),r->{
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            int n=0;for(Row row:ResearchData.distinct(r)){
                if(n++>=80)break;
                add(entries,actions,ResearchData.label(r,row,"damage.source")+" / turn "+ResearchData.label(r,row,"damage.turn"),"Occurrence "+row.id,()->damageRecoveryWindow(m,r,row));
            }
            if(entries.isEmpty())entries.add(WndUimsdList.Entry.warning("No exact recorded hero damage matches. Older runs may not have damage observations."));
            menu(m,"Choose hero-damage occurrence","First 80 matching occurrences, in the current sort order. Narrow the Damage filters to find another. No occurrence is invented.",entries,actions,()->damageRecoveryWindows(m));
        });
    }
    public static void damageRecoveryWindow(UimsdModel m,QueryResult anchors,Row anchor){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(int turns:new int[]{1,5,10,20,50})add(entries,actions,"Next "+turns+" turns","Strictly after the damage turn, through damage turn + "+turns+" inclusive. Counts recorded HP healing, not shielding.",()->{
            Catalog cat=m.catalog();String rules=m.currentRuleset();work(m,"Healing after damage",()->DamageRecoveryWindow.answer(cat,anchors,anchor,turns,rules),d->document(m,d));
        });
        menu(m,"Healing after damage","Choose the time window. This reports observed recovery after an event; it does not claim that the damage caused the heal.",entries,actions,()->damageRecoveryWindow(m,anchors,anchor));
    }

    private static final class Loading extends Window {
        private final Runnable restore;
        Loading(String title,Runnable restore){this.restore=restore;RenderedTextBlock t=PixelScene.renderTextBlock(title+"\nAnalyzing recorded history... Close to cancel.",7);int w=WndUimsdList.windowWidth();t.maxWidth(w-8);t.setPos(4,4);add(t);resize(w,(int)t.bottom()+4);}
        @Override public Runnable restoreOnSceneReset(){return restore;}
    }
    private static <T> void work(UimsdModel m,String title,Callable<T> compute,Consumer<T> done){
        workSnapshot(m,title,compute,done,m.catalog(),m.query());
    }
    private static <T> void workSnapshot(UimsdModel m,String title,Callable<T> compute,Consumer<T> done,Catalog catalog,Query query){
        if(m.catalog()!=catalog || !m.query().equals(query)){tell(title,"The query or history changed. Reopen the analysis.");return;}
        Loading loading=new Loading(title,()->workSnapshot(m,title,compute,done,catalog,query));ShatteredPixelDungeon.scene().addToFront(loading);
        Thread worker=new Thread(()->{T value=null;Throwable failure=null;try{value=compute.call();}catch(Throwable e){failure=e;}
            final T result=value;final Throwable error=failure;
            Game.runOnRenderThread(()->{if(!loading.alive)return;loading.hide();
                if(m.catalog()!=catalog || !m.query().equals(query)){tell(title,"History or the selected query changed. Open the lab again to use the current data.");return;}
                if(error!=null || result==null){tell(title,error==null ? "This analysis cannot use the current schema or filters.":"Analysis could not finish: "+error.getMessage());return;}
                done.accept(result);
            });},"UIMSD-research");worker.setDaemon(true);worker.start();
    }
    private static void runs(UimsdModel m,Consumer<QueryResult> done){
        if(!Datasets.RUNS.equals(m.query().dataset)){
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"Open Runs","Apply any run filters there, then open Research lab. Filters from other datasets are not silently discarded.",()->UimsdScene.showResearchEvidence(m,Query.of(Datasets.RUNS).withScope(m.query().scope).withShowInternal(m.query().showInternal)));
            menu(m,"Run analyses","Run DNA, luck and discoveries use the Runs table. Population comparisons can also use other tables.",entries,actions,()->runs(m,done));return;
        }
        Catalog cat=m.catalog();Query q=m.query();String rules=m.currentRuleset();work(m,"Preparing run analysis",()->ResearchData.select(cat,q,rules),done);
    }
    public static void dnaRun(UimsdModel m,Row row){runs(m,r->{RunDna.Profile p=RunDna.build(r,row);if(p==null)tell("Run DNA","This run is no longer in the selected query.");else document(m,ResearchDocument.dna(r,p));});}
    public static void luckRun(UimsdModel m,Row row){runs(m,r->presentLuck(m,r,row));}
    private static void presentLuck(UimsdModel m,QueryResult r,Row row){
        RunLuck.Result l=RunLuck.build(r,row);
        if(l==null){tell("Luck laboratory","This run is no longer in the selected query.");return;}
        if(!r.query.showInternal && row.finished){
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"Use generation details for finished runs","Turn on hidden-information viewing for the selected filtered query and restrict it to finished runs. No save or history is changed.",()->{
                UimsdScene.showResearchEvidence(m,r.query.withShowInternal(true).withScope(r.query.scope.withFinishedOnly(true)));luckRun(m,row);
            });
            menu(m,"Luck laboratory","Generation facts are hidden. You can explicitly include them for finished runs; unfinished run secrets remain unavailable.",entries,actions,()->presentLuck(m,r,row));return;
        }
        document(m,ResearchDocument.luck(r,l));
    }
    private static void chooseRun(UimsdModel m,QueryResult r,String title,Consumer<Row> picked){chooseRows(m,title,new ResearchDocument.Evidence("Matching runs",r,ResearchData.distinct(r)),0,picked);}
    private static void chooseRows(UimsdModel m,String title,ResearchDocument.Evidence evidence,int page,Consumer<Row> picked){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();int start=page*80,end=Math.min(start+80,evidence.rows.size());
        for(int i=start;i<end;i++){final Row row=evidence.rows.get(i);String label=Datasets.RUNS.equals(evidence.source.def.id) ? ResearchData.label(evidence.source,row,"run.heroClass")+" / "+ResearchData.label(evidence.source,row,"result.outcome")+" / floor "+ResearchData.label(evidence.source,row,"result.deepest"):evidence.source.def.title;
            add(entries,actions,label,"Record "+row.id,()->picked.accept(row));}
        if(page>0)add(entries,actions,"Previous records","",()->chooseRows(m,title,evidence,page-1,picked));
        if(end<evidence.rows.size())add(entries,actions,"Next records","",()->chooseRows(m,title,evidence,page+1,picked));
        menu(m,title,evidence.rows.size()+" source records. Showing "+(evidence.rows.isEmpty()?0:start+1)+"-"+end+". "+(m.catalog().synthetic?"DEMO DATA: made up.":"Recorded history."),entries,actions,()->chooseRows(m,title,evidence,page,picked));
    }
    private static void document(UimsdModel m,ResearchDocument d){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        if(d.cards.isEmpty())entries.add(WndUimsdList.Entry.warning(d.notes.isEmpty() ? "No result for this query.":d.notes.get(d.notes.size()-1)));
        for(ResearchDocument.Card c:d.cards)add(entries,actions,c.label,c.value,()->inspect(m,d,c));
        add(entries,actions,"Export this analysis","Text, Markdown, HTML or CSV; includes definitions, exclusions and capped evidence lists.",()->export(m,d));
        menu(m,d.title,(m.catalog().synthetic?"DEMO DATA (made up).\n":"")+String.join("\n\n",d.notes),entries,actions,()->document(m,d));
    }
    private static void export(UimsdModel m,ResearchDocument d){
        Catalog catalog=m.catalog();Query query=m.query();boolean synthetic=catalog.synthetic;
        WndUimsdReport.show(m,d.title,()->{
            if(m.catalog()!=catalog || !m.query().equals(query))throw new IllegalStateException("The query or history changed; reopen the analysis before exporting.");
            return d.report(System.currentTimeMillis(),Game.version,synthetic);
        });
    }
    private static void inspect(UimsdModel m,ResearchDocument d,ResearchDocument.Card c){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        for(ResearchDocument.Evidence evidence:c.evidence)add(entries,actions,evidence.label,"Inspect "+evidence.rows.size()+" contributing records.",()->chooseRows(m,evidence.label,evidence,0,row->openRow(m,evidence,row)));
        if(!c.evidence.isEmpty()){
            ResearchDocument.Evidence e=c.evidence.get(0);
            if(e.rows.size()==1 && Datasets.RUNS.equals(e.source.def.id)){
                Row row=e.rows.get(0);
                for(String id:new String[]{EventDatasets.ATTACKS,EventDatasets.DAMAGE,EventDatasets.HEALS,EventDatasets.FLOORS,EventDatasets.EQUIPMENT}){
                    Dataset dataset=m.catalog().dataset(id);if(dataset==null || m.catalog().view(id)==null || row.runId.isEmpty())continue;
                    String key=EventDatasets.prefixOf(id)+".run";if(dataset.def.column(key)==null)continue;
                    add(entries,actions,"Recorded "+dataset.def.title,"Browse context events for this run. Event rows may include more than this metric's numerator.",()->
                            UimsdScene.showResearchEvidence(m,Query.of(id).withScope(e.source.query.scope).withShowInternal(e.source.query.showInternal).addFilter(Filter.text(key,Op.GROUP_TEXT,row.runId))));
                }
            }
        }
        add(entries,actions,"Back to analysis","",()->document(m,d));
        menu(m,c.label,c.value+"\n\n"+c.detail,entries,actions,()->inspect(m,d,c));
    }
    private static void openRow(UimsdModel m,ResearchDocument.Evidence evidence,Row row){
        UimsdScene.showResearchEvidence(m,evidence.source.query);
        QueryResult result=m.result();if(result==null || result.isGrouped())return;
        int index=result.rows.indexOf(row);
        if(index<0){tell("Evidence","This record changed or is not in the current result. Reopen the analysis.");return;}
        ResultTable table=ResultTable.build(result,index/50,50);WndUimsdRow.show(m,table.lines.get(index%50));
    }
    private static ResearchPins pins(UimsdModel m)throws IOException{
        if(m.root()!=null)return ResearchPins.load(ResearchPins.file(m.root()));
        ResearchPins pins=sessionPins.get(m.catalog());return pins==null ? new ResearchPins(null,null):pins;
    }
    private static void pin(UimsdModel m,boolean first){
        try{ResearchPins p=pins(m).with(first,m.query(),m.currentRuleset());
            if(m.root()!=null)p.store(ResearchPins.file(m.root()));else sessionPins.put(m.catalog(),p);
            tell("Population "+(first?"A":"B"),"Captured the matching rows before grouping, with the current filters, exact current rules and disclosure choices. "+(m.root()==null?"This session has no history folder, so this pin lasts for this session only.":"The query snapshot is saved; numbers are recalculated from history when compared."));
        }catch(Exception e){tell("Population snapshot",e.getMessage());}
    }
    public static void studyMenu(UimsdModel m){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        add(entries,actions,"Capture current query as population A","Keeps the current dataset, filters, rules and disclosure choices; before grouping and paging.",()->pin(m,true));
        add(entries,actions,"Capture current query as population B","Change table filters first to define the second population.",()->pin(m,false));
        add(entries,actions,"Compare populations A and B","Choose a numeric metric or recorded run win rate. Missing and uncertain values are excluded explicitly.",()->pickMeasure(m));
        add(entries,actions,"Save named experiment","Keep a question, both population definitions and a metric. Matching future records join when history is reloaded.",()->pickMeasure(m,true));
        add(entries,actions,"Saved experiments","Reopen results, inspect populations, rename or delete a saved definition.",()->savedExperiments(m));
        add(entries,actions,"Open population A","Reopen its exact captured query.",()->openPin(m,true));
        add(entries,actions,"Open population B","Reopen its exact captured query.",()->openPin(m,false));
        menu(m,"Statistical investigation lab","Capture A, change your table filters, then capture B. These snapshots are separate from saved views and never change run history.",entries,actions,()->studyMenu(m));
    }
    private static void progressMenu(UimsdModel m){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        add(entries,actions,"Capture current query as earlier cohort (A)","Stores the exact ungrouped query, rules and disclosure choices in persistent population A.",()->pin(m,true));
        add(entries,actions,"Capture current query as recent cohort (B)","Stores the exact ungrouped query, rules and disclosure choices in persistent population B.",()->pin(m,false));
        add(entries,actions,"Compare earlier A with recent B","Choose a metric. Results are recent minus earlier and fail closed unless recorded conditions and dates are comparable.",()->pickProgressMeasure(m));
        add(entries,actions,"Open earlier cohort A","Reopen the exact stored query.",()->openPin(m,true));
        add(entries,actions,"Open recent cohort B","Reopen the exact stored query.",()->openPin(m,false));
        menu(m,ComparableProgressEntry.TITLE,"This reuses the persistent A/B population snapshots from Statistical investigation lab. Capturing here explicitly replaces that A or B definition, never run history. The result is observational and is not proof of improvement or causation.",entries,actions,()->progressMenu(m));
    }
    private static void pickProgressMeasure(UimsdModel m){
        try{
            ResearchPins p=pins(m);
            if(p.a==null || p.b==null){tell(ComparableProgressEntry.TITLE,"Capture both earlier A and recent B first.");return;}
            if(!Datasets.RUNS.equals(p.a.dataset) || !Datasets.RUNS.equals(p.b.dataset)){tell(ComparableProgressEntry.TITLE,"Both stored cohorts must come from the Runs table. No other-table conditions were ignored.");return;}
            Dataset data=m.catalog().dataset(Datasets.RUNS);
            if(data==null){tell(ComparableProgressEntry.TITLE,"The Runs dataset is unavailable in this build.");return;}
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"Win rate (%)","Finished runs with exact outcomes; ascensions count as wins.",()->progress(m,p,PopulationStudy.WIN_RATE));
            for(ColumnDef column:data.def.columns())if(column.type==ColumnDef.Type.NUM && (!column.isInternal() || p.a.showInternal && p.b.showInternal && m.query().showInternal))
                add(entries,actions,column.label,column.description,()->progress(m,p,column.id));
            menu(m,"Progress metric","Recent B minus earlier A. Exact metric values contribute; matching unknown or uncertain rows remain inspectable and are never zero.",entries,actions,()->pickProgressMeasure(m));
        }catch(IOException e){tell(ComparableProgressEntry.TITLE,e.getMessage());}
    }
    private static void progress(UimsdModel m,ResearchPins p,String measure){
        Catalog cat=m.catalog();String rules=m.currentRuleset();boolean hidden=m.query().showInternal;
        work(m,ComparableProgressEntry.TITLE,()->ComparableProgressEntry.open(cat,p,rules,hidden,measure).document(),d->document(m,d));
    }

    private static void openPin(UimsdModel m,boolean first){try{ResearchPins p=pins(m);Query q=first?p.a:p.b;if(q==null)tell("Population","Capture this population first.");else UimsdScene.showResearchEvidence(m,q);}catch(IOException e){tell("Population",e.getMessage());}}
    private static void pickMeasure(UimsdModel m){
        pickMeasure(m,false);
    }
    private static void pickMeasure(UimsdModel m,boolean saving){
        try{ResearchPins p=pins(m);if(p.a==null || p.b==null || !p.a.dataset.equals(p.b.dataset)){tell("Compare populations","Capture both populations from the same dataset first.");return;}
            Dataset data=m.catalog().dataset(p.a.dataset);if(data==null){tell("Compare populations","The captured dataset is no longer available.");return;}
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            if(Datasets.RUNS.equals(data.def.id))add(entries,actions,"Win rate (%)","Finished runs with exactly recorded outcomes; ascensions count as wins.",()->{if(saving)saveExperiment(m,p,PopulationStudy.WIN_RATE);else compare(m,p,PopulationStudy.WIN_RATE);});
            for(ColumnDef column:data.def.columns())if(column.type==ColumnDef.Type.NUM && (!column.isInternal() || p.a.showInternal && p.b.showInternal))add(entries,actions,column.label,column.description,()->{if(saving)saveExperiment(m,p,column.id);else compare(m,p,column.id);});
            menu(m,"Comparison metric","Only exact recorded values contribute. Inspect both contributor lists and exclusions.",entries,actions,()->pickMeasure(m,saving));
        }catch(IOException e){tell("Compare populations",e.getMessage());}
    }
    private static void compare(UimsdModel m,ResearchPins p,String measure){
        Catalog cat=m.catalog();String rules=m.currentRuleset();work(m,"Comparing populations",()->{
            QueryResult a=ResearchData.select(cat,p.a,rules),b=ResearchData.select(cat,p.b,rules);
            PopulationStudy.Comparison c=PopulationStudy.compare(PopulationStudy.sample(a,measure),PopulationStudy.sample(b,measure));
            return c==null ? null:ResearchDocument.study(a,b,c);
        },d->document(m,d));
    }

    private static File experimentFile(UimsdModel m)throws IOException{
        if(m.root()==null)throw new IOException("No local history folder is available. Open your recorded history before saving experiments; a temporary session is not persistent.");
        return SavedExperiments.file(m.root());
    }
    private static void saveExperiment(UimsdModel m,ResearchPins p,String measure){
        try{experimentFile(m);}catch(IOException e){tell("Saved experiments",e.getMessage());return;}
        Catalog cat=m.catalog();Query query=m.query();
        ShatteredPixelDungeon.scene().addToFront(new WndUimsdTextInput("Experiment question","Use a short question as its name (up to 60 characters). Both captured populations and this metric are saved; results recalculate on opening.","",SavedViews.MAX_NAME,false,"Save","Cancel",(positive,text)->{
            if(!positive)return;
            if(m.catalog()!=cat || !m.query().equals(query)){tell("Saved experiments","History or disclosure choices changed. Reopen the lab before saving.");return;}
            try{SavedExperiments.Experiment e=SavedExperiments.create(experimentFile(m),text,text,p.a,p.b,measure,m.currentRuleset(),System.currentTimeMillis());experimentMenu(m,e.id);}
            catch(IOException error){tell("Saved experiments",error.getMessage());}
        }));
    }
    public static void savedExperiments(UimsdModel m){
        try{
            List<SavedExperiments.Experiment> saved=SavedExperiments.load(experimentFile(m));
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            for(SavedExperiments.Experiment e:saved)add(entries,actions,e.name,e.a.dataset+" / "+e.measure,()->experimentMenu(m,e.id));
            if(saved.isEmpty())entries.add(WndUimsdList.Entry.warning("No saved experiments yet. Capture A and B in Statistical investigation lab, then choose Save named experiment."));
            menu(m,"Saved experiments","Definitions stay separate from saved table views and run history. Each results view recalculates using the currently loaded history, including qualifying new records. Demo definitions stay in the demo history folder.",entries,actions,()->savedExperiments(m));
        }catch(IOException e){tell("Saved experiments",e.getMessage());}
    }
    private static SavedExperiments.Experiment experiment(UimsdModel m,String id)throws IOException{
        for(SavedExperiments.Experiment e:SavedExperiments.load(experimentFile(m)))if(e.id.equals(id))return e;
        throw new IOException("This experiment no longer exists. Reopen Saved experiments.");
    }
    private static void experimentMenu(UimsdModel m,String id){
        try{
            SavedExperiments.Experiment e=experiment(m,id);
            List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
            add(entries,actions,"View current results","Recalculate both populations, sample sizes, exclusions, condition differences and uncertainty from current history.",()->{
                Catalog cat=m.catalog();String rules=m.currentRuleset();boolean hidden=m.query().showInternal;
                work(m,e.name,()->ExperimentReport.build(e,e.evaluate(cat,rules,hidden)),d->document(m,d));
            });
            add(entries,actions,"Open population A","Exact saved conditions; no population filters are silently dropped.",()->openExperimentPopulation(m,e.a));
            add(entries,actions,"Open population B","Exact saved conditions; no population filters are silently dropped.",()->openExperimentPopulation(m,e.b));
            add(entries,actions,"Rename experiment","Changes the name, preserving its question, populations and metric.",()->renameExperiment(m,e));
            add(entries,actions,"Delete experiment","Requires confirmation; only this definition is removed.",()->deleteExperiment(m,e));
            menu(m,e.name,"Question: "+e.question+"\nMetric: "+e.measure+"\nCreated: "+new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date(e.createdAtMillis))+"\nFuture matching records join on history reload. Results describe an association, not a guaranteed cause.",entries,actions,()->experimentMenu(m,id));
        }catch(IOException e){tell("Saved experiments",e.getMessage());}
    }
    private static void openExperimentPopulation(UimsdModel m,Query q){
        if(q.showInternal&&!m.query().showInternal){tell("Population","This saved population includes hidden-information viewing. Explicitly enable it in table Options first.");return;}
        if(ResearchData.select(m.catalog(),q,m.currentRuleset())==null){tell("Population","The saved population is unavailable in this build. No filter was ignored.");return;}
        UimsdScene.showResearchEvidence(m,q);
    }
    private static void renameExperiment(UimsdModel m,SavedExperiments.Experiment e){
        Catalog cat=m.catalog();Query q=m.query();
        ShatteredPixelDungeon.scene().addToFront(new WndUimsdTextInput("Rename experiment","The original question and both population definitions are preserved.",e.name,SavedViews.MAX_NAME,false,"Rename","Cancel",(positive,text)->{
            if(!positive)return;if(m.catalog()!=cat || !m.query().equals(q)){tell("Saved experiments","History or disclosure choices changed. Reopen the lab.");return;}
            try{SavedExperiments.rename(experimentFile(m),e.id,text);experimentMenu(m,e.id);}catch(IOException error){tell("Saved experiments",error.getMessage());}
        }));
    }
    private static void deleteExperiment(UimsdModel m,SavedExperiments.Experiment e){
        List<WndUimsdList.Entry> entries=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        add(entries,actions,"Delete this definition","Run history, saved views and other experiments remain available.",()->{try{SavedExperiments.delete(experimentFile(m),e.id);savedExperiments(m);}catch(IOException error){tell("Saved experiments",error.getMessage());}});
        add(entries,actions,"Keep experiment","",()->experimentMenu(m,e.id));
        menu(m,"Delete "+e.name+"?","Only the saved experiment definition will be deleted.",entries,actions,()->deleteExperiment(m,e));
    }
}
