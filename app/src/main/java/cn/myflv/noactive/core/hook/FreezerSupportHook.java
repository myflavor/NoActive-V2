package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
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
        return ClassEnum.CachedAppOptimizer;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.isFreezerSupported;
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
