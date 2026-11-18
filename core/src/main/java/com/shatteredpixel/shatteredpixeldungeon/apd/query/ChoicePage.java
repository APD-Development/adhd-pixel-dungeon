package com.shatteredpixel.shatteredpixeldungeon.apd.query;

//A presentation page, never a limit on the available comparison records.
public final class ChoicePage {
 public static final int PAGE_SIZE = 40;
 public final int page, pageCount, start, end;
 public ChoicePage(int total, int requestedPage){
  if(total<0)throw new IllegalArgumentException("negative choice count");
  pageCount=Math.max(1, total/PAGE_SIZE + (total%PAGE_SIZE==0?0:1));
  page=Math.max(0,Math.min(requestedPage,pageCount-1));
  start=page*PAGE_SIZE;
  end=start+Math.min(PAGE_SIZE,total-start);
 }
 public boolean hasPrevious(){return page>0;}
 public boolean hasNext(){return page+1<pageCount;}
}
