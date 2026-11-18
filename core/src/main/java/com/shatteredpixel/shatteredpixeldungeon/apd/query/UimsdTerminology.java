/*
 * Shattered Pixel Dungeon / ADHD Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 * Licensed under the GNU GPL, version 3 or later.
 */
package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Immutable offline vocabulary for UIMSD discovery and the small exact question grammar.
 *
 * Aliases name schema, never recorded values. Extensions return a new vocabulary so tests,
 * plugins and future translated catalogs cannot mutate another active query session.
 */
public final class UimsdTerminology {

    private final Map<String,List<String>> datasets;
    private final Map<String,List<String>> columns;
    private final EnumMap<Question.Measure,List<String>> measures;
    private final List<String> byWords;
    private final List<String> ofWords;

    private static final UimsdTerminology STANDARD = standardBuilder().build();

    private UimsdTerminology(Builder b) {
        datasets=freeze(b.datasets);
        columns=freeze(b.columns);
        measures=new EnumMap<>(Question.Measure.class);
        for(Map.Entry<Question.Measure,List<String>> e:b.measures.entrySet())
            measures.put(e.getKey(),Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        byWords=Collections.unmodifiableList(new ArrayList<>(b.byWords));
        ofWords=Collections.unmodifiableList(new ArrayList<>(b.ofWords));
    }

    private static Map<String,List<String>> freeze(Map<String,List<String>> source) {
        Map<String,List<String>> out=new LinkedHashMap<>();
        for(Map.Entry<String,List<String>> e:source.entrySet())
            out.put(e.getKey(),Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        return Collections.unmodifiableMap(out);
    }

    public static UimsdTerminology standard() {
        return STANDARD;
    }

    public UimsdTerminology withDatasetAliases(String dataset,String... aliases) {
        return new Builder(this).dataset(dataset,aliases).build();
    }

    public UimsdTerminology withColumnAliases(String dataset,String column,String... aliases) {
        return new Builder(this).column(dataset,column,aliases).build();
    }

    public UimsdTerminology withMeasureAliases(Question.Measure measure,String... aliases) {
        return new Builder(this).measure(measure,aliases).build();
    }

    public UimsdTerminology withByWords(String... words) {
        return new Builder(this).by(words).build();
    }

    public UimsdTerminology withOfWords(String... words) {
        return new Builder(this).of(words).build();
    }

    public List<String> datasetAliases(String dataset) {
        List<String> value=datasets.get(dataset);
        return value==null ? Collections.<String>emptyList():value;
    }

    public List<String> columnAliases(String dataset,String column) {
        List<String> value=columns.get(dataset+"\n"+column);
        return value==null ? Collections.<String>emptyList():value;
    }

    public List<String> measureAliases(Question.Measure measure) {
        List<String> value=measures.get(measure);
        return value==null ? Collections.<String>emptyList():value;
    }

    public List<String> byWords() {
        return byWords;
    }

    public List<String> ofWords() {
        return ofWords;
    }

    public String searchAliases(String view,String dataset) {
        LinkedHashSet<String> all=new LinkedHashSet<>();
        all.addAll(datasetAliases(view));
        all.addAll(datasetAliases(dataset));
        StringBuilder out=new StringBuilder();
        for(String alias:all) out.append(' ').append(alias);
        return out.toString();
    }

    public String datasetText(String dataset) {
        return join(datasetAliases(dataset));
    }

    public String columnText(String dataset,String column) {
        return join(columnAliases(dataset,column));
    }

    private static String join(List<String> terms) {
        StringBuilder out=new StringBuilder();
        for(String term:terms) out.append(' ').append(term);
        return out.toString();
    }

    private static Builder standardBuilder() {
        Builder b=new Builder();
        b.measure(Question.Measure.COUNT,"how many","count","cuantos","combien de","wie viele");
        b.measure(Question.Measure.TOTAL,"total","sum","suma","somme","gesamt");
        b.measure(Question.Measure.AVERAGE,"average","mean","promedio","moyenne","durchschnitt");
        b.measure(Question.Measure.HIGHEST,"highest","maximum","maximo","maximum","hochste");
        b.measure(Question.Measure.LOWEST,"lowest","minimum","minimo","minimum","niedrigste");
        b.measure(Question.Measure.TYPICAL,"median","typical","mediana","mediane","median");
        b.by("by","por","par","nach");
        b.of("of","de","des","von");

        b.dataset("deaths","deaths","died","dying","mortality","muerte","muertes","morir","mort","deces",
                "morti","tod","sterben","смерть","смерти","умер","смертью","死亡","死","去世");
        b.dataset("outcomes","deaths","victory","win","wins","muerte","victoire","sieg","победа","胜利","勝利","승리");
        b.dataset("runs","run","runs","game","games","victory","win","wins","carreras","partidas","parties",
                "siege","sieg","победа","забег","прохождение","胜利","游戏","游戏局","記録","勝利","승리");
        b.dataset("attacks","attack","attacks","fight","fighting","combat","combate","ataques","attaques",
                "angriffe","kampf","атаки","атака","бой","攻击","攻擊","戦闘","攻撃","전투","공격");
        b.dataset("damage","damage","hurt","daño","degats","dégats","schaden","урон","伤害","ダメージ","피해");
        b.dataset("items","item","items","gear","loot","inventory","objetos","equipo","armas","items","objets",
                "equipement","équipement","gegenstände","ausrüstung","предмет","вещи","оружие","物品","装备","裝備","アイテム","装備","아이템","장비");
        b.dataset("equipment","equipment","gear","weapon","armor","wand","worn","equipo","armas","equipement",
                "équipement","ausrüstung","waffen","оружие","装备","裝備","装備","장비");
        b.dataset("bosses","boss","bosses","jefes","patrons","босс","首领","首領","ボス","보스");
        b.dataset("enemies","enemy","enemies","monster","monsters","enemigos","ennemis","gegner","feinde","враги","敌人","敵","적");
        b.dataset("kills","kill","kills","killed","asesinatos","tues","getotet","убийство","击杀","キル","처치");
        b.dataset("floors","floor","floors","level","levels","piso","pisos","etage","étage","etages","étages",
                "stockwerk","stockwerke","этаж","этажи","楼层","階層","층");
        b.dataset("steps","step","steps","movement","travel","pasos","schritte","шаги","步数","歩数","걸음");
        b.dataset("exploration","exploration","travel","movement","exploracion","exploration","erkundung","исследование","探索","탐험");

        b.column("damage","damage.amount","damage","daño","degats","dégats","schaden","урон","伤害","ダメージ","피해");
        b.column("attacks","attack.weapon","weapon","arma","arme","waffe","оружие","武器","무기");
        b.column("equipment","equip.item","item","equipment","objeto","objet","gegenstand","предмет","物品","アイテム","아이템");
        b.column("floors","floor.depth","floor","piso","etage","étage","stockwerk","этаж","楼层","階層","층");
        return b;
    }

    public static final class Builder {
        private final Map<String,List<String>> datasets=new LinkedHashMap<>();
        private final Map<String,List<String>> columns=new LinkedHashMap<>();
        private final EnumMap<Question.Measure,List<String>> measures=new EnumMap<>(Question.Measure.class);
        private final List<String> byWords=new ArrayList<>();
        private final List<String> ofWords=new ArrayList<>();

        public Builder(){}

        private Builder(UimsdTerminology source) {
            copy(source.datasets,datasets);
            copy(source.columns,columns);
            for(Map.Entry<Question.Measure,List<String>> e:source.measures.entrySet())
                measures.put(e.getKey(),new ArrayList<>(e.getValue()));
            byWords.addAll(source.byWords);
            ofWords.addAll(source.ofWords);
        }

        private static void copy(Map<String,List<String>> from,Map<String,List<String>> to) {
            for(Map.Entry<String,List<String>> e:from.entrySet()) to.put(e.getKey(),new ArrayList<>(e.getValue()));
        }

        public Builder dataset(String dataset,String... aliases) {
            add(datasets,dataset,aliases);
            return this;
        }

        public Builder column(String dataset,String column,String... aliases) {
            add(columns,dataset+"\n"+column,aliases);
            return this;
        }

        public Builder measure(Question.Measure measure,String... aliases) {
            List<String> out=measures.get(measure);
            if(out==null){out=new ArrayList<>();measures.put(measure,out);}
            append(out,aliases);
            return this;
        }

        public Builder by(String... words) {
            append(byWords,words);
            return this;
        }

        public Builder of(String... words) {
            append(ofWords,words);
            return this;
        }

        private static void add(Map<String,List<String>> map,String key,String... values) {
            if(key==null || key.isEmpty()) throw new IllegalArgumentException("empty terminology key");
            List<String> out=map.get(key);
            if(out==null){out=new ArrayList<>();map.put(key,out);}
            append(out,values);
        }

        private static void append(List<String> out,String... values) {
            if(values==null) return;
            for(String value:values) if(value!=null && !value.trim().isEmpty() && !out.contains(value)) out.add(value);
        }

        public UimsdTerminology build() {
            return new UimsdTerminology(this);
        }
    }
}
