package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.NetworkManagementService;
import de.robv.android.xposed.XC_MethodHook;

public class NetworkManagerHook extends MethodHook {
    private final MemData memData;

    public NetworkManagerHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.NetworkManagementService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.systemReady;
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
                NetworkManagementService networkManagementService = new NetworkManagementService(classLoader, param.thisObject);
                memData.setNetworkManagementService(networkManagementService);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Auto block network";
    }
}
