package cn.myflv.noactive.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程工具类.
 */
public class ThreadUtils {

    private final static String LOCK_KEY_PREFIX = "lock:key:";
    // 解冻专用线程池
    private final static ExecutorService thawActionPool = new ThreadPoolExecutor(
            2, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());
    // 其他动作线程池
    private final static ExecutorService otherActionPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());
    // 单线程定时线程池
    private final static ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();
    private final static Map<String, Long> threadTokenMap = new HashMap<>();
    private final static Map<String, Thread> threadMap = new HashMap<>();


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
        otherActionPool.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                printStackTrace(throwable);
            }
        });
    }

    public static void thawThread(String key, Runnable runnable) {
        thawActionPool.execute(() -> {
            synchronized (threadTokenMap) {
                threadTokenMap.remove(key);
            }
            try {
                runWithLock(key, runnable);
            } catch (Throwable throwable) {
                Log.e("thawThread-" + key, throwable);
                printStackTrace(throwable);
            }
        });
    }

    /**
     * 新开线程.
     *
     * @param key      线程Key
     * @param runnable 执行方法
     * @param delay    延迟
     */
    public static void newThread(String key, Runnable runnable, long delay) {
        // 生成Token
        long currentToken = System.currentTimeMillis();
        // 新开线程
        otherActionPool.execute(() -> {
            // 锁线程Map
            synchronized (threadTokenMap) {
                // 存放Token
                threadTokenMap.put(key, currentToken);
            }
            // 延迟
            sleep(delay);
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
            runWithLock(key, runnable);
        });
    }

    /**
     * 锁key执行.
     *
     * @param key      线程Key
     * @param runnable 执行方法
     */
    public static void runWithLock(String key, Runnable runnable) {
        // 锁线程Map
        synchronized (threadMap) {
            // 移除线程并获取被移除的线程
            Thread remove = threadMap.remove(key);
            if (remove != null) {
                // 中断线程
                remove.interrupt();
            }
            // 放入当前线程
            threadMap.put(key, Thread.currentThread());
        }
        // 带锁运行
        synchronized (getLockKey(key)) {
            // 运行
            runnable.run();
        }
        // 锁线程Map
        synchronized (threadMap) {
            // 获取存放的线程
            Thread thread = threadMap.get(key);
            // 如果还是当前线程
            if (Thread.currentThread().equals(thread)) {
                // 执行移除
                threadMap.remove(key);
            }
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
    public static boolean sleep(int ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException ignored) {
            return false;
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
        }
    }


    /**
     * 打印调用堆栈.
     */
    public static void printStackTrace(Throwable throwable) {
        Log.e("---------------> ");
        Log.e(throwable.getMessage());
        StackTraceElement[] stackElements = throwable.getStackTrace();
        for (StackTraceElement element : stackElements) {
            Log.e("at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.e(" <---------------");
    }

    public static void safeRun(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            printStackTrace(throwable);
        }
    }

    public static void runNoThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
        }
    }
}
