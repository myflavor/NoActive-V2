package cn.myflv.noactive.utils;

import android.os.Build;

public class VersionUtil {

    public static String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 33:
                return "13";
            case Build.VERSION_CODES.S_V2:
                return "12.1";
            case Build.VERSION_CODES.S:
                return "12";
            case Build.VERSION_CODES.R:
                return "11";
            case Build.VERSION_CODES.Q:
                return "10";
            case Build.VERSION_CODES.P:
                return "9";
            case Build.VERSION_CODES.O_MR1:
                return "8.1";
            case Build.VERSION_CODES.O:
                return "8";
            default:
                return "unknown";
        }
    }
}
