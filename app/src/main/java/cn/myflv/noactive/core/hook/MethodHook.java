package cn.myflv.noactive.core.hook;

import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public abstract class MethodHook {

    public final int ANY_VERSION = -1;

    public final ClassLoader classLoader;

    public MethodHook(ClassLoader classLoader) {
        this.classLoader = classLoader;
        if (isToHook()) {
            try {
                hook();
            } catch (Throwable throwable) {
                onError(throwable);
            }
        }
    }

    public abstract String getTargetClass();

    public abstract String getTargetMethod();

    public abstract Object[] getTargetParam();

    public abstract XC_MethodHook getTargetHook();

    public abstract int getMinVersion();

    public abstract String successLog();

    public boolean isIgnoreError() {
        return false;
    }

    public void hook() {
        int minVersion = getMinVersion();
        if (minVersion == ANY_VERSION || Build.VERSION.SDK_INT >= minVersion) {
            ArrayList<Object> param = new ArrayList<>(Arrays.asList(getTargetParam()));
            param.add(getTargetHook());
            XposedHelpers.findAndHookMethod(getTargetClass(), classLoader, getTargetMethod(), param.toArray());
            onSuccess();
        }
    }

    public void printStackTrace() {
        Throwable throwable = new Throwable();
        Log.i("---------------> " + getTargetMethod());
        StackTraceElement[] stackElements = throwable.getStackTrace();
        for (StackTraceElement element : stackElements) {
            Log.i("at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.i(getTargetMethod() + " <---------------");
    }

    public boolean isToHook() {
        return true;
    }

    public Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }

    public void logSuccess() {
        String log = successLog();
        if (log == null) {
            return;
        }
        Log.i(log);

    }

    public void onSuccess() {
        logSuccess();
    }

    public void logError(Throwable throwable) {
        if (isIgnoreError()) {
            return;
        }
        Log.e(getTargetClass() + "." + getTargetMethod() + " failed: " + throwable.getMessage());
    }

    public void onError(Throwable throwable) {
        logError(throwable);
    }

    public XC_MethodReplacement constantResult(final Object result) {
        return XC_MethodReplacement.returnConstant(result);
    }
}
