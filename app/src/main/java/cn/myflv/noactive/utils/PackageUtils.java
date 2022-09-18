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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.myflv.noactive.core.utils.FreezerConfig;
import cn.myflv.noactive.entity.AppInfo;
import cn.myflv.noactive.entity.AppItem;

public class PackageUtils {

    private final static String TAG = "NoActive";

    public static Drawable getAlipayLogo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo("com.eg.android.AlipayGphone", PackageManager.MATCH_UNINSTALLED_PACKAGES);
            return applicationInfo.loadIcon(packageManager);
        } catch (Exception ignored) {
        }

        return null;
    }

    public static List<AppItem> filter(Context context, int type, String text) {
        Set<String> blackAppSet = ConfigUtils.get(FreezerConfig.blackSystemAppConfig);
        Set<String> whiteAppSet = ConfigUtils.get(FreezerConfig.whiteAppConfig);
        Set<String> directAppSet = ConfigUtils.get(FreezerConfig.directAppConfig);
        Set<String> socketAppSet = ConfigUtils.get(FreezerConfig.socketAppConfig);
        Set<String> killProcessSet = ConfigUtils.get(FreezerConfig.killProcessConfig);
        Set<String> whiteProcessSet = ConfigUtils.get(FreezerConfig.whiteProcessConfig);
        Map<String, List<String>> killProcessMap = killProcessSet.stream().collect(Collectors.groupingBy(proc -> proc.split(":")[0]));
        Map<String, List<String>> whiteProcessMap = whiteProcessSet.stream().collect(Collectors.groupingBy(proc -> proc.split(":")[0]));
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
            boolean isWhite = whiteAppSet.contains(packageName);
            boolean isBlack = blackAppSet.contains(packageName);
            boolean isDirect = directAppSet.contains(packageName);
            boolean isSocket = socketAppSet.contains(packageName);
            int killProcCount = killProcessMap.computeIfAbsent(packageName, k -> new ArrayList<>()).size();
            int whiteProcCount = whiteProcessMap.computeIfAbsent(packageName, k -> new ArrayList<>()).size();
            AppItem appItem = new AppItem(appName, packageName, appIcon, installedPackage, isWhite, isBlack, isDirect, isSocket, killProcCount, whiteProcCount);
            appItemList.add(appItem);
        }
        if (appItemList.size() > 0) {
            appItemList = appItemList.stream()
                    .sorted(Comparator.comparing(AppItem::isWhite).thenComparing(AppItem::isDirect).thenComparing(AppItem::isBlack).thenComparing(AppItem::isSocket).thenComparing(AppItem::getWhiteProcCount).thenComparing(AppItem::getKillProcCount).reversed())
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
                    processSet.add(absoluteProcessName(packageName, processName));
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
            Set<String> socketAppConfig = ConfigUtils.get(FreezerConfig.socketAppConfig);
            appInfo.setWhite(whiteAppConfig.contains(packageName));
            appInfo.setDirect(directAppConfig.contains(packageName));
            appInfo.setSocket(socketAppConfig.contains(packageName));
            Set<String> whiteProcessConfig = ConfigUtils.get(FreezerConfig.whiteProcessConfig);
            appInfo.setWhiteProcessSet(whiteProcessConfig);
            Set<String> killProcessConfig = ConfigUtils.get(FreezerConfig.killProcessConfig);
            appInfo.setKillProcessSet(killProcessConfig);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appInfo;

    }

    /**
     * 绝对进程名
     *
     * @param packageName 包名
     * @param processName 进程名
     */
    public static String absoluteProcessName(String packageName, String processName) {
        // 相对进程名
        if (processName.startsWith(".")) {
            // 拼成绝对进程名
            return packageName + ":" + processName.substring(1);
        } else {
            // 是绝对进程直接返回
            return processName;
        }
    }


}
