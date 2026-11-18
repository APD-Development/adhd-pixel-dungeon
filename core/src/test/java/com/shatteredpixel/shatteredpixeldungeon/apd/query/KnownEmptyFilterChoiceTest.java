package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
public class KnownEmptyFilterChoiceTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 private UimsdModel fixture(){
  DatasetDef def=new DatasetDef("empty-fixture","Values","").add(ColumnDef.text("value","Value"));
  List<Row> rows=new ArrayList<>();
  for(String id:new String[]{"empty","other","unknown"}){
   Row row=new Row(id,null,"run-"+id,def.size());row.finished=true;row.ruleset="test";
   if(!id.equals("unknown"))row.setText(0,id.equals("empty")?"":"Rat",Fact.PROV_RECORDED);rows.add(row);
  }
  UimsdModel model=new UimsdModel("test","");model.setCatalog(new Catalog().add(QuerySupport.dataset(def,rows)).add(new ViewDef(def.id,"Values","",Query.of(def.id))));model.showRoot(def.id);return model;
 }
 @Test public void knownEmptyValuesAreOfferedAndUnknownRecordsAreNot(){
  UimsdModel model=fixture();List<UimsdModel.Choice> choices=model.choicesFor(model.dataset().def.column("value"),60);
  assertEquals(2,choices.size());UimsdModel.Choice empty=null;for(UimsdModel.Choice choice:choices)if(choice.value.isEmpty())empty=choice;
  assertNotNull("a known empty actor/value remains selectable",empty);assertEquals("(empty)",empty.label);assertEquals(1,empty.count);
  model.addFilter(Filter.text("value",Op.EQ,empty.value));assertEquals(1,model.result().rows.size());assertEquals("empty",model.result().rows.get(0).id);
 }
 @Test public void selectingAndSavingTheEmptyChoiceDoesNotTurnItIntoAMissingFilter()throws Exception{
  UimsdModel model=fixture();UimsdModel.Choice empty=null;for(UimsdModel.Choice choice:model.choicesFor(model.dataset().def.column("value"),60))if(choice.value.isEmpty())empty=choice;
  assertNotNull(empty);model.addFilter(Filter.text("value",Op.EQ,empty.value));
  SavedViews views=new SavedViews();assertEquals(SavedViews.Result.SAVED,views.save("empty only",model.query(),1));File file=new File(tmp.getRoot(),"views.txt");views.store(file);assertTrue(file.isFile());
  SavedViews saved=SavedViews.load(file);assertEquals(1,saved.list().size());Query restored=saved.list().get(0).query();
  QueryResult result=QueryEngine.run(model.dataset(),restored,"test");assertEquals(1,result.rows.size());assertEquals("empty",result.rows.get(0).id);
 }
}
