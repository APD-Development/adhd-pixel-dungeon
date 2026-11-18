package com.shatteredpixel.shatteredpixeldungeon.apd.history;

import java.util.*;
/** Offline build metadata. Build and TEST identifiers are deliberately separate. */
public final class ApdHistory {
 public static final int PAGE_SIZE=12;
 public static final class Build {
  public final String id,label,date,status;
  Build(String[] fields){id=fields[0];label=fields[1];date=fields[2];status=fields[3];}
 }
 public static final class Entry {
  public final String category,sprite,title,text;
  Entry(String[] f){category=f[0];sprite=f[1];title=f[2];text=f[3].replace("\\n","\n");}
 }
 private ApdHistory(){}
 public static List<Build> builds(String data){
  List<Build> result=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(String line:data.split("\n")){
   if(line.trim().isEmpty()||line.startsWith("#"))continue;
   String[] f=line.split("\t",-1);
   if(f.length!=4||!f[0].matches("[a-z0-9-]+")||!ids.add(f[0]))throw new IllegalArgumentException("Invalid or duplicate build identifier");
   result.add(new Build(f));
  }return Collections.unmodifiableList(result);
 }
 public static List<Entry> entries(String data){
  List<Entry> result=new ArrayList<>();
  for(String line:data.split("\n")){
   if(line.trim().isEmpty()||line.startsWith("#"))continue;
   String[] f=line.split("\t",-1);
   if(f.length!=4||f[2].isEmpty()||f[3].isEmpty())throw new IllegalArgumentException("Invalid history entry");
   result.add(new Entry(f));
  }return Collections.unmodifiableList(result);
 }
 /** Keep every entry, with one native category heading in first-seen order. */
 public static Map<String,List<Entry>> categories(List<Entry> entries){
  Map<String,List<Entry>> grouped=new LinkedHashMap<>();
  for(Entry entry:entries)grouped.computeIfAbsent(entry.category,k->new ArrayList<>()).add(entry);
  Map<String,List<Entry>> result=new LinkedHashMap<>();
  for(Map.Entry<String,List<Entry>> group:grouped.entrySet())result.put(group.getKey(),Collections.unmodifiableList(group.getValue()));
  return Collections.unmodifiableMap(result);
 }
 public static int clamp(int selected,int size){return Math.max(0,Math.min(selected,Math.max(0,size-1)));}
 public static List<Entry> page(List<Entry> entries,int page){
  int start=Math.min(entries.size(),Math.max(0,page)*PAGE_SIZE);
  return entries.subList(start,Math.min(entries.size(),start+PAGE_SIZE));
 }
}
