package com.shatteredpixel.shatteredpixeldungeon.apd.stats;
import java.io.*;import java.nio.file.*;import com.shatteredpixel.shatteredpixeldungeon.apd.query.*;
public class LegacyUtf8CacheProbe {
 public static void main(String[] args)throws Exception {
  File root=new File(args[0]),runs=new File(root,"runs"),raw=new File(runs,"badtext.apdlog");runs.mkdirs();
  if(args.length>1){
   ByteSink body=new ByteSink().writeUnsigned(TelemetryVersion.CURRENT.major).writeUnsigned(TelemetryVersion.CURRENT.minor)
     .writeUnsigned(1).writeString("badtext").writeUnsigned(2).writeBytes(new byte[]{(byte)0xC0,(byte)0xAF});
   ByteSink file=new ByteSink().writeBytes(LogFormat.MAGIC).writeByte(LogFormat.CONTAINER_FORMAT).writeByte(body.size()>>>8).writeByte(body.size()).writeBytes(body.toByteArray());
   file.writeInt32((int)LogFormat.crc(file.rawBuffer(),4,file.size()-4));Files.write(raw.toPath(),file.toByteArray());
  }
  byte[] before=Files.readAllBytes(raw.toPath());
  RunIndex index=RunIndex.load(new File(root,"index.apdi"),runs);
  DerivedStore derived=new DerivedStore(root);FactTable facts=derived.load();Catalog catalog=new DatasetStore(root).load();
  Dataset ds=catalog.dataset("runs");
  System.out.println("index="+index.get("badtext").status+" rebuilt="+index.wasRebuilt()+" derived="+derived.lastSource()
    +" status="+facts.apdRows.get(0).textOr(FactKeys.STATUS,"?")+" rowsCached="+catalog.logsCached+" rowsRebuilt="+catalog.logsRebuilt
    +" queryStatus="+ds.rows.get(0).text(ds.def.columns().indexOf(ds.def.column(FactKeys.STATUS))));
  if(!java.util.Arrays.equals(before,Files.readAllBytes(raw.toPath())))throw new AssertionError("source bytes changed");
 }
}
