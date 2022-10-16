package cn.myflv.noactive.core.entity;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.FileObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.myflv.noactive.constant.CommonConstants;
import cn.myflv.noactive.core.server.ActivityManagerService;
import cn.myflv.noactive.core.server.AppStandbyController;
import cn.myflv.noactive.core.server.DeviceIdleController;
import cn.myflv.noactive.core.server.GreezeManagerService;
import cn.myflv.noactive.core.server.NetworkManagementService;
import cn.myflv.noactive.core.server.PowerManagerService;
import cn.myflv.noactive.core.server.ProcessList;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.ConfigFileObserver;
import cn.myflv.noactive.core.utils.FreezerConfig;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import lombok.Data;

/**
 * 内存数据类
 */
@Data
public class MemData {

    /**
     * 上一次事件应用信息
     */
    private AppInfo lastAppInfo = AppInfo.getInstance(ActivityManagerService.MAIN_USER, CommonConstants.ANDROID);

    /**
     * 已冻结APP.
     */
    private final Set<String> freezerAppSet = Collections.synchronizedSet(FreezerConfig.isScheduledOn() ? new LinkedHashSet<>() : new HashSet<>());
    /**
     * 配置文件监听.
     */
    private final FileObserver fileObserver;
    /**
     * 正在执行广播的APP.
     */
    private AppInfo broadcastApp = null;
    /**
     * 白名单APP.
     */
    private Set<String> whiteApps = new HashSet<>();
    /**
     * 可见窗口APP.
     */
    private Set<String> topApps = new HashSet<>();
    /**
     * 忽略前台APP.
     */
    private Set<String> directApps = new HashSet<>();
    /**
     * 系统黑名单APP.
     */
    private Set<String> blackSystemApps = new HashSet<>();
    /**
     * 白名单进程.
     */
    private Set<String> whiteProcessList = new HashSet<>();
    /**
     * 杀死进程.
     */
    private Set<String> killProcessList = new HashSet<>();
    /**
     * 保持连接.
     */
    private Set<String> socketApps = new HashSet<>();
    private Set<String> idleApps = new HashSet<>();
    /**
     * PMS.
     */
    private PowerManagerService powerManagerService = null;
    /**
     * AMS.
     */
    private ActivityManagerService activityManagerService = null;
    private AppStandbyController appStandbyController = null;
    private NetworkManagementService networkManagementService = null;
    private GreezeManagerService greezeManagerService = null;
    private DeviceIdleController deviceIdleController = null;
    private Boolean screenOn = true;

    public boolean setScreenOn(boolean isOn) {
        if (screenOn == isOn) {
            return false;
        }
        screenOn = isOn;
        return true;
    }

    public Context getContext() {
        if (activityManagerService == null) {
            return null;
        }
        return activityManagerService.getContext();
    }

    private final Map<String, Boolean> targetAppMap = new HashMap<>();

    public MemData() {
        // 初始化监听
        fileObserver = new ConfigFileObserver(this);
        // 开始监听配置文件
        fileObserver.startWatching();
    }

    public void notifyConfigChanged() {
        targetAppMap.clear();
        if (deviceIdleController != null) {
            Set<String> whiteSet = deviceIdleController.getWhiteList();
            whiteSet.addAll(getWhiteApps());
            whiteSet.add(CommonConstants.NOACTIVE_PACKAGE_NAME);
            List<String> whiteList = new ArrayList<>();
            for (String pkg : whiteSet) {
                if (isIdlePkg(pkg)) {
                    deviceIdleController.removeWhiteList(pkg);
                } else {
                    whiteList.add(pkg);
                }
            }
            deviceIdleController.addWhiteList(whiteList);
        }
    }

    public boolean isIdlePkg(String pkg) {
        if (whiteProcessList.contains(pkg)) {
            return false;
        }
        if (idleApps.contains(pkg)) {
            return true;
        }
        return isTargetApp(pkg);
    }

    /**
     * 等待应用未执行广播.
     *
     * @param appInfo 包名
     */
    public boolean waitBroadcastIdle(AppInfo appInfo) {
        while (isBroadcastApp(appInfo)) {
            Log.d(appInfo.getKey() + " is executing broadcast");
            boolean sleep = ThreadUtils.sleep(100);
            if (!sleep) {
                Log.d(appInfo.getKey() + " broadcast idle wait canceled ");
                return false;
            }
        }
        Log.d(appInfo.getKey() + " broadcast state idle");
        return true;
    }

