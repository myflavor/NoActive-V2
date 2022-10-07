package cn.myflv.noactive.core.entity;

import cn.myflv.noactive.core.server.ActivityManagerService;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppInfo {
    private Integer userId;
    private String packageName;


    public String getKey() {
        if (userId == ActivityManagerService.MAIN_USER) {
            return packageName;
        }
        return packageName + ":" + userId;
    }

    public static AppInfo getInstance(String key) {
        String[] info = key.split(":");
        if (info.length == 1) {
            return new AppInfo(ActivityManagerService.MAIN_USER, key);
        }
        return new AppInfo(Integer.valueOf(info[0]), info[1]);
    }
}
