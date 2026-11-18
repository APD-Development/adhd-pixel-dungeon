package com.shatteredpixel.shatteredpixeldungeon.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AboutScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.watabou.noosa.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

/** The dedicated workflow force-stops the app between these phases. No save or player history is touched. */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdultHumorRestartDeviceTest {
 private static SharedPreferences fixture(){return InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("apd-test-humor-restart",Context.MODE_PRIVATE);}
 private static void render(Runnable task)throws Exception {
  CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
  Game.runOnRenderThread(()->{try{task.run();}catch(Throwable t){failure.set(t);}finally{done.countDown();}});
  assertTrue(done.await(45,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());
 }
 @SuppressWarnings("unchecked") private static List<Gizmo> children(Group group)throws Exception {
  Field f=Group.class.getDeclaredField("members");f.setAccessible(true);return new ArrayList<>((List<Gizmo>)f.get(group));
 }
 private static String text(Group group)throws Exception {
  StringBuilder result=new StringBuilder();for(Gizmo child:children(group)){
   if(child instanceof RenderedTextBlock)result.append(((RenderedTextBlock)child).text()).append('\n');
   if(child instanceof Group)result.append(text((Group)child));
  }return result.toString();
 }
 private static RedButton why(Group group)throws Exception {
  for(Gizmo child:children(group)){
   if(child instanceof RedButton && ((RedButton)child).text().equals("Why APD Exists"))return (RedButton)child;
   if(child instanceof Group){RedButton found=why((Group)child);if(found!=null)return found;}
  }return null;
 }
 private static void ready()throws Exception {
  long end=System.currentTimeMillis()+90000;
  while((Game.scene()==null||PixelScene.uiCamera==null)&&System.currentTimeMillis()<end)Thread.sleep(100);
  assertNotNull(Game.scene());assertNotNull(PixelScene.uiCamera);Thread.sleep(1200);
 }
 private static void inspect(String screenshot)throws Exception {
  render(()->ShatteredPixelDungeon.switchScene(AboutScene.class));
  long end=System.currentTimeMillis()+15000;while(!(Game.scene() instanceof AboutScene)&&System.currentTimeMillis()<end)Thread.sleep(100);
  assertTrue(Game.scene() instanceof AboutScene);
  render(()->{try{String shown=text(Game.scene());assertTrue(shown.contains("shielding should have worked."));assertFalse(shown.toLowerCase(Locale.ROOT).contains("fucking"));}catch(Exception e){throw new RuntimeException(e);}});
  Thread.sleep(500);
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
  android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,screenshot);values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/APD-Test-Visuals");
  android.net.Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
  try(java.io.OutputStream output=context.getContentResolver().openOutputStream(uri)){assertNotNull(output);assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));}finally{image.recycle();}
  render(()->{try{RedButton button=why(Game.scene());assertNotNull(button);Method click=button.getClass().getDeclaredMethod("onClick");click.setAccessible(true);click.invoke(button);
   WndUimsdText popup=null;for(Gizmo child:children(Game.scene()))if(child instanceof WndUimsdText && child.alive)popup=(WndUimsdText)child;
   assertNotNull(popup);String shown=text(popup);assertTrue(shown.contains("shielding should have worked."));assertFalse(shown.toLowerCase(Locale.ROOT).contains("fucking"));popup.hide();
  }catch(Exception e){throw new RuntimeException(e);}});
 }
 @Test public void aWriteOffPreferenceAndCheckAboutBeforeRestart()throws Exception {
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   ready();render(()->{assertTrue(fixture().edit().putBoolean("original",SPDSettings.apdAdultHumor()).putBoolean("prepared",true).commit());SPDSettings.apdAdultHumor(false);assertFalse(SPDSettings.apdAdultHumor());});
   inspect("adult-off-before-restart.png");
  }
 }
 @Test public void bReadOffPreferenceAndCheckAboutAfterRestart()throws Exception {
  assertTrue("Write phase must finish before cold restart",fixture().getBoolean("prepared",false));
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   ready();try{render(()->assertFalse("Cold-started Android preferences must remain OFF",SPDSettings.apdAdultHumor()));inspect("adult-off-after-restart.png");}
   finally{render(()->{SPDSettings.apdAdultHumor(fixture().getBoolean("original",false));assertTrue(fixture().edit().clear().commit());});}
  }
 }
}
