package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.hook.base.AbstractReplaceHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

public class TaskTrimHook extends MethodHook {

    public TaskTrimHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.RecentTasks;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.trimInactiveRecentTasks;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractReplaceHook() {
            @Override
            protected Object replaceMethod(MethodHookParam param) throws Throwable {
                Log.d("avoid trimInactiveRecentTasks");
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
        return "Disable task trim";
    }

}
