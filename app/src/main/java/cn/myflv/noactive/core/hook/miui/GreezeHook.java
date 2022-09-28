package cn.myflv.noactive.core.hook.miui;

import android.content.Context;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.GreezeManagerService;
import de.robv.android.xposed.XC_MethodHook;

public class GreezeHook extends MethodHook {

    private final MemData memData;

    public GreezeHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return null;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{Context.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
                GreezeManagerService greezeManagerService = new GreezeManagerService(param.thisObject);
                memData.setGreezeManagerService(greezeManagerService);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Network helper";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }
}
