package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.core.entity.ClassEnum;
import cn.myflv.noactive.core.entity.FieldEnum;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.entity.MethodEnum;
import cn.myflv.noactive.core.server.BroadcastFilter;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.server.ReceiverList;
import cn.myflv.noactive.core.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 广播分发Hook.
 */
public class BroadcastDeliverHook extends MethodHook {
    /**
     * 内存数据.
     */
    private final MemData memData;

    public BroadcastDeliverHook(ClassLoader classLoader, MemData memData) {
        super(classLoader);
        this.memData = memData;
    }

    @Override
    public String getTargetClass() {
        return ClassEnum.BroadcastQueue;
    }

    @Override
    public String getTargetMethod() {
        return MethodEnum.deliverToRegisteredReceiverLocked;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassEnum.BroadcastRecord, ClassEnum.BroadcastFilter, boolean.class, int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Object[] args = param.args;
                if (args[1] == null) {
                    return;
                }
                BroadcastFilter broadcastFilter = new BroadcastFilter(args[1]);
                ReceiverList receiverList = broadcastFilter.getReceiverList();
                // 如果广播为空就不处理
                if (receiverList == null) {
                    return;
                }
                ProcessRecord processRecord = receiverList.getProcessRecord();
                // 如果进程或者应用信息为空就不处理
                if (processRecord == null) {
                    return;
                }

                // 不是目标进程就不处理
                if (!memData.isTargetProcess(processRecord)) {
                    return;
                }

                String packageName = processRecord.getApplicationInfo().getPackageName();

                // 不是冻结APP就不处理
                if (!memData.getFreezerAppSet().contains(packageName)) {
                    // 意味着广播执行
                    broadcastStart(param, packageName);
                    return;
                }

                // 暂存
                Object app = processRecord.getProcessRecord();
                param.setObjectExtra(FieldEnum.app, app);
                // Log.d(processRecord.getProcessName() + " clear broadcast");
                // 清楚广播
                receiverList.clear();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                // 恢复被修改的参数
                restore(param);
                // 广播结束
                broadcastFinish(param);
            }
        };
    }

    /**
     * 广播开始执行
     *
     * @param packageName 包名
     */
    private void broadcastStart(XC_MethodHook.MethodHookParam param, String packageName) {
        memData.setBroadcastApp(packageName);
        param.setObjectExtra(FieldEnum.packageName, packageName);
        // Log.d(packageName + " broadcast executing start");
    }

    /**
     * 广播结束执行
     */
    private void broadcastFinish(XC_MethodHook.MethodHookParam param) {
        Object obj = param.getObjectExtra(FieldEnum.packageName);
        if (obj == null) {
            return;
        }
        memData.setBroadcastApp(null);
        String packageName = (String) obj;
        // Log.d(packageName + " broadcast executing finish");
    }

    /**
     * 恢复被修改的参数
     */
    private void restore(XC_MethodHook.MethodHookParam param) {
        // 获取进程
        Object app = param.getObjectExtra(FieldEnum.app);
        if (app == null) {
            return;
        }

        Object[] args = param.args;
        if (args[1] == null) {
            return;
        }
        Object receiverList = XposedHelpers.getObjectField(args[1], FieldEnum.receiverList);
        if (receiverList == null) {
            return;
        }
        // 还原修改
        XposedHelpers.setObjectField(receiverList, FieldEnum.app, app);
    }

    @Override
    public int getMinVersion() {
        return ANY_VERSION;
    }

    @Override
    public String successLog() {
        return "Listen broadcast deliver";
    }

}
