package cn.myflv.noactive.core.hook;

import android.app.usage.UsageEvents;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.server.ActivityManagerService;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Activity切换Hook
 */
public class ActivitySwitchHook extends MethodHook {

    /**
     * 进入前台.
     */
    private final int ACTIVITY_RESUMED = UsageEvents.Event.MOVE_TO_FOREGROUND;
    /**
     * 进入后台.
     */
    private final int ACTIVITY_PAUSED = UsageEvents.Event.MOVE_TO_BACKGROUND;
    /**
     * 内存数据.
     */
    private final MemData memData;

    private final FreezerHandler freezerHandler;
    private final FreezeUtils freezeUtils;
    /**
     * 上一次事件包名.
     */
    private String lastPackageName = "android";

    public ActivitySwitchHook(ClassLoader classLoader, MemData memData, FreezerHandler freezerHandler, FreezeUtils freezeUtils) {
        super(classLoader);
        this.memData = memData;
        this.freezerHandler = freezerHandler;
        this.freezeUtils = freezeUtils;
    }


    @Override
    public String getTargetClass() {
        return ClassEnum.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.updateActivityUsageStats;
    }

    @Override
    public Object[] getTargetParam() {
        // Hook 切换事件
        if (Build.MANUFACTURER.equals("samsung")) {
            return new Object[]{
                    ClassEnum.ComponentName, int.class, int.class,
                    ClassEnum.IBinder, ClassEnum.ComponentName, Intent.class};
        } else {
            return new Object[]{ClassEnum.ComponentName, int.class, int.class,
                    ClassEnum.IBinder, ClassEnum.ComponentName};
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
        return new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // 获取方法参数
                Object[] args = param.args;

                // 判断事件用户
                int userId = (int) args[1];
                if (userId != ActivityManagerService.MAIN_USER) {
                    return;
                }

                // 获取切换事件
                int event = (int) args[2];
                if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
                    return;
                }


                // 本次事件包名
                String toPackageName = ((ComponentName) args[0]).getPackageName();
                if (toPackageName == null) {
                    return;
                }
                Log.d(toPackageName + " " + (event == ACTIVITY_PAUSED ? "paused" : "resumed"));

                // 本次等于上次 即无变化 不处理
                if (toPackageName.equals(lastPackageName)) {
                    Log.d(toPackageName + " activity changed");
                    return;
                }

                // 忽略系统框架
                if (toPackageName.equals("android")) {
                    Log.d("android(" + lastPackageName + ") -> ignored");
                    return;
                }

                // 切换前的包名等于上次包名
                String fromPackageName = lastPackageName;
                // 重新设置上次包名为切换后的包名 下次用
                lastPackageName = toPackageName;

                // 为防止一直new，存到内存数据
                if (memData.getActivityManagerService() == null) {
                    memData.setActivityManagerService(new ActivityManagerService(param.thisObject));
                }
                ThreadUtils.newThread(() -> {
                    // 是否解冻
                    boolean handleTo = memData.isTargetApp(toPackageName) || memData.getFreezerAppSet().contains(toPackageName);
                    // 是否冻结
                    boolean handleFrom = memData.isTargetApp(fromPackageName);
                    Log.d(fromPackageName + covertHandle(handleFrom) + " -> " + toPackageName + covertHandle(handleTo));
                    // 执行进入前台
                    freezerHandler.onResume(handleTo, toPackageName);
                    // 执行进入后台
                    freezerHandler.onPause(handleFrom, fromPackageName, 3000);
                });
            }
        };
    }


    public String covertHandle(boolean handle) {
        return "(" + (handle ? "handle" : "ignore") + ")";
    }


}
