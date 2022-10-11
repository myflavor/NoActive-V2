package cn.myflv.noactive.core.app;

import cn.myflv.noactive.core.app.base.AbstractAppHook;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.hook.ANRHook;
import cn.myflv.noactive.core.hook.ActivitySwitchHook;
import cn.myflv.noactive.core.hook.AppStandbyHook;
import cn.myflv.noactive.core.hook.BroadcastDeliverHook;
import cn.myflv.noactive.core.hook.CacheFreezerHook;
import cn.myflv.noactive.core.hook.DeviceIdleHook;
import cn.myflv.noactive.core.hook.NetworkManagerHook;
import cn.myflv.noactive.core.hook.PowerManagerHook;
import cn.myflv.noactive.core.hook.ScreenStateHook;
import cn.myflv.noactive.core.hook.TaskTrimHook;
import cn.myflv.noactive.core.hook.miui.BinderTransHook;
import cn.myflv.noactive.core.hook.miui.GreezeHook;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 系统框架Hook.
 */
public class AndroidHook extends AbstractAppHook {

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
        new AppStandbyHook(classLoader, memData);
        new NetworkManagerHook(classLoader, memData);
        new DeviceIdleHook(classLoader, memData);
        // new ActivityManagerHook(classLoader, memData);

        FreezeUtils freezeUtils = new FreezeUtils(classLoader, memData);
        FreezerHandler freezerHandler = new FreezerHandler(classLoader, memData, freezeUtils);

        // Hook 切换事件
        new ActivitySwitchHook(classLoader, memData, freezerHandler);
        new ScreenStateHook(classLoader, memData, freezerHandler);

        // Hook 广播分发
        new BroadcastDeliverHook(classLoader, memData);

        // Hook ANR
        new ANRHook(classLoader, memData);

        new TaskTrimHook(classLoader);

        new BinderTransHook(classLoader, freezerHandler);

        new GreezeHook(classLoader, memData);

        // new NetReceiveHook(classLoader, freezerHandler);

        // 进程移除监听
        // new ProcessRemoveHook(classLoader, memData, freezeUtils);
        // 进程新增监听
        // new ProcessAddHook(classLoader, memData, freezeUtils);

        // Hook 禁用暂停执行
        new CacheFreezerHook(classLoader);

        // 显示暂停已缓存开关
        // new FreezerSupportHook(classLoader);

        Log.i("Load success");
    }

}
