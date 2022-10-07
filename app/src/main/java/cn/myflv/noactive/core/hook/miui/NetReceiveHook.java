package cn.myflv.noactive.core.hook.miui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 网络接收Hook.
 */
public class NetReceiveHook extends MethodHook {
    private final static String REASON = "received socket data";
    /**
     * 应用切换Hook
     */
    private final FreezerHandler freezerHandler;

    public NetReceiveHook(ClassLoader classLoader, FreezerHandler freezerHandler) {
        super(classLoader);
        this.freezerHandler = freezerHandler;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.reportNet;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, long.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
                Object[] args = param.args;
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
        return "Network Awake";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }

}
