/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
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
package com.shatteredpixel.shatteredpixeldungeon.apd.humor;
import com.shatteredpixel.shatteredpixeldungeon.apd.text.AuthoredLanguage;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthoredLanguageTest {
 @Test public void everyRegisteredAdultTemplateHasAnUnchangedAdultAndCleanAlternative(){
  assertTrue(AuthoredLanguage.alternatives().size()>=25);
  Pattern formats=Pattern.compile("%(?:[0-9]+\\$)?[-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z%]");
  for(Map.Entry<String,String> pair:AuthoredLanguage.alternatives().entrySet()){
   assertSame(pair.getKey(),AuthoredLanguage.select(pair.getKey(),true));
   assertEquals(pair.getValue(),AuthoredLanguage.select(pair.getKey(),false));
   List<String> before=new ArrayList<>(),after=new ArrayList<>();
   Matcher m=formats.matcher(pair.getKey());while(m.find())before.add(m.group());
   m=formats.matcher(pair.getValue());while(m.find())after.add(m.group());assertEquals(before,after);
  }
 }
 @Test public void everyEnglishMessageHasAProfanityFreeOffVariantAndKeepsAdultText()throws Exception{
  int files=0,values=0;
  try(java.util.stream.Stream<Path> paths=Files.walk(Paths.get("messages"))){
   for(Path p:(Iterable<Path>)paths.filter(x->x.toString().endsWith(".properties")&&!x.getFileName().toString().contains("_"))::iterator){
    Properties props=new Properties();try(Reader r=Files.newBufferedReader(p,StandardCharsets.UTF_8)){props.load(r);}files++;
    for(String key:props.stringPropertyNames()){
     String value=props.getProperty(key);values++;
     assertFalse(p+":"+key,AuthoredLanguage.containsEnglishProfanity(AuthoredLanguage.english(value,false)));
     assertSame(value,AuthoredLanguage.english(value,true));
     if(AuthoredLanguage.containsEnglishProfanity(value))assertNotEquals("English fallback needs an exact clean alternative in every language: "+p+":"+key,value,AuthoredLanguage.select(value,false));
    }
   }
  }
  // Exact review: 4,998 inherited English messages, three Projecting UI keys,
  // 60 individually source-audited Arcana effect/strength keys, one rounding notice, and
  // seven exact reviewed per-region healing strings and seven neutral progression-pickup labels.
  // Plus 22 reviewed dew settings, searched-wall feedback and numeric ring-contribution messages.
  // Every value, including all new labels, is checked above with Adult Humor OFF and ON.
  // Plus 186 neutral origin/classification labels and two source-generation range labels, plus four action-evidence wall-search labels.
  // Plus the reviewed Wild Magic range message and one neutral seal-cooldown-ready message, and one unknown-charge privacy message.
  // Plus three neutral Arcana comparison labels: decrease, percentage-point increase and decrease.
  // Plus two reviewed ring knowledge/suppression notices; no new description interface.
  // Plus two neutral backpack description/capacity labels, describing existing container behavior.
  // Plus one neutral floor label using a key’s existing recorded depth.
  // Plus one neutral remains-item consumption/time label, using the existing action cost.
  // Plus two Petrified Seed base-level descriptions that omit its zero grass-loot bonus.
  // Plus the known Healing potion warning for the existing Pharmacophobia challenge.
  // Plus Shielding amount/challenge descriptions and Earthen Armor protection, using existing effects.
  // Plus two existing elixir challenge warnings and the live Arcane Armor protection description.
  // Plus a Might elixir description without a made-up health amount when no hero is loaded.
  // Plus the existing Shard identification interaction in the known Identify scroll description.
  // Plus three current-rule notices when the existing Healing modifier is zero.
  // Plus a typical Pickaxe ability description that does not reveal unidentified upgrades.
  // Plus a Transfusion description without a fabricated HP cost when no hero is loaded.
  assertEquals(9,files);// Plus the Noisemaker’s own arming instructions, avoiding the ordinary bomb pickup promise.
  // Plus one existing Elemental Blast paragraph with live talent-adjusted radius.
  // Plus one neutral known-Lullaby description for the existing zero-Healing sleep rule.
  // Plus one known-Passage destination line using its existing floor rule.
  // Plus two neutral zero-Healing notices for dew and stored dew; no action changes.
  // Plus three neutral live Power of Many descriptions, including marked-target and linked-duration conditions.
  // Plus one neutral Berserk description which withholds an unidentified armor shield preview.
  // Plus six neutral Hold Fast descriptions for known, single, penalty, unknown, moved and unavailable states.
  // Plus eight neutral seed-history labels for available records, unknown provenance and depth coverage.
  // Plus Greater Metamorphosis, the scaling Katana guard and seven new katana-family/shield weapons.
  assertEquals("Reviewed English message inventory",5427,values);
 }
 @Test public void localizedAlternativesDoNotDamageInnocentWordsOrOtherLanguages(){
  for(String innocent:new String[]{"Hell","slut","kesialan","Verdammte Charaktere","dictionary","classic","Scunthorpe","Prick","Fan of Blades"})assertEquals(innocent,AuthoredLanguage.select(innocent,false));
  for(String text:AuthoredLanguage.alternatives().values())assertFalse(AuthoredLanguage.containsEnglishProfanity(text));
  assertEquals("SERIOUSLY",AuthoredLanguage.english("FUCK",false));
  assertEquals("FUCK",AuthoredLanguage.english("FUCK",true));
 }
 @Test public void actualMessageLookupRespondsToPreferenceAndLeavesUserArgumentsUntouched(){
  HeadlessTestSupport.start();boolean saved=SPDSettings.apdAdultHumor();Languages language=Messages.lang();
  try{
   Messages.setup(Languages.ENGLISH);
   SPDSettings.apdAdultHumor(false);
   assertEquals("CLERIC, THAT'S WHY",Messages.get("badges$badge.apd_fuck_cleric.title"));
   assertFalse(Messages.get("journal.document.prison_warden.no_support.body").contains("damned"));
   String playerText="My fucking seed and raw technical shit";
   assertEquals(playerText,Messages.format("%s",playerText));
   assertEquals(playerText+" of anti-entropy",Messages.get("items.armor.curses.antientropy.name",(Object)playerText));
   SPDSettings.apdAdultHumor(true);
   assertEquals("FUCK CLERIC, THAT'S WHY",Messages.get("badges$badge.apd_fuck_cleric.title"));
   SPDSettings.apdAdultHumor(false);
   Messages.setup(Languages.ENGLISH);
   assertEquals("You tried to play Cleric 10 times. Take the hint.",Messages.get("badges$badge.apd_fuck_cleric.desc"));
  }finally{SPDSettings.apdAdultHumor(saved);Messages.setup(language);}
 }
 @Test public void allFiftyLoreEntriesKeepCleanAndAdultVariantsWithoutChangingContext(){
  List<Line> lines=HumorTestSupport.shipped();assertEquals(50,lines.size());
  for(Line line:lines){assertFalse(line.id,AuthoredLanguage.containsEnglishProfanity(line.clean));assertEquals(line.clean,AuthoredLanguage.english(line.clean,false));}
 }
 @Test public void newHardcodedAdultProseCannotBypassReviewedCleanBranches()throws Exception{
  Path root=Paths.get(System.getProperty("apd.game.src"));int reviewed=0;
  Pattern strings=Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
  try(java.util.stream.Stream<Path> sources=Files.walk(root)){
   for(Path p:(Iterable<Path>)sources.filter(x->x.toString().endsWith(".java"))::iterator){
    String relative=root.relativize(p).toString().replace('\\','/');
    if(relative.equals("apd/text/AuthoredLanguage.java")||relative.equals("apd/humor/LinePack.java"))continue;
    Matcher literals=strings.matcher(Files.readString(p));
    while(literals.find())if(AuthoredLanguage.containsEnglishProfanity(literals.group())){
     assertEquals("New developer prose needs a reviewed clean branch: "+relative,"scenes/AboutScene.java",relative);reviewed++;
    }
   }
  }
  assertEquals("Both original adult About statements are retained",2,reviewed);
  String about=Files.readString(root.resolve("scenes/AboutScene.java"));
  assertTrue(about.contains("Founded on the principle that shielding should have worked."));
  assertTrue(about.contains("APD began with the shielding mechanic: shielding should have worked."));
 }
 @Test public void actualLocalizedLookupsUseReviewedAlternativesAndPreserveAdultOriginals(){
  HeadlessTestSupport.start();Languages previous=Messages.lang();boolean adult=SPDSettings.apdAdultHumor();
  try {
   for(Languages language:Languages.values()){
    Messages.setup(language);SPDSettings.apdAdultHumor(true);
    for(String key:new String[]{"journal.document.prison_warden.no_support.body","journal.document.halls_king.amulet.body","actors.mobs.npcs.imp.old_golems_1"}){
     String original=Messages.get(key);SPDSettings.apdAdultHumor(false);
     String expected=language==Languages.ENGLISH?AuthoredLanguage.english(original,false):AuthoredLanguage.select(original,false);
     assertEquals(language+":"+key,expected,Messages.get(key));
     SPDSettings.apdAdultHumor(true);assertEquals(original,Messages.get(key));
    }
   }
  }finally {SPDSettings.apdAdultHumor(adult);Messages.setup(previous);}
 }
 @Test public void preferenceSurvivesActualDiskReloadWithoutChangingPlayerContent()throws Exception{
  HeadlessTestSupport.start();java.lang.reflect.Field field=com.watabou.utils.GameSettings.class.getDeclaredField("prefs");field.setAccessible(true);
  com.badlogic.gdx.Preferences original=(com.badlogic.gdx.Preferences)field.get(null);
  java.io.File file=java.io.File.createTempFile("apd-humor-preference", ".xml",HeadlessTestSupport.tempDir());file.delete();
  try {
   com.watabou.utils.GameSettings.set(new com.badlogic.gdx.backends.headless.HeadlessPreferences(new com.badlogic.gdx.files.FileHandle(file)));
   SPDSettings.apdAdultHumor(false);
   com.watabou.utils.GameSettings.set(new com.badlogic.gdx.backends.headless.HeadlessPreferences(new com.badlogic.gdx.files.FileHandle(file)));
   assertFalse(SPDSettings.apdAdultHumor());
   SPDSettings.apdAdultHumor(true);
   com.watabou.utils.GameSettings.set(new com.badlogic.gdx.backends.headless.HeadlessPreferences(new com.badlogic.gdx.files.FileHandle(file)));
   assertTrue(SPDSettings.apdAdultHumor());
  }finally {com.watabou.utils.GameSettings.set(original);}
 }
 @Test public void localizedResourceEditsRequireCleanAlternativesInsteadOfSilentLeaks()throws Exception{
  Map<String,Pattern> vocabulary=new HashMap<>();
  vocabulary.put("be",Pattern.compile("да чорта",Pattern.CASE_INSENSITIVE));
  vocabulary.put("cs",Pattern.compile("sakra|zatraceně",Pattern.CASE_INSENSITIVE));
  vocabulary.put("de",Pattern.compile("verdammt nochmal|verdammt noch eins|zum Henker|Teufel, und warum|schei[ßs]\\w*",Pattern.CASE_INSENSITIVE));
  vocabulary.put("el",Pattern.compile("διάολο|την κατάρα μου",Pattern.CASE_INSENSITIVE));
  vocabulary.put("eo",Pattern.compile("diable|diablen|damnan|Damne",Pattern.CASE_INSENSITIVE));
  vocabulary.put("es",Pattern.compile("malditos sean|maldito apoyo|En qué demonios|Demonios, por qué|mierda|joder|coño",Pattern.CASE_INSENSITIVE));
  vocabulary.put("fr",Pattern.compile("fichus|maudits soient-ils|Bon sang|merde|putain",Pattern.CASE_INSENSITIVE));
  vocabulary.put("hu",Pattern.compile("fenébe|a fenét|átkozott támogatásra|A pokolba is",Pattern.CASE_INSENSITIVE));
  vocabulary.put("in",Pattern.compile("\\bsialan\\b|Neraka, mengapa",Pattern.CASE_INSENSITIVE));
  vocabulary.put("it",Pattern.compile("erranti, dannati|dannati rinforzi|diamine|cazzo|minchia",Pattern.CASE_INSENSITIVE));
  vocabulary.put("ko",Pattern.compile("망할 지원|젠장|씨발",Pattern.CASE_INSENSITIVE));
  vocabulary.put("nl",Pattern.compile("vervloek ze|in godsnaam|verdomde|Verdorie",Pattern.CASE_INSENSITIVE));
  vocabulary.put("pl",Pattern.compile("cholerne|cholery|cholernego|Cholera|kurwa|a niech ich",Pattern.CASE_INSENSITIVE));
  vocabulary.put("pt",Pattern.compile("malditos sejam|maldito apoio|O que infernos|porra",Pattern.CASE_INSENSITIVE));
  vocabulary.put("ru",Pattern.compile("к черту|долбанная|бес их побери",Pattern.CASE_INSENSITIVE));
  vocabulary.put("sv",Pattern.compile("helvete|jävla|\\bfan\\b|förbaskade",Pattern.CASE_INSENSITIVE));
  vocabulary.put("tr",Pattern.compile("lanet olsun onlara|kahrolası|Kahretsin",Pattern.CASE_INSENSITIVE));
  vocabulary.put("uk",Pattern.compile("щоб їх!|в біса|до біса|клята підтримка",Pattern.CASE_INSENSITIVE));
  vocabulary.put("vi",Pattern.compile("chết tiệt|cái quái gì",Pattern.CASE_INSENSITIVE));
  vocabulary.put("zh",Pattern.compile("真是该死|他娘的",Pattern.CASE_INSENSITIVE));
  vocabulary.put("zh-hant",Pattern.compile("該死",Pattern.CASE_INSENSITIVE));
  vocabulary.put("ja",Pattern.compile("クソ|畜生",Pattern.CASE_INSENSITIVE));
  int guarded=0;List<String> issues=new ArrayList<>();
  try(java.util.stream.Stream<Path> paths=Files.walk(Paths.get("messages"))){
   for(Path p:(Iterable<Path>)paths.filter(x->x.toString().endsWith(".properties"))::iterator){
    String file=p.getFileName().toString();int underscore=file.indexOf('_');if(underscore<0)continue;
    String language=file.substring(underscore+1,file.length()-11);Pattern words=vocabulary.get(language);if(words==null)continue;
    Properties props=new Properties();try(Reader reader=Files.newBufferedReader(p,StandardCharsets.UTF_8)){props.load(reader);}
    for(String key:props.stringPropertyNames()){
     String adult=props.getProperty(key);if(!words.matcher(adult).find())continue;guarded++;
     String clean=AuthoredLanguage.select(adult,false);
     if(adult.equals(clean))issues.add(p+":"+key+" :: "+adult);
     else if(words.matcher(clean).find())issues.add(p+":"+key+" :: residual "+clean);
     assertSame(adult,AuthoredLanguage.select(adult,true));
    }
   }
  }
  assertTrue("The translated alternatives must actually be exercised",guarded>=50);
  assertTrue(String.join("\n",issues),issues.isEmpty());
 }
}
