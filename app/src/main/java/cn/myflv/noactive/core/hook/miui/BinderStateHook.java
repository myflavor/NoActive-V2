package cn.myflv.noactive.core.hook.miui;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Binder通信Hook.
 */
public class BinderStateHook extends MethodHook {

    private final static String REASON = "check binder";
    private final static int BINDER_BUSY = 1;

    /**
     * 应用切换Hook
     */
    private final FreezerHandler freezerHandler;

    public BinderStateHook(ClassLoader classLoader, FreezerHandler freezerHandler) {
        super(classLoader);
        this.freezerHandler = freezerHandler;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.reportBinderState;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, int.class, int.class, int.class, long.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
                Object[] args = param.args;
                int state = (int) args[3];
                if (state != BINDER_BUSY) {
                    return;
                }
                int uid = (int) args[0];
                freezerHandler.temporaryUnfreezeIfNeed(uid, REASON);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Listen binder state";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }

}
