package cn.myflv.noactive.core;

import cn.myflv.noactive.core.app.AndroidHook;
import cn.myflv.noactive.core.app.PowerKeeperHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HandleHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageParam) throws Throwable {
        new AndroidHook(packageParam);
        new PowerKeeperHook(packageParam);
    }

}
