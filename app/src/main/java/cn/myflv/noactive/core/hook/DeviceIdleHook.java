package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.DeviceIdleController;
import de.robv.android.xposed.XC_MethodHook;

public class DeviceIdleHook extends MethodHook {

    private final MemData memData;

    public DeviceIdleHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.DeviceIdleController;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.onStart;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
                DeviceIdleController deviceIdleController = new DeviceIdleController(param.thisObject);
                memData.setDeviceIdleController(deviceIdleController);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Auto deep doze";
    }
}
