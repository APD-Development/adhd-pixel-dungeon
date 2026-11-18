package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Named definitions, never frozen answers. Every evaluation selects the current catalogue again. */
public final class SavedExperiments {
    private SavedExperiments(){}
    public static final int MAX_EXPERIMENTS=50,MAX_QUESTION=240;
    private static final int MAX_BYTES=1048576;
    private static final Object LOCK=new Object();
    public static File file(File root){return new File(root,"research-experiments-v1.properties");}

    public static final class Experiment {
        public final String id,name,question,measure;
        public final Query a,b;
        public final long createdAtMillis;
        private Experiment(String id,String name,String question,Query a,Query b,String measure,long created){
            this.id=id;this.name=name;this.question=question;this.a=a;this.b=b;this.measure=measure;createdAtMillis=created;
        }
        public Evaluation evaluate(Catalog catalog,String currentRules,boolean allowInternal){
            if((a.showInternal || b.showInternal) && !allowInternal)
                return new Evaluation(null,null,null,"This experiment includes hidden-information viewing. Explicitly enable it in table Options first; unfinished-run secrets remain protected.");
            QueryResult ar=ResearchData.select(catalog,a,currentRules),br=ResearchData.select(catalog,b,currentRules);
            if(ar==null || br==null)return new Evaluation(ar,br,null,"A saved dataset, condition or rule is unavailable in this build. No condition was ignored and no answer was invented.");
            PopulationStudy.Comparison c=PopulationStudy.compare(PopulationStudy.sample(ar,measure),PopulationStudy.sample(br,measure));
            return new Evaluation(ar,br,c,c==null?"The saved metric is unavailable or incompatible with these populations. No value was substituted.":"");
        }
    }
    public static final class Evaluation {
        public final QueryResult a,b;public final PopulationStudy.Comparison comparison;public final String problem;
        private Evaluation(QueryResult a,QueryResult b,PopulationStudy.Comparison c,String p){this.a=a;this.b=b;comparison=c;problem=p;}
    }

