package cn.myflv.noactive.core.handler;

import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.AppInfo;
import cn.myflv.noactive.core.entity.MemData;
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
            for (String key : memData.getFreezerAppSet()) {
                AppInfo appInfo = AppInfo.getInstance(key);
                ThreadUtils.runWithLock(appInfo.getKey(), () -> {
                    // 再次检查是否被冻结
                    if (!memData.getFreezerAppSet().contains(key)) {
                        return;
                    }
                    // 获取应用进程
                    List<ProcessRecord> processRecords = processMap.get(appInfo.getPackageName());
                    if (processRecords == null) {
                        return;
                    }
                    // 冻结
                    for (ProcessRecord processRecord : processRecords) {
                        if (memData.isTargetProcess(true, appInfo.getUserId(), processRecord)) {
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
            for (String key : memData.getFreezerAppSet()) {
                AppInfo appInfo = AppInfo.getInstance(key);
                Log.d(appInfo.getKey() + " interval unfreeze start");
                // 解冻
                onResume(true, appInfo, true, () -> {
                    // 冻结
                    onPause(true, appInfo, 3000, () -> {
                        Log.d(appInfo.getKey() + " interval unfreeze finish");
                    });
                });
                // 结束循环
                // 相当于只解冻没有最久没有打开的 APP
                break;
            }
        }, 1);
        Log.i("Interval unfreeze");
    }


    public void onResume(boolean handle, AppInfo appInfo) {
        onResume(handle, appInfo, false, null);
    }

    /**
     * APP切换至前台.
     *
     * @param appInfo 事件信息
     */
    public void onResume(boolean handle, AppInfo appInfo, boolean temporary, Runnable runnable) {
        // 不处理就跳过
        if (!handle) {
            return;
        }
        ThreadUtils.thawThread(appInfo.getKey(), () -> {
            ThreadUtils.safeRun(() -> {
                if (temporary) {
                    return;
                }
                // 获取包名
                String packageName = appInfo.getPackageName();
                // 白名单主进程跳过
                if (memData.getWhiteProcessList().contains(packageName)) {
                    return;
                }
                if (!memData.getSocketApps().contains(packageName)) {
                    // 恢复StandBy
                    memData.getAppStandbyController().forceIdleState(appInfo, false);
                }
            });
            // 获取目标进程
            List<ProcessRecord> targetProcessRecords = memData.getTargetProcessRecords(appInfo);
            // 解冻
            freezeUtils.unFreezer(targetProcessRecords);
            // 移除被冻结APP
            memData.getFreezerAppSet().remove(appInfo.getKey());
            if (Thread.currentThread().isInterrupted()) {
                Log.d(appInfo.getKey() + " event updated");
                return;
            }

            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public void onPause(boolean handle, AppInfo appInfo) {
        onPause(handle, appInfo, 0, null);
    }

    public void onPause(boolean handle, AppInfo appInfo, long delay) {
        onPause(handle, appInfo, delay, null);
    }

    /**
     * APP切换至后台.
     *
     * @param appInfo 包名
     */
    public void onPause(boolean handle, AppInfo appInfo, long delay, Runnable runnable) {
        // 不处理就跳过
        if (!handle) {
            return;
        }
        ThreadUtils.newThread(appInfo.getKey(), () -> {
            // 如果是前台应用就不处理
            if (isAppForeground(appInfo)) {
                Log.d(appInfo.getKey() + " is in foreground");
                return;
            }
            // 获取目标进程
            List<ProcessRecord> targetProcessRecords = memData.getTargetProcessRecords(appInfo);
            // 如果目标进程为空就不处理
            if (targetProcessRecords.isEmpty()) {
                return;
            }
            // 后台应用添加包名
            memData.getFreezerAppSet().add(appInfo.getKey());
            // 等待应用未执行广播
            boolean broadcastIdle = memData.waitBroadcastIdle(appInfo);
            if (!broadcastIdle) {
                return;
            }
            // 等待 Binder 休眠
            boolean binderIdle = waitBinderIdle(appInfo);
            if (!binderIdle) {
                return;
            }
            ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(appInfo);
            // 存放杀死进程
            List<ProcessRecord> killProcessList = new ArrayList<>();
            // 遍历目标进程
            for (ProcessRecord targetProcessRecord : targetProcessRecords) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(appInfo.getKey() + " event updated");
                    return;
                }
                // 目标进程名
                String processName = targetProcessRecord.getProcessName();
                if (memData.getKillProcessList().contains(processName)) {
                    killProcessList.add(targetProcessRecord);
                } else {
                    // 冻结
                    freezeUtils.freezer(targetProcessRecord);
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                Log.d(appInfo.getKey() + " event updated");
                return;
            }
            ThreadUtils.safeRun(() -> {
                // 如果白名单进程不包含主进程就释放唤醒锁
                if (memData.getWhiteProcessList().contains(appInfo.getPackageName())) {
                    return;
                }
                // 是否唤醒锁
                memData.getPowerManagerService().releaseWakeLocks(appInfo, applicationInfo.uid);
                // memData.getAlarmMangerService().remove(appInfo, applicationInfo.uid);
                if (!memData.getSocketApps().contains(appInfo.getPackageName())) {
                    memData.getAppStandbyController().forceIdleState(appInfo, true);
                    memData.getNetworkManagementService().socketDestroy(appInfo,applicationInfo);
                }else {
                    //else里可以删除，他是保持链接里网络解冻用的
                    //memData.monitorNet(applicationInfo);
                      }
            });
            if (Thread.currentThread().isInterrupted()) {
                Log.d(appInfo.getKey() + " event updated");
                return;
            }
            ThreadUtils.safeRun(() -> {
                freezeUtils.kill(killProcessList);
            });
            if (runnable != null) {
                runnable.run();
            }
        }, delay);
    }

    /**
     * 应用是否前台.
     */
    public boolean isAppForeground(AppInfo appInfo) {
        // 获取包名
        String packageName = appInfo.getPackageName();
        if (memData.getTopApps().contains(packageName)) { // 如果设置后台级别为可见窗口
            // 判断是否可见窗口
            return memData.getActivityManagerService().isTopApp(appInfo);
        } else if (memData.getDirectApps().contains(packageName)) { // 如果设置了强制冻结
            // 直接认为不在前台
            return false;
        } else {
            // 默认判断是否有前台服务
            return memData.getActivityManagerService().isForegroundApp(appInfo);
        }
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
        String key = memData.getActivityManagerService().getNameForUid(uid);
        if (key == null) {
            Log.w("uid  " + uid + "  not found");
            return;
        }
        if (!memData.getFreezerAppSet().contains(key)) {
            return;
        }
        AppInfo appInfo = AppInfo.getInstance(key);
        Log.i(appInfo.getKey() + " " + reason);
        onResume(true, appInfo, true, () -> {
            onPause(true, appInfo, 3000);
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
            Class<?> GreezeManagerService = XposedHelpers.findClass(ClassConstants.GreezeManagerService, classLoader);
            return (int) XposedHelpers.callStaticMethod(GreezeManagerService, MethodConstants.nQueryBinder, uid);
        } catch (Throwable ignored) {
        }
        // 报错就返回已休眠，相当于这个功能不存在
        return BINDER_IDLE;
    }


    /**
     * 等待Binder休眠
     *
     * @param appInfo 包名
     */
    public boolean waitBinderIdle(AppInfo appInfo) {
        // 获取应用信息
        ApplicationInfo applicationInfo = memData.getActivityManagerService().getApplicationInfo(appInfo);
        if (applicationInfo == null) {
            return true;
        }
        // 重试次数
        int retry = 0;
        // 3次重试，如果不进休眠就直接冻结了
        while (binderState(applicationInfo.uid) != BINDER_IDLE && retry < 3) {
            Log.w(appInfo.getKey() + " binder busy");
            boolean sleep = ThreadUtils.sleep(1000);
            if (!sleep) {
                Log.d(appInfo.getKey() + " binder idle wait canceled");
                return false;
            }
            retry++;
        }
        Log.d(appInfo.getKey() + " binder idle");
        return true;
    }

}
