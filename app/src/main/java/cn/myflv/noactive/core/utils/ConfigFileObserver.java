package cn.myflv.noactive.core.utils;

import android.os.FileObserver;

import java.util.HashSet;
import java.util.Set;

import cn.myflv.noactive.core.entity.MemData;

public class ConfigFileObserver extends FileObserver {
    private final MemData memData;

    public ConfigFileObserver(MemData memData) {
        super(FreezerConfig.ConfigDir);
        this.memData = memData;
        FreezerConfig.checkAndInit();
        FreezerConfig.cleanLog();
        reload();
    }

    @Override
    public void startWatching() {
        super.startWatching();
        for (String file : FreezerConfig.listenConfig) {
            Log.d("Start monitor " + file);
        }
    }

    @Override
    public void onEvent(int event, String path) {
        int e = event & ALL_EVENTS;
        switch (e) {
            case DELETE:
            case DELETE_SELF:
                FreezerConfig.checkAndInit();
                break;
            case MODIFY:
            case MOVE_SELF:
                ThreadUtils.sleep(2000);
                reload();
        }
    }

    public void reload() {
        for (String file : FreezerConfig.listenConfig) {
            Log.d("Reload " + file);
            Set<String> newConfig = new HashSet<>(FreezerConfig.get(file));
            newConfig.forEach(config -> Log.d(file.replace(".conf", "") + " " + config));
            switch (file) {
                case FreezerConfig.whiteAppConfig:
                    memData.setWhiteApps(newConfig);
                    break;
                case FreezerConfig.whiteProcessConfig:
                    memData.setWhiteProcessList(newConfig);
                    break;
                case FreezerConfig.killProcessConfig:
                    memData.setKillProcessList(newConfig);
                    break;
                case FreezerConfig.blackSystemAppConfig:
                    memData.setBlackSystemApps(newConfig);
                    break;
                case FreezerConfig.directAppConfig:
                    memData.setDirectApps(newConfig);
                    break;
                case FreezerConfig.socketAppConfig:
                    memData.setSocketApps(newConfig);
                    break;
            }
        }
    }
}