    /**
     * 应用是否正在执行广播.
     *
     * @param appInfo 包名
     */
    public boolean isBroadcastApp(AppInfo appInfo) {
        return appInfo.equals(broadcastApp);
    }


    public boolean isTargetApp(String packName) {
        Boolean targetApp = targetAppMap.get(packName);
        if (targetApp == null) {
            synchronized (targetAppMap) {
                targetApp = targetAppMap.get(packName);
                if (targetApp == null) {
                    targetApp = isTargetAppNoCache(packName);
                    targetAppMap.put(packName, targetApp);
                }
            }
        }
        return targetApp;
    }

    /**
     * 是否目标APP.
     *
     * @param packageName 包名
     */
    private boolean isTargetAppNoCache(String packageName) {
        if (activityManagerService == null) {
            Log.i(packageName + " activityManagerService is null");
            return false;
        }
        // 系统框架
        if (CommonConstants.ANDROID.equals(packageName) || CommonConstants.NOACTIVE_PACKAGE_NAME.equals(packageName)) {
            return false;
        }
        // 重要系统APP
        boolean isImportantSystemApp = activityManagerService.isImportantSystemApp(packageName);
        if (isImportantSystemApp) {
            return false;
        }
        // 系统APP
        boolean isSystem = activityManagerService.isSystem(packageName);
        // 判断是否白名单系统APP
        if (isSystem && !blackSystemApps.contains(packageName)) {
            return false;
        }
        // 不是白名单就是目标
        return !whiteApps.contains(packageName);
    }

    /**
     * 是否目标进程，
     *
     * @param processRecord 进程
     * @return 是否目标进程
     */
    public boolean isTargetProcess(int userId, ProcessRecord processRecord) {
        return isTargetProcess(false, userId, processRecord);
    }

    /**
     * 是否目标进程.
     *
     * @param ignoreApp     是否忽略APP判断
     * @param processRecord 进程
     */
    public boolean isTargetProcess(boolean ignoreApp, int userId, ProcessRecord processRecord) {
        if (activityManagerService == null) {
            return false;
        }
        // 不是主用户就不是目标APP
        if (processRecord.getUserId() != userId) {
            return false;
        }
        // 获取进程名
        String processName = processRecord.getProcessName();
        String packageName = processRecord.getPackageName();

        // 系统进程可能没有包名
        if (packageName == null) {
            return false;
        }

        // 不是目标APP就不是目标进程
        if (!ignoreApp && !isTargetApp(packageName)) {
            return false;
        }

        // 白名单进程不是目标进程
        return !whiteProcessList.contains(processName);
    }


    /**
     * 获取目标进程.
     *
     * @return 目标进程列表
     */
    public List<ProcessRecord> getTargetProcessRecords(AppInfo appInfo) {
        int userId = appInfo.getUserId();
        String packageName = appInfo.getPackageName();
        // 存放需要冻结/解冻的 processRecord
        List<ProcessRecord> targetProcessRecords = new ArrayList<>();
        if (activityManagerService == null) {
            return targetProcessRecords;
        }
        // 不是目标APP不需要处理
        if (!isTargetApp(packageName)) {
            return targetProcessRecords;
        }
        // 从AMS获取进程列表对象
        ProcessList processList = activityManagerService.getProcessList();
        // 通过包名从进程列表对象获取所有进程
        List<ProcessRecord> processRecords = processList.getProcessList(packageName);
        // 遍历进程列表
        for (ProcessRecord processRecord : processRecords) {
            boolean targetProcess = isTargetProcess(true, userId, processRecord);
            if (targetProcess) {
                // 添加目标进程
                targetProcessRecords.add(processRecord);
            }
        }
        return targetProcessRecords;
    }


    public void monitorNet(ApplicationInfo applicationInfo) {
        if (greezeManagerService == null) {
            return;
        }
        greezeManagerService.monitorNet(applicationInfo);
    }

    public void clearMonitorNet(ApplicationInfo applicationInfo) {
        if (greezeManagerService == null) {
            return;
        }
        greezeManagerService.clearMonitorNet(applicationInfo);
    }

}
