package cn.myflv.noactive.core.utils;

import java.util.HashMap;
import java.util.Map;
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
    private final static Map<String, Long> threadTokenMap = new HashMap<>();


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
        cachedThreadPool.execute(runnable);
    }

    /**
     * 新开线程.
     *
     * @param key      线程Key
     * @param runnable 执行方法
     * @param delay    延迟
     */
    public synchronized static void newThread(String key, Runnable runnable, long delay) {
        // 生成Token
        long currentToken = System.currentTimeMillis();
        // 新开线程
        newThread(() -> {
            // 锁线程Map
            synchronized (threadTokenMap) {
                threadTokenMap.put(key, currentToken);
            }
            // 延迟
            sleep(delay);
            // 锁线程Map
            synchronized (threadTokenMap) {
                // 获取Token
                Long token = threadTokenMap.get(key);
                // 比较Token
                if (token != null && !token.equals(currentToken)) {
                    return;
                }
                cachedThreadPool.submit(runnable);
            }
        });
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
