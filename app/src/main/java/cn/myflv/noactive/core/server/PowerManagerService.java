package cn.myflv.noactive.core.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class PowerManagerService {
    private final Object powerManagerService;
    private final Map<String, List<WakeLock>> wakeLockMap = new HashMap<>();

    public PowerManagerService(Object powerManagerService) {
        this.powerManagerService = powerManagerService;
        Object mWakeLocks = XposedHelpers.getObjectField(powerManagerService, FieldEnum.mWakeLocks);
        synchronized (XposedHelpers.getObjectField(powerManagerService, FieldEnum.mWakeLocks)) {
            List<?> wakeLocks = (List<?>) mWakeLocks;
            for (Object item : wakeLocks) {
                WakeLock wakeLock = new WakeLock(item);
                List<WakeLock> list = wakeLockMap.computeIfAbsent(wakeLock.getPackageName(), k -> new ArrayList<>());
                list.add(wakeLock);
            }
        }
    }


    public void release(String packageName) {
        List<WakeLock> wakeLocks = wakeLockMap.get(packageName);
        if (wakeLocks == null) {
            return;
        }
        for (WakeLock wakeLock : wakeLocks) {
            release(wakeLock);
        }
    }


    public void release(WakeLock wakeLock) {
        XposedHelpers.callMethod(powerManagerService, MethodEnum.releaseWakeLockInternal, wakeLock.getLock(), wakeLock.getFlags());
        Log.d(wakeLock.getPackageName() + "(" + wakeLock.getTag() + ") wakelock released");
    }


}
