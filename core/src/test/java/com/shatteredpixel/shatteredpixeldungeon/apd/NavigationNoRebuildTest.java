package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.scenes.JournalScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Compiled scene guards: ordinary local navigation must not request scene resets. */
public class NavigationNoRebuildTest {
 private static String classConstants(Class<?> type) throws Exception {
  try(InputStream in=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")){
   ByteArrayOutputStream data=new ByteArrayOutputStream();
   byte[] buf=new byte[4096];
   for(int n;(n=in.read(buf))>0;)data.write(buf,0,n);
   return new String(data.toByteArray(),StandardCharsets.ISO_8859_1);
  }
 }
 @Test public void journalTabSwitchHasNoWholeSceneResetCall() throws Exception {
  JournalScene.class.getDeclaredMethod("selectTab",int.class);
  assertFalse(classConstants(JournalScene.class).contains("seamlessResetScene"));
  assertTrue(classConstants(JournalScene.class).contains("showSelectedTab"));
 }
 @Test public void historyAndNewsPagingDoNotResetTheScene() throws Exception {
  APDTextScene.class.getDeclaredMethod("refreshPage");
  assertFalse(classConstants(APDTextScene.class).contains("seamlessResetScene"));
  assertTrue(classConstants(APDTextScene.class).contains("visiblePage"));
 }
}
