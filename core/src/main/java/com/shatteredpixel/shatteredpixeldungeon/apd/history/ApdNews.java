package com.shatteredpixel.shatteredpixeldungeon.apd.history;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** Offline articles; draft content never enters the visible list. No transport or automatic publication. */
public final class ApdNews {
 public static final class Article {
  public final String id,status,date,sprite,title,text,destination;
  Article(String[] f){id=f[0];status=f[1];date=f[2];sprite=f[3];title=f[4];text=f[5].replace("\\n","\n");destination=f[6];}
  public String displayDate(){return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy",Locale.ENGLISH));}
 }
 private ApdNews(){}
 public static List<Article> published(String data){
  List<Article> result=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(String line:data.split("\n")){
   if(line.trim().isEmpty()||line.startsWith("#"))continue;
   String[] f=line.split("\t",-1);
   if(f.length!=7||!f[0].matches("[a-z0-9-]+")||!ids.add(f[0]))throw new IllegalArgumentException("Invalid or duplicate article");
   if(!f[1].equals("published")&&!f[1].equals("draft"))throw new IllegalArgumentException("Unknown publication status");
   LocalDate.parse(f[2]);
   if(f[4].isEmpty()||f[5].isEmpty())throw new IllegalArgumentException("Missing article content");
   if(!f[6].isEmpty()&&!f[6].equals("UIMSD")&&!f[6].equals("COMMUNITY"))throw new IllegalArgumentException("Unknown destination");
   if(f[1].equals("published"))result.add(new Article(f));
  }
  result.sort(Comparator.comparing((Article a)->a.date).reversed().thenComparing(a->a.id));
  return Collections.unmodifiableList(result);
 }
 public static List<Article> page(List<Article> articles,int page){
  int start=(int)Math.min(articles.size(),Math.max(0L,(long)page)*ApdHistory.PAGE_SIZE);
  return articles.subList(start,Math.min(articles.size(),start+ApdHistory.PAGE_SIZE));
 }
}
