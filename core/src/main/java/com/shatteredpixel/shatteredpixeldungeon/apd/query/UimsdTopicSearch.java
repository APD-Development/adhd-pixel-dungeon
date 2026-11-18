/*
 * Shattered Pixel Dungeon / ADHD Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 * Licensed under the GNU GPL, version 3 or later.
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// Offline discovery of *schema*, not arbitrary recorded values.
// A topic result opens the existing Questions builder. Nothing is inferred,
// transmitted, or searched in hidden cells or raw gameplay history.
public final class UimsdTopicSearch {
    private UimsdTopicSearch(){}

    private static final int LIMIT = 48;

    public static final class Hit {
        public final String datasetId;
        public final String columnId; // "" = the dataset itself
        public final String label;
        public final String detail;

        private Hit(String datasetId, String columnId, String label, String detail) {
            this.datasetId = datasetId;
            this.columnId = columnId;
            this.label = label;
            this.detail = detail;
        }

        public Question question(Question previous, DatasetDef definition) {
            Question q = previous.withDataset(datasetId);
            if (columnId.isEmpty()) return q;
            ColumnDef c = definition.column(columnId);
            if (c == null || c.isInternal()) return q;
            if (c.type == ColumnDef.Type.NUM)
                return q.withMeasure(Question.Measure.TOTAL, columnId);
            if (Questions.groupable(definition, false).contains(c))
                return q.withGroupBy(Collections.singletonList(columnId));
            return q;
        }
    }

    public static List<Hit> find(Catalog catalog, String words) {
        return find(catalog,words,UimsdTerminology.standard());
    }

    public static List<Hit> find(Catalog catalog, String words, UimsdTerminology terminology) {
        if (catalog == null || words == null || words.trim().isEmpty() || terminology == null)
            return Collections.emptyList();
        String query = normalize(words);
        if (query.isEmpty()) return Collections.emptyList();
        String[] terms = query.split(" ");
        List<Hit> hits = new ArrayList<>();
        for (Dataset dataset : Questions.askable(catalog)) {
            DatasetDef def = dataset.def;
            if (matches(terms, def.title + " " + def.countLabel + " " + def.description + " " + def.id
                    + " " + terminology.datasetText(def.id))) {
                hits.add(new Hit(def.id, "", def.title, "Ask about " + def.countLabel.toLowerCase(Locale.ROOT)));
                if (hits.size() == LIMIT) return hits;
            }
            for (ColumnDef column : def.columns()) {
                if (column.isInternal()) continue;
                if (!matches(terms, column.label + " " + column.description + " " + column.id
                        + " " + terminology.columnText(def.id,column.id))) continue;
                String action = column.type == ColumnDef.Type.NUM ? "Start with its total" : "Start with a breakdown";
                hits.add(new Hit(def.id, column.id, def.title + " / " + column.label, action));
                if (hits.size() == LIMIT) return hits;
            }
        }
        return hits;
    }

    public static Question interpret(Catalog catalog, String words, Question previous) {
        return interpret(catalog,words,previous,UimsdTerminology.standard());
    }

    // A deliberately small, deterministic offline grammar. Only exact public schema
    // labels or registered aliases are accepted. Unsupported phrases fail closed.
    public static Question interpret(Catalog catalog, String words, Question previous, UimsdTerminology terminology) {
        if (catalog == null || words == null || previous == null || terminology == null) return null;
        String phrase = normalize(words);
        ParsedMeasure parsed=parseMeasure(phrase,terminology);
        if(parsed==null || parsed.tail.isEmpty()) return null;
        Question.Measure measure=parsed.measure;
        String tail=parsed.tail;

        Split groupSplit=splitOnce(tail,terminology.byWords());
        if(groupSplit.invalid)return null;
        String group=groupSplit.found ? groupSplit.right:"";
        if(groupSplit.found)tail=groupSplit.left;
        if(tail.isEmpty() || (groupSplit.found && group.isEmpty()))return null;

        String measured="";
        if(measure!=Question.Measure.COUNT){
            Split ofSplit=splitOnce(tail,terminology.ofWords());
            if(ofSplit.invalid || !ofSplit.found || ofSplit.left.isEmpty() || ofSplit.right.isEmpty())return null;
            measured=ofSplit.left;tail=ofSplit.right;
        }

        Dataset selected=null;
        for(Dataset ds:Questions.askable(catalog)){
            DatasetDef def=ds.def;
            if(!matchesExact(tail,def.title,def.countLabel,def.unitSingular(),terminology.datasetAliases(def.id)))continue;
            if(selected!=null)return null;
            selected=ds;
        }
        if(selected==null)return null;
        DatasetDef def=selected.def;
        ColumnDef measuredColumn=null;
        if(measure!=Question.Measure.COUNT){
            for(ColumnDef col:Questions.measurable(def,false)){
                if(!matchesExact(measured,col.label,null,null,terminology.columnAliases(def.id,col.id)))continue;
                if(measuredColumn!=null)return null;
                measuredColumn=col;
            }
            if(measuredColumn==null)return null;
        }
        ColumnDef grouping=null;
        if(!group.isEmpty()){
            for(ColumnDef col:Questions.groupable(def,false)){
                if(!matchesExact(group,col.label,null,null,terminology.columnAliases(def.id,col.id)))continue;
                if(grouping!=null)return null;
                grouping=col;
            }
            if(grouping==null)return null;
        }
        Question q=Question.about(def.id).withScope(previous.scope);
        if(measuredColumn!=null)q=q.withMeasure(measure,measuredColumn.id);
        if(grouping!=null)q=q.withGroupBy(Collections.singletonList(grouping.id));
        return q;
    }

    private static final class ParsedMeasure {
        final Question.Measure measure;final String tail;
        ParsedMeasure(Question.Measure measure,String tail){this.measure=measure;this.tail=tail;}
    }

    private static ParsedMeasure parseMeasure(String phrase,UimsdTerminology terminology){
        ParsedMeasure found=null;int longest=-1;
        for(Question.Measure measure:Question.Measure.values())for(String raw:terminology.measureAliases(measure)){
            String prefix=normalize(raw);
            if(prefix.isEmpty() || !phrase.startsWith(prefix+" "))continue;
            if(prefix.length()>longest){found=new ParsedMeasure(measure,phrase.substring(prefix.length()+1));longest=prefix.length();}
            else if(prefix.length()==longest && found!=null && found.measure!=measure)return null;
        }
        return found;
    }

    private static final class Split {
        final boolean found,invalid;final String left,right;
        Split(boolean found,boolean invalid,String left,String right){this.found=found;this.invalid=invalid;this.left=left;this.right=right;}
    }

    private static Split splitOnce(String text,List<String> words){
        int at=-1,length=0;
        for(String raw:words){
            String word=normalize(raw);if(word.isEmpty())continue;
            String needle=" "+word+" ";int first=text.indexOf(needle);
            if(first<0)continue;
            if(text.indexOf(needle,first+needle.length())>=0 || at>=0)return new Split(false,true,"","");
            at=first;length=needle.length();
        }
        return at<0 ? new Split(false,false,text,""):new Split(true,false,text.substring(0,at),text.substring(at+length));
    }

    private static boolean matchesExact(String wanted,String a,String b,String c,List<String> aliases){
        if(a!=null && wanted.equals(normalize(a)))return true;
        if(b!=null && wanted.equals(normalize(b)))return true;
        if(c!=null && wanted.equals(normalize(c)))return true;
        for(String alias:aliases)if(wanted.equals(normalize(alias)))return true;
        return false;
    }

    private static boolean matches(String[] terms, String haystack) {
        String text = normalize(haystack);
        for (String term : terms) if (!text.contains(term)) return false;
        return true;
    }

    private static String normalize(String text) {
        return UimsdSearch.normalize(text);
    }
}
