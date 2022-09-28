package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.hook.base.MethodHook;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Happy Freezer.
 */
public class FreezerSupportHook extends MethodHook {

    public FreezerSupportHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.CachedAppOptimizer;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.isFreezerSupported;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        // 返回支持暂停已缓存
        return constantResult(true);
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.R;
    }

    @Override
    public String successLog() {
        return "Happy Freezer";
    }

}
