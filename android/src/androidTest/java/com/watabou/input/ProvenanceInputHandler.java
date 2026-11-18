package com.watabou.input;
import com.badlogic.gdx.Input;
/** Diagnostic-only execution of the consolidated Back handler on the existing APK. */
public class ProvenanceInputHandler extends InputHandler {
 private final Input backend;
 private boolean accepted;
 private com.watabou.noosa.Scene acceptedScene;
 private void log(String message){android.util.Log.w("APDBACKPROTOTYPE",message+" scene="+(com.watabou.noosa.Game.instance==null?"none":String.valueOf(com.watabou.noosa.Game.scene()))+" accepted="+accepted+" pending="+KeyEvent.hasPendingBackPress());}
 public ProvenanceInputHandler(Input input){super(input);backend=input;ProvenanceBackGate.setSystemBackOwner(this);ProvenanceBackGate.reset();}
 private long stamp(){long n=backend.getCurrentEventTime();return n>0&&n<=System.nanoTime()?n:0;}
 @Override public synchronized boolean keyDown(int code){
  if(code!=Input.Keys.BACK)return super.keyDown(code);
  long source=stamp();
  log("raw-down source="+source);
  if(!ProvenanceBackGate.acceptLegacy(System.nanoTime(),source)){log("down-rejected");return true;}
  accepted=true;acceptedScene=com.watabou.noosa.Game.instance==null?null:com.watabou.noosa.Game.scene();log("down-accepted");
  if(KeyBindings.isKeyBound(code)){KeyEvent event=new KeyEvent(code,true);event.sourceEventNanos=source;KeyEvent.addKeyEvent(event);return true;}
  return false;
 }
 @Override public synchronized boolean keyUp(int code){
  if(code!=Input.Keys.BACK)return super.keyUp(code);
  long source=stamp();log("raw-up source="+source);ProvenanceBackGate.recordLegacyRelease(source);
  if(!accepted){log("up-orphan");return true;}accepted=false;acceptedScene=null;log("up-accepted");
  if(KeyBindings.isKeyBound(code)){KeyEvent event=new KeyEvent(code,false);event.sourceEventNanos=source;KeyEvent.addKeyEvent(event);return true;}
  return false;
 }
 @Override public synchronized void scenePresented(long completed){
  super.scenePresented(completed);
  boolean sameScene=com.watabou.noosa.Game.instance!=null&&acceptedScene==com.watabou.noosa.Game.scene();
  log("presented same="+sameScene);
  if(!sameScene&&!KeyEvent.hasPendingBackPress()){accepted=false;acceptedScene=null;}
 }
}
