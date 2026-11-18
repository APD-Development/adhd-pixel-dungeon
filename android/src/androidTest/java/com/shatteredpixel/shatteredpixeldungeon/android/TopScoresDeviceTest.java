package com.shatteredpixel.shatteredpixeldungeon.android;
import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.view.*;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.*;
import com.watabou.utils.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
@RunWith(AndroidJUnit4.class)
public class TopScoresDeviceTest {
    static void render(Runnable task)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Game.runOnRenderThread(()->{try{task.run();}catch(Throwable t){failure.set(t);}finally{done.countDown();}});
        assertTrue(done.await(60,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    @SuppressWarnings("unchecked") private static List<Gizmo> members(Group group)throws Exception {
        Field f=Group.class.getDeclaredField("members");f.setAccessible(true);
        return new ArrayList<>((List<Gizmo>)f.get(group));
    }
    static <T> List<T> find(Group group,Class<T> type)throws Exception {
        List<T> found=new ArrayList<>();
        for(Gizmo child:members(group)){if(type.isInstance(child))found.add(type.cast(child));if(child instanceof Group)found.addAll(find((Group)child,type));}
        return found;
    }
    private static Object field(Object object,String name)throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static void tap(Point point)throws Exception {
        PointerEvent.addPointerEvent(new PointerEvent(point.x,point.y,11,PointerEvent.Type.DOWN,PointerEvent.LEFT));
        Thread.sleep(150);
        PointerEvent.addPointerEvent(new PointerEvent(point.x,point.y,11,PointerEvent.Type.UP,PointerEvent.LEFT));
        Thread.sleep(500);
    }
    static void screenshot(String name)throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/APD-Test-Visuals");
        android.net.Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
        try(java.io.OutputStream out=context.getContentResolver().openOutputStream(uri)) {assertNotNull(out);assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        finally{bitmap.recycle();}
    }

 private static void touch(Point p)throws Exception{
  android.app.Instrumentation in=InstrumentationRegistry.getInstrumentation();long down=SystemClock.uptimeMillis();
  for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
   MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p.x,p.y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);in.sendPointerSync(e);e.recycle();Thread.sleep(100);
  }
  Thread.sleep(800);
 }
 private static void waitScene(Class<?> type)throws Exception{long end=System.currentTimeMillis()+30000;while(!type.isInstance(Game.scene())&&System.currentTimeMillis()<end)Thread.sleep(100);assertTrue("Expected "+type.getSimpleName(),type.isInstance(Game.scene()));Thread.sleep(600);}
 private static byte[] rankingBytes(){com.badlogic.gdx.files.FileHandle f=FileUtils.getFileHandle(Rankings.RANKINGS_FILE);return f.exists()?f.readBytes():null;}
 @Test public void androidTopScoresOpensOriginalRankingsAndBackPreservesStoredHistory()throws Exception{
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   long end=System.currentTimeMillis()+120000;while((Game.scene()==null||PixelScene.uiCamera==null)&&System.currentTimeMillis()<end)Thread.sleep(200);
   render(()->ShatteredPixelDungeon.switchNoFade(TitleScene.class));waitScene(TitleScene.class);
   final Point[] tapPoint=new Point[1];final java.util.ArrayList<Rankings.Record>[] saved=new java.util.ArrayList[1];final int[] counts=new int[3];final byte[][] bytes=new byte[1][];
   render(()->{try{
    Rankings.INSTANCE.load();saved[0]=Rankings.INSTANCE.records;counts[0]=Rankings.INSTANCE.totalNumber;counts[1]=Rankings.INSTANCE.wonNumber;counts[2]=Rankings.INSTANCE.lastRecord;bytes[0]=rankingBytes();
    StyledButton top=(StyledButton)field(Game.scene(),"btnQuit"),options=(StyledButton)field(Game.scene(),"btnOptions"),uimsd=(StyledButton)field(Game.scene(),"btnStatistics");
    assertEquals("Top Scores",top.text());assertEquals("UIMSD",uimsd.text());assertTrue(top.visible&&top.active);assertEquals(options.top(),top.top(),0.01f);assertTrue(top.left()>options.left());assertEquals(options.width(),top.width(),0.01f);assertEquals(options.height(),top.height(),0.01f);
    tapPoint[0]=top.camera().cameraToScreen(top.left()+top.width()/2,top.top()+top.height()/2);
   }catch(Exception e){throw new RuntimeException(e);}});
   try{
    screenshot("top-scores-main-menu.png");touch(tapPoint[0]);waitScene(RankingsScene.class);
    render(()->{assertSame(saved[0],Rankings.INSTANCE.records);assertArrayEquals(bytes[0],rankingBytes());});
    // Isolated memory-only fixtures exercise victory, death and abandoned-record presentation. Never saved to disk.
    render(()->{
     java.util.ArrayList<Rankings.Record> fixtures=new java.util.ArrayList<>();
     for(int i=0;i<3;i++){Rankings.Record r=new Rankings.Record();r.heroClass=HeroClass.WARRIOR;r.herolevel=10;r.depth=8;r.armorTier=2;r.score=new int[]{1200,3000,700}[i];r.gameID="menu-fixture-"+i;r.customSeed="";r.win=i==1;r.cause=i==0?com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class:null;fixtures.add(r);}
     fixtures.sort(Rankings.scoreComparator);Rankings.INSTANCE.records=fixtures;Rankings.INSTANCE.lastRecord=-1;Rankings.INSTANCE.totalNumber=3;Rankings.INSTANCE.wonNumber=1;
     ShatteredPixelDungeon.switchNoFade(RankingsScene.class);
    });waitScene(RankingsScene.class);
    render(()->{try{
     List<RankingsScene.Record> rows=find(Game.scene(),RankingsScene.Record.class);assertEquals(3,rows.size());int previous=Integer.MAX_VALUE;for(RankingsScene.Record row:rows){Rankings.Record record=(Rankings.Record)field(row,"rec");assertTrue(record.score<=previous);previous=record.score;}assertArrayEquals(bytes[0],rankingBytes());
    }catch(Exception e){throw new RuntimeException(e);}});
    screenshot("top-scores-rankings.png");
    InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitScene(TitleScene.class);
    render(()->{try{assertEquals("Top Scores",((StyledButton)field(Game.scene(),"btnQuit")).text());assertArrayEquals(bytes[0],rankingBytes());}catch(Exception e){throw new RuntimeException(e);}});
   }finally{render(()->{Rankings.INSTANCE.records=saved[0];Rankings.INSTANCE.totalNumber=counts[0];Rankings.INSTANCE.wonNumber=counts[1];Rankings.INSTANCE.lastRecord=counts[2];});}
  }
 }
}

