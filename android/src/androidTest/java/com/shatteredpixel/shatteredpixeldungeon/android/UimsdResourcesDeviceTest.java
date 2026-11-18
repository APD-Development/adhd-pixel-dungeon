package com.shatteredpixel.shatteredpixeldungeon.android;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.ui.UimsdHomePane;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.UimsdHome;
import com.shatteredpixel.shatteredpixeldungeon.apd.query.Catalog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.glwrap.Vertexbuffer;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

//Exercise a real drawn Home image buffer; replacement must dispose its owned graphics resource.
@RunWith(AndroidJUnit4.class)
public class UimsdResourcesDeviceTest {
    @Test public void replacingHomeContentsDisposesPreviouslyDrawnImages() throws Exception {
        try(ActivityScenario<AndroidLauncher> scenario=ActivityScenario.launch(AndroidLauncher.class)) {
            long end=System.currentTimeMillis()+30000;
            while(Game.scene()==null&&System.currentTimeMillis()<end)Thread.sleep(100);
            assertNotNull(Game.scene());
            CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
            Game.runOnRenderThread(()->{
                UimsdHomePane pane=null;Image old=null;
                try {
                    java.lang.reflect.Constructor<UimsdHome> factory=UimsdHome.class.getDeclaredConstructor(int.class,int.class,boolean.class,int.class);factory.setAccessible(true);
                    UimsdHome home=factory.newInstance(0,0,false,0);
                    java.lang.reflect.Constructor<UimsdHome.Card> card=UimsdHome.Card.class.getDeclaredConstructor(String.class,String.class);card.setAccessible(true);home.cards.add(card.newInstance("runs","Runs"));
                    pane=new UimsdHomePane();Game.scene().add(pane);pane.setRect(0,0,160,100);pane.setHome(home);
                    Field members=Group.class.getDeclaredField("members");members.setAccessible(true);
                    for(Gizmo g:(List<Gizmo>)members.get(pane.content()))if(g instanceof Image){old=(Image)g;break;}
                    assertNotNull("Home must contain actual native artwork",old);
                    old.draw();
                    Field buffer=Image.class.getDeclaredField("buffer");buffer.setAccessible(true);
                    Vertexbuffer owned=(Vertexbuffer)buffer.get(old);assertNotNull(owned);
                    Field registry=Vertexbuffer.class.getDeclaredField("buffers");registry.setAccessible(true);
                    assertTrue(((List<?>)registry.get(null)).contains(owned));
                    pane.setHome(home);
                    assertFalse("Replaced artwork must release its owned buffer",((List<?>)registry.get(null)).contains(owned));old=null;
                }catch(Throwable e){failure.set(e);}finally{
                    if(old!=null)old.destroy();if(pane!=null){Game.scene().remove(pane);pane.destroy();}done.countDown();
                }
            });
            assertTrue(done.await(30,TimeUnit.SECONDS));if(failure.get()!=null)throw new AssertionError(failure.get());
        }
    }
}
