package cn.myflv.noactive.core.hook;

import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 方法Hook抽象类.
 */
public abstract class MethodHook {
    /**
     * 任何版本
     */
    public final int ANY_VERSION = -1;
    /**
     * 类加载器
     */
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

    /**
     * @return 目标类
     */
    public abstract String getTargetClass();

    /**
     * @return 目标方法
     */
    public abstract String getTargetMethod();

    /**
     * @return 目标参数
     */
    public abstract Object[] getTargetParam();

    /**
     * @return Hook方法
     */
    public abstract XC_MethodHook getTargetHook();

    /**
     * @return 最低支持版本
     */
    public abstract int getMinVersion();

    /**
     * @return 成功日志
     */
    public abstract String successLog();

    /**
     * @return 忽略错误
     */
    public boolean isIgnoreError() {
        return false;
    }

    /**
     * Hook包装.
     */
    public void hook() {
        int minVersion = getMinVersion();
        if (minVersion == ANY_VERSION || Build.VERSION.SDK_INT >= minVersion) {
            ArrayList<Object> param = new ArrayList<>(Arrays.asList(getTargetParam()));
            param.add(getTargetHook());
            String targetMethod = getTargetMethod();
            if (targetMethod == null) {
                XposedHelpers.findAndHookConstructor(getTargetClass(), classLoader, param.toArray());
            } else {
                XposedHelpers.findAndHookMethod(getTargetClass(), classLoader, getTargetMethod(), param.toArray());
            }
            onSuccess();
        }
    }

    /**
     * 打印调用堆栈.
     */
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

    /**
     * @return 是否Hook
     */
    public boolean isToHook() {
        return true;
    }

    /**
     * 调用原方法.
     *
     * @param param 方法参数
     * @return 原方法返回值
     * @throws Throwable 移除
     */
    public Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }

    /**
     * 打印成功日志包装.
     */
    public void logSuccess() {
        String log = successLog();
        if (log == null) {
            return;
        }
        Log.i(log);

    }

    /**
     * 成功后执行方法.
     */
    public void onSuccess() {
        logSuccess();
    }

    /**
     * 打印错误.
     *
     * @param throwable 异常
     */
    public void logError(Throwable throwable) {
        if (isIgnoreError()) {
            return;
        }
        Log.e(getTargetClass() + "." + getTargetMethod() + " failed: " + throwable.getMessage());
    }

    /**
     * 错误后执行方法.
     *
     * @param throwable 异常
     */
    public void onError(Throwable throwable) {
        logError(throwable);
    }

    /**
     * 返回常量Hook.
     *
     * @param result 结果
     * @return 返回常量Hook
     */
    public XC_MethodReplacement constantResult(final Object result) {
        return XC_MethodReplacement.returnConstant(result);
    }

    public boolean runNoThrow(Runnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
