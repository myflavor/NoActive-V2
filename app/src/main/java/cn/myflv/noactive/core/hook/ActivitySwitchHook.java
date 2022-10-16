package cn.myflv.noactive.core.hook;

import android.app.usage.UsageEvents;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.AppInfo;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.ActivityManagerService;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Activity切换Hook
 */
public class ActivitySwitchHook extends MethodHook {

    /**
     * 进入前台.
     */
    private final int ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED;
    /**
     * 进入后台.
     */
    private final int ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED;
    /**
     * 内存数据.
     */
    private final MemData memData;

    private final FreezerHandler freezerHandler;

    public ActivitySwitchHook(ClassLoader classLoader, MemData memData, FreezerHandler freezerHandler) {
        super(classLoader);
        this.memData = memData;
        this.freezerHandler = freezerHandler;
    }


    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.updateActivityUsageStats;
    }

    @Override
    public Object[] getTargetParam() {
        // Hook 切换事件
        if (Build.MANUFACTURER.equals("samsung")) {
            return new Object[]{
                    ClassConstants.ComponentName, int.class, int.class,
                    ClassConstants.IBinder, ClassConstants.ComponentName, Intent.class};
        } else {
            return new Object[]{ClassConstants.ComponentName, int.class, int.class,
                    ClassConstants.IBinder, ClassConstants.ComponentName};
        }
    }


    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.Q;
    }

    @Override
    public String successLog() {
        return "Listen app switch";
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
                // 获取方法参数
                Object[] args = param.args;

                // 获取切换事件
                int event = (int) args[2];
                if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
                    return;
                }

                // 本次事件用户
                int userId = (int) args[1];
                // 本次事件包名
                String packageName = ((ComponentName) args[0]).getPackageName();
                if (packageName == null) {
                    return;
                }

                // 忽略系统框架
                if (packageName.equals("android")) {
                    Log.d("android(" + memData.getLastAppInfo().getPackageName() + ") -> ignored");
                    return;
                }

                // 当前事件应用
                AppInfo eventTo = AppInfo.getInstance(userId, packageName);

                Log.d(eventTo.getKey() + " " + (event == ACTIVITY_PAUSED ? "paused" : "resumed"));


                // 本次等于上次 即无变化 不处理
                if (eventTo.equals(memData.getLastAppInfo())) {
                    Log.d(eventTo.getKey() + " activity changed");
                    return;
                }


                // 切换前的包名等于上次包名
                AppInfo eventFrom = memData.getLastAppInfo();
                // 重新设置上次包名为切换后的包名 下次用
                memData.setLastAppInfo(eventTo);

                // 是否解冻
                boolean handleTo = memData.isTargetApp(eventTo.getPackageName()) || memData.getFreezerAppSet().contains(eventTo.getKey());
                // 是否冻结
                boolean handleFrom = memData.isTargetApp(eventFrom.getPackageName());
                Log.d(eventFrom.getKey() + covertHandle(handleFrom) + " -> " + eventTo.getKey() + covertHandle(handleTo));
                // 执行进入前台
                freezerHandler.onResume(handleTo, eventTo);
                if (memData.getDirectApps().contains(eventFrom.getPackageName())) {
                    // 执行进入后台
                    freezerHandler.onPause(handleFrom, eventFrom);
                } else {
                    freezerHandler.onPause(handleFrom, eventFrom, 3000);
                }

            }
        };
    }


    public String covertHandle(boolean handle) {
        return "(" + (handle ? "handle" : "ignore") + ")";
    }


}
