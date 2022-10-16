package cn.myflv.noactive.core.hook.idle;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.hook.base.AbstractReplaceHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

public class IdleWhiteListRemoveHook extends MethodHook {


    public IdleWhiteListRemoveHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.DeviceIdleControllerBinderService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.removePowerSaveWhitelistApp;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{String.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractReplaceHook() {
            @Override
            protected Object replaceMethod(MethodHookParam param) throws Throwable {
                Log.d("avoid remove power idle white list");
                return null;
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Listen power idle remove";
    }
}
