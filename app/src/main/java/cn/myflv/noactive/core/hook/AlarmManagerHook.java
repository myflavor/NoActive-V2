package cn.myflv.noactive.core.hook;

import android.os.Build;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.AlarmMangerService;
import de.robv.android.xposed.XC_MethodHook;

public class AlarmManagerHook extends MethodHook {
    private final MemData memData;

    public AlarmManagerHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return ClassConstants.AlarmManagerService_R;
        }
        return ClassConstants.AlarmManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.onBootPhase;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
                AlarmMangerService alarmMangerService = new AlarmMangerService(param.thisObject);
                memData.setAlarmMangerService(alarmMangerService);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Alarm helper";
    }
}
