package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.PowerManagerService;
import de.robv.android.xposed.XC_MethodHook;

/**
 * PMS启动Hook.
 */
public class PowerManagerHook extends MethodHook {

    /**
     * 数据类.
     */
    private final MemData memData;

    public PowerManagerHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.PowerManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.onStart;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                PowerManagerService powerManagerService = new PowerManagerService(param.thisObject);
                // 存进数据类
                memData.setPowerManagerService(powerManagerService);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Auto wakelock";
    }

}
