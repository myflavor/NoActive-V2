package cn.myflv.noactive.core.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 线程工具类.
 */
public class ThreadUtils {

    // 缓存线程池
    private final static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    // 单线程定时线程池
    private final static ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();

    /**
     * 间隔定时执行.
     *
     * @param runnable 执行内容
     * @param minute   间隔分钟
     */
    public static void scheduleInterval(Runnable runnable, int minute) {
        scheduledThreadPool.scheduleWithFixedDelay(runnable, 0, minute, TimeUnit.MINUTES);
    }

    /**
     * 新线程执行.
     *
     * @param runnable 执行内容
     */
    public static void newThread(Runnable runnable) {
        cachedThreadPool.submit(runnable);
    }

    public static String getAppLock(String packageName) {
        return ("lock:app:" + packageName).intern();
    }

    /**
     * 延迟.
     *
     * @param ms 毫秒
     */
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Log.w("Thread sleep failed");
        }
    }

    /**
     * 延迟.
     *
     * @param ms 毫秒
     */
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Log.w("Thread sleep failed");
        }
    }
}
