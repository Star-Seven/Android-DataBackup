package com.xayah.databackup.fragment.home

import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    val root = "${GlobalString.symbolDot} Root"
    val bin = "${GlobalString.symbolDot} Bin"
    val bashrc = "${GlobalString.symbolDot} Bashrc"
    var rootCheck = ObservableField(GlobalString.symbolCross)
    var binCheck = ObservableField(GlobalString.symbolCross)
    var bashrcCheck = ObservableField(GlobalString.symbolCross)
    var architectureCurrent = ObservableField("")
    var architectureSupport = ObservableField("${GlobalString.support}: arm64-v8a, x86_64")
    var versionCurrent = ObservableField(App.versionName)
    var versionLatest = ObservableField("")
    var downloadBtnVisible = ObservableBoolean(false)
    var logEnable = ObservableBoolean(false)
    var logText = ObservableField("")
    var isLogging = false
    var dynamicColorsEnable = ObservableBoolean(false)

    private fun checkRoot(): String {
        Command.mkdir(Path.getExternalStorageDataBackupDirectory()).apply {
            if (!this) {
                Toast.makeText(
                    App.globalContext, GlobalString.backupDirCreateFailed, Toast.LENGTH_SHORT
                ).show()
            }
        }
        return if (Command.checkRoot()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun checkBin(): String {
        return if (Command.checkBin(App.globalContext)) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun checkBashrc(): String {
        return if (Command.checkBashrc()) GlobalString.symbolTick else GlobalString.symbolCross
    }

    private fun setArchitecture() {
        architectureCurrent.set(Command.getABI())
    }

    private fun setUpdate() {
        // 设置默认值
        versionLatest.set(GlobalString.fetching)
        downloadBtnVisible.set(false)
        CoroutineScope(Dispatchers.IO).launch {
            Server.releases({ releaseList ->
                val mReleaseList = releaseList.filter { !it.name.contains("Check") }
                if (mReleaseList.isEmpty()) {
                    versionLatest.set(GlobalString.fetchFailed)
                } else {
                    versionLatest.set("${GlobalString.latest}: ${mReleaseList[0].name}")
                    if (!versionLatest.get()!!.contains(versionCurrent.get()!!)) {
                        downloadBtnVisible.set(true)
                    }
                }
            }, { versionLatest.set(GlobalString.fetchFailed) })
        }
    }

    fun refresh(v: View? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            rootCheck.set(checkRoot())
            binCheck.set(checkBin())
            bashrcCheck.set(checkBashrc())
            setArchitecture()
            setUpdate()
            updateLogCard()
        }
    }

    fun initialize() {
        refresh()
        setLogCard()
        setDynamicColorsCard()
    }

    private fun setLogCard() {
        logEnable.set(App.globalContext.readLogEnable())
        updateLogCard()
        if (logEnable.get() && !isLogging) {
            saveLog()
            isLogging = true
        }
    }

    private fun updateLogCard() {
        val logPath = Path.getShellLogPath()
        logText.set("${Command.countFile(logPath)} ${GlobalString.log}, ${Command.countSize(logPath)} ${GlobalString.size}\n${GlobalString.storedIn} ${logPath}")
    }

    fun onLogEnableCheckedChanged(v: View, checked: Boolean) {
        logEnable.set(checked)
        App.globalContext.saveLogEnable(logEnable.get())
    }

    fun onLogClearClick(v: View) {
        val context = v.context
        CoroutineScope(Dispatchers.IO).launch {
            Command.rm(Path.getShellLogPath())
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context, GlobalString.success, Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
        }
    }

    private fun saveLog() {
        CoroutineScope(Dispatchers.IO).launch {
            val date = Command.getDate().replace(" ", "_")
            Command.mkdir(Path.getShellLogPath())
            Command.saveShellLog("${Path.getShellLogPath()}/${date}")
        }
    }

    private fun setDynamicColorsCard() {
        dynamicColorsEnable.set(App.globalContext.readIsDynamicColors())
    }

    fun onDynamicColorsEnableCheckedChanged(v: View, checked: Boolean) {
        dynamicColorsEnable.set(checked)
        App.globalContext.saveIsDynamicColors(dynamicColorsEnable.get())
    }
}