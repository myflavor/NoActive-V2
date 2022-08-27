//
// Decompiled by Jadx - 917ms
//
package cn.myflv.noactive.core.utils;

import android.os.Process;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.ProcessRecord;
import de.robv.android.xposed.XposedHelpers;

public class FreezeUtils {

    private final static int CONT = 18;

    private static final int FREEZE_ACTION = 1;
    private static final int UNFREEZE_ACTION = 0;

    private static final String V1_FREEZER_FROZEN_PORCS = "/sys/fs/cgroup/freezer/perf/frozen/cgroup.procs";
    private static final String V1_FREEZER_THAWED_PORCS = "/sys/fs/cgroup/freezer/perf/thawed/cgroup.procs";

    private final boolean freezerApi;
    private final int freezerVersion;
    private final int stopSignal;
    private final boolean useKill;
    private final ClassLoader classLoader;


    public FreezeUtils(ClassLoader classLoader) {
        this.classLoader = classLoader;
        String freezerVersion = FreezerConfig.getFreezerVersion(classLoader);
        switch (freezerVersion) {
            case FreezerConfig.API:
                this.freezerApi = true;
                this.freezerVersion = 2;
                break;
            case FreezerConfig.V2:
                this.freezerApi = false;
                this.freezerVersion = 2;
                break;
            case FreezerConfig.V1:
            default:
                this.freezerApi = false;
                this.freezerVersion = 1;
        }
        this.stopSignal = FreezerConfig.getKillSignal();
        this.useKill = FreezerConfig.isUseKill();
        if (useKill) {
            Log.i("Kill -" + stopSignal);
        } else {
            Log.i("Freezer " + freezerVersion);
        }
    }


    public static List<Integer> getFrozenPids() {
        List<Integer> pids = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(V1_FREEZER_FROZEN_PORCS));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                try {
                    pids.add(Integer.parseInt(line));
                } catch (NumberFormatException ignored) {
                }
            }
            reader.close();
        } catch (IOException ignored) {
        }
        return pids;
    }

    public void freezer(ProcessRecord processRecord) {
        if (useKill) {
            Process.sendSignal(processRecord.getPid(), stopSignal);
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord.getPid(), processRecord.getUid(), true);
                } else {
                    freezePid(processRecord.getPid(), processRecord.getUid());
                }
            } else {
                if (processRecord.isSandboxProcess()) {
                    return;
                }
                freezePid(processRecord.getPid());
            }
        }
        Log.d(processRecord.getProcessName() + " freezer");
    }


    public void unFreezer(List<ProcessRecord> processRecords) {
        if (processRecords.isEmpty()) {
            return;
        }
        for (ProcessRecord processRecord : processRecords) {
            unFreezer(processRecord);
        }
    }

    public void unFreezer(ProcessRecord processRecord) {
        if (useKill) {
            Process.sendSignal(processRecord.getPid(), CONT);
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord.getPid(), processRecord.getUid(), false);
                } else {
                    thawPid(processRecord.getPid(), processRecord.getUid());
                }
            } else {
                if (processRecord.isSandboxProcess()) {
                    return;
                }
                thawPid(processRecord.getPid());
            }
        }
        Log.d(processRecord.getProcessName() + " unFreezer");
    }

    public static boolean isFrozonPid(int pid) {
        return getFrozenPids().contains(pid);
    }


    public static void freezePid(int pid) {
        writeNode(V1_FREEZER_FROZEN_PORCS, pid);
    }


    public static void thawPid(int pid) {
        writeNode(V1_FREEZER_THAWED_PORCS, pid);
    }


    private static void writeNode(String path, int val) {
        try {
            PrintWriter writer = new PrintWriter(path);
            writer.write(Integer.toString(val));
            writer.close();
        } catch (FileNotFoundException e) {
            Log.e("Freezer V1 not supported");
        } catch (Exception e) {
            Log.e("Freezer V1 failed: " + e.getMessage());
        }
    }


    private static void setFreezeAction(int pid, int uid, boolean action) {
        String path = "/sys/fs/cgroup/uid_" + uid + "/pid_" + pid + "/cgroup.freeze";
        try {
            PrintWriter writer = new PrintWriter(path);
            if (action) {
                writer.write(Integer.toString(FREEZE_ACTION));
            } else {
                writer.write(Integer.toString(UNFREEZE_ACTION));
            }
            writer.close();
        } catch (FileNotFoundException e) {
            Log.e("Freezer V2 not supported");
        } catch (Exception e) {
            Log.e("Freezer V2 failed: " + e.getMessage());
        }
    }

    public static void thawPid(int pid, int uid) {
        setFreezeAction(pid, uid, false);
    }


    public static void freezePid(int pid, int uid) {
        setFreezeAction(pid, uid, true);
    }

    public static void kill(ProcessRecord processRecord) {
        Process.killProcess(processRecord.getPid());
        Log.d(processRecord.getProcessName() + " kill");
    }

    public void setProcessFrozen(int pid, int uid, boolean frozen) {
        Class<?> Process = XposedHelpers.findClass(ClassEnum.Process, classLoader);
        XposedHelpers.callStaticMethod(Process, MethodEnum.setProcessFrozen, pid, uid, frozen);
    }

}
