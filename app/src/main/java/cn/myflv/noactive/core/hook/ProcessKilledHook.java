package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.ProcessRecord;
import de.robv.android.xposed.XC_MethodHook;
@Deprecated
public class ProcessKilledHook extends MethodHook {

    private final MemData memData;

    public ProcessKilledHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.ProcessRecord;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.setKilled;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{boolean.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Object[] args = param.args;
                boolean killed = (boolean) args[0];
                if (!killed) {
                    return;
                }
                ProcessRecord processRecord = new ProcessRecord(param.thisObject);
                if (!processRecord.isSandboxProcess()) {
                    return;
                }
                String packageName = processRecord.getApplicationInfo().getPackageName();
                if (!memData.getFreezerAppSet().contains(packageName)) {
                    return;
                }
                memData.getActivityManagerService().killApp(packageName);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.S;
    }

    @Override
    public String successLog() {
        return "Listen process killed";
    }

}
