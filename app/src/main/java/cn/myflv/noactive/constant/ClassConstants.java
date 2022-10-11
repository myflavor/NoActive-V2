package cn.myflv.noactive.constant;

/**
 * 类枚举.
 */
public interface ClassConstants {
    String ActivityManagerService = "com.android.server.am.ActivityManagerService";
    String ComponentName = "android.content.ComponentName";
    String IBinder = "android.os.IBinder";
    String BroadcastQueue = "com.android.server.am.BroadcastQueue";
    String BroadcastRecord = "com.android.server.am.BroadcastRecord";
    String BroadcastFilter = "com.android.server.am.BroadcastFilter";
    String AnrHelper = "com.android.server.am.AnrHelper";
    String ProcessRecord = "com.android.server.am.ProcessRecord";
    String ApplicationInfo = "android.content.pm.ApplicationInfo";
    String WindowProcessController = "com.android.server.wm.WindowProcessController";
    String AnrRecord = AnrHelper + "$AnrRecord";
    String AppErrors = "com.android.server.am.AppErrors";
    String ActivityRecord_P = "com.android.server.am.ActivityRecord";
    String ProcessStateRecord = "com.android.server.am.ProcessStateRecord";
    String OomAdjuster = "com.android.server.am.OomAdjuster";
    String MilletConfig = "com.miui.powerkeeper.millet.MilletConfig";
    String CachedAppOptimizer = "com.android.server.am.CachedAppOptimizer";
    String ProcessList = "com.android.server.am.ProcessList";
    String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
    String Process = "android.os.Process";
    String IApplicationThread = "android.app.IApplicationThread";
    String ProfilerInfo = "android.app.ProfilerInfo";
    String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";
    String LocalServices = "com.android.server.LocalServices";
    String PowerManagerService = " com.android.server.power.PowerManagerService";
    String ProcessManager = "miui.process.ProcessManager";
    String ProcessConfig = "miui.process.ProcessConfig";

    String ActivityTaskSupervisor = "com.android.server.wm.ActivityTaskSupervisor";
    String ActivityRecord = "com.android.server.wm.ActivityRecord";
    String Configuration = "android.content.res.Configuration";
    String ActivityStackSupervisor = "com.android.server.wm.ActivityStackSupervisor";
    String ActivityStackSupervisorHandler = ActivityStackSupervisor + "$ActivityStackSupervisorHandler";

    String RecentTasks = "com.android.server.wm.RecentTasks";
    String GreezeManagerService = "com.miui.server.greeze.GreezeManagerService";
    String Task = "com.android.server.wm.Task";
    String ProcessManagerService = "com.android.server.am.ProcessManagerService";
    String FreezeUtils = "com.miui.server.greeze.FreezeUtils";
    String PidMap = "com.android.server.am.ActivityManagerService.PidMap";
    String UsageStatsService = "com.android.server.usage.UsageStatsService";
    String AppStandbyController = "com.android.server.usage.AppStandbyController";
    String NetworkManagementService = "com.android.server.NetworkManagementService";
    String UidRangeParcel = "android.net.UidRangeParcel";
    String FreezeBinder = "com.miui.powerkeeper.millet.FreezeBinder";
    String DisplayPowerController = "com.android.server.display.DisplayPowerController";
    String DeviceIdleController = "com.android.server.DeviceIdleController";
}
