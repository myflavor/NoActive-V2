package cn.myflv.noactive.core.server;

import cn.myflv.noactive.core.entity.FieldEnum;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class ApplicationInfo {
    private final int FLAG_SYSTEM;
    private final int FLAG_UPDATED_SYSTEM_APP;
    private final int flags;
    private final int uid;
    private final String processName;
    private final String packageName;
    private Object applicationInfo;

    public ApplicationInfo(Object applicationInfo) {
        this.applicationInfo = applicationInfo;
        this.processName = (String) XposedHelpers.getObjectField(applicationInfo, FieldEnum.processName);
        this.packageName = (String) XposedHelpers.getObjectField(applicationInfo, FieldEnum.packageName);
        this.flags = XposedHelpers.getIntField(applicationInfo, FieldEnum.flags);
        this.uid = XposedHelpers.getIntField(applicationInfo, FieldEnum.uid);
        this.FLAG_SYSTEM = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_SYSTEM");
        this.FLAG_UPDATED_SYSTEM_APP = XposedHelpers.getStaticIntField(applicationInfo.getClass(), "FLAG_UPDATED_SYSTEM_APP");
    }


    public boolean isSystem() {
        return (flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;
    }
}
