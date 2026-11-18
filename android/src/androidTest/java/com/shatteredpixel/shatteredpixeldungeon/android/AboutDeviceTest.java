package com.shatteredpixel.shatteredpixeldungeon.android;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.watabou.noosa.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class AboutDeviceTest {
 private void render(Runnable r)throws Exception{
  CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> err=new AtomicReference<>();
  Game.runOnRenderThread(()->{try{r.run();}catch(Throwable t){err.set(t);}finally{done.countDown();}});
  assertTrue(done.await(30,TimeUnit.SECONDS));if(err.get()!=null)throw new AssertionError(err.get());
 }
 private List<Gizmo> children(Group group)throws Exception{
  Field f=Group.class.getDeclaredField("members");f.setAccessible(true);
  return new ArrayList<>((List<Gizmo>)f.get(group));
 }
 private RedButton find(Group group,String label)throws Exception{
  for(Gizmo g:children(group)){
   if(g instanceof RedButton && label.equals(((RedButton)g).text()))return (RedButton)g;
   if(g instanceof Group){RedButton b=find((Group)g,label);if(b!=null)return b;}
  }return null;
 }
 @Test public void everyAboutPopupOpensScrollsAndCloses()throws Exception{
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   render(()->ShatteredPixelDungeon.switchScene(AboutScene.class));
   long end=System.currentTimeMillis()+15000;
   while(!(Game.scene() instanceof AboutScene)&&System.currentTimeMillis()<end)Thread.sleep(100);
   assertTrue(Game.scene() instanceof AboutScene);
   for(String label:new String[]{"Why APD Exists","Features & Vision","Community & Voting","Developer's Note","Development Roadmap","Source & Licenses"}){
    render(()->{try{
     RedButton button=find(Game.scene(),label);assertNotNull(label,button);
     assertTrue(button.width()>=40);assertTrue(button.height()>=20);
     float oldWidth=button.width();button.setSize(53,20);
     Field labelField=StyledButton.class.getDeclaredField("text");labelField.setAccessible(true);RenderedTextBlock labelText=(RenderedTextBlock)labelField.get(button);
     assertTrue("label stays inside a narrow native button",labelText.width()<=button.width());
     assertTrue("wrapped label stays inside button height",labelText.height()<=button.height());button.setSize(oldWidth,20);
     Method click=button.getClass().getDeclaredMethod("onClick");click.setAccessible(true);click.invoke(button);
     if("Source & Licenses".equals(label)){
      com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions options=null;
      for(Gizmo g:children(Game.scene()))if(g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions && g.alive)options=(com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions)g;
      assertNotNull("Source & Licenses choices open",options);assertSame(Game.scene(),options.parent);
      RedButton offline=find(options,"GPLv3 (offline)");assertNotNull("offline GPL choice",offline);
      Method choose=offline.getClass().getDeclaredMethod("onClick");choose.setAccessible(true);choose.invoke(offline);
      com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList license=null;
      for(Gizmo g:children(Game.scene()))if(g instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList && g.alive)license=(com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList)g;
      assertNotNull("complete GPL index opens offline",license);assertSame(Game.scene(),license.parent);
      assertTrue(renderedText(license).contains("Complete license, available offline."));license.hide();
     }else{
      WndUimsdText popup=null;
      for(Gizmo g:children(Game.scene()))if(g instanceof WndUimsdText && g.alive)popup=(WndUimsdText)g;
      assertNotNull(label,popup);assertSame(Game.scene(),popup.parent);assertTrue(popup.camera.width>0);
      for(Gizmo g:children(popup))if(g instanceof ScrollPane)((ScrollPane)g).scrollTo(0,1000);
      popup.hide();
     }
    }catch(Exception e){throw new RuntimeException(e);}});
   }
   render(()->{try{assertNotNull(find(Game.scene(),"APD News"));assertNotNull(find(Game.scene(),"APD Changes"));}catch(Exception e){throw new RuntimeException(e);}});
  }
 }
 @Test public void offlineNewsCardsOpenDatedSpritePopups()throws Exception{
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   render(()->{com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.page=com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.Page.NEWS;ShatteredPixelDungeon.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene.class);});
   long end=System.currentTimeMillis()+15000;
   while(!(Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene)&&System.currentTimeMillis()<end)Thread.sleep(100);
   assertTrue(Game.scene() instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.APDTextScene);
   render(()->{try{
    int[] cards={0};checkNews(Game.scene(),cards);assertEquals(com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdNews.published(com.badlogic.gdx.Gdx.files.internal("apd/news.tsv").readString("UTF-8")).size(),cards[0]);
   }catch(Exception e){throw new RuntimeException(e);}});
  }
 }
 private void checkNews(Group group,int[] count)throws Exception{
  for(Gizmo g:children(group)){
   if(g.getClass().getName().endsWith("$NewsCard")){
    count[0]++;assertTrue(((com.watabou.noosa.ui.Component)g).width()>0);assertTrue(((com.watabou.noosa.ui.Component)g).height()>=32);
    Method click=g.getClass().getDeclaredMethod("onClick");click.setAccessible(true);click.invoke(g);
    com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage popup=null;
    for(Gizmo w:children(Game.scene()))if(w instanceof com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage&&w.alive)popup=(com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage)w;
    assertNotNull(popup);assertSame(Game.scene(),popup.parent);popup.hide();
   }else if(g instanceof Group)checkNews((Group)g,count);
  }
 }

 private String renderedText(Group group)throws Exception{
  StringBuilder text=new StringBuilder();
  for(Gizmo g:children(group)){
   if(g instanceof RenderedTextBlock)text.append(((RenderedTextBlock)g).text()).append('\n');
   if(g instanceof Group)text.append(renderedText((Group)g));
  }
  return text.toString();
 }
 @Test public void aboutFoundingStatementAndPopupFollowAdultHumorOnEveryOpening()throws Exception{
  boolean saved=com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdAdultHumor();
  try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)){
   try {
    for(boolean adult:new boolean[]{false,true,false}){
     render(()->{
      com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdAdultHumor(adult);
      ShatteredPixelDungeon.switchScene(AboutScene.class);
     });
     long end=System.currentTimeMillis()+15000;
     while(!(Game.scene() instanceof AboutScene)&&System.currentTimeMillis()<end)Thread.sleep(100);
     assertTrue(Game.scene() instanceof AboutScene);
     render(()->{try{
      String main=renderedText(Game.scene());
      assertTrue(main.contains(adult?"shielding should have fucking worked.":"shielding should have worked."));
      assertEquals(adult,main.toLowerCase(java.util.Locale.ROOT).contains("fucking"));
      RedButton button=find(Game.scene(),"Why APD Exists");assertNotNull(button);
      Method click=button.getClass().getDeclaredMethod("onClick");click.setAccessible(true);click.invoke(button);
      WndUimsdText popup=null;
      for(Gizmo g:children(Game.scene()))if(g instanceof WndUimsdText && g.alive)popup=(WndUimsdText)g;
      assertNotNull(popup);
      String detail=renderedText(popup);
      assertTrue(detail.contains(adult?"shielding should have fucking worked.":"shielding should have worked."));
      assertEquals(adult,detail.toLowerCase(java.util.Locale.ROOT).contains("fucking"));popup.hide();
     }catch(Exception e){throw new RuntimeException(e);}});
     render(()->ShatteredPixelDungeon.switchScene(TitleScene.class));
     end=System.currentTimeMillis()+15000;
     while(!(Game.scene() instanceof TitleScene)&&System.currentTimeMillis()<end)Thread.sleep(100);
    }
   } finally {render(()->com.shatteredpixel.shatteredpixeldungeon.SPDSettings.apdAdultHumor(saved));}
  }
 }
}
