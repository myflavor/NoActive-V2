package cn.myflv.noactive.core.server;

import android.os.IBinder;

import cn.myflv.noactive.core.entity.FieldEnum;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class WakeLock {
    private String packageName;
    private int flags;
    private final IBinder lock;
    private final String tag;

    public WakeLock(Object wakeLock) {
        this.packageName = (String) XposedHelpers.getObjectField(wakeLock, FieldEnum.mPackageName);
        this.tag = (String) XposedHelpers.getObjectField(wakeLock, FieldEnum.mTag);
        this.flags = XposedHelpers.getIntField(wakeLock, FieldEnum.mFlags);
        this.lock = (IBinder) XposedHelpers.getObjectField(wakeLock, FieldEnum.mLock);
    }
}
