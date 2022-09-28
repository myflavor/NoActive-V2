package cn.myflv.noactive.core.hook;

import cn.myflv.noactive.constant.ClassConstants;
import cn.myflv.noactive.constant.FieldConstants;
import cn.myflv.noactive.constant.MethodConstants;
import cn.myflv.noactive.core.entity.MemData;
import cn.myflv.noactive.core.hook.base.AbstractMethodHook;
import cn.myflv.noactive.core.hook.base.MethodHook;
import cn.myflv.noactive.core.server.BroadcastFilter;
import cn.myflv.noactive.core.server.ProcessRecord;
import cn.myflv.noactive.core.server.ReceiverList;
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
        return ClassConstants.BroadcastQueue;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.deliverToRegisteredReceiverLocked;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassConstants.BroadcastRecord, ClassConstants.BroadcastFilter, boolean.class, int.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) throws Throwable {
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

                String packageName = processRecord.getPackageName();

                // 不是冻结APP就不处理
                if (!memData.getFreezerAppSet().contains(packageName)) {
                    // 意味着广播执行
                    broadcastStart(param, packageName);
                    return;
                }

                // 暂存
                Object app = processRecord.getProcessRecord();
                param.setObjectExtra(FieldConstants.app, app);
                // Log.d(processRecord.getProcessName() + " clear broadcast");
                // 清楚广播
                receiverList.clear();
            }

            @Override
            protected void afterMethod(MethodHookParam param) throws Throwable {
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
        param.setObjectExtra(FieldConstants.packageName, packageName);
        // Log.d(packageName + " broadcast executing start");
    }

    /**
     * 广播结束执行
     */
    private void broadcastFinish(XC_MethodHook.MethodHookParam param) {
        Object obj = param.getObjectExtra(FieldConstants.packageName);
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
        Object app = param.getObjectExtra(FieldConstants.app);
        if (app == null) {
            return;
        }

        Object[] args = param.args;
        if (args[1] == null) {
            return;
        }
        Object receiverList = XposedHelpers.getObjectField(args[1], FieldConstants.receiverList);
        if (receiverList == null) {
            return;
        }
        // 还原修改
        XposedHelpers.setObjectField(receiverList, FieldConstants.app, app);
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
