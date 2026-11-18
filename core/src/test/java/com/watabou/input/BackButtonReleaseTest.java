package com.watabou.input;
import com.badlogic.gdx.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.HeadlessTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackButtonReleaseTest {
 private InputHandler handler(long stamp){
  Input source=(Input)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Input.class},(p,m,a)->{
   if(m.getName().equals("getCurrentEventTime"))return stamp;
   Class<?> t=m.getReturnType();if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;if(t==float.class)return 0f;return null;
  });
  return new InputHandler(source);
 }
 private Button button(int[] clicks){
  return new Button(){
   @Override public GameAction keyAction(){return GameAction.BACK;}
   @Override protected void onClick(){clicks[0]++;}
  };
 }
 @Test public void registeredFallbackCompletesARealBackBoundButtonClick(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(System.nanoTime());int[] clicks={0};Button button=button(clicks);
  try{
   handler.keyDown(Input.Keys.BACK);handler.keyUp(Input.Keys.BACK);KeyEvent.processKeyEvents();
   assertEquals("The real Back button activates only after its matching release",1,clicks[0]);
  }finally{button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();}
 }
 @Test public void rejectingARepeatedDownDoesNotLoseTheAcceptedPressRelease(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(System.nanoTime());int[] clicks={0};Button button=button(clicks);
  try{
   handler.keyDown(Input.Keys.BACK);handler.keyDown(Input.Keys.BACK);handler.keyUp(Input.Keys.BACK);handler.keyUp(Input.Keys.BACK);
   KeyEvent.processKeyEvents();assertEquals("Exactly one completed click; no orphan release",1,clicks[0]);
  }finally{button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();}
 }
 @Test public void deliveredSystemPressDoesNotLetItsOldBackendCopyClickAgain(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  long stamp=System.nanoTime();Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(stamp);int[] clicks={0};Button button=button(clicks);
  try{
   assertTrue(BackGate.acceptSystem(stamp+1));
   handler.keyDown(Input.Keys.BACK);handler.keyUp(Input.Keys.BACK);KeyEvent.processKeyEvents();
   assertEquals("The already handled press cannot activate another Back-bound button",0,clicks[0]);
  }finally{button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();}
 }

 @Test public void aQueuedBackKeepsItsReleaseAcrossTheFirstPresentation(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(System.nanoTime());int[] clicks={0};Button button=button(clicks);
  try{
   handler.keyDown(Input.Keys.BACK);handler.scenePresented(System.nanoTime());handler.keyUp(Input.Keys.BACK);
   KeyEvent.processKeyEvents();assertEquals("A queued Back remains a complete press in the new scene",1,clicks[0]);
  }finally{button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();}
 }
 @Test public void presentationCancelsAReleaseWhoseDownWasAlreadyDispatched(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  com.watabou.noosa.Game previous=com.watabou.noosa.Game.instance;
  SceneGame game=new SceneGame();Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(System.nanoTime());int[] clicks={0},events={0};Button button=button(clicks);
  com.watabou.utils.Signal.Listener<KeyEvent> observer=event->{events[0]++;return false;};
  try{
   handler.keyDown(Input.Keys.BACK);KeyEvent.processKeyEvents();
   Button.cancelPressedButton();game.nextScene();handler.scenePresented(System.nanoTime());
   KeyEvent.addKeyListener(observer);handler.keyUp(Input.Keys.BACK);KeyEvent.processKeyEvents();
   assertEquals("A release cannot leak into the newly presented scene",0,events[0]);
   assertEquals(0,clicks[0]);
  }finally{KeyEvent.removeKeyListener(observer);button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();com.watabou.noosa.Game.instance=previous;}
 }
 private static class SceneGame extends com.watabou.noosa.Game {
  void nextScene(){scene=new com.watabou.noosa.Scene();}
  SceneGame(){super(com.watabou.noosa.Scene.class,com.watabou.noosa.Game.platform);scene=new com.watabou.noosa.Scene();requestedReset=false;}
 }
 @Test public void firstDrawOfTheSameSceneKeepsAnAlreadyDispatchedBackRelease(){
  HeadlessTestSupport.start();KeyBindings.addHardBinding(Input.Keys.BACK,GameAction.BACK);
  com.watabou.noosa.Game previous=com.watabou.noosa.Game.instance;
  new SceneGame();Object owner=new Object();BackGate.setSystemBackOwner(owner);BackGate.reset();
  InputHandler handler=handler(System.nanoTime());int[] clicks={0};Button button=button(clicks);
  try{
   handler.keyDown(Input.Keys.BACK);KeyEvent.processKeyEvents();
   handler.scenePresented(System.nanoTime());handler.keyUp(Input.Keys.BACK);KeyEvent.processKeyEvents();
   assertEquals("First draw of the same scene must not strand its armed Back button",1,clicks[0]);
  }finally{button.destroy();BackGate.clearSystemBackOwner(owner);BackGate.reset();InputHandler.discardPendingEvents();com.watabou.noosa.Game.instance=previous;}
 }

}
