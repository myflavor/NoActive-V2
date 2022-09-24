package cn.myflv.noactive.core.hook.miui;

import android.content.Context;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.MethodHook;
import cn.myflv.noactive.core.server.GreezeManagerService;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class GreezeHook extends MethodHook {

    private final MemData memData;

    public GreezeHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.GreezeManagerService;
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
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                GreezeManagerService greezeManagerService = new GreezeManagerService(param.thisObject);
                memData.setGreezeManagerService(greezeManagerService);
                try {
                    XposedHelpers.setStaticBooleanField(param.thisObject.getClass(), FieldEnum.sEnable, false);
                    XposedHelpers.setBooleanField(param.thisObject, FieldEnum.mPowerMilletEnable, false);
                    Log.i("Disable Millet");
                } catch (Throwable throwable) {
                    Log.e("Disable Millet", throwable);
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
        return "Network helper";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }
}
