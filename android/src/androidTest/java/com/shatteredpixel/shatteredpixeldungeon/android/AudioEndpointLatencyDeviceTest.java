/*
 * ADHD Pixel Dungeon
 * Copyright (C) 2026 Chris Charlton
 * GPLv3-or-later; no warranty. See LICENSE.txt.
 */
package com.shatteredpixel.shatteredpixeldungeon.android;

import android.os.Handler;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.android.AsynchronousSound;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real SoundPool dispatch timings. Neither acoustic output nor natural encounter reproduction. */
@RunWith(AndroidJUnit4.class)
public class AudioEndpointLatencyDeviceTest {
    private static final class Event {
        final String asset;final long requested=SystemClock.elapsedRealtimeNanos();
        long dispatched,returned,stream;
        Event(String asset){this.asset=asset;}
    }
    @Test public void realEndpointTimingsForSewersPrisonAndTenguEffectMixes()throws Exception {
        Map<String,Sound> originals=new LinkedHashMap<>();List<Event> events=Collections.synchronizedList(new ArrayList<>());
        Map<String,ArrayDeque<Event>> pending=new LinkedHashMap<>();List<Handler> handlers=new ArrayList<>();
        String[][] mixes={{Assets.Sounds.HIT,Assets.Sounds.MISS,Assets.Sounds.DEATH},{Assets.Sounds.HIT_SLASH,Assets.Sounds.BONES,Assets.Sounds.SHATTER},{Assets.Sounds.PUFF,Assets.Sounds.BLAST,Assets.Sounds.BURNING,Assets.Sounds.LIGHTNING}};
        Set<String> selected=new LinkedHashSet<>();for(String[] mix:mixes)Collections.addAll(selected,mix);
        boolean enabled=Sample.INSTANCE.isEnabled();
        Field idsField=Sample.class.getDeclaredField("ids");idsField.setAccessible(true);
        Field handlerField=AsynchronousSound.class.getDeclaredField("handler"),endpointField=AsynchronousSound.class.getDeclaredField("sound");handlerField.setAccessible(true);endpointField.setAccessible(true);
        try(ActivityScenario<AndroidLauncher> app=ActivityScenario.launch(AndroidLauncher.class)) {
            long wait=SystemClock.uptimeMillis()+30000;
            while(Game.scene()==null&&SystemClock.uptimeMillis()<wait)Thread.sleep(100);
            assertNotNull(Game.scene());
            boolean[] loaded={false};
            while(!loaded[0]&&SystemClock.uptimeMillis()<wait){TopScoresDeviceTest.render(()->{Map<?,?> ids=(Map<?,?>)get(idsField,Sample.INSTANCE);loaded[0]=ids.keySet().containsAll(selected);});if(!loaded[0])Thread.sleep(100);}
            assertTrue("real game assets loaded before sampling",loaded[0]);
            TopScoresDeviceTest.render(()->{
                @SuppressWarnings("unchecked") Map<Object,Sound> ids=(Map<Object,Sound>)get(idsField,Sample.INSTANCE);
                Sample.INSTANCE.enable(true);
                for(String asset:selected){
                    Sound original=ids.get(asset);assertTrue(original instanceof AsynchronousSound);originals.put(asset,original);
                    Handler handler=(Handler)get(handlerField,original);Sound endpoint=(Sound)get(endpointField,original);handlers.add(handler);
                    ArrayDeque<Event> queue=new ArrayDeque<>();pending.put(asset,queue);
                    Sound observedEndpoint=(Sound)Proxy.newProxyInstance(Sound.class.getClassLoader(),new Class[]{Sound.class},(proxy,method,args)->{
                        Event e=null;if(method.getName().equals("play")){synchronized(queue){e=queue.removeFirst();}e.dispatched=SystemClock.elapsedRealtimeNanos();}
                        Object result=method.invoke(endpoint,args);
                        if(e!=null){e.returned=SystemClock.elapsedRealtimeNanos();e.stream=((Number)result).longValue();}
                        return result;
                    });
                    AsynchronousSound asynchronous=new AsynchronousSound(observedEndpoint,handler);
                    Sound observedRequests=(Sound)Proxy.newProxyInstance(Sound.class.getClassLoader(),new Class[]{Sound.class},(proxy,method,args)->{
                        if(method.getName().equals("play")){Event e=new Event(asset);events.add(e);synchronized(queue){queue.addLast(e);}}
                        return method.invoke(asynchronous,args);
                    });
                    ids.put(asset,observedRequests);
                }
            });
            JSONArray scenarios=new JSONArray();
            for(int mix=0;mix<mixes.length;mix++)for(int interval:new int[]{500,125,16}){
                int first=events.size();long start=SystemClock.elapsedRealtimeNanos();
                for(int burst=0;burst<16;burst++){final String[] effects=mixes[mix];TopScoresDeviceTest.render(()->{for(String asset:effects)Sample.INSTANCE.play(asset);});Thread.sleep(interval);}
                for(Handler handler:new LinkedHashSet<>(handlers)){CountDownLatch drained=new CountDownLatch(1);handler.post(drained::countDown);assertTrue("real audio worker drained without an injected stall",drained.await(15,TimeUnit.SECONDS));}
                long maxQueue=0,maxEndpoint=0;int failedStarts=0;
                for(int i=first;i<events.size();i++){Event e=events.get(i);assertTrue(e.returned>=e.dispatched&&e.dispatched>=e.requested);maxQueue=Math.max(maxQueue,e.dispatched-e.requested);maxEndpoint=Math.max(maxEndpoint,e.returned-e.dispatched);if(e.stream<0)failedStarts++;}
                scenarios.put(new JSONObject().put("effect_mix",new String[]{"sewers_control","prison_assets","tengu_assets"}[mix]).put("requested_interval_ms",interval).put("requests",events.size()-first).put("elapsed_ms",(SystemClock.elapsedRealtimeNanos()-start)/1e6).put("maximum_queue_ms",maxQueue/1e6).put("maximum_endpoint_ms",maxEndpoint/1e6).put("failed_soundpool_starts",failedStarts));
            }
            JSONArray raw=new JSONArray();for(Event e:events)raw.put(new JSONObject().put("asset",e.asset).put("request_ns",e.requested).put("dispatch_ns",e.dispatched).put("returned_ns",e.returned).put("stream_id",e.stream));
            JSONObject report=new JSONObject().put("real_android_soundpool",true).put("injected_worker_stall",false).put("actual_encounter_played",false).put("acoustic_onset_measured",false).put("physical_phone_verified",false).put("scenarios",scenarios).put("events",raw);
            java.io.File file=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"audio-real-endpoint.json");try(java.io.FileWriter writer=new java.io.FileWriter(file)){writer.write(report.toString(2));}
            System.out.println("APD_REAL_AUDIO_ENDPOINT "+scenarios+" acoustic_onset_measured=false actual_encounter_played=false physical_phone_verified=false");
            TopScoresDeviceTest.render(()->{Map<Object,Sound> ids=(Map<Object,Sound>)get(idsField,Sample.INSTANCE);ids.putAll(originals);Sample.INSTANCE.enable(enabled);});originals.clear();
        } finally {
            if(!originals.isEmpty()&&com.badlogic.gdx.Gdx.app!=null)TopScoresDeviceTest.render(()->{Map<Object,Sound> ids=(Map<Object,Sound>)get(idsField,Sample.INSTANCE);ids.putAll(originals);Sample.INSTANCE.enable(enabled);});
        }
    }
    private static Object get(Field field,Object target){try{return field.get(target);}catch(IllegalAccessException error){throw new RuntimeException(error);}}
}
