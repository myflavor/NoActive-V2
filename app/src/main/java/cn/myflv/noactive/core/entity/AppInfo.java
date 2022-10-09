package cn.myflv.noactive.core.entity;

import java.util.HashMap;
import java.util.Map;

import cn.myflv.noactive.core.server.ActivityManagerService;
import lombok.Data;

@Data
public class AppInfo {
    private Integer userId;
    private String packageName;

    private AppInfo(Integer userId, String packageName) {
        this.userId = userId;
        this.packageName = packageName;
    }

    public String getKey() {
        if (userId == ActivityManagerService.MAIN_USER) {
            return packageName;
        }
        return packageName + ":" + userId;
    }

    private final static Map<String, AppInfo> cacheMap = new HashMap<>();

    public static String getKey(Integer userId, String packageName) {
        if (userId == ActivityManagerService.MAIN_USER) {
            return packageName;
        }
        return packageName + ":" + userId;
    }

    public static AppInfo getInstance(Integer userId, String packageName) {
        String key = getKey(userId, packageName);
        AppInfo appInfo = cacheMap.get(key);
        if (appInfo == null) {
            synchronized (cacheMap) {
                appInfo = cacheMap.get(key);
                if (appInfo == null) {
                    appInfo = new AppInfo(userId, packageName);
                    cacheMap.put(key, appInfo);
                }
            }
        }
        return cacheMap.get(key);
    }

    public static AppInfo getInstance(String key) {
        String[] info = key.split(":");
        if (info.length == 1) {
            return AppInfo.getInstance(ActivityManagerService.MAIN_USER, key);
        }
        return AppInfo.getInstance(Integer.valueOf(info[0]), info[1]);
    }
}
