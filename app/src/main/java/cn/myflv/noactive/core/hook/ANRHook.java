package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class ANRHook extends MethodHook {

    private final MemData memData;

    public ANRHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return ClassEnum.AnrHelper;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return ClassEnum.ProcessRecord;
        } else {
            return ClassEnum.AppErrors;
        }
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.appNotResponding;
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return new Object[]{
                    ClassEnum.ProcessRecord, String.class, ClassEnum.ApplicationInfo,
                    String.class, ClassEnum.WindowProcessController,
                    boolean.class, String.class};
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            return new Object[]{
                    String.class, ClassEnum.ApplicationInfo, String.class,
                    ClassEnum.WindowProcessController, boolean.class, String.class};
        } else {
            return new Object[]{
                    ClassEnum.ProcessRecord, ClassEnum.ActivityRecord_P,
                    ClassEnum.ActivityRecord_P, boolean.class, String.class};
        }
    }

    @Override
    public XC_MethodHook getTargetHook() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    // 获取方法参数
                    Object[] args = param.args;
                    // ANR进程为空就不处理
                    if (args[0] == null) return null;
                    // ANR进程
                    ProcessRecord processRecord = new ProcessRecord(args[0]);
                    // 进程对应包名
                    String packageName = processRecord.getApplicationInfo().getPackageName();
                    // 不是目标APP就调用原方法
                    if (!memData.isTargetApp(packageName)) {
                        return invokeOriginalMethod(param);
                    }
                    Log.d("Keep " + (processRecord.getProcessName() != null ? processRecord.getProcessName() : packageName));
                    // 不处理
                    return null;
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
