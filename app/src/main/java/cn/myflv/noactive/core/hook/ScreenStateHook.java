package cn.myflv.noactive.core.hook;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
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

    private final Handler handler = new Handler();

    private final MemData memData;

    private final FreezerHandler freezerHandler;

    private final AlarmManager.OnAlarmListener deepDozeListener;
    private final AlarmManager.OnAlarmListener freezerListener;
    private boolean isDozeNow = false;
    private boolean isFreezeNow = false;

    public ScreenStateHook(ClassLoader classLoader, MemData memData, FreezerHandler freezerHandler) {
        super(classLoader);
        this.memData = memData;
        this.freezerHandler = freezerHandler;
        freezerListener = () -> {
            synchronized (ScreenStateHook.this) {
                freezerHandler.onPause(true, memData.getLastAppInfo());
                isFreezeNow = false;
            }
        };
        deepDozeListener = () -> {
            synchronized (ScreenStateHook.this) {
                memData.getDeviceIdleController().deepDoze();
                isDozeNow = false;
            }
        };
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
                screenChange(isOn);
            }
        };
    }

    public void screenChange(boolean isOn) {
        synchronized (ScreenStateHook.this) {
            Log.d("screen " + (isOn ? "on" : "off"));
            AppInfo lastAppInfo = memData.getLastAppInfo();
            boolean targetApp = memData.isTargetApp(lastAppInfo.getPackageName());
            if (isOn) {
                cancelAll();
                freezerHandler.onResume(targetApp, lastAppInfo);
                memData.getDeviceIdleController().exitDeepDoze();
            } else {
                if (targetApp) {
                    isFreezeNow = true;
                    run(30 * 1000, "Freezer", freezerListener);
                }
                isDozeNow = true;
                run(60 * 1000, "Doze", deepDozeListener);
            }
        }
    }

    public void cancelAll() {
        Context context = memData.getContext();
        if (context == null) {
            return;
        }
        // 获取定时器
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        // 取消冻结定时器
        if (isFreezeNow) {
            alarmManager.cancel(freezerListener);
            Log.d("give up to freeze");
        }
        if (isDozeNow) {
            // 取消Doze定时器
            alarmManager.cancel(deepDozeListener);
            Log.d("give up to doze");
        }
    }


    public void run(long delay, String tag, AlarmManager.OnAlarmListener listener) {
        Context context = memData.getContext();
        if (context == null) {
            return;
        }
        // 获取定时器
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        // 设置指定时间后唤醒
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                "NoActive." + tag, listener, handler);
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
