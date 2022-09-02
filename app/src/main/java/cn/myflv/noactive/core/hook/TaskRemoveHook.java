package cn.myflv.noactive.core.hook;

import android.os.Build;

import java.util.Arrays;
import java.util.List;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

@Deprecated
public class TaskRemoveHook extends MethodHook {

    private final List<String> blockedReasonList = Arrays.asList("recent-task-trimmed", "remove-hidden-task");

    public TaskRemoveHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.ActivityTaskSupervisor;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.removeTask;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassEnum.Task, boolean.class, boolean.class, String.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Log.i("removeTask task -> " + param.args[0]);
                Log.i("removeTask killProcess -> " + param.args[1]);
                Log.i("removeTask removeFromRecents -> " + param.args[2]);
                Log.i("removeTask reason -> " + param.args[3]);
                printStackTrace();
                return null;
                /*
                if (param.args[3] != null) {
                    String reason = (String) param.args[3];
                    if (blockedReasonList.contains(reason)) {
                        return null;
                    }
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                 */
            }
        };
    }


    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.S;
    }

    @Override
    public String successLog() {
        return "Listen task remove";
    }


}
