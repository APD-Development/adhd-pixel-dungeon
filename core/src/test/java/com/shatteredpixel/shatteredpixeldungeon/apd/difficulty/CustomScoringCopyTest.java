package com.shatteredpixel.shatteredpixeldungeon.apd.difficulty;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Catch stale current-status explanations in every developer-authored UI source and offline asset. */
public class CustomScoringCopyTest{
 @Test public void playerFacingFilesDoNotClaimActiveMultipliersAreStillAbsent()throws Exception{
  List<String> stale=Arrays.asList("These settings currently do not affect your earned score", "Custom scoring is still in development", "currently leave your earned score unchanged", "current settings preserve original earned scores");
  for(Path root:new Path[]{Paths.get(System.getProperty("apd.game.src")),Paths.get("apd")})try(java.util.stream.Stream<Path> paths=Files.walk(root)){
   Iterator<Path> files=paths.iterator();while(files.hasNext()){Path p=files.next();if(!Files.isRegularFile(p))continue;String n=p.toString();if(!(n.endsWith(".java")||n.endsWith(".txt")||n.endsWith(".tsv")||n.endsWith(".properties")))continue;String text=new String(Files.readAllBytes(p),StandardCharsets.UTF_8);for(String phrase:stale)assertFalse(p+": "+phrase,text.contains(phrase));}
  }
 }
}
