package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.hook.base.MethodHook;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 禁用暂停执行已缓存Hook.
 */
public class CacheFreezerHook extends MethodHook {


    public CacheFreezerHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.CachedAppOptimizer;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.useFreezer;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        // 返回不使用暂停执行已缓存
        return constantResult(false);
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
