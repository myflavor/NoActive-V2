package cn.myflv.noactive.core.server;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class AnrHelper {
    private final ClassLoader classLoader;
    private final Object anrHelper;
    private final Object anrRecords;

    public AnrHelper(ClassLoader classLoader, Object anrHelper) {
        this.classLoader = classLoader;
        this.anrHelper = anrHelper;
        this.anrRecords = XposedHelpers.getObjectField(anrHelper, FieldEnum.mAnrRecords);
    }


    public void startAnrConsumerIfNeeded() {
        XposedHelpers.callMethod(anrHelper, MethodEnum.startAnrConsumerIfNeeded);
    }

    public void add(Object... args) {
        Class<?> AnrRecord = XposedHelpers.findClass(ClassEnum.AnrRecord, classLoader);
        Object anrRecord = XposedHelpers.newInstance(AnrRecord, args);
        XposedHelpers.callMethod(anrRecords, MethodEnum.add, anrRecord);
    }
}
