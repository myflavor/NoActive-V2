package cn.myflv.noactive.core.server;

import android.content.pm.ApplicationInfo;

import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class GreezeManagerService {
    private final Object greezeManagerService;

    public GreezeManagerService(Object greezeManagerService) {
        this.greezeManagerService = greezeManagerService;
    }

    public void monitorNet(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return;
        }
        try {
            XposedHelpers.callMethod(greezeManagerService, MethodEnum.monitorNet, applicationInfo.uid);
            Log.d(applicationInfo.packageName + " monitorNet");
        } catch (Throwable throwable) {
            Log.e("monitorNet", throwable);
        }

    }

    public void clearMonitorNet(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return;
        }
        try {
            XposedHelpers.callMethod(greezeManagerService, MethodEnum.clearMonitorNet, applicationInfo.uid);
            Log.d(applicationInfo.packageName + " clearMonitorNet");
        } catch (Throwable throwable) {
            Log.e("clearMonitorNet", throwable);
        }
    }
}
