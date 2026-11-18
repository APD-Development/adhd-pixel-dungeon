package com.shatteredpixel.shatteredpixeldungeon.ui;
import com.watabou.noosa.Group;
import org.junit.Test;
public class UimsdDestroyedContentTest {
 @Test public void lateClearAfterDestructionKeepsNativeGroupNoOpBehavior(){
  Group baseline=new Group();baseline.destroy();baseline.clear();
  UimsdContent content=new UimsdContent();content.destroy();content.clear();
 }
}
