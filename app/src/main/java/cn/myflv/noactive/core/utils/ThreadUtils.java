package cn.myflv.noactive.core.utils;

public class ThreadUtils {
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Log.w("Thread sleep failed");
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Log.w("Thread sleep failed");
        }
    }
}
