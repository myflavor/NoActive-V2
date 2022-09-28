package cn.myflv.noactive.core.hook;

import android.os.Build;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 进程新增Hook.
 */
public class ProcessAddHook extends MethodHook {
    /**
     * 内存数据.
     */
    private final MemData memData;
    /**
     * 应用切换Hook.
     */
    private final FreezeUtils freezeUtils;

    private final Set<String> packageMap = Collections.synchronizedSet(new HashSet<>());

    public ProcessAddHook(ClassLoader classLoader, MemData memData, FreezeUtils freezeUtils) {
        super(classLoader);
        this.memData = memData;
        this.freezeUtils = freezeUtils;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.PidMap;
    }

    @Override
    public String getTargetMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MethodConstants.doAddInternal;
        } else {
            return MethodConstants.put;
        }
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Object[]{int.class, ClassConstants.ProcessRecord};
        } else {
            return new Object[]{ClassConstants.ProcessRecord};
        }
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
                int position;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    position = 1;
                } else {
                    position = 0;
                }
                ProcessRecord processRecord = new ProcessRecord(param.args[position]);
                // 不是目标就不处理
                if (!memData.isTargetProcess(processRecord)) {
                    return;
                }
                // 新开线程
                ThreadUtils.newThread(() -> {
                    // 包名
                    String packageName = processRecord.getPackageName();
                    // 正在处理就不重复处理看
                    if (packageMap.contains(packageName)) {
                        return;
                    }
                    packageMap.add(packageName);
                    freezeIfNeed(packageName);
                    // 处理完毕
                    packageMap.remove(packageName);
                });
                Log.d(processRecord.getProcessName() + " process added");
            }
        };
    }

    public void freezeIfNeed(String packageName) {
        // 等待3s处理
        ThreadUtils.sleep(3000);
        // 前台就不处理
        if (!memData.getFreezerAppSet().contains(packageName)) {
            return;
        }
        if (!memData.isTargetApp(packageName)) {
            return;
        }
        // 包名查找进程
        List<ProcessRecord> processList = memData.getTargetProcessRecords(packageName);
        // 空不处理
        if (processList.isEmpty()) {
            return;
        }
        // 冻结APP添加
        memData.getFreezerAppSet().add(packageName);
        // 等待广播休眠
        memData.waitBroadcastIdle(packageName);
        // 锁包名
        // 再次确定不在前台
        if (!memData.getFreezerAppSet().contains(packageName)) {
            return;
        }
        for (ProcessRecord processRecord : processList) {
            freezeUtils.freezer(processRecord);
        }
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Listen process add";
    }
}
