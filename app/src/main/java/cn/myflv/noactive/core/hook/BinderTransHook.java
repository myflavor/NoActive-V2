package cn.myflv.noactive.core.hook;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import de.robv.android.xposed.XC_MethodHook;

public class BinderTransHook extends MethodHook {

    private final Set<Integer> binderTransSet = Collections.synchronizedSet(new HashSet<>());

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
                boolean isOneway = (boolean) args[5];
                if (isOneway) {
                    return;
                }
                if (binderTransSet.add(uid)) {
                    activitySwitchHook.binderReceived(uid);
                    binderTransSet.remove(uid);
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
        return "Perfect Freezer";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }

}
