package cn.myflv.noactive.core.hook;

import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public abstract class MethodHook {

    public final int ANY_VERSION = -1;

    public final ClassLoader classLoader;

    public MethodHook(ClassLoader classLoader) {
        this.classLoader = classLoader;
        try {
            hook();
        } catch (Throwable throwable) {
            Log.e(getTargetClass() + "." + getTargetMethod() + " failed: " + throwable.getMessage());
        }
    }

    public abstract String getTargetClass();

    public abstract String getTargetMethod();

    public abstract Object[] getTargetParam();

    public abstract XC_MethodHook getTargetHook();

    public abstract int getMinVersion();

    public abstract String successLog();

    public void hook() {
        int minVersion = getMinVersion();
        if (minVersion == ANY_VERSION || Build.VERSION.SDK_INT >= minVersion) {
            ArrayList<Object> param = new ArrayList<>(Arrays.asList(getTargetParam()));
            param.add(getTargetHook());
            XposedHelpers.findAndHookMethod(getTargetClass(), classLoader, getTargetMethod(), param.toArray());
            String log = successLog();
            if (log != null) {
                Log.i(log);
            }
        }
    }

    public void printStackTrace() {
        printStackTrace(new Throwable());
    }

    public void printStackTrace(Throwable throwable) {
        Log.i("---------------> " + getTargetMethod());
        StackTraceElement[] stackElements = throwable.getStackTrace();
        for (StackTraceElement element : stackElements) {
            Log.i("at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.i(getTargetMethod() + " <---------------");
    }

}
