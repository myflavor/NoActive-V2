package cn.myflv.noactive.core.hook;

import android.content.Intent;
import android.os.Build;

import java.util.Arrays;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
@Deprecated
public class ActivityIdleHook extends MethodHook {
    private final MemData memData;

    public ActivityIdleHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.Q:
                return ClassEnum.ActivityStackSupervisorHandler;
            case Build.VERSION_CODES.R:
                return ClassEnum.ActivityStackSupervisor;
            default:
                return ClassEnum.ActivityTaskSupervisor;
        }
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.activityIdleInternal;
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            return new Object[]{ClassEnum.ActivityRecord, boolean.class, boolean.class, ClassEnum.Configuration};
        } else {
            return new Object[]{ClassEnum.ActivityRecord, boolean.class};
        }
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object[] args = param.args;
                if (args[0] != null) {
                    Object activityRecord = args[0];
                    if (XposedHelpers.getObjectField(activityRecord, FieldEnum.intent) != null) {
                        Intent intent = (Intent) XposedHelpers.getObjectField(activityRecord, FieldEnum.intent);
                        String packageName = intent.getComponent().getPackageName();
                        if (memData.isTargetApp(packageName)) {
                            Log.i("Avoid " + packageName + " activity idle");
                            return null;
                        }
                    }
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.Q;
    }

    @Override
    public String successLog() {
        return "Listen activity idle";
    }

}
