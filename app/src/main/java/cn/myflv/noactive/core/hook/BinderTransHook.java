package cn.myflv.noactive.core.hook;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.handler.FreezerHandler;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Binder通信Hook.
 */
public class BinderTransHook extends MethodHook {

    private final static String REASON = "received sync binder";
    /**
     * 正在Binder通信
     */
    private final Set<Integer> binderTransSet = Collections.synchronizedSet(new HashSet<>());

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
        return ClassEnum.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.reportBinderTrans;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, int.class, int.class, int.class, int.class, boolean.class, long.class, int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
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
