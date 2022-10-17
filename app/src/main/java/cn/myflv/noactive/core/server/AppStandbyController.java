package cn.myflv.noactive.core.server;

import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.AppInfo;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class AppStandbyController {

    private final Object appStandbyController;

    public AppStandbyController(Object appStandbyController) {
        this.appStandbyController = appStandbyController;
    }

    public void forceIdleState(AppInfo appInfo, boolean idle) {
        int userId = appInfo.getUserId();
        String packageName = appInfo.getPackageName();
        try {
            XposedHelpers.callMethod(appStandbyController, MethodConstants.forceIdleState, packageName, userId, idle);
            Log.d(appInfo.getKey() + " " + (idle ? "idle" : "active"));
        } catch (Throwable throwable) {
            Log.e("forceIdleState", throwable);
        }
    }
}
