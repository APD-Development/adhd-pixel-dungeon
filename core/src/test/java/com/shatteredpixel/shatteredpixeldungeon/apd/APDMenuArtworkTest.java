package com.shatteredpixel.shatteredpixeldungeon.apd;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Pin the frame/swatch boundary that caused stretched silver strips on the menu. */
public class APDMenuArtworkTest {
 @Test public void buttonFrameEndsBeforeTransparentSeparatorAndOpaqueSwatch()throws Exception{
  File javaRoot=new File(System.getProperty("apd.game.src"));
  File main=javaRoot;
  while(main!=null&&!new File(main,"assets").isDirectory())main=main.getParentFile();
  assertNotNull("Find actual shipped assets",main);
  BufferedImage art=ImageIO.read(new File(main,"assets/interfaces/apd_button.png"));
  assertEquals(24,art.getWidth());assertEquals(24,art.getHeight());
  for(int y=18;y<21;y++)for(int x=0;x<24;x++)assertEquals("Frame separator is transparent",0,art.getRGB(x,y)>>>24);
  assertEquals("Actual frame bottom remains included",255,art.getRGB(12,17)>>>24);
  assertEquals("Lower opaque swatch must not become the button bottom",255,art.getRGB(12,21)>>>24);
 }
}
