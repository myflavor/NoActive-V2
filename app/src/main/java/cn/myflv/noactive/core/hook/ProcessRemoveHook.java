package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.FreezeUtils;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 进程移除Hook.
 */
public class ProcessRemoveHook extends MethodHook {
    /**
     * 内存数据.
     */
    private final MemData memData;
    /**
     * 冻结工具类.
     */
    private final FreezeUtils freezeUtils;

    public ProcessRemoveHook(ClassLoader classLoader, MemData memData, FreezeUtils freezeUtils) {
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
            return MethodConstants.doRemoveInternal;
        } else {
            return MethodConstants.remove;
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
                // 如果APP没有冻结就不处理
                if (!memData.getFreezerAppSet().contains(processRecord.getPackageName())) {
                    return;
                }
                Log.i(processRecord.getProcessName() + " process removed");
                if (freezeUtils.isUseV1()) {
                    // 解冻
                    freezeUtils.unFreezer(processRecord);
                }
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Listen process remove";
    }
}