    public static List<Experiment> load(File file)throws IOException{
        synchronized(LOCK){return Collections.unmodifiableList(read(file));}
    }
    public static Experiment create(File file,String name,String question,Query a,Query b,String measure,String rules,long now)throws IOException{
        synchronized(LOCK){
            String n=name(name);String text=question==null?"":question.trim();
            if(text.isEmpty() || text.length()>MAX_QUESTION || text.chars().anyMatch(Character::isISOControl))throw new IOException("Use a question of 1-240 characters on one line.");
            if(a==null || b==null || !a.dataset.equals(b.dataset) || measure==null || measure.isEmpty())throw new IOException("Capture both populations from the same table and choose a metric first.");
            ResearchPins pins;
            try{pins=new ResearchPins(null,null).with(true,a,rules).with(false,b,rules);}catch(RuntimeException e){throw new IOException("The population definitions could not be saved safely.",e);}
            List<Experiment> all=read(file);unique(all,n,null);
            if(all.size()>=MAX_EXPERIMENTS)throw new IOException("The limit is 50 saved experiments. Delete an unused definition first.");
            Experiment e=new Experiment(UUID.randomUUID().toString(),n,text,pins.a,pins.b,measure,now);all.add(e);write(file,all);return e;
        }
    }
    public static Experiment rename(File file,String id,String value)throws IOException{
        synchronized(LOCK){List<Experiment> all=read(file);String n=name(value);unique(all,n,id);
            for(int i=0;i<all.size();i++){Experiment e=all.get(i);if(e.id.equals(id)){
                Experiment replacement=new Experiment(e.id,n,e.question,e.a,e.b,e.measure,e.createdAtMillis);all.set(i,replacement);write(file,all);return replacement;
            }}throw new IOException("This experiment no longer exists. Reopen the list.");}
    }
    public static void delete(File file,String id)throws IOException{
        synchronized(LOCK){List<Experiment> all=read(file);if(!all.removeIf(e->e.id.equals(id)))throw new IOException("This experiment no longer exists.");write(file,all);}
    }
    private static String name(String n)throws IOException{
        String value=SavedViews.cleanName(n);if(value==null)throw new IOException("Use a name of 1-60 characters on one line.");return value;
    }
    private static void unique(List<Experiment> all,String name,String except)throws IOException{
        for(Experiment e:all)if(!e.id.equals(except)&&e.name.equalsIgnoreCase(name))throw new IOException("An experiment already has that name. Choose another name; nothing was overwritten.");
    }
    private static Query query(String value)throws IOException{
        try{Query q=QueryCodec.decode(value);if(q==null || !QueryCodec.encode(q).equals(value) || q.group!=null || q.scope.rules==Scope.Rules.CURRENT)throw new IllegalArgumentException();return q;}
        catch(RuntimeException e){throw new IOException("An experiment query cannot be read safely; the original file is untouched.",e);}
    }
    private static String hash(String text){
        try{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private static String payload(Experiment e){return String.join("\u0000",e.id,e.name,e.question,QueryCodec.encode(e.a),QueryCodec.encode(e.b),e.measure,Long.toString(e.createdAtMillis));}
    private static List<Experiment> read(File file)throws IOException{
        List<Experiment> out=new ArrayList<>();if(!file.exists())return out;
        if(file.length()>MAX_BYTES)throw new IOException("Experiment file is too large; it is left untouched.");
        Properties p=new Properties();try(InputStream in=new FileInputStream(file)){p.load(in);}catch(IllegalArgumentException e){throw new IOException("Damaged experiment file; nothing was overwritten.",e);}
        if(!"1".equals(p.getProperty("version")))throw new IOException("Unsupported experiment file version; it is left untouched.");
        try{
            int count=Integer.parseInt(p.getProperty("count"));if(count<0 || count>MAX_EXPERIMENTS || p.size()!=2+count*8)throw new IllegalArgumentException();
            Set<String> ids=new HashSet<>();
            for(int i=0;i<count;i++){
                String k=i+".";String id=p.getProperty(k+"id"),n=name(p.getProperty(k+"name")),question=p.getProperty(k+"question"),measure=p.getProperty(k+"measure");
                if(id==null || id.isEmpty() || !ids.add(id) || question==null || question.isEmpty() || question.length()>MAX_QUESTION || question.chars().anyMatch(Character::isISOControl) || measure==null || measure.isEmpty())throw new IllegalArgumentException();
                Query a=query(p.getProperty(k+"a")),b=query(p.getProperty(k+"b"));if(!a.dataset.equals(b.dataset))throw new IllegalArgumentException();
                Experiment e=new Experiment(id,n,question,a,b,measure,Long.parseLong(p.getProperty(k+"created")));unique(out,n,null);
                if(!hash(payload(e)).equals(p.getProperty(k+"sha256")))throw new IllegalArgumentException();out.add(e);
            }
        }catch(RuntimeException e){throw new IOException("Damaged experiment definitions; the original file is untouched.",e);}
        out.sort(Comparator.comparing(e->e.name.toLowerCase(Locale.ROOT)));return out;
    }
    private static void write(File file,List<Experiment> all)throws IOException{
        Properties p=new Properties();p.setProperty("version","1");p.setProperty("count",Integer.toString(all.size()));
        for(int i=0;i<all.size();i++){Experiment e=all.get(i);String k=i+".";p.setProperty(k+"id",e.id);p.setProperty(k+"name",e.name);p.setProperty(k+"question",e.question);p.setProperty(k+"a",QueryCodec.encode(e.a));p.setProperty(k+"b",QueryCodec.encode(e.b));p.setProperty(k+"measure",e.measure);p.setProperty(k+"created",Long.toString(e.createdAtMillis));p.setProperty(k+"sha256",hash(payload(e)));}
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();p.store(bytes,"UIMSD named experiment definitions; results recalculated on opening");
        if(bytes.size()>MAX_BYTES)throw new IOException("Experiment definitions are too large to save safely.");
        File parent=file.getAbsoluteFile().getParentFile();if(!parent.isDirectory()&&!parent.mkdirs()&&!parent.isDirectory())throw new IOException("Cannot create the experiment folder.");
        File temp=File.createTempFile("experiments-",".tmp",parent);
        try{try(FileOutputStream stream=new FileOutputStream(temp)){bytes.writeTo(stream);stream.getFD().sync();}
            try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        }finally{if(temp.exists())temp.delete();}
    }
}
