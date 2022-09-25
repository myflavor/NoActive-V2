package cn.myflv.noactive.core.handler;

import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.FreezerConfig;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XposedHelpers;

public class FreezerHandler {
    /**
     * Binder休眠.
     */
    private final static int BINDER_IDLE = 0;
    private final ClassLoader classLoader;
    private final MemData memData;
    private final FreezeUtils freezeUtils;

    public FreezerHandler(ClassLoader classLoader, MemData memData, FreezeUtils freezeUtils) {
        this.classLoader = classLoader;
        this.memData = memData;
        this.freezeUtils = freezeUtils;
        if (FreezerConfig.isConfigOn(FreezerConfig.IntervalUnfreeze)) {
            enableIntervalUnfreeze();
        }
        if (FreezerConfig.isConfigOn(FreezerConfig.IntervalFreeze)) {
            enableIntervalFreeze();
        }
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
                ThreadUtils.runWithLock(packageName, () -> {
                    // 再次检查是否被冻结
                    if (!memData.getFreezerAppSet().contains(packageName)) {
                        return;
                    }
                    // 获取应用进程
                    List<ProcessRecord> processRecords = processMap.get(packageName);
                    if (processRecords == null) {
                        return;
                    }
                    // 冻结
                    for (ProcessRecord processRecord : processRecords) {
                        if (memData.isTargetProcess(true, processRecord)) {
                            freezeUtils.freezer(processRecord);
                        }
                    }
                });
            }
        }, 1);
        Log.i("Interval freeze");
    }

    /**
     * 定时轮番解冻
     */
    public void enableIntervalUnfreeze() {
        ThreadUtils.scheduleInterval(() -> {
            // 遍历被冻结的进程
            for (String packageName : memData.getFreezerAppSet()) {
                Log.d(packageName + " interval unfreeze start");
                // 解冻
                onResume(true, packageName, () -> {
                    // 冻结
                    onPause(true, packageName, 3000);
                    Log.d(packageName + " interval unfreeze finish");
                });
                // 结束循环
                // 相当于只解冻没有最久没有打开的 APP
                break;
            }
        }, 1);
        Log.i("Interval unfreeze");
    }


    public void onResume(boolean handle, String packageName) {
        onResume(handle, packageName, null);
    }

    /**
     * APP切换至前台.
     *
     * @param packageName 包名
     */
    public void onResume(boolean handle, String packageName, Runnable runnable) {
        // 不处理就跳过
        if (!handle) {
            return;
        }
        ThreadUtils.newThread(packageName, () -> {
            if (!memData.getFreezerAppSet().contains(packageName)) {
                return;
            }
            // 获取目标进程
            List<ProcessRecord> targetProcessRecords = memData.getTargetProcessRecords(packageName);
            // 解冻
            freezeUtils.unFreezer(targetProcessRecords);
            // 移除被冻结APP
            memData.getFreezerAppSet().remove(packageName);
            ThreadUtils.run(() -> {
                // 获取应用信息
                ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(packageName);
                if (memData.getWhiteProcessList().contains(packageName)) {
                    return;
                }
                if (memData.getSocketApps().contains(packageName)) {
                    // 清除网络监控
                    memData.clearMonitorNet(applicationInfo);
                } else {
                    // 恢复StandBy
                    memData.getAppStandbyController().forceIdleState(packageName, false);
                }
            });
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public void onPause(boolean handle, String packageName, long delay) {
        onPause(handle, packageName, delay, null);
    }

    /**
     * APP切换至后台.
     *
     * @param packageName 包名
     */
    public void onPause(boolean handle, String packageName, long delay, Runnable runnable) {
        // 不处理就跳过
        if (!handle) {
            return;
        }
        ThreadUtils.newThread(packageName, () -> {
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
            // 等待应用未执行广播
            memData.waitBroadcastIdle(packageName);
            // 等待 Binder 休眠
            waitBinderIdle(packageName);
            ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(packageName);
            // 存放杀死进程
            List<ProcessRecord> killProcessList = new ArrayList<>();
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
            ThreadUtils.run(() -> {
                // 如果白名单进程不包含主进程就释放唤醒锁
                if (memData.getWhiteProcessList().contains(packageName)) {
                    return;
                }
                // 是否唤醒锁
                memData.getPowerManagerService().release(packageName);
                if (!memData.getSocketApps().contains(packageName)) {
                    memData.getAppStandbyController().forceIdleState(packageName, true);
                    memData.getNetworkManagementService().socketDestroy(applicationInfo);
                } else {
                    memData.monitorNet(applicationInfo);
                }
            });
            ThreadUtils.run(() -> {
                freezeUtils.kill(killProcessList);
            });

            if (runnable != null) {
                runnable.run();
            }
        }, delay);
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

    /**
     * 临时解冻.
     *
     * @param uid 应用ID
     */
    public void temporaryUnfreezeIfNeed(int uid, String reason) {
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
        onResume(true, packageName, () -> {
            onPause(true, packageName, 3000);
        });
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
