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
public class BinderTransHook extends MethodHook {

    private final static String REASON = "received sync binder";

    /**
     * 应用切换Hook
     */
    private final FreezerHandler freezerHandler;

    public BinderTransHook(ClassLoader classLoader, FreezerHandler freezerHandler) {
        super(classLoader);
        this.freezerHandler = freezerHandler;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.reportBinderTrans;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, int.class, int.class, int.class, int.class, boolean.class, long.class, int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
                Object[] args = param.args;
                int uid = (int) args[0];
                // 是否异步
                boolean isOneway = (boolean) args[5];
                if (isOneway) {
                    // 异步不处理
                    return;
                }
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
        return "Perfect Freezer";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }

}
