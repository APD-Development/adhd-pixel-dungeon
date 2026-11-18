package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import com.shatteredpixel.shatteredpixeldungeon.apd.stats.Fact;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/** Offline text matching never reads stored row values or hidden/internal fields. */
public class UimsdSearchTest {
    private static Catalog fixture() {
        Catalog catalog=new Catalog();
        DatasetDef dead=new DatasetDef("deaths","Deaths","How adventurers died and on which floor")
                .add(ColumnDef.num("death.floor","Floor").shown())
                .add(ColumnDef.text("death.cause","Cause of death").shown())
                .add(ColumnDef.text("death.secret","Unidentified potion true name").internal());
        DatasetDef fight=new DatasetDef("attacks","Attacks","Hero and monster combat")
                .add(ColumnDef.num("attack.damage","Damage").shown())
                .add(ColumnDef.text("attack.element","Bleeding and Burning").shown());
        DatasetDef gear=new DatasetDef("items","Items","Equipment, scrolls, wands, potions")
                .add(ColumnDef.text("item.name","Name").shown());
        Row secret=new Row("one","","run1",dead.size());
        secret.setText(dead.indexOf("death.cause"),"private-actual-row-value",Fact.PROV_RECORDED);
        catalog.add(new Dataset(dead,java.util.Collections.singletonList(secret),new ArrayList<>()));
        catalog.add(new Dataset(fight,new ArrayList<>(),new ArrayList<>()));
        catalog.add(new Dataset(gear,new ArrayList<>(),new ArrayList<>()));
        catalog.add(new ViewDef("deaths","Deaths","Recorded deaths",Query.of("deaths")).group("Runs"));
        catalog.add(new ViewDef("attacks","Attacks","All attacks and damage",Query.of("attacks")).group("Play by play"));
        catalog.add(new ViewDef("items","Items","Every collected item",Query.of("items")).group("Items"));
        return catalog;
    }
    private static String first(Catalog c,String q){
        List<UimsdSearch.Hit> r=UimsdSearch.find(c,q,10);
        assertFalse("Expected result for "+q,r.isEmpty());
        return r.get(0).viewId;
    }

    @Test public void exactTitleRanksBeforeColumnMentions() {
        Catalog c=fixture();
        assertEquals("deaths",first(c,"deaths"));
        assertEquals("attacks",first(c,"attacks"));
        assertEquals("items",first(c,"items"));
    }
    @Test public void accentedTextAndUppercaseFullwidthNormalizeWithoutCallingInternet(){
        assertEquals("deces",UimsdSearch.normalize("DÉCÈS"));
        assertEquals("attacks",UimsdSearch.normalize("ＡＴＴＡＣＫＳ"));
        assertEquals("deaths",first(fixture(),"DÉCÈS"));
    }
    @Test public void multipleRealLanguagesFindSameRegisteredView(){
        Catalog c=fixture();
        for(String language:new String[]{"muerte","tod","смерть","死亡"})assertEquals(language,"deaths",first(c,language));
        for(String language:new String[]{"combate","angriffe","атаки","攻撃"})assertEquals(language,"attacks",first(c,language));
        for(String language:new String[]{"objetos","оружие","アイテム"})assertEquals(language,"items",first(c,language));
    }
    @Test public void multipleTermsAllRequiredInsteadOfReturningUnrelatedTables(){
        Catalog c=fixture();
        assertEquals("attacks",first(c,"combat damage"));
        assertTrue(UimsdSearch.find(c,"combat death",20).isEmpty());
    }
    @Test public void publicColumnNamesSearchableButSecretColumnsAndRowValuesAreNot(){
        Catalog c=fixture();
        assertEquals("attacks",first(c,"Bleeding"));
        assertTrue("Do not disclose row-specific information",UimsdSearch.find(c,"private actual row value",15).isEmpty());
        assertTrue("Do not advertise hidden column metadata",UimsdSearch.find(c,"Unidentified potion true name",15).isEmpty());
    }
    @Test public void queriesAreReadOnlyAndBounded(){
        Catalog c=fixture();
        assertTrue(UimsdSearch.find(c,"",10).isEmpty());
        assertTrue(UimsdSearch.find(c,null,10).isEmpty());
        assertTrue(UimsdSearch.find(null,"attacks",10).isEmpty());
        assertTrue(UimsdSearch.find(c,"attack",0).isEmpty());
        assertEquals(1,UimsdSearch.find(c,"a",1).size());
        assertEquals("run1",c.dataset("deaths").rows.get(0).runId);
    }
    @Test public void matchingUnicodeLabelsWorksWithoutTranslations(){
        Catalog c=fixture();
        DatasetDef d=new DatasetDef("local","地牢记录","可浏览的本地记录")
                .add(ColumnDef.num("local.turn","回合").shown());
        c.add(new Dataset(d,new ArrayList<>(),new ArrayList<>()));
        c.add(new ViewDef("local","地牢记录","本地记录",Query.of("local")));
        assertEquals("local",first(c,"地牢"));
    }
}
