package cn.myflv.noactive.core.hook;

import android.view.Display;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.AppInfo;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.handler.FreezerHandler;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;

public class ScreenStateHook extends MethodHook {

    private final MemData memData;

    private final FreezerHandler freezerHandler;

    public ScreenStateHook(ClassLoader classLoader, MemData memData, FreezerHandler freezerHandler) {
        super(classLoader);
        this.memData = memData;
        this.freezerHandler = freezerHandler;
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.DisplayPowerController;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setScreenState;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, boolean.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
                // 方法是否执行成功
                boolean isChange = (boolean) param.getResult();
                if (!isChange) {
                    return;
                }
                Object[] args = param.args;
                // 显示状态
                int state = (int) args[0];
                if (state != Display.STATE_OFF && state != Display.STATE_ON) {
                    return;
                }
                // 是否关闭
                boolean isOn = (state == Display.STATE_ON);
                // 存储状态
                if (!memData.setScreenOn(isOn)) {
                    return;
                }
                Log.d("screen " + (isOn ? "on" : "off"));
                AppInfo lastAppInfo = memData.getLastAppInfo();
                if (isOn) {
                    freezerHandler.onResume(true, lastAppInfo);
                } else {
                    freezerHandler.onPause(true, lastAppInfo, 30 * 1000);
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
        return "Listen screen state";
    }
}
