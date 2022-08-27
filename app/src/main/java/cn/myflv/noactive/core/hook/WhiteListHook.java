package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class WhiteListHook extends MethodHook {
    private final MemData memData;

    public WhiteListHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.ProcessManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.isPackageInList;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{String.class, int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object[] args = param.args;
                if (args[0] != null) {
                    String packageName = (String) args[0];
                    if (memData.getFreezerAppSet().contains(packageName)) {
                        Log.i(packageName+" is whiteList");
                        return true;
                    }
                }
                return invokeOriginalMethod(param);
            }
        };
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Auto whiteList";
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }
}
