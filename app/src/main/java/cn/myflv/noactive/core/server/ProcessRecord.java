package cn.myflv.noactive.core.server;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.utils.PackageUtils;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class ProcessRecord {
    private final int uid;
    private final int pid;
    private final String processName;
    private final int userId;
    private final ApplicationInfo applicationInfo;
    private final String packageName;
    private Object processRecord;


    public ProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.pid = XposedHelpers.getIntField(processRecord, FieldConstants.mPid);
        } else {
            this.pid = XposedHelpers.getIntField(processRecord, FieldConstants.pid);
        }
        this.uid = XposedHelpers.getIntField(processRecord, FieldConstants.uid);

        this.userId = XposedHelpers.getIntField(processRecord, FieldConstants.userId);
        this.applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(processRecord, FieldConstants.info);
        this.packageName = applicationInfo.packageName;
        String processName = (String) XposedHelpers.getObjectField(processRecord, FieldConstants.processName);
        this.processName = PackageUtils.absoluteProcessName(packageName, processName);
    }

    public void setCurAdj(int curAdj) {
        XposedHelpers.setIntField(processRecord, FieldConstants.curAdj, curAdj);
    }

    public boolean isSandboxProcess() {
        return uid >= 99000;
    }

    public boolean isMainProcess() {
        return processName.equals(packageName);
    }

    public String getProcessNameWithUser() {
        if (userId == 0) {
            return processName;
        } else {
            return processName + ":" + userId;
        }
    }

    public String getPackageNameWithUser() {
        if (userId == 0) {
            return packageName;
        } else {
            return packageName + ":" + userId;
        }
    }
}
