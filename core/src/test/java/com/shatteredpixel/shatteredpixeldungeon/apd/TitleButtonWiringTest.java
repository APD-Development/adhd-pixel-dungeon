package com.shatteredpixel.shatteredpixeldungeon.apd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.UimsdScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Behavioural check of the title screen's UIMSD button. It does not read source text: it takes the COMPILED click handler of the
//button (the same class the APK contains), runs its onClick(), and looks at which scene the game was then asked to show.
//
//The title scene itself needs graphics to be built, so the handler object is created without running its constructor; its
//onClick() only needs the static scene-switching state.
public class TitleButtonWiringTest {

	private Game previousInstance;
	private Preferences previousPrefs;

	@Before
	public void setUp() throws Exception {
		previousInstance = Game.instance;
		new Game(TitleScene.class, null);
		SPDSettings.set(defaultsOnlyPreferences());
	}

	@After
	public void tearDown() {
		Game.instance = previousInstance;
		SPDSettings.set(null);
	}

	//settings that were never saved: every read gives the default it was asked with
	private static Preferences defaultsOnlyPreferences(){
		return (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(), new Class[]{ Preferences.class }, new InvocationHandler(){
			@Override
			public Object invoke( Object proxy, Method m, Object[] args ){
				//a player who has unlocked UIMSD (the gate is on in every build since Nov 18), so the button goes straight to it
				if (m.getName().equals("getBoolean") && args != null && SPDSettings.KEY_APD_UIMSD_UNLOCKED.equals(args[0])) return true;
				if (m.getName().startsWith("get") && args != null && args.length == 2) return args[1];
				if (m.getName().equals("contains")) return false;
				Class<?> r = m.getReturnType();
				if (r == boolean.class) return false;
				if (r == int.class) return 0;
				if (r == long.class) return 0L;
				if (r == float.class) return 0f;
				return null;
			}
		});
	}

	private static Object allocate( Class<?> c ) throws Exception {
		Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
		Field f = unsafeClass.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		Object unsafe = f.get(null);
		return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, c);
	}

	private static Class<?> requestedScene() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		return (Class<?>) f.get(null);
	}

	private static void forgetRequest() throws Exception {
		Field f = Game.class.getDeclaredField("sceneClass");
		f.setAccessible(true);
		f.set(null, TitleScene.class);
		Field r = Game.class.getDeclaredField("requestedReset");
		r.setAccessible(true);
		r.set(Game.instance, false);
	}

	//the compiled anonymous classes of TitleScene's buttons, in the order the compiler numbered them (source order)
	private static List<Class<?>> clickHandlers() throws Exception {
		List<Class<?>> out = new ArrayList<>();
		for (int i = 1; i < 40; i++){
			Class<?> c;
			try {
				c = Class.forName(TitleScene.class.getName() + "$" + i);
			} catch (ClassNotFoundException e){
				continue;
			}
			try {
				c.getDeclaredMethod("onClick");
				out.add(c);
			} catch (NoSuchMethodException ignored){
			}
		}
		return out;
	}

	//which scene the button handler asks for
	private static Class<?> sceneAfterClick( Class<?> handler ) throws Exception {
		forgetRequest();
		Method onClick = handler.getDeclaredMethod("onClick");
		onClick.setAccessible(true);
		onClick.invoke(allocate(handler));
		return requestedScene();
	}

	//the number javac gave the UIMSD button's class: the buttons are the anonymous classes of the scene, numbered in source order
	private static int uimsdButtonNumber() throws Exception {
		String src = new String(Files.readAllBytes(new File(System.getProperty("apd.game.src"), "scenes/TitleScene.java").toPath()), "UTF-8");
		int at = src.indexOf("new APDMenuButton( \"UIMSD\"");
		assertTrue("the UIMSD button is declared in TitleScene", at > 0);
		//count the anonymous classes ('new X(...){') that open before it
		Matcher m = Pattern.compile("new\\s+[A-Za-z_.]+\\s*\\([^;{}]*\\)\\s*\\{").matcher(src.substring(0, at));
		int n = 0;
		while (m.find()) n++;
		return n + 1;
	}

	@Test
	public void theUimsdButtonsHandlerAsksForTheUimsdScreen() throws Exception {
		Class<?> handler = Class.forName(TitleScene.class.getName() + "$" + uimsdButtonNumber());
		Class<?> scene = sceneAfterClick(handler);
		assertEquals("tapping the title screen's UIMSD button", UimsdScene.class, scene);
		assertFalse(scene == RankingsScene.class);
	}

	private static byte[] compiledBytes(Class<?> c)throws Exception{
		try(java.io.InputStream in=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){
			assertNotNull(c.getName(),in);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];
			for(int n;(n=in.read(buf))>0;)out.write(buf,0,n);return out.toByteArray();
		}
	}
	private static Class<?> topScoresHandler()throws Exception{
		Class<?> found=null;
		for(Class<?> c:clickHandlers())if(new String(compiledBytes(c),"ISO-8859-1").contains("RankingsScene")){
			assertTrue("Only the approved Top Scores handler references Rankings",found==null);found=c;
		}
		assertNotNull("Top Scores targets original Rankings",found);
		assertFalse(new String(compiledBytes(TitleScene.class),"ISO-8859-1").contains("RankingsScene"));
		return found;
	}
	private static void clickTopScores(boolean android)throws Exception{
		Class<?> handler=topScoresHandler();Object button=allocate(handler);
		Field platform=handler.getDeclaredField("val$androidTopScores");platform.setAccessible(true);platform.setBoolean(button,android);
		Method click=handler.getDeclaredMethod("onClick");click.setAccessible(true);click.invoke(button);
	}
	private static class TrackingGame extends Game{
		boolean finished;TrackingGame(){super(TitleScene.class,null);}
		@Override public void finish(){finished=true;}
	}
	@Test public void compiledAndroidTopScoresTargetsOriginalRankingsWithoutExiting()throws Exception{
		TrackingGame game=new TrackingGame();forgetRequest();clickTopScores(true);
		assertEquals(RankingsScene.class,requestedScene());assertFalse(game.finished);
	}
	@Test public void compiledDesktopQuitStillFinishesWithoutOpeningRankings()throws Exception{
		TrackingGame game=new TrackingGame();forgetRequest();clickTopScores(false);
		assertEquals(TitleScene.class,requestedScene());assertTrue(game.finished);
	}

	@Test
	public void theGameCanActuallyCreateTheUimsdScreen() {
		//Game.step() silently ignores a scene change when the scene cannot be created, so this must work
		assertNotNull(Reflection.newInstance(UimsdScene.class));
	}

	@Test
	public void openingFromTheTitleMakesBackReturnToTheTitle() throws Exception {
		forgetRequest();
		UimsdScene.open(TitleScene.class);
		assertEquals(UimsdScene.class, requestedScene());
		assertEquals(TitleScene.class, UimsdScene.backTo);
	}
}
