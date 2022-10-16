package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.ActivityManagerService;
import de.robv.android.xposed.XC_MethodHook;

public class ActivityManagerHook extends MethodHook {

    private final MemData memData;

    public ActivityManagerHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setSystemProcess;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
                ActivityManagerService activityManagerService = new ActivityManagerService(param.thisObject);
                memData.setActivityManagerService(activityManagerService);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Activity helper";
    }
}
