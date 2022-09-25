package cn.myflv.noactive.utils;

import android.os.Process;
import android.util.Log;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


public class BaseFreezeUtils {

    public final static int SIG_CONT = 18;
    public final static int SIG_STOP = 19;
    public final static int SIG_TSTP = 20;
    public final static int SIG_KILL = 9;
    private final static String TAG = "NoActive";
    private static final int FREEZE_ACTION = 1;
    private static final int UNFREEZE_ACTION = 0;

    private static final String V1_FREEZER_FROZEN_PORCS = "/sys/fs/cgroup/freezer/perf/frozen/cgroup.procs";
    private static final String V1_FREEZER_THAWED_PORCS = "/sys/fs/cgroup/freezer/perf/thawed/cgroup.procs";

    private static final String UID_ROOT = "/uid_0";
    private static final String UID_SYSTEM = "/uid_1000";
    private static final String FROZEN_PATH = "/frozen/cgroup.procs";
    private static final String UNFROZEN_PATH = "/unfrozen/cgroup.procs";
    private static final String[] FREEZER_PATH_ENUM = {"/sys/fs/cgroup", "/dev/freezer", "/dev/cg2_bpf"};
    private static Boolean commonV2 = null;
    private static String freezerPath = null;

    private synchronized static boolean isCommonV2(boolean su) {
        if (commonV2 != null) {
            return commonV2;
        }
        for (String path : FREEZER_PATH_ENUM) {
            if (pathExist(su, path + UID_ROOT) || pathExist(su, path + UID_SYSTEM)) {
                commonV2 = true;
                freezerPath = path;
                return commonV2;
            }
            if (pathExist(su, path + FROZEN_PATH) && pathExist(su, path + UNFROZEN_PATH)) {
                commonV2 = false;
                freezerPath = path;
                return commonV2;
            }
        }
        commonV2 = true;
        freezerPath = FREEZER_PATH_ENUM[0];
        return false;
    }

    private static boolean pathExist(boolean su, String path) {
        if (su) {
            return SuFile.open(path).exists();
        } else {
            return new File(path).exists();
        }
    }

    private static boolean writeNode(boolean su, String path, int val) {
        try {
            PrintWriter writer = getWriter(su, path);
            writer.write(Integer.toString(val));
            writer.close();
            return true;
        } catch (Exception e) {
            if (val == FREEZE_ACTION) {
                Log.e(TAG, "Freezer V1 failed: " + e.getMessage());
            }
        }
        return false;
    }

    public static boolean freezePid(boolean su, int pid) {
        return writeNode(su, V1_FREEZER_FROZEN_PORCS, pid);
    }

    public static boolean thawPid(boolean su, int pid) {
        return writeNode(su, V1_FREEZER_THAWED_PORCS, pid);
    }

    private static boolean setFreezeAction(boolean su, int pid, int uid, boolean action) {
        String path = freezerPath + "/uid_" + uid + "/pid_" + pid + "/cgroup.freeze";
        try {
            PrintWriter writer = getWriter(su, path);
            if (action) {
                writer.write(Integer.toString(FREEZE_ACTION));
            } else {
                writer.write(Integer.toString(UNFREEZE_ACTION));
            }
            writer.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Freezer V2 failed: " + e.getMessage());
        }
        return false;
    }

    public static boolean thawPid(boolean su, int pid, int uid) {
        if (isCommonV2(su)) {
            return setFreezeAction(su, pid, uid, false);
        }
        return writeNode(su, freezerPath + UNFROZEN_PATH, pid);
    }

    public static boolean freezePid(boolean su, int pid, int uid) {
        if (isCommonV2(su)) {
            return setFreezeAction(su, pid, uid, true);
        }
        return writeNode(su, freezerPath + FROZEN_PATH, pid);
    }

    public static boolean kill(boolean su, int sig, int pid) {
        try {
            if (su) {
                Shell.cmd("kill -" + sig + " " + pid).exec();
            } else {
                Process.sendSignal(pid, sig);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Kill " + sig + " failed: " + e.getMessage());
        }
        return false;
    }

    public static PrintWriter getWriter(boolean su, String path) throws FileNotFoundException {
        if (su) {
            return new PrintWriter(SuFileOutputStream.open(path));
        } else {
            return new PrintWriter(path);
        }
    }
}
