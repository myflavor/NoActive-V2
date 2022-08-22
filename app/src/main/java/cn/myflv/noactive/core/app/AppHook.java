package cn.myflv.noactive.core.app;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * APP抽象Hook
 */
public abstract class AppHook {

    public final XC_LoadPackage.LoadPackageParam packageParam;

    public AppHook(XC_LoadPackage.LoadPackageParam packageParam) {
        String targetPackageName = getTargetPackageName();
        if (targetPackageName == null) {
            log("Target packageName is null");
        }
        this.packageParam = packageParam;
        String packageName = packageParam.packageName;
        if (packageName.equals(targetPackageName)) {
            // 尝试Hook可以防止软重启
            try {
                hook();
            } catch (Throwable throwable) {
                log("Hook failed: " + throwable.getMessage());
            }
        }
    }

    //目标应用包名
    public abstract String getTargetPackageName();

    // 目标应用名称
    public abstract String getTargetAppName();

    // Hook处理方法
    public abstract void hook();

    // 集成Log
    public void log(String msg) {
        String targetAppName = getTargetAppName();
        if (targetAppName == null) {
            targetAppName = "null";
        }
        XposedBridge.log("NoActive(" + targetAppName + ") -> " + msg);
    }
}
