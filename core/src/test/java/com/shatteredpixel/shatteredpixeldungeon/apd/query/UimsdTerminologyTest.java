package com.shatteredpixel.shatteredpixeldungeon.apd.query;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

/** Extensible offline vocabulary remains schema-only, immutable and fail-closed. */
public class UimsdTerminologyTest {

    private Catalog catalog(){
        DatasetDef def=new DatasetDef("spells","Spell casts","Recorded spell events");
        def.add(ColumnDef.num("mana","Mana spent").shown());
        def.add(ColumnDef.text("spell","Spell").shown());
        def.add(ColumnDef.text("secret","True spell identity").internal());
        Row row=new Row("one","", "run",def.size());
        Catalog catalog=new Catalog().add(new Dataset(def,Collections.singletonList(row),Collections.emptyList()));
        catalog.add(new ViewDef("spell-view","Spell casts","Recorded spells",Query.of("spells")));
        return catalog;
    }

    private UimsdTerminology spanish(){
        return UimsdTerminology.standard()
                .withDatasetAliases("spells","incantaciones")
                .withColumnAliases("spells","mana","mana gastado")
                .withColumnAliases("spells","spell","conjuro");
    }

    @Test public void registeredDatasetAndColumnAliasesDriveTheExactExistingQuestion(){
        Catalog catalog=catalog();
        Question q=UimsdTopicSearch.interpret(catalog,
                "promedio maná gastado de incantaciones por conjuro",Question.about("old"),spanish());
        assertNotNull(q);
        assertEquals("spells",q.dataset);
        assertEquals(Question.Measure.AVERAGE,q.measure);
        assertEquals("mana",q.column);
        assertEquals(Collections.singletonList("spell"),q.groupBy);
        assertEquals(Aggregate.Fn.AVG,Questions.toQuery(q,catalog.dataset("spells").def).group.aggregates.get(0).fn);
    }

    @Test public void extensionsAreImmutableAndDoNotLeakIntoStandardVocabulary(){
        Catalog catalog=catalog();
        assertNull(UimsdTopicSearch.interpret(catalog,"promedio mana gastado de incantaciones",
                Question.about("old"),UimsdTerminology.standard()));
        assertNotNull(UimsdTopicSearch.interpret(catalog,"promedio mana gastado de incantaciones",
                Question.about("old"),spanish()));
        assertNull(UimsdTopicSearch.interpret(catalog,"promedio mana gastado de incantaciones",
                Question.about("old"),UimsdTerminology.standard()));
    }

    @Test public void discoveryAndViewSearchUseTheSameRegisteredSchemaTerms(){
        Catalog catalog=catalog();UimsdTerminology terms=spanish();
        assertEquals("spells",UimsdTopicSearch.find(catalog,"incantaciones",terms).get(0).datasetId);
        assertEquals("mana",UimsdTopicSearch.find(catalog,"mana gastado",terms).get(0).columnId);
        assertEquals("spell-view",UimsdSearch.find(catalog,"incantaciones",10,terms).get(0).viewId);
    }

    @Test public void internalAliasesNeverMakeAHiddenColumnDiscoverableOrAskable(){
        Catalog catalog=catalog();
        UimsdTerminology terms=spanish().withColumnAliases("spells","secret","identidad verdadera");
        assertTrue(UimsdTopicSearch.find(catalog,"identidad verdadera",terms).isEmpty());
        assertNull(UimsdTopicSearch.interpret(catalog,"total identidad verdadera de incantaciones",
                Question.about("old").withShowInternal(true),terms));
    }

    @Test public void ambiguousAliasesAndUnsupportedConditionsFailClosed(){
        Catalog catalog=catalog();
        DatasetDef other=new DatasetDef("rituals","Rituals","");
        other.add(ColumnDef.num("mana","Mana").shown());
        catalog.add(new Dataset(other,Collections.singletonList(new Row("two","", "run",other.size())),Collections.emptyList()));
        UimsdTerminology terms=spanish().withDatasetAliases("rituals","incantaciones");
        assertNull(UimsdTopicSearch.interpret(catalog,"promedio mana de incantaciones",Question.about("old"),terms));
        assertNull(UimsdTopicSearch.interpret(catalog,"promedio mana gastado de incantaciones por conjuro desde ayer",
                Question.about("old"),spanish()));
    }
}
