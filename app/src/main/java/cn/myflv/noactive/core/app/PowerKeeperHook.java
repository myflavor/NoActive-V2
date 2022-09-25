package cn.myflv.noactive.core.app;

import android.content.Context;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 电量和性能Hook
 */
public class PowerKeeperHook extends AppHook {

    public PowerKeeperHook(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);
    }

    @Override
    public String getTargetPackageName() {
        return "com.miui.powerkeeper";
    }

    @Override
    public String getTargetAppName() {
        return "PowerKeeper";
    }

    @Override
    public void hook() {
        // 禁用Millet
        runNoThrow(() -> {
            XposedHelpers.findAndHookMethod(ClassEnum.MilletConfig, packageParam.classLoader, MethodEnum.getEnable, Context.class, XC_MethodReplacement.returnConstant(false));
        }, "Disable Millet");
        runNoThrow(() -> {
            XposedHelpers.findAndHookMethod(ClassEnum.ProcessManager, packageParam.classLoader, MethodEnum.kill, ClassEnum.ProcessConfig, XC_MethodReplacement.returnConstant(false));
        }, "Disable kill process");
    }

    public void runNoThrow(Runnable runnable, String msg) {
        try {
            runnable.run();
            log(msg);
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError ignored) {
        } catch (Throwable throwable) {
            log(msg + " failed: " + throwable.getMessage());
        }
    }
}
