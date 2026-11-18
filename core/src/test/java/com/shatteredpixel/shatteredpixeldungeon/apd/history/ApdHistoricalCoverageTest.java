package com.shatteredpixel.shatteredpixeldungeon.apd.history;
import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.ChangeIcons;
import static org.junit.Assert.*;
public class ApdHistoricalCoverageTest {
 private Path root() {
  Path p=Paths.get(System.getProperty("apd.game.src")).toAbsolutePath();
  while(p!=null&&!Files.isRegularFile(p.resolve("docs/APD-HISTORY-AUDIT-2026-10-12.json")))p=p.getParent();
  assertNotNull("Historical evidence manifest must exist",p);return p;
 }
 private JSONObject audit()throws Exception{return new JSONObject(Files.readString(root().resolve("docs/APD-HISTORY-AUDIT-2026-10-12.json")));}
 private Path assets(){return root().resolve("core/src/main/assets/apd/history");}
 private Map<String,ApdHistory.Build> builds()throws Exception{
  Map<String,ApdHistory.Build> out=new HashMap<>();for(ApdHistory.Build b:ApdHistory.builds(Files.readString(assets().resolve("index.tsv"))))assertNull(out.put(b.id,b));return out;
 }
 @Test public void everyHistoricalNumberHasItsOwnNamespaceAndHonestAvailability()throws Exception{
  Map<String,ApdHistory.Build> index=builds();JSONArray records=audit().getJSONArray("records");Set<String> seen=new HashSet<>();int complete=0,none=0,alias=0;
  for(int i=0;i<records.length();i++){
   JSONObject r=records.getJSONObject(i);String id=r.getString("id");assertTrue(seen.add(id));assertTrue(index.containsKey(id));assertEquals(r.getString("label"),index.get(id).label);
   List<ApdHistory.Entry> entries=ApdHistory.entries(Files.readString(assets().resolve(id+".tsv")));assertFalse(entries.isEmpty());
   switch(r.getString("status")){
    case "complete":complete++;assertTrue(r.getString("source").matches("[a-f0-9]{40}"));assertTrue(r.getString("apk_sha256").matches("[a-f0-9]{64}"));break;
    case "no_apk":none++;assertTrue(entries.get(0).text.contains("retained APK"));break;
    case "delivery_alias":alias++;assertEquals("private-test-1",r.getString("actual_apk"));break;
    default:fail("Unreviewed historical status "+id);
   }
   for(ApdHistory.Entry e:entries)assertFalse(id,e.text.toLowerCase(Locale.ROOT).contains("not yet been reconstructed"));
  }
  assertEquals(132,complete);assertEquals(21,none);assertEquals(1,alias);assertEquals(154,seen.size());
  for(int n=1;n<=84;n++)assertTrue(seen.contains("build-"+n));
  for(int n=1;n<=67;n++)assertTrue(seen.contains("codex-test-"+n));
  for(int n=1;n<=3;n++)assertTrue(seen.contains("private-test-"+n));
 }
 @Test public void everyChangedFileIsCoveredAndEveryHistoricalRecordMatchesItsReviewedFingerprint()throws Exception{
  JSONArray records=audit().getJSONArray("records");int files=0;
  for(int i=0;i<records.length();i++){
   JSONObject r=records.getJSONObject(i);byte[] bytes=Files.readAllBytes(assets().resolve(r.getString("id")+".tsv"));
   StringBuilder hex=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hex.append(String.format(Locale.ROOT,"%02x",b&255));assertEquals(r.getString("id"),r.getString("history_sha256"),hex.toString());
   if(!r.has("files"))continue;
   int entryCount=ApdHistory.entries(new String(bytes,StandardCharsets.UTF_8)).size();assertEquals(r.getInt("entry_count"),entryCount);
   JSONArray changed=r.getJSONArray("files");Set<String> paths=new HashSet<>();
   for(int f=0;f<changed.length();f++){
    JSONObject file=changed.getJSONObject(f);assertTrue(paths.add(file.getString("path")));JSONArray refs=file.getJSONArray("entries");assertTrue(refs.length()>0);
    for(int k=0;k<refs.length();k++)assertTrue(refs.getInt(k)>=0&&refs.getInt(k)<entryCount);
    files++;
   }
  }
  assertEquals(1902,files);
 }
 @Test public void allHistoricalPopupsUseNativeSpritesConciseTextAndNoPrivateRepositoryLinks()throws Exception{
  for(ApdHistory.Build build:builds().values()){
   Path file=assets().resolve(build.id+".tsv");if(!Files.isRegularFile(file))continue;
   Set<String> changes=new HashSet<>();
   for(ApdHistory.Entry e:ApdHistory.entries(Files.readString(file))){
    if(e.sprite.startsWith("ITEM_"))assertNotNull("Unknown item sprite in "+build.id+": "+e.sprite,ApdSpriteRef.itemImage(e.sprite));
    else if(e.sprite.startsWith("ICON_"))Icons.valueOf(e.sprite.substring(5));else ChangeIcons.valueOf(e.sprite);
    assertTrue(build.id+" "+e.title,e.text.split("\\s+").length<=100);assertFalse(e.text.contains("github.com"));assertFalse(e.text.contains("raw.githubusercontent.com"));
    assertTrue("Duplicate entry in "+build.id,changes.add(e.category+"\t"+e.text));
   }
  }
 }
 @Test public void largeHistoricalBuildsKeepEveryIconAndOneHeadingPerCategory()throws Exception{
  List<ApdHistory.Entry> entries=ApdHistory.entries(Files.readString(assets().resolve("build-22.tsv")));assertTrue(entries.size()>=250);
  int count=0;for(List<ApdHistory.Entry> group:ApdHistory.categories(entries).values())count+=group.size();assertEquals(entries.size(),count);
  List<ApdHistory.Entry> large=new ArrayList<>();for(int i=0;i<1000;i++)large.addAll(ApdHistory.entries("Testing\tICON_CHECKED\tCase "+i+"\tVerified case "+i+"."));
  assertEquals(1000,ApdHistory.categories(large).get("Testing").size());
 }
}
