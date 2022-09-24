package cn.myflv.noactive.core.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.myflv.noactive.utils.VersionUtil;
import de.robv.android.xposed.XposedBridge;

public class Log {
    public final static String TAG = "NoActive";
    public static final boolean isDebug;
    public final static String ERROR = "error";
    public final static String WARN = "warn";
    public final static String INFO = "info";
    public final static String DEBUG = "debug";

    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private final static ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final static File currentLog = new File(FreezerConfig.LogDir, FreezerConfig.currentLog);


    static {
        File config = new File(FreezerConfig.ConfigDir, "debug");
        isDebug = config.exists();
        i("Debug " + (isDebug ? "on" : "off"));
        i("Android " + VersionUtil.getAndroidVersion());
    }

    public static void d(String msg) {
        if (isDebug) {
            unify(DEBUG, msg);
        }
    }

    public static void i(boolean condition, String msg) {
        if (condition) {
            i(msg);
        }
    }

    public static void i(String msg) {
        unify(INFO, msg);
    }

    public static void w(String msg) {
        unify(WARN, msg);
    }

    public static void e(String msg) {
        unify(ERROR, msg);
    }


    public static void e(String msg, Throwable throwable) {
        unify(ERROR, msg + " failed : " + throwable.getMessage());
    }

    public static void unify(String level, String msg) {
        executorService.submit(() -> {
//            xposedLog(TAG + "(" + level + ") -> " + msg);
            fileLog(simpleDateFormat.format(new Date()) + " " + level.toUpperCase() + " -> " + msg);
        });
    }

    public static void xposedLog(String msg) {
        XposedBridge.log(msg);
    }

    public static void fileLog(String msg) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentLog, true)));
            bufferedWriter.write(msg + "\r\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            xposedLog("Log write failed: " + e.getMessage());
        }
    }
}
