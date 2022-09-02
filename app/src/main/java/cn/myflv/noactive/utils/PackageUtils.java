package cn.myflv.noactive.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.myflv.noactive.core.utils.FreezerConfig;
import cn.myflv.noactive.entity.AppInfo;
import cn.myflv.noactive.entity.AppItem;

public class PackageUtils {

    private final static String TAG = "NoActive";

    public static List<AppItem> filter(Context context, int type, String text) {
        Set<String> blackApps = ConfigUtils.get(FreezerConfig.blackSystemAppConfig);
        Set<String> whiteApps = ConfigUtils.get(FreezerConfig.whiteAppConfig);
        List<AppItem> appItemList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES);
        for (PackageInfo installedPackage : installedPackages) {
            ApplicationInfo applicationInfo = installedPackage.applicationInfo;
            if (applicationInfo.uid < 10000) {
                continue;
            }
            boolean isSystem = (applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            if ((type == 1 && isSystem) || (type == 2 && !isSystem)) {
                continue;
            }
            ActivityInfo[] activities = installedPackage.activities;
            if (activities == null || activities.length <= 0) {
                continue;
            }
            String appName = applicationInfo.loadLabel(packageManager).toString().trim();
            if (text != null && !text.equals("") && !appName.toLowerCase().contains(text.toLowerCase())) {
                continue;
            }
            String packageName = installedPackage.packageName;
            Drawable appIcon = applicationInfo.loadIcon(packageManager);
            boolean isWhite = whiteApps.contains(packageName);
            boolean isBlack = blackApps.contains(packageName);
            AppItem appItem = new AppItem(appName, packageName, appIcon, installedPackage, isWhite, isBlack);
            appItemList.add(appItem);
        }
        if (appItemList.size() > 0) {
            appItemList = appItemList.stream()
                    .sorted(Comparator.comparing(AppItem::isWhite).thenComparing(AppItem::isBlack).reversed())
                    .collect(Collectors.toList());
        }
        return appItemList;
    }

    public static AppInfo getProcessSet(Context context, String packageName) {
        AppInfo appInfo = new AppInfo();
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS);
            Set<String> processSet = new LinkedHashSet<>();
            List<ComponentInfo> componentInfoList = new ArrayList<>();
            if (packageInfo.activities != null) {
                componentInfoList.addAll(Arrays.asList(packageInfo.activities));
            }
            if (packageInfo.services != null) {
                componentInfoList.addAll(Arrays.asList(packageInfo.services));
            }
            if (packageInfo.receivers != null) {
                componentInfoList.addAll(Arrays.asList(packageInfo.receivers));
            }
            if (packageInfo.providers != null) {
                componentInfoList.addAll(Arrays.asList(packageInfo.providers));
            }
            for (ComponentInfo componentInfo : componentInfoList) {
                String processName = componentInfo.processName;
                if (processName != null) {
                    processSet.add(processName);
                }
            }
            appInfo.setProcessSet(processSet);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            boolean isSystem = (applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            appInfo.setSystem(isSystem);
            if (isSystem) {
                Set<String> blackSystemAppConfig = ConfigUtils.get(FreezerConfig.blackSystemAppConfig);
                appInfo.setBlack(blackSystemAppConfig.contains(packageName));
            }
            Set<String> whiteAppConfig = ConfigUtils.get(FreezerConfig.whiteAppConfig);
            Set<String> directAppConfig = ConfigUtils.get(FreezerConfig.directAppConfig);
            appInfo.setWhite(whiteAppConfig.contains(packageName));
            appInfo.setDirect(directAppConfig.contains(packageName));
            Set<String> whiteProcessConfig = ConfigUtils.get(FreezerConfig.whiteProcessConfig);
            appInfo.setWhiteProcessSet(whiteProcessConfig);
            Set<String> killProcessConfig = ConfigUtils.get(FreezerConfig.killProcessConfig);
            appInfo.setKillProcessSet(killProcessConfig);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appInfo;

    }


}
