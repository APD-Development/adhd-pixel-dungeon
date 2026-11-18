package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AboutScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** A title return must not spend another full second under TitleScene's Fader. */
public class TitleReturnNoFadeTest {

 private Game previousGame;
 private Class<?> previousSceneClass;
 private Class<? extends PixelScene> previousBackTo;
 private boolean previousNoFade;

 @Before public void before() throws Exception {
  previousGame=Game.instance;
  Field scene=Game.class.getDeclaredField("sceneClass");
  scene.setAccessible(true);
  previousSceneClass=(Class<?>)scene.get(null);
  previousBackTo=APDTextScene.backTo;
  previousNoFade=PixelScene.noFade;
  new Game(TitleScene.class,null);
  PixelScene.noFade=false;
 }

 @After public void after() throws Exception {
  PixelScene.noFade=previousNoFade;
  APDTextScene.backTo=previousBackTo;
  Game.instance=previousGame;
  Field scene=Game.class.getDeclaredField("sceneClass");
  scene.setAccessible(true);
  scene.set(null,previousSceneClass);
 }

 private static void back(Class<?> type) throws Exception {
  Object target=type.getDeclaredConstructor().newInstance();
  Method method=type.getDeclaredMethod("onBackPressed");
  method.setAccessible(true);
  method.invoke(target);
 }

 @Test public void aboutBackToTitleSkipsOnlyTheEntryFade() throws Exception {
  back(AboutScene.class);
  assertTrue("back to the title should be a real scene switch",Game.switchingScene());
  assertTrue("title should not be hidden by a one-second fade",PixelScene.noFade);
 }

 @Test public void apdNewsAndChangesReturnToTitleSkipsTheEntryFade() throws Exception {
  APDTextScene.backTo=TitleScene.class;
  back(APDTextScene.class);
  assertTrue(Game.switchingScene());
  assertTrue(PixelScene.noFade);
 }

 @Test public void apdNewsAndChangesReturnToAboutDoesNotLeakTheTitleFadeFlag() throws Exception {
  APDTextScene.backTo=AboutScene.class;
  back(APDTextScene.class);
  assertTrue(Game.switchingScene());
  assertFalse(PixelScene.noFade);
 }
}
