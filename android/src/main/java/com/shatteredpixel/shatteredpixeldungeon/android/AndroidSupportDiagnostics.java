package com.shatteredpixel.shatteredpixeldungeon.android;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import com.watabou.utils.SupportDiagnostics;
import com.watabou.utils.SupportActionJournal;

/** Self-package OS exit evidence; no logcat/storage permissions or other-app reads. */
final class AndroidSupportDiagnostics {
    private static boolean handlerInstalled;
    static void initialize(Context activity){
        Context context=activity.getApplicationContext();
        // initialize() has not installed Gdx.files yet; use Android's own private files directory.
        SupportDiagnostics.configure(new java.io.File(context.getFilesDir(),"apd-diagnostics"),
                "Version: "+Game.version+"\nVersion code: "+Game.versionCode+"\nApplication ID: "+context.getPackageName()
                +"\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")");
        SupportActionJournal.configure(new java.io.File(context.getFilesDir(),"apd-diagnostics"));
        SupportDiagnostics.platformSnapshot=()->snapshot(context);
        if(!handlerInstalled){
            Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
            if(previous!=null){
                Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
                    try{SupportDiagnostics.uncaught(thread,error);}finally{previous.uncaughtException(thread,error);}
                });handlerInstalled=true;
            }
        }
        SupportDiagnostics.event("activity.create", "");
    }
    private static String snapshot(Context context){
        StringBuilder out=new StringBuilder("ANDROID MEMORY AND PROCESS EXIT EVIDENCE\n");
        out.append("APK SHA-256 (installed base): ").append(apkHash(context)).append('\n');
        out.append("Supported ABIs: ").append(java.util.Arrays.toString(Build.SUPPORTED_ABIS)).append('\n');
        android.util.DisplayMetrics display=context.getResources().getDisplayMetrics();
        out.append("Android display: ").append(display.widthPixels).append('x').append(display.heightPixels)
                .append(" densityDpi=").append(display.densityDpi).append("; game surface: ").append(Game.width).append('x').append(Game.height).append('\n');
        out.append("Native heap allocated bytes: ").append(Debug.getNativeHeapAllocatedSize()).append('\n');
        out.append("Current process PSS KiB: ").append(Debug.getPss()).append('\n');
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager==null)return out.append("ActivityManager unavailable.\n").toString();
        ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);
        out.append("System available bytes: ").append(memory.availMem).append("; low-memory threshold bytes: ").append(memory.threshold)
                .append("; current OS low-memory flag: ").append(memory.lowMemory).append('\n');
        if(Build.VERSION.SDK_INT>=30){
            try{
                java.util.List<ApplicationExitInfo> exits=manager.getHistoricalProcessExitReasons(context.getPackageName(),0,8);
                if(exits.isEmpty())out.append("No historical process-exit records supplied by Android.\n");
                for(ApplicationExitInfo exit:exits){
                    out.append("Exit timestamp ms: ").append(exit.getTimestamp()).append("; reason: ").append(reason(exit.getReason()))
                            .append(" (").append(exit.getReason()).append("); status: ").append(exit.getStatus())
                            .append("; PSS KiB: ").append(exit.getPss()).append("; RSS KiB: ").append(exit.getRss()).append('\n');
                }
            }catch(RuntimeException error){out.append("Process-exit evidence unavailable: ").append(error.getClass().getSimpleName()).append('\n');}
        }else out.append("Historical process-exit evidence requires Android 11 or newer.\n");
        out.append("A force-close/user-requested exit is not proof of a crash or memory exhaustion. Missing OS records do not rule either out.\n");
        return out.toString();
    }
    private static String apkHash(Context context){
        try(java.io.InputStream in=new java.io.FileInputStream(context.getApplicationInfo().sourceDir)){
            java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
            byte[] block=new byte[16384];int length;while((length=in.read(block))!=-1)hash.update(block,0,length);
            StringBuilder text=new StringBuilder();for(byte value:hash.digest())text.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
            return text.toString();
        }catch(Exception e){return "unavailable ("+e.getClass().getSimpleName()+")";}
    }
    private static String reason(int reason){
        switch(reason){
            case ApplicationExitInfo.REASON_LOW_MEMORY:return "OS low-memory termination";
            case ApplicationExitInfo.REASON_ANR:return "Application not responding";
            case ApplicationExitInfo.REASON_CRASH:return "Java crash";
            case ApplicationExitInfo.REASON_CRASH_NATIVE:return "Native crash";
            case ApplicationExitInfo.REASON_USER_REQUESTED:return "User-requested termination";
            case ApplicationExitInfo.REASON_SIGNALED:return "Signal termination (cause not established)";
            default:return "Other / unknown OS exit";
        }
    }
}
