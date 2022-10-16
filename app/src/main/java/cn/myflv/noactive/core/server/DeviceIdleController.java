package cn.myflv.noactive.core.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.myflv.noactive.constant.CommonConstants;
import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class DeviceIdleController {
    private final Object instance;
    private final int STATE_IDLE;
    private final static String KEY = "NoActive-DeviceIdleController";

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
        Log.d("exit deep doze");
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


    public Set<String> getWhiteList() {
        synchronized (instance) {
            Object mPowerSaveWhitelistUserApps = XposedHelpers.getObjectField(instance, FieldConstants.mPowerSaveWhitelistUserApps);
            Set<?> whiteSet = (Set<?>) XposedHelpers.callMethod(mPowerSaveWhitelistUserApps, MethodConstants.keySet);
            Set<String> result = new HashSet<>();
            for (Object o : whiteSet) {
                if (o == null) {
                    continue;
                }
                String pkg = (String) o;
                result.add(pkg);
            }
            return result;
        }
    }

    public void addWhiteList(List<String> pkgNames) {
        for (String pkgName : pkgNames) {
            Log.d("power white list add " + pkgName);
        }
        XposedHelpers.callMethod(instance, MethodConstants.addPowerSaveWhitelistAppsInternal, pkgNames);
    }

    public void removeWhiteList(String pkgName) {
        Log.d("power white list remove " + pkgName);
        XposedHelpers.callMethod(instance, MethodConstants.removePowerSaveWhitelistAppInternal, pkgName);
    }

}
