package cn.myflv.noactive.core.app;

import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.ANRHook;
import cn.myflv.noactive.core.hook.ActivitySwitchHook;
import cn.myflv.noactive.core.hook.BinderTransHook;
import cn.myflv.noactive.core.hook.BroadcastDeliverHook;
import cn.myflv.noactive.core.hook.CacheFreezerHook;
import cn.myflv.noactive.core.hook.FreezerSupportHook;
import cn.myflv.noactive.core.hook.PowerManagerHook;
import cn.myflv.noactive.core.hook.TaskTrimHook;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 系统框架Hook
 */
public class AndroidHook extends AppHook {

    public AndroidHook(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        super(loadPackageParam);
    }

    @Override
    public String getTargetPackageName() {
        return "android";
    }

    @Override
    public String getTargetAppName() {
        return "Android";
    }

    @Override
    public void hook() {
        // 类加载器
        ClassLoader classLoader = packageParam.classLoader;

        // 加载内存配置
        MemData memData = new MemData();

        new PowerManagerHook(classLoader, memData);

        // Hook 切换事件
        ActivitySwitchHook activitySwitchHook = new ActivitySwitchHook(classLoader, memData);

        // Hook 广播分发
        new BroadcastDeliverHook(classLoader, memData);

        // Hook ANR
        new ANRHook(classLoader, memData);

        // Hook 禁用暂停执行
        new CacheFreezerHook(classLoader);

        // new ProcessKilledHook(classLoader, memData);

        // new ActivityIdleHook(classLoader, memData);

        // new TaskRemoveHook(classLoader);

        new TaskTrimHook(classLoader);

        // new WhiteListHook(classLoader, memData);

        new BinderTransHook(classLoader, activitySwitchHook);

        // 显示暂停已缓存开关
        new FreezerSupportHook(classLoader);

        Log.i("Load success");
    }


}
