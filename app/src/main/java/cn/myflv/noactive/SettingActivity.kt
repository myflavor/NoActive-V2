package cn.myflv.noactive

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import cn.fkj233.ui.activity.MIUIActivity
import cn.fkj233.ui.activity.view.SpinnerV
import cn.fkj233.ui.activity.view.SwitchV
import cn.fkj233.ui.activity.view.TextV
import cn.myflv.noactive.core.utils.FreezerConfig
import cn.myflv.noactive.utils.ConfigUtils

class SettingActivity : MIUIActivity() {
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val activity = this

    init {
        initView {
            registerMain(resources.getString(R.string.settings), true) {
                TextWithSpinner(TextV(resources.getString(R.string.freeze_method)), SpinnerV(getMode()) {
                    add(resources.getString(R.string.auto)) { setMode(null) }
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                        add("API") { setMode(FreezerConfig.freezerApi) }
                    }
                    add("V2") { setMode(FreezerConfig.freezerV2) }
                    add("V1") { setMode(FreezerConfig.freezerV1) }
                    add("SIGSTOP") { setMode(FreezerConfig.kill19) }
                    add("SIGTSTP") { setMode(FreezerConfig.kill20) }
                })
                TextWithSwitch(TextV(resources.getString(R.string.root_mode)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.SuExcute)) {
                    setConfig(FreezerConfig.SuExcute, it)
                })
                TextWithSwitch(TextV(resources.getString(R.string.boot_freeze)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.BootFreeze)) {
                    setConfig(FreezerConfig.BootFreeze, it)
                })
                TextWithSpinner(
                    TextV(resources.getString(R.string.boot_freeze_delay)),
                    SpinnerV(getDelay(FreezerConfig.BootFreezeDelay)) {
                        add("1分钟") { setDelay(FreezerConfig.BootFreezeDelay, "1") }
                        add("2分钟") { setDelay(FreezerConfig.BootFreezeDelay, "2") }
                        add("3分钟") { setDelay(FreezerConfig.BootFreezeDelay, "3") }
                        add("5分钟") { setDelay(FreezerConfig.BootFreezeDelay, "5") }
                        add("10分钟") { setDelay(FreezerConfig.BootFreezeDelay, "10") }
                        add("15分钟") { setDelay(FreezerConfig.BootFreezeDelay, "15") }
                    })
                TextWithSwitch(TextV(resources.getString(R.string.interval_freeze)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.IntervalFreeze)) {
                    setConfig(FreezerConfig.IntervalFreeze, it)
                })
                TextWithSpinner(
                    TextV(resources.getString(R.string.interval_freeze_delay)),
                    SpinnerV(getDelay(FreezerConfig.IntervalFreezeDelay)) {
                        add("1分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "1") }
                        add("2分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "2") }
                        add("3分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "3") }
                        add("5分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "5") }
                        add("10分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "10") }
                        add("15分钟") { setDelay(FreezerConfig.IntervalFreezeDelay, "15") }
                    })
                TextWithSwitch(TextV(resources.getString(R.string.interval_unfreeze)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.IntervalUnfreeze)) {
                    setConfig(FreezerConfig.IntervalUnfreeze, it)
                })
                TextWithSpinner(
                    TextV(resources.getString(R.string.interval_unfreeze_delay)),
                    SpinnerV(getDelay(FreezerConfig.IntervalUnfreezeDelay)) {
                        add("1分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "1") }
                        add("2分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "2") }
                        add("3分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "3") }
                        add("5分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "5") }
                        add("10分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "10") }
                        add("15分钟") { setDelay(FreezerConfig.IntervalUnfreezeDelay, "15") }
                    })
                TextWithSwitch(TextV(resources.getString(R.string.debug_log)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.Debug)) {
                    setConfig(FreezerConfig.Debug, it)
                })
                Line()
                TitleText(resources.getString(R.string.developer))
                Author(getImg(R.mipmap.ic_head)!!,
                        resources.getString(R.string.author),
                        resources.getString(R.string.author_blog), round = 16f, onClickListener = {
                    jumpUrl("http://www.myflv.cn")
                })
            }
        }
    }

    fun getImg(id: Int): Drawable? {
        return AppCompatResources.getDrawable(this@SettingActivity, id)
    }


    fun jumpUrl(url: String) {
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        val content_url: Uri = Uri.parse(url) //此处填链接
        intent.data = content_url
        startActivity(intent)
    }


    fun setConfig(name: String, on: Boolean) {
        ConfigUtils.setBoolean(name, on)
        showToast(resources.getString(R.string.config_set_after_reboot))
    }

    fun isConfigOn(name: String): Boolean {
        return ConfigUtils.getBoolean(name)
    }

    fun setMode(mode: String?) {
        ConfigUtils.setBoolean(FreezerConfig.freezerApi,false)
        ConfigUtils.setBoolean(FreezerConfig.freezerV2,false)
        ConfigUtils.setBoolean(FreezerConfig.freezerV1,false)
        ConfigUtils.setBoolean(FreezerConfig.kill19,false)
        ConfigUtils.setBoolean(FreezerConfig.kill20,false)
        if (mode != null) {
            ConfigUtils.setBoolean(mode,true)
        }
        showToast(resources.getString(R.string.config_set_after_reboot))
    }

    fun getMode(): String {
        if (ConfigUtils.getBoolean(FreezerConfig.freezerApi)) {
            return "API";
        }
        if (ConfigUtils.getBoolean(FreezerConfig.freezerV2)) {
            return "V2";
        }
        if (ConfigUtils.getBoolean(FreezerConfig.freezerV1)) {
            return "V1";
        }
        if (ConfigUtils.getBoolean(FreezerConfig.kill19)) {
            return "SIGSTOP";
        }
        if (ConfigUtils.getBoolean(FreezerConfig.kill20)) {
            return "SIGTSTP";
        }
        return resources.getString(R.string.auto)
    }

    fun setDelay(name: String, delay: String) {
        ConfigUtils.setString(name, delay)
        showToast(resources.getString(R.string.config_set_after_reboot))
    }

    fun getDelay(name: String): String {
        var delay = ConfigUtils.getString(name, "")
        if (delay == "") {
            setDelay(name, "1")
            delay = "1"
        }

        return delay + "分钟"
    }

    private fun showToast(string: String) {
        handler.post {
            Toast.makeText(this, string, Toast.LENGTH_LONG).show()
        }
    }
}