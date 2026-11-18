import com.shatteredpixel.shatteredpixeldungeon.apd.stats.*;
import java.util.*;import java.lang.management.ManagementFactory;
public class TimelineAllocationProbe {
 public static void main(String[] args){
  List<ApdEvent> events=new ArrayList<>();events.add(new ApdEvent(0,EventTypes.SAVE,0,0,new byte[0]));
  for(int i=1;i<=40000;i++)events.add(new ApdEvent(i,EventTypes.LEVEL_ENTER,0,i,new byte[0]));
  byte[] payload=new ByteSink().writeUnsigned(0).writeSigned(0).writeUnsigned(0).writeSigned(0).toByteArray();
  for(int i=40001;i<=41000;i++)events.add(new ApdEvent(i,EventTypes.ROLLBACK,0,i,payload));
  for(int i=0;i<2;i++)RunTimeline.of(events);
  com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
  long id=Thread.currentThread().getId(),before=bean.getThreadAllocatedBytes(id),start=System.nanoTime(),hash=0;
  for(int i=0;i<3;i++){
   RunTimeline t=RunTimeline.of(events);if(t.abandonedCount()!=40000 || t.rollbackCount()!=1000 || t.canonical().size()!=1001)throw new AssertionError();
   for(ApdEvent e:t.canonical())hash=31*hash+e.index;
   for(ApdEvent e:t.abandoned())hash=31*hash+e.index;
  }
  System.out.println("allocated="+(bean.getThreadAllocatedBytes(id)-before)+" ns="+(System.nanoTime()-start)+" hash="+hash);
 }
}
