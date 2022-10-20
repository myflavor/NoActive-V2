package cn.myflv.noactive.core.app;

import android.content.Context;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.app.base.AbstractAppHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 电量和性能Hook
 */
public class PowerKeeperHook extends AbstractAppHook {

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
            XposedHelpers.findAndHookMethod(ClassConstants.MilletConfig, packageParam.classLoader, MethodConstants.getEnable, Context.class, XC_MethodReplacement.returnConstant(false));
        }, "Disable Millet");
        //阻止电量与性能调用系统jar中的kill方法杀后台
        runNoThrow(() -> {
            XposedHelpers.findAndHookMethod(ClassConstants.ProcessManager, packageParam.classLoader, MethodConstants.kill, ClassConstants.ProcessConfig, XC_MethodReplacement.returnConstant(false));
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
