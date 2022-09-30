package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractReplaceHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

/**
 * ANR相关Hook.
 */
public class ANRHook extends MethodHook {

    /**
     * 内存数据
     */
    private final MemData memData;

    public ANRHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return ClassConstants.AnrHelper;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return ClassConstants.ProcessRecord;
        } else {
            return ClassConstants.AppErrors;
        }
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.appNotResponding;
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return new Object[]{
                    ClassConstants.ProcessRecord, String.class, ClassConstants.ApplicationInfo,
                    String.class, ClassConstants.WindowProcessController,
                    boolean.class, String.class};
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return new Object[]{
                    String.class, ClassConstants.ApplicationInfo, String.class,
                    ClassConstants.WindowProcessController, boolean.class, String.class};
        } else {
            return new Object[]{
                    ClassConstants.ProcessRecord, ClassConstants.ActivityRecord_P,
                    ClassConstants.ActivityRecord_P, boolean.class, String.class};
        }
    }

    @Override
    public XC_MethodHook getTargetHook() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return new AbstractReplaceHook() {
                @Override
                protected Object replaceMethod(MethodHookParam param) throws Throwable {
                    // 获取方法参数
                    Object[] args = param.args;
                    // ANR进程为空就不处理
                    if (args[0] == null) return null;
                    // ANR进程
                    ProcessRecord processRecord = new ProcessRecord(args[0]);
                    // 进程对应包名
                    String packageName = processRecord.getPackageName();
                    if (memData.isAppFreezer(processRecord.getUserId(), packageName)) {
                        Log.d("Keep " + (processRecord.getProcessName() != null ? processRecord.getProcessName() : packageName));
                        // 不处理
                        return null;
                    }
                    return invokeOriginalMethod(param);
                }
            };
        } else {
            return XC_MethodReplacement.DO_NOTHING;
        }
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.P;
    }

    @Override
    public String successLog() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return "Auto keep ANR";
        } else {
            return "Force keep ANR";
        }
    }


}
