package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

public class TaskRemoveHook extends MethodHook {

    public TaskRemoveHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.wm.ActivityTaskSupervisor";
    }

    @Override
    public String getTargetMethod() {
        return "removeTask";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{"com.android.server.wm.Task", boolean.class, boolean.class, String.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Log.i("removeTask task -> " + param.args[0]);
                Log.i("removeTask killProcess -> " + param.args[1]);
                Log.i("removeTask removeFromRecents -> " + param.args[2]);
                if (param.args[3] != null) {
                    Log.i("removeTask reason -> " + param.args[3]);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                printStackTrace();
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
