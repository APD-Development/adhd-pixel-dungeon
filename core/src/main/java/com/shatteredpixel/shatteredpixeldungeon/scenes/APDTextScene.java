/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdHistory;
import com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdNews;
import com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdReleaseNotes;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdText;
import com.watabou.noosa.*;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;
import java.util.*;

public class APDTextScene extends PixelScene {
 public enum Page {NEWS,CHANGES}
 public static Page page=Page.NEWS;
 public static Class<? extends PixelScene> backTo=AboutScene.class;
 private static int buildSelected,newsPage;
 private static boolean technicalHistory;
 private static final int BUILDS_PER_PAGE=6;
 private final List<ChangeInfo> changes=new ArrayList<>();
 private static String authored(String text){return com.shatteredpixel.shatteredpixeldungeon.apd.text.AuthoredLanguage.english(text,SPDSettings.apdAdultHumor());}
 private String read(String path){try{return Gdx.files.internal(path).readString("UTF-8");}catch(Exception e){return "";}}
 private Group visiblePage;
 @Override public void create(){
  super.create();
  int w=Camera.main.width,h=Camera.main.height;
  add(new TitleBackground(w,h));add(new ColorBlock(w,h,0x44000000));
  refreshPage();
 }
 // A page change replaces only page widgets and their own cameras/listeners.
 // Keep the scene, background and main scene input routing alive.
 private void refreshPage(){
  if(visiblePage!=null){remove(visiblePage);visiblePage.destroy();}
  visiblePage=new Group();add(visiblePage);changes.clear();
  int w=Camera.main.width,h=Camera.main.height;
  RectF insets=getCommonInsets();
  Component title;
  if(page==Page.CHANGES){
   com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle nativeTitle=new com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle(Icons.CHANGES.get(),"APD Changes");
   nativeTitle.setSize(135,0);nativeTitle.setPos(insets.left+(w-insets.left-insets.right-nativeTitle.reqWidth())/2f,insets.top+(20-nativeTitle.height())/2f);align(nativeTitle);title=nativeTitle;
  }else{
   RenderedTextBlock newsTitle=renderTextBlock("APD News",12);newsTitle.hardlight(Window.TITLE_COLOR);newsTitle.setPos((w-newsTitle.width())/2,insets.top+6);title=newsTitle;
  }
  visiblePage.add(title);
  final int paneWidth=(int)Math.min(page==Page.CHANGES?135:220,w-insets.left-insets.right-12);
  final float left=insets.left+(w-insets.left-insets.right-paneWidth)/2f;
  ScrollPane pane=new ScrollPane(new Component()){
   @Override public void onClick(float x,float y){for(ChangeInfo change:changes)if(change.onClick(x,y))return;}
  };
  com.watabou.noosa.NinePatch panel=Chrome.get(page==Page.CHANGES?Chrome.Type.WINDOW:Chrome.Type.TOAST);
  if(page==Page.CHANGES)panel.hardlight(0xE8CC72);
  float paneTop=title.bottom()+6;
  float paneHeight=Math.max(20,h-insets.bottom-28-paneTop);
  panel.size(paneWidth+panel.marginHor(),paneHeight+panel.marginVer());
  panel.x=left-panel.marginLeft();panel.y=paneTop-panel.marginTop();
  visiblePage.add(panel);visiblePage.add(pane);Component content=pane.content();float y=0;
  int total=0;
  if(page==Page.CHANGES){
   List<ApdHistory.Build> builds=historyBuilds();
   total=builds.size();buildSelected=ApdHistory.clamp(buildSelected,total);
   if(total>0){
    for(int buildIndex=buildSelected;buildIndex<Math.min(total,buildSelected+BUILDS_PER_PAGE);buildIndex++){
    ApdHistory.Build build=builds.get(buildIndex);
    ChangeInfo heading=new ChangeInfo(authored(historyLabel(build)),true,build.date);
    heading.hardlight(Window.TITLE_COLOR);changes.add(heading);
    if(technicalHistory&&!build.status.isEmpty())heading.addButton(new ChangeButton(Icons.INFO.get(),"Build record",authored(build.label)+"\n\n"+authored(build.status)));
    List<ApdHistory.Entry> entries=technicalHistory?ApdHistory.entries(read("apd/history/"+build.id+".tsv")):ApdReleaseNotes.entries(read("apd/releases/"+build.id+".tsv"));
    if(entries.isEmpty())entries=ApdHistory.entries("History status\tV061_PAGE\tHistory unavailable\tVerified changes for this build have not yet been reconstructed.");
    for(Map.Entry<String,List<ApdHistory.Entry>> category:ApdHistory.categories(entries).entrySet()){
     String name=category.getKey().equals("Statistics / UIMSD")?"UIMSD":category.getKey().equals("Developer Commentary")?"Commentary":category.getKey();
     ChangeInfo info=new ChangeInfo(authored(name),false,null);info.hardlight(categoryColor(category.getKey()));
     for(ApdHistory.Entry entry:category.getValue())info.addButton(changeButton(build,entry));
     changes.add(info);
    }
    }
    // Same paired-category placement as ChangesScene.create (SPD), with the final odd row included.
    float posY=0,nextPosY=0;boolean second=false;boolean columns=paneWidth>=130;
    for(ChangeInfo info:changes){
     if(info.major||!columns){
      posY=nextPosY;second=false;info.setRect(0,posY,paneWidth,0);content.add(info);posY=nextPosY=info.bottom();
     }else if(!second){
      second=true;info.setRect(0,posY,paneWidth/2f,0);content.add(info);nextPosY=info.bottom();
     }else{
      second=false;info.setRect(paneWidth/2f,posY,paneWidth/2f,0);content.add(info);posY=nextPosY=Math.max(nextPosY,info.bottom());
     }
    }
    y=nextPosY;

   }
  }else{
   List<ApdNews.Article> articles=ApdNews.published(read("apd/news.tsv"));
   total=(articles.size()+ApdHistory.PAGE_SIZE-1)/ApdHistory.PAGE_SIZE;
   newsPage=ApdHistory.clamp(newsPage,total);
   for(ApdNews.Article article:ApdNews.page(articles,newsPage)){
    NewsCard card=new NewsCard(article);
    card.setRect(0,y,paneWidth,36);content.add(card);y=card.bottom()+3;
   }
  }
  if(total==0){RenderedTextBlock empty=renderTextBlock("No verified entries available.",6);empty.maxWidth(paneWidth);content.add(empty);y=empty.height();}
  content.setSize(paneWidth,y+4);
  pane.setRect(left,title.bottom()+6,paneWidth,Math.max(20,h-insets.bottom-28-title.bottom()-6));
  final int count=total;
  StyledButton older=new StyledButton(page==Page.CHANGES?Chrome.Type.GREY_BUTTON_TR:Chrome.Type.RED_BUTTON,"Older",6){@Override protected void onClick(){if(page==Page.CHANGES)buildSelected+=BUILDS_PER_PAGE;else newsPage++;refreshPage();}};
  StyledButton newer=new StyledButton(page==Page.CHANGES?Chrome.Type.GREY_BUTTON_TR:Chrome.Type.RED_BUTTON,"Newer",6){@Override protected void onClick(){if(page==Page.CHANGES)buildSelected=Math.max(0,buildSelected-BUILDS_PER_PAGE);else newsPage--;refreshPage();}};
  int selected=page==Page.CHANGES?buildSelected:newsPage;
  older.enable(selected+(page==Page.CHANGES?BUILDS_PER_PAGE:1)<count);newer.enable(selected>0);
  float buttonWidth=page==Page.CHANGES?(paneWidth-4)/3f:(paneWidth-2)/2f;
  newer.setRect(left,h-insets.bottom-22,buttonWidth,20);visiblePage.add(newer);
  float olderLeft=newer.right()+2;
  if(page==Page.CHANGES){
   StyledButton versions=new StyledButton(Chrome.Type.GREY_BUTTON_TR,"Versions",6){@Override protected void onClick(){showHistoryOptions();}};
   versions.setRect(olderLeft,newer.top(),buttonWidth,20);visiblePage.add(versions);olderLeft=versions.right()+2;
  }
  older.setRect(olderLeft,newer.top(),buttonWidth,20);visiblePage.add(older);
  ExitButton exit=new ExitButton(){@Override protected void onClick(){onBackPressed();}};
  exit.setPos(w-insets.right-exit.width(),insets.top);visiblePage.add(exit);
 }
 private List<ApdHistory.Build> historyBuilds(){
  return technicalHistory?ApdHistory.builds(read("apd/history/index.tsv")):ApdReleaseNotes.releases(read("apd/releases/index.tsv"));
 }
 private String historyLabel(ApdHistory.Build build){
  return technicalHistory?ApdReleaseNotes.technicalLabel(build):build.label;
 }
 private void showHistoryOptions(){
  List<String> options=Arrays.asList("Release notes","Build records (technical)","Choose version");
  addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList("History","",options,null,(index,checked)->{
   if(index==2)showBuildRanges(historyBuilds(),0);
   else{technicalHistory=index==1;buildSelected=0;refreshPage();}
  }));
 }
 private int categoryColor(String category){
  if(category.equals("Changes"))return 0xFFAA44;
  if(category.equals("Buffs"))return com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE;
  if(category.equals("Nerfs"))return com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE;
  if(category.equals("Bug Fixes"))return 0xFFFFFF;
  if(category.equals("Statistics / UIMSD"))return 0x44DD88;
  if(category.equals("Developer Commentary"))return 0xCCCCCC;
  return Window.TITLE_COLOR;
 }
 private void showBuildRanges(List<ApdHistory.Build> builds,int rangePage){
  int rangeCount=(builds.size()+ApdHistory.PAGE_SIZE-1)/ApdHistory.PAGE_SIZE;
  int first=rangePage*ApdHistory.PAGE_SIZE,last=Math.min(rangeCount,first+ApdHistory.PAGE_SIZE);
  List<String> labels=new ArrayList<>();
  for(int range=first;range<last;range++){
   int start=range*ApdHistory.PAGE_SIZE,end=Math.min(builds.size()-1,start+ApdHistory.PAGE_SIZE-1);
   labels.add(historyLabel(builds.get(start))+" - "+historyLabel(builds.get(end)));
  }
  final int ranges=labels.size();if(last<rangeCount)labels.add("Older ranges");if(rangePage>0)labels.add("Newer ranges");
  addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList("Versions","",labels,null,(index,checked)->{
   if(index<ranges)showBuildPage(builds,(first+index)*ApdHistory.PAGE_SIZE);
   else if(index==ranges&&last<rangeCount)showBuildRanges(builds,rangePage+1);
   else showBuildRanges(builds,rangePage-1);
  }));
 }
 private void showBuildPage(List<ApdHistory.Build> builds,int start){
  int end=Math.min(builds.size(),start+ApdHistory.PAGE_SIZE);List<String> labels=new ArrayList<>();
  for(int i=start;i<end;i++)labels.add(historyLabel(builds.get(i)));
  addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndUimsdList("Choose version","",labels,null,(index,checked)->{
   buildSelected=start+index;refreshPage();
  }));
 }
 private class NewsCard extends StyledButton {
  private final ApdNews.Article article;
  private BitmapText date;
  NewsCard(ApdNews.Article article){
   super(Chrome.Type.GREY_BUTTON_TR,authored(article.title),6);this.article=article;
   multiline=true;icon(APDTextScene.this.icon(article.sprite));
   date=new BitmapText(article.displayDate(),pixelFont);date.scale.set(PixelScene.align(0.5f));
   date.hardlight(0x888888);date.measure();add(date);
  }
  @Override protected void layout(){
   super.layout();
   icon.x=x+bg.marginLeft()+(16-icon.width())/2f;PixelScene.align(icon);
   text.maxWidth((int)Math.max(20,width-bg.marginHor()-20));
   text.setPos(x+bg.marginLeft()+18,y+bg.marginTop()+2);
   if(date!=null){date.x=x+width-bg.marginRight()-date.width()+1;date.y=y+height-bg.marginBottom()-date.height()+2;align(date);}
  }
  @Override protected void onClick(){
   APDTextScene.this.addToFront(new com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage(
    APDTextScene.this.icon(article.sprite),authored(article.title),article.displayDate()+"\n\n"+authored(article.text)));
  }
 }
 private Image icon(String sprite){
  if(sprite==null||sprite.isEmpty())return Icons.CHANGES.get();
  Integer item=com.shatteredpixel.shatteredpixeldungeon.apd.history.ApdSpriteRef.itemImage(sprite);
  if(item!=null)return new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(item);
  try{return sprite.startsWith("ICON_")?Icons.valueOf(sprite.substring(5)).get():ChangeIcons.valueOf(sprite).get();}catch(IllegalArgumentException e){return Icons.CHANGES.get();}
 }
 private ChangeButton changeButton(ApdHistory.Build build,ApdHistory.Entry entry){
  Image sprite;
  String reference=entry.sprite;
  if(entry.title.equals("Crystal path rewards")&&reference.equals("ICON_INFO"))reference="ITEM_CRYSTAL_KEY";
  if(!reference.equals("V061_PAGE"))sprite=icon(reference);
  else {
   String title=entry.title.toLowerCase(Locale.ROOT);
   sprite=title.contains("news")?Icons.NEWS.get():title.contains("about")||title.contains("popup")?Icons.INFO.get():title.contains("histor")?Icons.CHANGES.get():title.contains("shortcut")?Icons.RANKINGS.get():entry.category.equals("Statistics / UIMSD")?Icons.STATS.get():ChangeIcons.V081_MISC.get();
  }
  return new ChangeButton(sprite,authored(entry.title),authored(historyLabel(build))+"\n\n"+authored(entry.text));
 }
 @Override protected void onBackPressed(){
  technicalHistory=false;buildSelected=0;
  // The title should show immediately on Back; About uses its normal route.
  if(backTo==TitleScene.class)ShatteredPixelDungeon.switchNoFade(backTo);
  else ShatteredPixelDungeon.switchScene(backTo);
 }
}

