package com.shatteredpixel.shatteredpixeldungeon.apd.query;
import java.lang.management.ManagementFactory;
import java.util.*;
public class AggregateAllocationProbe {
 public static void main(String[] args) {
  DatasetDef d=QuerySupport.def(); List<Row> rows=new ArrayList<>();
  for(int i=0;i<1000;i++) rows.add(QuerySupport.row(d,""+i,"r"+i,(long)i,0L,0L,true,"r"));
  Query q=Query.of("things"); Aggregate a=Aggregate.of(Aggregate.Fn.SUM,"score");
  for(int i=0;i<10000;i++) if(QueryEngine.evaluate(d,q,a,rows).value!=499500) throw new AssertionError();
  com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
  bean.setThreadAllocatedMemoryEnabled(true); long id=Thread.currentThread().getId();
  long before=bean.getThreadAllocatedBytes(id), start=System.nanoTime();
  for(int i=0;i<20000;i++) if(QueryEngine.evaluate(d,q,a,rows).value!=499500) throw new AssertionError();
  System.out.println("calls=20000 allocatedBytes="+(bean.getThreadAllocatedBytes(id)-before)+" elapsedNanos="+(System.nanoTime()-start));
 }
}

