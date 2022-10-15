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
import cn.myflv.noactive.utils.PackageUtils
import com.topjohnwu.superuser.io.SuFile


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
                TextWithSwitch(TextV(resources.getString(R.string.interval_freeze)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.IntervalFreeze)) {
                    setConfig(FreezerConfig.IntervalFreeze, it)
                })
                TextWithSwitch(TextV(resources.getString(R.string.interval_unfreeze)), SwitchV("binding", defValue = isConfigOn(FreezerConfig.IntervalUnfreeze)) {
                    setConfig(FreezerConfig.IntervalUnfreeze, it)
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
        val config = SuFile(FreezerConfig.ConfigDir, name)
        if (on) {
            config.createNewFile()
        } else {
            config.delete()
        }
        showToast(resources.getString(R.string.config_set_after_reboot))
    }

    fun isConfigOn(name: String): Boolean {
        return SuFile(FreezerConfig.ConfigDir, name).exists()
    }

    fun setMode(mode: String?) {
        SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerApi).delete()
        SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerV2).delete()
        SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerV1).delete()
        SuFile(FreezerConfig.ConfigDir, FreezerConfig.kill19).delete()
        SuFile(FreezerConfig.ConfigDir, FreezerConfig.kill20).delete()
        if (mode != null) {
            SuFile(FreezerConfig.ConfigDir, mode).createNewFile()
        }
        showToast(resources.getString(R.string.config_set_after_reboot))
    }

    fun getMode(): String {
        if (SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerApi).exists()) {
            return "API";
        }

        if (SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerV2).exists()) {
            return "V2";
        }
        if (SuFile(FreezerConfig.ConfigDir, FreezerConfig.freezerV1).exists()) {
            return "V1";
        }
        if (SuFile(FreezerConfig.ConfigDir, FreezerConfig.kill19).exists()) {
            return "SIGSTOP";
        }
        if (SuFile(FreezerConfig.ConfigDir, FreezerConfig.kill20).exists()) {
            return "SIGTSTP";
        }
        return resources.getString(R.string.auto)
    }

    private fun showToast(string: String) {
        handler.post {
            Toast.makeText(this, string, Toast.LENGTH_LONG).show()
        }
    }
}