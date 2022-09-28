package cn.myflv.noactive.core.server;


import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.constant.MethodConstants;
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
        this.processList = new ProcessList(XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList));
        this.activeServices = new ActiveServices(XposedHelpers.getObjectField(activityManagerService, FieldConstants.mServices));
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
    }

    public boolean isForegroundApp(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return true;
        }
        int uid = applicationInfo.uid;
        Class<?> clazz = activityManagerService.getClass();
        while (clazz != null && !clazz.getName().equals(Object.class.getName()) && !clazz.getName().equals(ClassConstants.ActivityManagerService)) {
            clazz = clazz.getSuperclass();
        }
        if (clazz == null || !clazz.getName().equals(ClassConstants.ActivityManagerService)) {
            Log.e("super activityManagerService is not found");
            return true;
        }
        try {
            return (boolean) XposedHelpers.findMethodBestMatch(clazz, MethodConstants.isAppForeground, uid).invoke(activityManagerService, uid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.e("call isAppForeground method error");
        }
        return true;
    }


    public boolean isTopApp(String packageName) {
        try {
            ApplicationInfo applicationInfo = getApplicationInfo(packageName);
            if (applicationInfo == null) {
                return true;
            }
            int uid = applicationInfo.uid;
            synchronized (getLock()) {
                Object mProcessList = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList);
                Object mActiveUids = XposedHelpers.getObjectField(mProcessList, FieldConstants.mActiveUids);
                Object uidRec = XposedHelpers.callMethod(mActiveUids, MethodConstants.get, uid);
                if (uidRec == null) {
                    return false;
                }
                boolean idle;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    idle = (boolean) XposedHelpers.callMethod(uidRec, MethodConstants.isIdle);
                } else {
                    idle = XposedHelpers.getBooleanField(uidRec, FieldConstants.idle);
                }
                if (idle) {
                    return false;
                }
                int curProcState = (int) XposedHelpers.callMethod(uidRec, MethodConstants.getCurProcState);
                int PROCESS_STATE_BOUND_TOP = XposedHelpers.getStaticIntField(ActivityManager.class, FieldConstants.PROCESS_STATE_BOUND_TOP);
                return curProcState <= PROCESS_STATE_BOUND_TOP;
            }
        } catch (Throwable throwable) {
            Log.e("isAppTop", throwable);
        }
        return true;
    }

    public Object getLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcLock);
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
        XposedHelpers.callMethod(activityManagerService, MethodConstants.forceStopPackage, packageName, MAIN_USER);
        Log.d(packageName + " was killed");
    }

    public String getNameForUid(int uid) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getNameForUid(uid);
    }

}
