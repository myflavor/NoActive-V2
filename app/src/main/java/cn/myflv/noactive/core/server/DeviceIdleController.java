package cn.myflv.noactive.core.server;

import android.os.Handler;

import cn.myflv.noactive.constant.CommonConstants;
import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.utils.Log;
import cn.myflv.noactive.core.utils.ThreadUtils;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class DeviceIdleController {
    private final Object instance;
    private final int STATE_IDLE;
    private final static String KEY = "NoActive-DeviceIdleController";
    private final Handler handler = new Handler();

    private boolean idle = false;

    public DeviceIdleController(Object instance) {
        this.instance = instance;
        STATE_IDLE = XposedHelpers.getStaticIntField(instance.getClass(), FieldConstants.STATE_IDLE);
    }

    public void deepDoze() {
        if (idle) {
            return;
        }
        setDeepEnabled(true);
        setForceIdle(true);
        becomeInactiveIfAppropriateLocked();
        int curState = getCurState();
        while (curState != STATE_IDLE) {
            stepIdleStateLocked();
            if (curState == getCurState()) {
                Log.w("deep doze failed");
                return;
            }
            curState = getCurState();
        }
        idle = true;
        Log.d("deep doze success");
    }


    public void exitDeepDoze() {
        if (!idle) {
            return;
        }
        XposedHelpers.callMethod(instance, MethodConstants.exitForceIdleLocked);
        idle = false;
        Log.d("deep doze exited");
    }

    public void stepIdleStateLocked() {
        XposedHelpers.callMethod(instance, MethodConstants.stepIdleStateLocked, CommonConstants.NOACTIVE_PACKAGE_NAME);
    }

    public void setDeepEnabled(boolean enabled) {
        XposedHelpers.setBooleanField(instance, FieldConstants.mDeepEnabled, enabled);
    }

    public void setForceIdle(boolean forceIdle) {
        XposedHelpers.setBooleanField(instance, FieldConstants.mForceIdle, forceIdle);
    }

    public void becomeInactiveIfAppropriateLocked() {
        XposedHelpers.callMethod(instance, MethodConstants.becomeInactiveIfAppropriateLocked);
    }

    public int getCurState() {
        return XposedHelpers.getIntField(instance, FieldConstants.mState);
    }

}
