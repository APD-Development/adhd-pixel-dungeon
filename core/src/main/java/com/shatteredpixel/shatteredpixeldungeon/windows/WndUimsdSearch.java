package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdSearch;
import java.util.ArrayList;
import java.util.List;

/**
 * Native, entirely offline UIMSD search input and scrollable results.
 * Search never opens raw logs directly; results navigate through normal
 * UIMSD views and all existing visibility, scope and filtering checks.
 */
public final class WndUimsdSearch {
    private WndUimsdSearch(){}
    public interface Opener { void open(String viewId); }

    public static void open(Catalog catalog, Opener opener) {
        if(catalog==null||opener==null)return;
        ShatteredPixelDungeon.scene().addToFront(new WndTextInput(
                "Search UIMSD", "Search tables, categories, and statistics. Offline; no AI required.",
                "", 120, false, "Search", "Cancel") {
            @Override public void onSelect(boolean confirmed, String value) {
                if(!confirmed || value==null || value.trim().isEmpty())return;
                results(catalog,value,opener);
            }
        });
    }

    public static void results(Catalog catalog,String term,Opener opener){
        List<UimsdSearch.Hit> matches=UimsdSearch.find(catalog,term,35);
        List<WndUimsdList.Entry> choices=new ArrayList<>();
        for(UimsdSearch.Hit hit:matches){
            choices.add(WndUimsdList.Entry.item(hit.title,hit.detail));
        }
        if(choices.isEmpty()){
            ShatteredPixelDungeon.scene().addToFront(new WndMessage(
                    "No matching UIMSD tables for '"+term+"'. Try a mechanic, enemy, item, or statistic. "
                    +"This search covers table descriptions and public fields, not all recorded row values."));
            return;
        }
        ShatteredPixelDungeon.scene().addToFront(WndUimsdList.menu(
                "UIMSD Search: "+matches.size()+" results",
                "Choose a table. Its normal filters and hidden-information rules still apply.",
                choices,(index,checked)->{
                    if(index>=0 && index<matches.size())opener.open(matches.get(index).viewId);
                }).restoreWith(() -> results(catalog,term,opener)));
    }
}
