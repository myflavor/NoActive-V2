package cn.myflv.noactive.core.hook;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XC_MethodHook;

/**
 * 网络接收Hook.
 */
public class NetReceiveHook extends MethodHook {
    private final static String REASON = "received socket data";
    /**
     * 正在Binder通信
     */
    private final Set<Integer> netReceiveSet = Collections.synchronizedSet(new HashSet<>());

    /**
     * 应用切换Hook
     */
    private final ActivitySwitchHook activitySwitchHook;

    public NetReceiveHook(ClassLoader classLoader, ActivitySwitchHook activitySwitchHook) {
        super(classLoader);
        this.activitySwitchHook = activitySwitchHook;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.GreezeManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.reportNet;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, long.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Object[] args = param.args;
                int uid = (int) args[0];
                // 新开线程，冻结需要3s，会ANR
                ThreadUtils.newThread(() -> {
                    // 尝试添加UID，如果添加成功则说明没有正在Binder解冻
                    if (netReceiveSet.add(uid)) {
                        // 通知应用切换收到Binder
                        activitySwitchHook.temporaryUnfreeze(uid, REASON);
                        // 执行完毕移除
                        netReceiveSet.remove(uid);
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
        return "Network Awake";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }

}
