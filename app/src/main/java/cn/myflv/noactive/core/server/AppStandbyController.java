package cn.myflv.noactive.core.server;

import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class AppStandbyController {

    private final Object appStandbyController;

    public AppStandbyController(Object appStandbyController) {
        this.appStandbyController = appStandbyController;
    }

    public void forceIdleState(String packageName, boolean idle) {
        try {
            XposedHelpers.callMethod(appStandbyController, MethodEnum.forceIdleState, packageName, ActivityManagerService.MAIN_USER, idle);
            Log.i(packageName + " " + (idle ? "idle" : "active"));
        } catch (Throwable throwable) {
            Log.e("forceIdleState", throwable);
        }
    }
}
