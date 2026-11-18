import org.junit.runner.*;
public class QuickSlotBugBaseline {
 public static void main(String[] args)throws Exception{
  Class<?> test=Class.forName("com.shatteredpixel.shatteredpixeldungeon.QuickSlotCapacityTest");
  JUnitCore runner=new JUnitCore();int failures=0;
  for(String method:new String[]{"invalidAssignmentRetainsTheExistingItemAndShortcut","invalidSavedShortcutDoesNotPreventPhysicalItemRestoration"}){
   Result result=runner.run(Request.method(test,method));System.out.println(method+": run="+result.getRunCount()+" failures="+result.getFailureCount());
   for(org.junit.runner.notification.Failure failure:result.getFailures())System.out.println(failure.getException().getClass().getName()+": "+failure.getMessage());
   failures+=result.getFailureCount();
  }
  if(failures!=2)throw new AssertionError("expected both old-source bugs reproduced");
 }
}
