package cn.myflv.noactive.core.server;


import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XposedHelpers;
import lombok.Data;

@Data
public class ActivityManagerService {
    public final static int MAIN_USER = 0;
    private static final int STANDBY_BUCKET_RARE = 40;
    private static final int STANDBY_BUCKET_NEVER = 50;
    private final Object activityManagerService;
    private final ProcessList processList;
    private final ActiveServices activeServices;
    private final Context context;

    public ActivityManagerService(Object activityManagerService) {
        this.activityManagerService = activityManagerService;
        this.processList = new ProcessList(XposedHelpers.getObjectField(activityManagerService, FieldEnum.mProcessList));
        this.activeServices = new ActiveServices(XposedHelpers.getObjectField(activityManagerService, FieldEnum.mServices));
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldEnum.mContext);
    }

    public boolean isAppForeground(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return true;
        }
        int uid = applicationInfo.uid;
        Class<?> clazz = activityManagerService.getClass();
        while (clazz != null && !clazz.getName().equals(Object.class.getName()) && !clazz.getName().equals(ClassEnum.ActivityManagerService)) {
            clazz = clazz.getSuperclass();
        }
        if (clazz == null || !clazz.getName().equals(ClassEnum.ActivityManagerService)) {
            Log.e("super activityManagerService is not found");
            return true;
        }
        try {
            return (boolean) XposedHelpers.findMethodBestMatch(clazz, MethodEnum.isAppForeground, uid).invoke(activityManagerService, uid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.e("call isAppForeground method error");
        }
        return true;
    }


    public boolean isAppTop(String packageName) {
        try {
            ApplicationInfo applicationInfo = getApplicationInfo(packageName);
            if (applicationInfo == null) {
                return true;
            }
            int uid = applicationInfo.uid;
            synchronized (getLock()) {
                Object mProcessList = XposedHelpers.getObjectField(activityManagerService, FieldEnum.mProcessList);
                Object mActiveUids = XposedHelpers.getObjectField(mProcessList, FieldEnum.mActiveUids);
                Object uidRec = XposedHelpers.callMethod(mActiveUids, MethodEnum.get, uid);
                if (uidRec == null) {
                    return false;
                }
                boolean idle;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    idle = (boolean) XposedHelpers.callMethod(uidRec, MethodEnum.isIdle);
                } else {
                    idle = XposedHelpers.getBooleanField(uidRec, FieldEnum.idle);
                }
                if (idle) {
                    return false;
                }
                int curProcState = (int) XposedHelpers.callMethod(uidRec, MethodEnum.getCurProcState);
                int PROCESS_STATE_BOUND_TOP = XposedHelpers.getStaticIntField(ActivityManager.class, FieldEnum.PROCESS_STATE_BOUND_TOP);
                return curProcState <= PROCESS_STATE_BOUND_TOP;
            }
        } catch (Throwable throwable) {
            Log.e("isAppTop", throwable);
        }
        return true;
    }

    public Object getLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return XposedHelpers.getObjectField(activityManagerService, FieldEnum.mProcLock);
        } else {
            return activityManagerService;
        }
    }


    public boolean isSystem(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return true;
        }
        return (applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    public boolean isImportantSystemApp(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return true;
        }
        return applicationInfo.uid < 10000;
    }

    public ApplicationInfo getApplicationInfo(String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(packageName + " not found");
        }
        return null;
    }

    public void killApp(String packageName) {
        XposedHelpers.callMethod(activityManagerService, MethodEnum.forceStopPackage, packageName, MAIN_USER);
        Log.d(packageName + " was killed");
    }

    public String getNameForUid(int uid) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getNameForUid(uid);
    }

}
