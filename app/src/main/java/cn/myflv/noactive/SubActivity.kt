package cn.myflv.noactive

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import cn.fkj233.ui.activity.MIUIActivity
import cn.fkj233.ui.activity.view.SpinnerV
import cn.fkj233.ui.activity.view.SwitchV
import cn.fkj233.ui.activity.view.TextV
import cn.myflv.noactive.core.utils.FreezerConfig
import cn.myflv.noactive.utils.ConfigUtils
import cn.myflv.noactive.utils.PackageUtils
import cn.myflv.noactive.entity.SwitchData

class SubActivity : MIUIActivity() {
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val activity = this

    init {
        initView {
            // 应用名称
            val appName = intent.getStringExtra("appName")
            // 包名
            val packageName = intent.getStringExtra("packageName")
            // 应用信息
            val appInfo = PackageUtils.getProcessSet(activity, packageName)

            registerMain(appName!!, true) {

                val binding = GetDataBinding({ SwitchData.isOn() }) { view, flags, data ->
                    when (flags) {
                        1 -> (view as LinearLayout).visibility = if (data as Boolean) View.VISIBLE else View.GONE;
                        2 -> (view as LinearLayout).visibility = if (!(data as Boolean)) View.VISIBLE else View.GONE;
                    }
                }


                SwitchData.setOn(if (appInfo.system) appInfo.black else appInfo.white)


                TextWithSwitch(TextV(if (appInfo.system) "系统黑名单" else "应用白名单"), SwitchV("binding", defValue = SwitchData.isOn(), dataBindingSend = binding.bindingSend) {
                    val fileName = if (appInfo.system) FreezerConfig.blackSystemAppConfig else FreezerConfig.whiteAppConfig
                    if (it) {
                        ConfigUtils.addIfNot(fileName, packageName);
                    } else {
                        ConfigUtils.delIfExist(fileName, packageName);
                    }
                })

                TextWithSwitch(TextV("忽略前台"), SwitchV("binding", defValue = appInfo.direct) {
                    if (it) {
                        ConfigUtils.addIfNot(FreezerConfig.directAppConfig, packageName);
                    } else {
                        ConfigUtils.delIfExist(FreezerConfig.directAppConfig, packageName);
                    }
                }, dataBindingRecv = binding.getRecv(if (appInfo.system) 1 else 2))

                for (proc in appInfo.processSet) {
                    TextWithSpinner(TextV(proc, textSize = 15f), SpinnerV(if (appInfo.killProcessSet.contains(proc)) "杀死" else (if (appInfo.whiteProcessSet.contains(proc)) "白名单" else "冻结")) {
                        add("冻结") {
                            ConfigUtils.delIfExist(FreezerConfig.whiteProcessConfig, proc);
                            ConfigUtils.delIfExist(FreezerConfig.killProcessConfig, proc)
                        }
                        add("杀死") {
                            ConfigUtils.addIfNot(FreezerConfig.killProcessConfig, proc);
                            ConfigUtils.delIfExist(FreezerConfig.whiteProcessConfig, proc)
                        }
                        add("白名单") {
                            ConfigUtils.addIfNot(FreezerConfig.whiteProcessConfig, proc);
                            ConfigUtils.delIfExist(FreezerConfig.killProcessConfig, proc)
                        }
                    }, dataBindingRecv = binding.getRecv(if (appInfo.system) 1 else 2))
                }
            }
        }
    }

}