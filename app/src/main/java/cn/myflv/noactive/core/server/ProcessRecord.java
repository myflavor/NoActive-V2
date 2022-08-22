package cn.myflv.noactive.core.server;

import android.os.Build;

import cn.myflv.noactive.core.entity.FieldEnum;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class ProcessRecord {
    private final int uid;
    private final int pid;
    private final String processName;
    private final int userId;
    private final ApplicationInfo applicationInfo;
    private Object processRecord;


    public ProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.pid = XposedHelpers.getIntField(processRecord, FieldEnum.mPid);
        } else {
            this.pid = XposedHelpers.getIntField(processRecord, FieldEnum.pid);
        }
        this.uid = XposedHelpers.getIntField(processRecord, FieldEnum.uid);
        this.processName = (String) XposedHelpers.getObjectField(processRecord, FieldEnum.processName);
        this.userId = XposedHelpers.getIntField(processRecord, FieldEnum.userId);
        this.applicationInfo = new ApplicationInfo(XposedHelpers.getObjectField(processRecord, FieldEnum.info));
    }

    public void setCurAdj(int curAdj) {
        XposedHelpers.setIntField(processRecord, FieldEnum.curAdj, curAdj);
    }

    public boolean isSandboxProcess() {
        return uid >= 99000;
    }

}
