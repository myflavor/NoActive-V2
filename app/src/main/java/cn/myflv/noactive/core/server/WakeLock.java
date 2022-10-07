package cn.myflv.noactive.core.server;

import android.os.IBinder;

import cn.myflv.noactive.constant.FieldConstants;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class WakeLock {
    private final Object instance;
    private final IBinder lock;
    private final String tag;
    private String packageName;
    private int flags;
    private int uid;

    public WakeLock(Object wakeLock) {
        instance = wakeLock;
        this.packageName = (String) XposedHelpers.getObjectField(wakeLock, FieldConstants.mPackageName);
        this.tag = (String) XposedHelpers.getObjectField(wakeLock, FieldConstants.mTag);
        this.flags = XposedHelpers.getIntField(wakeLock, FieldConstants.mFlags);
        this.lock = (IBinder) XposedHelpers.getObjectField(wakeLock, FieldConstants.mLock);
        this.uid = XposedHelpers.getIntField(wakeLock, FieldConstants.mOwnerUid);
    }

    public void setDisabled(boolean disabled) {
        XposedHelpers.setObjectField(instance, FieldConstants.mDisabled, disabled);
    }
}
