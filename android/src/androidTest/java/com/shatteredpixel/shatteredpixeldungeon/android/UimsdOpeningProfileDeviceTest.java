package com.shatteredpixel.shatteredpixeldungeon.android;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.watabou.noosa.Game;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Diagnostic sampling only: this is NOT a timing acceptance result. */
@RunWith(AndroidJUnit4.class)
public class UimsdOpeningProfileDeviceTest {
    @Test public void profileTheExistingNativeOpeningRegressionWithoutChangingItsAssertions() throws Exception {
        AtomicBoolean running=new AtomicBoolean(true);
        Map<String,Integer> stacks=new HashMap<>();
        Thread sampler=new Thread(()->{
            while(running.get()){
                if(Game.instance != null && Game.scene() instanceof UimsdScene){
                    for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet()){
                        if(!entry.getKey().getName().startsWith("GLThread"))continue;
                        StringBuilder key=new StringBuilder();
                        int count=0;
                        for(StackTraceElement frame:entry.getValue()){
                            if(count++==12)break;
                            key.append(frame.toString()).append(" <- ");
                        }
                        synchronized(stacks){stacks.merge(key.toString(),1,Integer::sum);}
                    }
                }
                try{Thread.sleep(10);}catch(InterruptedException done){return;}
            }
        },"APD-diagnostic-sampler");
        sampler.start();
        try{
            // Reuse the exact actual-native-tap/72-run/two-opening regression.
            // If it fails, this diagnostic run also fails; never turn it green.
            new AndroidBackDeviceTest().warmUimsdTapDrawsTheComplete72RunHomeTwiceWithin250ms();
        }finally{
            running.set(false);sampler.interrupt();sampler.join(3000);
            List<Map.Entry<String,Integer>> ordered;
            synchronized(stacks){ordered=new ArrayList<>(stacks.entrySet());}
            ordered.sort((a,b)->Integer.compare(b.getValue(),a.getValue()));
            Log.w("APDOPENPROFILE","DIAGNOSTIC_ONLY sampling changes timing; not acceptance evidence");
            for(int i=0;i<Math.min(30,ordered.size());i++){
                Map.Entry<String,Integer> e=ordered.get(i);
                Log.w("APDOPENPROFILE","samples="+e.getValue()+" stack="+e.getKey());
            }
            Log.w("APDOPENPROFILE",com.watabou.utils.SupportDiagnostics.report("UIMSD synthetic timing investigation"));
        }
    }
}
