package cn.myflv.noactive.core.hook;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XC_MethodHook;

/**
 * Binder通信Hook.
 */
public class BinderTransHook extends MethodHook {
    /**
     * 正在Binder通信
     */
    private final Set<Integer> binderTransSet = Collections.synchronizedSet(new HashSet<>());

    /**
     * 应用切换Hook
     */
    private final ActivitySwitchHook activitySwitchHook;

    public BinderTransHook(ClassLoader classLoader, ActivitySwitchHook activitySwitchHook) {
        super(classLoader);
        this.activitySwitchHook = activitySwitchHook;
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
                // 新开线程，冻结需要3s，会ANR
                ThreadUtils.newThread(() -> {
                    // 尝试添加UID，如果添加成功则说明没有正在Binder解冻
                    if (binderTransSet.add(uid)) {
                        // 通知应用切换收到Binder
                        activitySwitchHook.binderReceived(uid);
                        // 执行完毕移除
                        binderTransSet.remove(uid);
                    }
                });
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
