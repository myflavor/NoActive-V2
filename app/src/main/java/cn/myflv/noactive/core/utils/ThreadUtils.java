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

    public final static long NO_DELAY = 0L;

    private final static String LOCK_KEY_PREFIX = "lock:key:";
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
                // 存放Token
                threadTokenMap.put(key, currentToken);
            }
            if (delay == NO_DELAY) {
                newThread(() -> runWirthLock(key, runnable));
                return;
            } else {
                // 延迟
                sleep(delay);
            }
            // 锁线程Map
            synchronized (threadTokenMap) {
                // 获取Token
                Long token = threadTokenMap.get(key);
                // 校验Token
                if (token != null && !token.equals(currentToken)) {
                    Log.d(key + " thread updated");
                    return;
                }
            }
            newThread(() -> runWirthLock(key, runnable));
        });
    }

    /**
     * 无延迟新开线程执行方法.
     *
     * @param key      线程Key
     * @param runnable 执行方法
     */
    public static void newThread(String key, Runnable runnable) {
        newThread(key, runnable, NO_DELAY);
    }

    /**
     * 锁key执行.
     *
     * @param key      线程Key
     * @param runnable 执行方法
     */
    public static void runWirthLock(String key, Runnable runnable) {
        synchronized (getLockKey(key)) {
            runnable.run();
        }
    }

    /**
     * 获取锁Key.
     *
     * @param key 线程Key
     * @return 锁Key
     */
    public static String getLockKey(String key) {
        return (LOCK_KEY_PREFIX + key).intern();
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


    /**
     * 打印调用堆栈.
     */
    public static void printStackTrace(Throwable throwable) {
        Log.e("---------------> ");
        StackTraceElement[] stackElements = throwable.getStackTrace();
        for (StackTraceElement element : stackElements) {
            Log.e("at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.e(" <---------------");
    }
}
