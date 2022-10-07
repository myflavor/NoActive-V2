package cn.myflv.noactive.core.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.core.entity.AppInfo;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class PowerManagerService {
    private final Object powerManagerService;
    private final Object wakeLocks;

    public PowerManagerService(Object powerManagerService) {
        this.powerManagerService = powerManagerService;
        this.wakeLocks = XposedHelpers.getObjectField(powerManagerService, FieldConstants.mWakeLocks);
    }


    public Map<Integer, List<WakeLock>> getWakeLockMap() {
        Map<Integer, List<WakeLock>> wakeLockMap = new HashMap<>();
        try {
            synchronized (wakeLocks) {
                List<?> wakeLockList = (List<?>) wakeLocks;
                for (Object item : wakeLockList) {
                    WakeLock wakeLock = new WakeLock(item);
                    List<WakeLock> list = wakeLockMap.computeIfAbsent(wakeLock.getUid(), k -> new ArrayList<>());
                    list.add(wakeLock);
                }
            }
        } catch (Throwable throwable) {
            Log.e("getWakeLockMap", throwable);
        }
        return wakeLockMap;
    }

    public void setWakeLocksDisabled(AppInfo appInfo, int uid, boolean disabled) {
        List<WakeLock> wakeLocks = getWakeLockMap().get(uid);
        if (wakeLocks == null || wakeLocks.isEmpty()) {
            return;
        }
        for (WakeLock wakeLock : wakeLocks) {
            wakeLock.setDisabled(disabled);
            String tag = wakeLock.getPackageName() + ":" + appInfo.getUserId() + "(" + wakeLock.getTag() + ")";
            Log.d(tag + " wakelock " + (disabled ? "disabled" : "enabled"));
        }
    }



}
