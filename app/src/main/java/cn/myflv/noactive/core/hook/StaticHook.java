package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;

public class StaticHook {
    public static void disableMillet(ClassLoader classLoader) {
        try {
            Class<?> GreezeManagerService = XposedHelpers.findClassIfExists(ClassEnum.GreezeManagerService, classLoader);
            if (GreezeManagerService == null) {
                return;
            }
            XposedHelpers.setStaticBooleanField(GreezeManagerService, FieldEnum.sEnable, false);
            Log.i("Disable Millet");
        } catch (Throwable ignored) {

        }
    }
}
