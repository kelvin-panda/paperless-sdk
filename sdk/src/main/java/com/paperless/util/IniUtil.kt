package com.paperless.util

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ResourceUtils
import org.ini4j.Config
import org.ini4j.Ini
import java.io.File
import java.io.IOException

/**
 * @author Created by xlk on 2021/9/26.
 */
object IniUtil  {
    private val ini = Ini()

    init {
        val config = Config()
        //不允许出现重复的部分和选项
        config.isMultiSection = false
        config.isMultiOption = false
        ini.config = config
    }

    /**
     * 加载ini文件
     *
     * @param filePath ini文件路径
     */
    fun loadFile(filePath: String): Boolean {
        return loadFile(File(filePath))
    }

    fun loadFile(file: File): Boolean {
        try {
            ini.file = file
            ini.load()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    operator fun get(sectionName: String, optionName: String): String {
        var s = ini[sectionName, optionName]
        if (s == null) {
            s = ""
        }
        return s
    }

    fun put(sectionName: String, optionName: String, value: Any) {
        ini.put(sectionName, optionName, value)
    }

    fun store(): Boolean {
        try {
            ini.store()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            LogUtils.e(e)
        }
        return false
    }

    fun defaultInitConfigFile(rootPath: String) {
        val iniPath = rootPath + File.separator + "client.ini"
        val devPath = rootPath + File.separator + "client.dev"
        if (!FileUtils.isFileExists(iniPath)) {
            ResourceUtils.copyFileFromAssets("client.ini", iniPath)
        }
        if (loadFile(iniPath)) {
            put("Buffer Dir", "configdir", rootPath)
            put("Buffer Dir", "mediadir", rootPath + "mediadir" + File.separator)
            store()
        }
        FileUtils.delete(devPath)
        ResourceUtils.copyFileFromAssets("client.dev", devPath)
    }

    val isTcpMode: Boolean
        get() {
            val result = ini["selfinfo", "streamprotol"]
            return result != null && result == "1"
        }
    val isAgenda3Mode: Boolean
        get() {
            val result = ini["debug", "agendav3"]
            return result != null && result == "1"
        }

    fun switchTcpMode(enable: Boolean) {
        ini.put("selfinfo", "streamprotol", if (enable) "1" else "0")
    }

    var hardver: String
        get() {
            val result = ini["selfinfo", "hardver"]
            return result ?: ""
        }
        set(hardver) {
            ini.put("selfinfo", "hardver", hardver)
        }
    var softver: String
        get() {
            val result = ini["selfinfo", "softver"]
            return result ?: ""
        }
        set(softver) {
            ini.put("selfinfo", "softver", softver)
        }
    var date: String
        get() {
            val result = ini["selfinfo", "date"]
            return result ?: ""
        }
        set(date) {
            ini.put("selfinfo", "date", date)
        }
    /**
     * 获取丢帧间隔
     *
     * @return 单位毫秒
     */
    var discardMs: String
        get() {
            val result = ini["selfinfo", "discardms"]
            return result ?: ""
        }
        set(ms) {
            ini.put("selfinfo", "discardms", ms)
        }
    /**
     * 获取视频播放缓冲
     *
     * @return 单位毫秒
     */
    var playCacheMs: String
        get() {
            val result = ini["selfinfo", "playcachems"]
            return result ?: ""
        }
        set(ms) {
            ini.put("selfinfo", "playcachems", ms)
        }

    /**
     * 是否已开启同屏调试
     */
    val isSameScreenDebug: Boolean
        get() {
            val result = ini["selfinfo", "capturedebug"]
            return "1" == result
        }

    /**
     * 切换同屏调试开关
     */
    fun switchSameScreenDebug(enable: Boolean) {
        ini.put("selfinfo", "capturedebug", if (enable) "1" else "0")
    }

    /**
     * 获取网络调试状态
     */
    val isTickDebug: Boolean
        get() {
            val result = ini["selfinfo", "tickdebug"]
            return result != null && result == "1"
        }

    /**
     * 切换同屏调试开关
     */
    fun switchTickDebug(enable: Boolean) {
        ini.put("selfinfo", "tickdebug", if (enable) "1" else "0")
    }
    /**
     * 获取断线重连间隔
     *
     * @return 单位毫秒
     */
    var reconnectInterval: String
        get() {
            val result = ini["tick", "int"]
            return result ?: ""
        }
        set(ms) {
            ini.put("tick", "int", ms)
        }

    /**
     * 获取是否短线重连
     */
    val isDisconnectReconnect: Boolean
        get() {
            val result = ini["debug", "streamreconnect"]
            return result != null && result == "1"
        }

    /**
     * 切换短线重连开关
     */
    fun switchDisconnectReconnect(enable: Boolean) {
        ini.put("debug", "streamreconnect", if (enable) "1" else "0")
    }

    /**
     * 获取当前是否打开调试开关
     */
    val isDebug: Boolean
        get() {
            val result = ini["debug", "enabledebug"]
            return result != null && result == "1"
        }

    /**
     * 切换打开调试开关
     */
    fun switchDebug(enable: Boolean) {
        ini.put("debug", "enabledebug", if (enable) "1" else "0")
    }

    /**
     * 是否启动桌面采集声卡回放 0关闭禁用 1开启禁用 启用将扬声器输出捕获并附加到2号视频通道
     * 当videoaudio=1启用时 注:优先使用输入音频附加
     */
    val isSameScreenSound: Boolean
        get() {
            val result = ini["debug", "disableaudio"]
            return result != null && result == "0"
        }

    /**
     * 切换同屏声音开关
     */
    fun switchSameScreenSound(enable: Boolean) {
        ini.put("debug", "disableaudio", if (enable) "0" else "1")
    }

    val isSameScreenMic: Boolean
        get() {
            val result = ini["debug", "videoaudio"]
            return result != null && result == "1"
        }

    /**
     * 切换同屏麦克风开关
     */
    fun switchSameScreenMic(enable: Boolean) {
        ini.put("debug", "videoaudio", if (enable) "1" else "0")
    }

    var configDir: String
        get() {
            val result = ini["Buffer Dir", "configdir"]
            return result ?: ""
        }
        set(configDir) {
            ini.put("Buffer Dir", "configdir", configDir)
        }
    var mediaDir: String
        get() {
            val result = ini["Buffer Dir", "mediadir"]
            return result ?: ""
        }
        set(mediaDir) {
            ini.put("Buffer Dir", "mediadir", mediaDir)
        }
    var mediaDirSize: String
        get() {
            val result = ini["Buffer Dir", "mediadirsize"]
            return result ?: ""
        }
        set(mediaDirSize) {
            ini.put("Buffer Dir", "mediadirsize", mediaDirSize)
        }
    var ip: String
        get() {
            val result = ini["areaaddr", "area0ip"]
            return result ?: ""
        }
        set(ip) {
            ini.put("areaaddr", "area0ip", ip)
        }
    var port: String
        get() {
            val result = ini["areaaddr", "area0port"]
            return result ?: ""
        }
        set(port) {
            ini.put("areaaddr", "area0port", port)
        }

    /**
     * 是否编码过滤
     */
    val isEncodeFilter: Boolean
        get() {
            val result = ini["nosdl", "disablebsf"]
            return result != null && result == "0"
        }

    fun switchEncodeFilter(enable: Boolean) {
        ini.put("nosdl", "disablebsf", if (enable) "0" else "1")
    }

    val isEncryption: Boolean
        get() {
            val result = ini["debug", "protolencrypt"]
            return result != null && result == "1"
        }

    fun switchEncryption(enable: Boolean) {
        ini.put("debug", "protolencrypt", if (enable) "1" else "0")
    }

    val isDisableMulticast: Boolean
        get() {
            val result = ini["debug", "disablemulticast"]
            return result != null && result == "1"
        }

    fun switchDisableMulticast(enable: Boolean) {
        ini.put("debug", "disablemulticast", if (enable) "1" else "0")
    }
    // PC 相关配置
    /**
     * 是否开启显卡硬编码
     */
    val isHwEncode: Boolean
        get() {
            val result = ini["debug", "hwencode"]
            return result != null && result == "1"
        }

    fun switchHwEncode(enable: Boolean) {
        ini.put("debug", "hwencode", if (enable) "1" else "0")
    }

    /**
     * 是否开启显卡硬解码
     */
    val isHwDecode: Boolean
        get() {
            val result = ini["debug", "hwdecode"]
            return result != null && result == "1"
        }

    fun switchHwDecode(enable: Boolean) {
        ini.put("debug", "hwdecode", if (enable) "1" else "0")
    }

    /**
     * 是否开启调试窗口
     */
    val isOpenDebugWindow: Boolean
        get() {
            val result = ini["debug", "console"]
            return result != null && result == "1"
        }

    fun switchOpenDebugWindow(enable: Boolean) {
        ini.put("debug", "console", if (enable) "1" else "0")
    }

    /**
     * 是否开启USB视频设备采集
     */
    val isOpenUsbCap: Boolean
        get() {
            val result = ini["debug", "camaracap"]
            return result != null && result == "1"
        }

    fun switchOpenUsbCap(enable: Boolean) {
        ini.put("debug", "camaracap", if (enable) "1" else "0")
    }

    /**
     * 上传超高清视频时转换流畅格式
     */
    val isMediaTranscode: Boolean
        get() {
            val result = ini["debug", "mediatranscode"]
            return result != null && result == "1"
        }

    fun switchMediaTranscode(enable: Boolean) {
        ini.put("debug", "mediatranscode", if (enable) "1" else "0")
    }

    /**
     * 设置流编码模式0高质量
     *
     * @return 1中等 2低带宽 默认0
     */
    val encodeMode: Int
        get() {
            val encmode = ini["debug", "encmode"]
            try {
                return encmode.toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        }
    val videoStream2MaxWidth: String
        get() {
            val stream3width = ini["debug", "stream2width"]
            return stream3width ?: ""
        }
    val videoStream2MaxHeight: String
        get() {
            val stream3height = ini["debug", "stream2height"]
            return stream3height ?: ""
        }
    val videoStream3MaxWidth: String
        get() {
            val stream3width = ini["debug", "stream3width"]
            return stream3width ?: ""
        }
    val videoStream3MaxHeight: String
        get() {
            val stream3height = ini["debug", "stream3height"]
            return stream3height ?: ""
        }

    fun switchAgenda3mode(value: Boolean) {
        ini.put("debug", "agendav3", if (value) "1" else "0") //议题模式3
    }
}