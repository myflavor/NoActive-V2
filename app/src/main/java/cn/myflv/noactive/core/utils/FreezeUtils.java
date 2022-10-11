package cn.myflv.noactive.core.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.List;

import cn.myflv.noactive.FreezerInterface;
import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.error.FreezeFailedException;
import cn.myflv.noactive.core.error.UnKnowException;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.utils.BaseFreezeUtils;
import de.robv.android.xposed.XposedHelpers;

public class FreezeUtils {
    private final static int BINDER_FREEZE_TRY = 3;
    private final boolean freezerApi;
    private final int freezerVersion;
    private final int stopSignal;
    private final boolean useKill;
    private final ClassLoader classLoader;
    private final MemData memData;
    private final boolean suExecute;
    private FreezerInterface freezerInterface = null;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            freezerInterface = FreezerInterface.Stub.asInterface(service);
            Log.i("su connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            freezerInterface = null;
            Log.w("su disconnected");
        }
    };

    public FreezeUtils(ClassLoader classLoader, MemData memData) {
        this.classLoader = classLoader;
        this.memData = memData;
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
        boolean useKill = FreezerConfig.isUseKill();
        if (!useKill && FreezerConfig.V1.equals(freezerVersion)) {
            this.useKill = !FreezerConfig.isXiaoMiV1(classLoader) && !FreezerConfig.isConfigOn(FreezerConfig.freezerV1);
        } else {
            this.useKill = useKill;
        }
        this.stopSignal = FreezerConfig.getKillSignal();
        if (this.useKill) {
            Log.i("Kill -" + stopSignal);
        } else {
            Log.i("Freezer " + freezerVersion);
        }
        this.suExecute = FreezerConfig.isConfigOn(FreezerConfig.SuExcute);
        if (suExecute) {
            Log.i("Su Execute");
        }
    }

    public boolean isUseV1() {
        return !useKill && freezerVersion == 1;
    }

    public void kill(List<ProcessRecord> processRecords) {
        if (processRecords == null || processRecords.isEmpty()) {
            return;
        }
        for (ProcessRecord processRecord : processRecords) {
            kill(processRecord);
        }
    }

    public void kill(ProcessRecord processRecord) {
        execute(() -> kill(BaseFreezeUtils.SIG_KILL, processRecord));
    }

    public void freezer(ProcessRecord processRecord) {
        if (useKill) {
            execute(() -> kill(stopSignal, processRecord));
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord, true);
                } else {
                    execute(() -> freezeV2(processRecord));
                }
            } else {
                execute(() -> freezeV1(processRecord));
            }
            // freezeBinder(processRecord, true);
        }
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
            execute(() -> kill(BaseFreezeUtils.SIG_CONT, processRecord));
        } else {
            if (freezerVersion == 2) {
                if (freezerApi) {
                    setProcessFrozen(processRecord, false);
                } else {
                    execute(() -> thawV2(processRecord));
                }
            } else {
                execute(() -> thawV1(processRecord));
            }
            // freezeBinder(processRecord, false);
        }
    }

    public void setProcessFrozen(ProcessRecord processRecord, boolean frozen) {
        int pid = processRecord.getPid();
        int uid = processRecord.getUid();
        ThreadUtils.runNoThrow(() -> {
            Class<?> Process = XposedHelpers.findClass(ClassConstants.Process, classLoader);
            XposedHelpers.callStaticMethod(Process, MethodConstants.setProcessFrozen, pid, uid, frozen);
            Log.d((frozen ? "freeze" : "thaw") + " " + processRecord.getProcessNameWithUser());
        });

    }


    public void freezeBinder(ProcessRecord processRecord, boolean frozen) {
        int pid = processRecord.getPid();
        ThreadUtils.runNoThrow(() -> {
            Class<?> CachedAppOptimizer = XposedHelpers.findClass(ClassConstants.CachedAppOptimizer, classLoader);
            for (int i = 0; i < BINDER_FREEZE_TRY; i++) {
                int result = (int) XposedHelpers.callStaticMethod(CachedAppOptimizer, MethodConstants.freezeBinder, pid, frozen);
                if (result == 0) {
                    Log.d((frozen ? "freeze" : "thaw") + " binder " + processRecord.getProcessNameWithUser());
                    return;
                }
            }
        });
    }

    public synchronized void connectIfNeed() {
        if (freezerInterface != null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setPackage("cn.myflv.noactive");
            intent.setAction("cn.myflv.noactive.action.FREEZE");
            if (memData.getActivityManagerService().getContext() == null) {
                return;
            }
            memData.getActivityManagerService().getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Throwable throwable) {
            Log.e("su connect", throwable);
        }
    }

    public void execute(Runnable runnable) {
        if (suExecute) {
            connectIfNeed();
        }
        boolean su = suExecute && freezerInterface != null;
        if (su || !suExecute) {
            runnable.run();
        }
    }


    public void thawV2(ProcessRecord processRecord) {
        try {
            boolean result;
            if (suExecute) {
                result = freezerInterface.thawV2(processRecord.getPid(), processRecord.getUid());
            } else {
                result = BaseFreezeUtils.thawPid(false, processRecord.getPid(), processRecord.getUid());
            }
            if (result) {
                Log.d("thaw " + processRecord.getProcessNameWithUser());
            } else {
                throw new Exception("process died or not supported");
            }
        } catch (Throwable throwable) {
            Log.e("thawV2", throwable);
        }
    }

    public void freezeV2(ProcessRecord processRecord) {
        try {
            boolean result;
            if (suExecute) {
                result = freezerInterface.freezeV2(processRecord.getPid(), processRecord.getUid());
            } else {
                result = BaseFreezeUtils.freezePid(false, processRecord.getPid(), processRecord.getUid());
            }
            if (result) {
                Log.d("freeze " + processRecord.getProcessNameWithUser());
            } else {
                throw new FreezeFailedException();
            }
        } catch (Throwable throwable) {
            Log.e("freezeV2", throwable);
        }
    }

    public void thawV1(ProcessRecord processRecord) {
        try {
            boolean result;
            if (suExecute) {
                result = freezerInterface.thawV1(processRecord.getPid());
            } else {
                result = BaseFreezeUtils.thawPid(false, processRecord.getPid());
            }
            if (result) {
                Log.d("thaw " + processRecord.getProcessNameWithUser());
            } else {
                throw new FreezeFailedException();
            }
        } catch (Throwable throwable) {
            Log.e("thawV1", throwable);
        }
    }

    public void freezeV1(ProcessRecord processRecord) {
        try {
            boolean result;
            if (suExecute) {
                result = freezerInterface.freezeV1(processRecord.getPid());
            } else {
                result = BaseFreezeUtils.freezePid(false, processRecord.getPid());
            }
            if (result) {
                Log.d("freeze " + processRecord.getProcessNameWithUser());

            } else {
                throw new FreezeFailedException();
            }
        } catch (Throwable throwable) {
            Log.e("freezeV1", throwable);
        }
    }

    public void kill(int sig, ProcessRecord processRecord) {
        try {
            boolean result;
            if (suExecute) {
                result = freezerInterface.kill(sig, processRecord.getPid());
            } else {
                result = BaseFreezeUtils.kill(false, sig, processRecord.getPid());
            }
            if (result) {
                Log.d("kill -" + sig + " " + processRecord.getProcessNameWithUser());
            } else {
                throw new UnKnowException();
            }
        } catch (Throwable throwable) {
            Log.e("kill", throwable);
        }
    }
}
