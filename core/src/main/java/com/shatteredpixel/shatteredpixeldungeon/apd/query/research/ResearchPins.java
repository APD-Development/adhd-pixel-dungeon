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


package com.shatteredpixel.shatteredpixeldungeon.apd.query.research;

import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Two separately versioned query snapshots; never writes saved views, run logs or statistics. */
public final class ResearchPins {
    public final Query a,b;
    public ResearchPins(Query a,Query b){this.a=a;this.b=b;}
    public ResearchPins with(boolean first,Query q,String currentRules){
        Query pinned=ResearchData.rows(q);
        if(pinned.scope.rules==Scope.Rules.CURRENT && (currentRules==null || currentRules.isEmpty()))throw new IllegalArgumentException("Current rules are unknown; choose explicit rules first.");
        if(pinned.scope.rules==Scope.Rules.CURRENT)pinned=pinned.withScope(pinned.scope.withRules(Scope.Rules.SPECIFIC,currentRules));
        pinned=QueryCodec.decode(QueryCodec.encode(pinned));
        if(pinned==null)throw new IllegalArgumentException("The population query cannot be stored safely.");
        return first ? new ResearchPins(pinned,b):new ResearchPins(a,pinned);
    }
    public static File file(File root){return new File(root,"research-populations-v1.properties");}
    public static ResearchPins load(File file)throws IOException{
        if(!file.exists())return new ResearchPins(null,null);
        if(file.length()>524288)throw new IOException("Population snapshot file is too large; left untouched.");
        Properties p=new Properties();try(InputStream in=new FileInputStream(file)){try{p.load(in);}catch(IllegalArgumentException e){throw new IOException("Damaged population encoding; file left untouched.",e);}}
        if(!"1".equals(p.getProperty("version")))throw new IOException("Unsupported population snapshot version; left untouched.");
        Query a=decode(p,"a"),b=decode(p,"b");return new ResearchPins(a,b);
    }
    private static Query decode(Properties p,String key)throws IOException{
        String s=p.getProperty(key);if(s==null)return null;Query q=QueryCodec.decode(s);
        if(q==null || !QueryCodec.encode(q).equals(s) || !hash(s).equals(p.getProperty(key+".sha256")))throw new IOException("Damaged population "+key+"; file left untouched.");return q;
    }
    private static String hash(String text){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:digest)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public void store(File file)throws IOException{
        Properties p=new Properties();p.setProperty("version","1");if(a!=null){p.setProperty("a",QueryCodec.encode(a));p.setProperty("a.sha256",hash(QueryCodec.encode(a)));}if(b!=null){p.setProperty("b",QueryCodec.encode(b));p.setProperty("b.sha256",hash(QueryCodec.encode(b)));}
        if(p.toString().length()>400000)throw new IOException("Population snapshots are too large to save safely");
        File parent=file.getAbsoluteFile().getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IOException("Cannot create population folder");
        File tmp=File.createTempFile("research-", ".tmp",parent);
        try{try(FileOutputStream out=new FileOutputStream(tmp)){p.store(out,"UIMSD research query snapshots");out.getFD().sync();}
            try{Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        }finally{if(tmp.exists())tmp.delete();}
    }
}
