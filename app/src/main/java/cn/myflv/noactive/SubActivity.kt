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
import cn.myflv.noactive.entity.SwitchData
import cn.myflv.noactive.utils.ConfigUtils
import cn.myflv.noactive.utils.PackageUtils

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
                        1 -> (view as LinearLayout).visibility = if (data as Boolean) View.VISIBLE else View.GONE
                        2 -> (view as LinearLayout).visibility = if (!(data as Boolean)) View.VISIBLE else View.GONE
                    }
                }


                SwitchData.setOn(if (appInfo.system) appInfo.black else appInfo.white)


                TextWithSwitch(TextV(if (appInfo.system) resources.getString(R.string.system_black) else resources.getString(R.string.user_white)),
                        SwitchV("binding", defValue = SwitchData.isOn(), dataBindingSend = binding.bindingSend) {
                            val fileName = if (appInfo.system) FreezerConfig.blackSystemAppConfig else FreezerConfig.whiteAppConfig
                            if (it) {
                                ConfigUtils.addIfNot(fileName, packageName)
                                if (appInfo.system) {
                                    ConfigUtils.delIfExist(FreezerConfig.idleAppConfig, packageName)
                                }
                            } else {
                                ConfigUtils.delIfExist(fileName, packageName)
                                if (!appInfo.system) {
                                    ConfigUtils.delIfExist(FreezerConfig.idleAppConfig, packageName)
                                }
                            }
                        })

                TextWithSwitch(TextV(resources.getString(R.string.battery_opt)), SwitchV("binding", defValue = appInfo.idle) {
                    if (it) {
                        ConfigUtils.addIfNot(FreezerConfig.idleAppConfig, packageName)
                    } else {
                        ConfigUtils.delIfExist(FreezerConfig.idleAppConfig, packageName)
                    }
                }, dataBindingRecv = binding.getRecv(if (appInfo.system) 2 else 1))


                TextWithSpinner(
                        TextV(resources.getString(R.string.background_level)),
                        SpinnerV(if (appInfo.top) resources.getString(R.string.top_app) else (
                                if (appInfo.direct) resources.getString(R.string.direct_app) else resources.getString(R.string.foreground_service))) {
                            add(resources.getString(R.string.foreground_service)) {
                                ConfigUtils.delIfExist(FreezerConfig.directAppConfig, packageName)
                                ConfigUtils.delIfExist(FreezerConfig.topAppConfig, packageName)
                            }
                            add(resources.getString(R.string.top_app)) {
                                ConfigUtils.addIfNot(FreezerConfig.topAppConfig, packageName)
                                ConfigUtils.delIfExist(FreezerConfig.directAppConfig, packageName)
                            }
                            add(resources.getString(R.string.direct_app)) {
                                ConfigUtils.addIfNot(FreezerConfig.directAppConfig, packageName)
                                ConfigUtils.delIfExist(FreezerConfig.topAppConfig, packageName)
                            }
                        }, dataBindingRecv = binding.getRecv(if (appInfo.system) 1 else 2))

                for (proc in appInfo.processSet) {
                    TextWithSpinner(
                            TextV(proc, textSize = 15f),
                            SpinnerV(if (appInfo.killProcessSet.contains(proc)) resources.getString(R.string.kill) else (
                                    if (appInfo.whiteProcessSet.contains(proc)) resources.getString(R.string.white_app) else resources.getString(R.string.freeze))) {
                                add(resources.getString(R.string.freeze)) {
                                    ConfigUtils.delIfExist(FreezerConfig.whiteProcessConfig, proc)
                                    ConfigUtils.delIfExist(FreezerConfig.killProcessConfig, proc)
                                }
                                add(resources.getString(R.string.kill)) {
                                    ConfigUtils.addIfNot(FreezerConfig.killProcessConfig, proc)
                                    ConfigUtils.delIfExist(FreezerConfig.whiteProcessConfig, proc)
                                }
                                add(resources.getString(R.string.white_app)) {
                                    ConfigUtils.addIfNot(FreezerConfig.whiteProcessConfig, proc)
                                    ConfigUtils.delIfExist(FreezerConfig.killProcessConfig, proc)
                                }
                            }, dataBindingRecv = binding.getRecv(if (appInfo.system) 1 else 2))
                }
            }
        }
    }

}