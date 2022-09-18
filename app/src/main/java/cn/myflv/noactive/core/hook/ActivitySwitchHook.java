package cn.myflv.noactive.core.hook;

import android.app.usage.UsageEvents;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.ActivityManagerService;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.FreezerConfig;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

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
     * Binder休眠.
     */
    private final int BINDER_IDLE = 0;
    /**
     * 内存数据.
     */
    private final MemData memData;
    /**
     * 冻结工具.
     */
    private final FreezeUtils freezeUtils;
    /**
     * 上一次事件包名.
     */
    private String lastPackageName = "android";

    public ActivitySwitchHook(ClassLoader classLoader, MemData memData, FreezeUtils freezeUtils) {
        super(classLoader);
        this.memData = memData;
        this.freezeUtils = freezeUtils;
        if (FreezerConfig.isScheduledOn()) {
            enableIntervalUnfreeze();
        }
        enableIntervalFreeze();
    }

    /**
     * 开启定时冻结
     */
    public void enableIntervalFreeze() {
        ThreadUtils.scheduleInterval(() -> {
            // 如果没有APP被冻结就不处理了
            if (memData.getFreezerAppSet().isEmpty()) {
                return;
            }
            // 获取包名分组进程
            Map<String, List<ProcessRecord>> processMap = memData.getActivityManagerService().getProcessList().getProcessMap();
            // 遍历被冻结的APP
            for (String packageName : memData.getFreezerAppSet()) {
                // 通过包名锁
                synchronized (ThreadUtils.getAppLock(packageName)) {
                    // 再次检查是否被冻结
                    if (!memData.getFreezerAppSet().contains(packageName)) {
                        // 获取应用进程
                        List<ProcessRecord> processRecords = processMap.get(packageName);
                        if (processRecords == null) {
                            continue;
                        }
                        // 冻结
                        processRecords.forEach(freezeUtils::freezer);
                    }
                }
            }
        }, 1);
        Log.i("Interval freeze");
    }

    /**
     * 定时轮番解冻
     */
    public void enableIntervalUnfreeze() {
        ThreadUtils.scheduleInterval(() -> {
            try {
                Log.d("Scheduled start");
                // 遍历被冻结的进程
                for (String packageName : memData.getFreezerAppSet()) {
                    Log.d(packageName + " unFreezer all");
                    // 解冻
                    onResume(true, packageName);
                    // 冻结
                    onPause(true, packageName, 3000);
                    Log.d(packageName + " freezer all");
                    // 结束循环
                    // 相当于只解冻没有最久没有打开的 APP
                    break;
                }
                Log.d("Scheduled finish");
            } catch (Throwable throwable) {
                Log.e("scheduled", throwable);
            }
        }, 1);
        Log.i("Interval unfreeze");
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
                    onResume(handleTo, toPackageName);
                    // 执行进入后台
                    onPause(handleFrom, fromPackageName, 3000);
                });
            }
        };
    }


    /**
     * APP切换至前台.
     *
     * @param packageName 包名
     */
    public void onResume(boolean handle, String packageName) {
        // 不处理就跳过
        if (!handle) {
            return;
        }
        // 获取目标进程
        List<ProcessRecord> targetProcessRecords = memData.getTargetProcessRecords(packageName);
        // 防止同时解冻冻结
        synchronized (ThreadUtils.getAppLock(packageName)) {
            // 移除被冻结APP
            memData.getFreezerAppSet().remove(packageName);
            // 去除冻结Token
            memData.getFreezerTokenMap().remove(packageName);
            // 前台App
            memData.getForegroundAppSet().add(packageName);
            memData.getAppStandbyController().forceIdleState(packageName, false);
            // 解冻
            freezeUtils.unFreezer(targetProcessRecords);
        }
    }

    /**
     * APP切换至后台.
     *
     * @param packageName 包名
     */
    public void onPause(boolean handle, String packageName, long delay) {
        // 不处理就跳过
        if (!handle) {
            return;
        }

        // 设置冻结Token
        long token = System.currentTimeMillis();
        memData.setToken(packageName, token);

        // 休眠3s
        ThreadUtils.sleep(delay);
        // 校验冻结Token
        if (memData.isInCorrectToken(packageName, token)) {
            Log.d(packageName + " event is updated");
            return;
        }

        // 如果是前台应用就不处理
        if (isAppForeground(packageName)) {
            Log.d(packageName + " is in foreground");
            return;
        }

        // 获取目标进程
        List<ProcessRecord> targetProcessRecords = memData.getTargetProcessRecords(packageName);
        // 如果目标进程为空就不处理
        if (targetProcessRecords.isEmpty()) {
            return;
        }
        // 后台应用添加包名
        memData.getFreezerAppSet().add(packageName);
        // 移出前台APP
        memData.getForegroundAppSet().remove(packageName);
        // 等待应用未执行广播
        memData.waitBroadcastIdle(packageName);
        // 等待 Binder 休眠
        waitBinderIdle(packageName);
        // 校验冻结Token
        if (memData.isInCorrectToken(packageName, token)) {
            Log.d(packageName + " event is updated");
            return;
        }
        ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(packageName);
        // 存放杀死进程
        List<ProcessRecord> killProcessList = new ArrayList<>();
        // 防止同时解冻冻结
        synchronized (ThreadUtils.getAppLock(packageName)) {
            // 确保应用没被解冻
            if (!memData.getFreezerAppSet().contains(packageName)) {
                return;
            }
            // 遍历目标进程
            for (ProcessRecord targetProcessRecord : targetProcessRecords) {
                // 目标进程名
                String processName = targetProcessRecord.getProcessName();
                if (memData.getKillProcessList().contains(processName)) {
                    killProcessList.add(targetProcessRecord);
                } else {
                    // 冻结
                    freezeUtils.freezer(targetProcessRecord);
                }
            }
            FreezeUtils.kill(killProcessList);
            // 如果白名单进程不包含主进程就释放唤醒锁
            if (memData.getWhiteProcessList().contains(packageName)) {
                return;
            }
            memData.getPowerManagerService().release(packageName);
            memData.getAppStandbyController().forceIdleState(packageName, true);
            if (!memData.getSocketApps().contains(packageName)) {
                memData.getNetworkManagementService().socketDestroy(applicationInfo);
            }

        }
    }


    /**
     * 应用是否前台.
     *
     * @param packageName 包名
     */
    public boolean isAppForeground(String packageName) {
        // 忽略前台 就代表不在后台
        if (memData.getDirectApps().contains(packageName)) {
            return false;
        }
        // 调用AMS的方法判断
        return memData.getActivityManagerService().isAppForeground(packageName);
    }


    public String covertHandle(boolean handle) {
        return "(" + (handle ? "handle" : "ignore") + ")";
    }


    /**
     * 临时解冻.
     *
     * @param uid 应用ID
     */
    public void temporaryUnfreeze(int uid, String reason) {
        if (uid < 10000) {
            return;
        }
        String packageName = memData.getActivityManagerService().getNameForUid(uid);
        if (packageName == null) {
            Log.w("uid  " + uid + "  not found");
            return;
        }
        if (!memData.getFreezerAppSet().contains(packageName)) {
            return;
        }
        Log.i(packageName + " " + reason);
        Log.d(packageName + " unFreezer all");
        onResume(true, packageName);
        onPause(true, packageName, 3000);
        Log.d(packageName + " freezer all");
    }


    /**
     * Binder状态.
     *
     * @param uid 应用ID
     * @return [IDLE|BUSY]
     */
    public int binderState(int uid) {
        try {
            Class<?> GreezeManagerService = XposedHelpers.findClass(ClassEnum.GreezeManagerService, classLoader);
            return (int) XposedHelpers.callStaticMethod(GreezeManagerService, MethodEnum.nQueryBinder, uid);
        } catch (Throwable ignored) {
        }
        // 报错就返回已休眠，相当于这个功能不存在
        return BINDER_IDLE;
    }


    /**
     * 等待Binder休眠
     *
     * @param packageName 包名
     */
    public void waitBinderIdle(String packageName) {
        // 获取应用信息
        ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return;
        }
        // 重试次数
        int retry = 0;
        // 3次重试，如果不进休眠就直接冻结了
        while (binderState(applicationInfo.uid) != BINDER_IDLE && retry < 3) {
            Log.w(packageName + " binder busy");
            ThreadUtils.sleep(1000);
            retry++;
        }
        Log.d(packageName + " binder idle");
    }

}
