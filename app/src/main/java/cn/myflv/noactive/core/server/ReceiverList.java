package cn.myflv.noactive.core.server;

import cn.myflv.noactive.core.entity.FieldEnum;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class ReceiverList {
    private final Object receiverList;
    private ProcessRecord processRecord;

    public ReceiverList(Object receiverList) {
        this.receiverList = receiverList;
        try {
            this.processRecord = new ProcessRecord(XposedHelpers.getObjectField(receiverList, FieldEnum.app));
        } catch (Exception ignored) {
        }
    }


    public void clear() {
        XposedHelpers.setObjectField(receiverList, FieldEnum.app, null);
    }


}
