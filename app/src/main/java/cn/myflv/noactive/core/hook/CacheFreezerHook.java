package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class CacheFreezerHook extends MethodHook {


    public CacheFreezerHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.CachedAppOptimizer;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.useFreezer;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return false;
            }
        };
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.R;
    }

    @Override
    public String successLog() {
        return "Disable Android Freezer";
    }
}
