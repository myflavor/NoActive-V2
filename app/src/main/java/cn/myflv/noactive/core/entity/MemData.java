package cn.myflv.noactive.core.entity;


import android.os.FileObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.myflv.noactive.core.server.ActivityManagerService;
import cn.myflv.noactive.core.server.AppStandbyController;
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
     * 冻结Token.
     */
    private final Map<String, Long> freezerTokenMap = Collections.synchronizedMap(new HashMap<>());
    /**
     * 正在执行广播的APP.
     */
    private final Map<String, Integer> broadcastAppMap = new HashMap<>();
    /**
     * 白名单APP.
     */
    private Set<String> whiteApps = new HashSet<>();
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
     * 已冻结APP.
     */
    private final Set<String> freezerAppSet = Collections.synchronizedSet(FreezerConfig.isScheduledOn() ? new LinkedHashSet<>() : new HashSet<>());
    /**
     * 前台应用.
     */
    private final Set<String> foregroundAppSet = Collections.synchronizedSet(new HashSet<>());

    /**
     * PMS.
     */
    private PowerManagerService powerManagerService = null;
    /**
     * AMS.
     */
    private ActivityManagerService activityManagerService = null;

    private AppStandbyController appStandbyController = null;

    /**
     * 配置文件监听.
     */
    private final FileObserver fileObserver;

    public MemData() {
        // 初始化监听
        fileObserver = new ConfigFileObserver(this);
        // 开始监听配置文件
        fileObserver.startWatching();
    }

    /**
     * 等待应用未执行广播.
     *
     * @param packageName 包名
     */
    public void waitBroadcastIdle(String packageName) {
        while (isBroadcastApp(packageName)) {
            Log.d(packageName + " is executing broadcast");
            ThreadUtils.sleep(100);
        }
        Log.d(packageName + " broadcast state idle");
    }

    /**
     * 应用是否正在执行广播.
     *
     * @param packageName 包名
     */
    public boolean isBroadcastApp(String packageName) {
        synchronized (broadcastAppMap) {
            return broadcastAppMap.containsKey(packageName);
        }
    }

    /**
     * 应用广播开始.
     *
     * @param packageName 包名
     */
    public void broadcastStart(String packageName) {
        Log.d(packageName + " broadcast executing start");
        synchronized (broadcastAppMap) {
            int count = broadcastAppMap.computeIfAbsent(packageName, k -> 0);
            count++;
            broadcastAppMap.put(packageName, count);
        }
    }

    /**
     * 应用广播结束.
     *
     * @param packageName 包名
     */
    public void broadcastFinish(String packageName) {
        Log.d(packageName + " broadcast executing finish");
        synchronized (broadcastAppMap) {
            int count = broadcastAppMap.computeIfAbsent(packageName, k -> 0);
            count--;
            if (count > 0) {
                broadcastAppMap.put(packageName, count);
            } else {
                Log.d(packageName + " broadcast state idle");
                broadcastAppMap.remove(packageName);
            }
        }
    }

    /**
     * 是否目标APP.
     *
     * @param packageName 包名
     */
    public boolean isTargetApp(String packageName) {
        if (activityManagerService == null) {
            return false;
        }
        // 系统框架
        if (packageName.equals("android")) {
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
    public boolean isTargetProcess(ProcessRecord processRecord) {
        return isTargetProcess(false, processRecord);
    }

    /**
     * 是否目标进程.
     *
     * @param ignoreApp     是否忽略APP判断
     * @param processRecord 进程
     */
    public boolean isTargetProcess(boolean ignoreApp, ProcessRecord processRecord) {
        if (activityManagerService == null) {
            return false;
        }
        // 不是主用户就不是目标APP
        if (processRecord.getUserId() != ActivityManagerService.MAIN_USER) {
            return false;
        }
        // 获取进程名
        String processName = processRecord.getProcessName();
        String packageName = processRecord.getApplicationInfo().getPackageName();

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
     * @param packageName 包名
     * @return 目标进程列表
     */
    public List<ProcessRecord> getTargetProcessRecords(String packageName) {

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
            boolean targetProcess = isTargetProcess(true, processRecord);
            if (targetProcess) {
                // 添加目标进程
                targetProcessRecords.add(processRecord);
            }
        }
        return targetProcessRecords;
    }


    /**
     * 设置冻结Token.
     *
     * @param packageName 应用包名
     * @param token       token
     */
    public void setToken(String packageName, long token) {
        freezerTokenMap.put(packageName, token);
    }

    /**
     * 校验Token.
     *
     * @param packageName 应用包名
     * @param value       值
     * @return 是否正确
     */
    public boolean isInCorrectToken(String packageName, long value) {
        Long token = freezerTokenMap.get(packageName);
        return token == null || value != token;
    }

}
