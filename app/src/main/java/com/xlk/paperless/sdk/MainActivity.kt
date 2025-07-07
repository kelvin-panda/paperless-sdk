package com.xlk.paperless.sdk

import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.mogujie.tt.protobuf.InterfaceBase
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEVALIDATE_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_READY_VALUE
import com.paperless.bus.EventBusMessage
import com.paperless.sdk.Call
import com.paperless.sdk.CallValue
import com.paperless.sdk.CallValue.Companion.localDeviceId
import com.paperless.sdk.Protocol
import com.paperless.sdk.ProtocolTool
import com.paperless.util.IniUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        EventBus.getDefault().register(this)
        Jni.loadLibrary()
        initConfigFile()
        Jni.initialization(
            InterfaceMacro.Pb_ProgramType.Pb_MEET_PROGRAM_TYPE_MEETCLIENT_VALUE,
            CallValue.root_dir + "client.ini", DeviceUtils.getUniqueDeviceId(), 4, 0
        )
    }

    private fun initConfigFile() {
        val currentTimeMillis = System.currentTimeMillis()
        FileUtils.createOrExistsDir(CallValue.root_dir)
        val exists = FileUtils.isFileExists(CallValue.root_dir + "client.ini")
        if (!exists) {
            ResourceUtils.copyFileFromAssets("client.ini", CallValue.root_dir + "client.ini")
        }
        val file = File(CallValue.root_dir + "client.ini")
        val isLoadIniFileSuccess = IniUtil.loadFile(file)
        LogUtils.e("配置文件是否存在：${file.exists()},文件大小：${file.length()},isLoadFile:${isLoadIniFileSuccess}")
        if (isLoadIniFileSuccess) {
            val isDebug = IniUtil.isDebug
            val ip = IniUtil.ip
            val port = IniUtil.port
            var discardms = IniUtil.discardMs
            var playcachems = IniUtil.playCacheMs
            val tickdebug = IniUtil.isTickDebug
            val tickint = IniUtil.reconnectInterval
            val keybind = IniUtil.get("debug", "keybind")
            LogUtils.e(
                "加载ini文件成功：ip=" + ip + ",port=" + port
                        + ",discardms=" + discardms
                        + ",playcachems=" + playcachems
                        + ",bitrate=" + CallValue.bitrate
                        + ",frameRate=" + CallValue.frameRate
                        + ",iframeInterval=" + CallValue.iframeInterval
                        + ",tickdebug=" + tickdebug
                        + ",tickint=" + tickint
                        + ",keybind=" + keybind
                        + ",isDebug=" + isDebug
            )
            if (discardms.isEmpty()) {
                discardms = 10000.toString()
            }
            if (playcachems.isEmpty()) {
                playcachems = 0.toString()
            }

            val split: Array<String> =
                BuildConfig.VERSION_NAME.split(".").toTypedArray()
            val softver = split[0]
            val hardver = split[1]
            if (split.size > 2) {
                val date = split[2]
                IniUtil.date = date
            }

            IniUtil.discardMs = discardms
            IniUtil.playCacheMs = playcachems
            IniUtil.hardver = hardver
            IniUtil.softver = softver
            IniUtil.configDir = CallValue.root_dir
            IniUtil.mediaDir = CallValue.root_dir + "mediadir" + File.separator
            IniUtil.store()
        }
        FileUtils.delete(CallValue.root_dir + "client.dev")
        ResourceUtils.copyFileFromAssets("client.dev", CallValue.root_dir + "client.dev")
        LogUtils.e("耗时：${System.currentTimeMillis() - currentTimeMillis}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
            //设备ID校验
            Pb_TYPE_MEET_INTERFACE_DEVICEVALIDATE_VALUE -> {
                val info = InterfaceBase.pbui_Type_DeviceValidate.parseFrom(msg.data)
                val valList = info.valList
                val valflag = info.valflag
                var onlineClientCount = 0
                val binaryString = Integer.toBinaryString(valflag)
                LogUtils.e("平台登录验证返回 valflag=$valflag,binaryString=$binaryString,valList=$valList")
                var count = 0
                var index: Int
                for (i in binaryString.indices) {
                    if (binaryString[binaryString.length - i - 1] == '1') {
                        count++
                        index = count - 1
                        val code = info.valList[index]
                        when (i) {
                            0 -> {
                                LogUtils.e("区域服务器ID：$code")
                            }

                            1 -> {
                                LogUtils.e("设备ID：$code")
                                localDeviceId = code
                            }

                            2 -> {
                                LogUtils.e("状态码：$code")
                                ProtocolTool.initializationResult(this, code)
                            }

                            3 -> {
                                LogUtils.e("到期时间：$code")
                            }

                            4 -> {
                                LogUtils.e("企业ID：$code")
                            }

                            5 -> {
                                LogUtils.e("协议版本：$code")
                            }

                            6 -> {
                                LogUtils.e("注册时自定义的32位整数值：$code")
                            }

                            7 -> {
                                LogUtils.e("当前在线设备数：$code")
                                onlineClientCount = code
                            }

                            8 -> {
                                LogUtils.e("最大在线设备数：$code")
                                if (onlineClientCount > code) {
                                    ToastUtils.showLong(com.paperless.sdk.R.string.maximum_number_of_online_devices_exceeded)
                                }
                            }
                        }
                    }
                }
            }
            //平台初始化完毕
            Pb_TYPE_MEET_INTERFACE_READY_VALUE -> {
                if (msg.method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    val info = InterfaceBase.pbui_Ready.parseFrom(msg.data)
                    LogUtils.e("平台初始化结果 连接上的区域服务器ID=${info.areaid}")
                    Call.initAndCapture(0, Protocol.channel_screen)
                    Call.initAndCapture(0, Protocol.channel_camera)
                }
            }
            //管理员
//            Pb_TYPE_MEET_INTERFACE_ADMIN_VALUE -> {
//                if (msg.method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_LOGON_VALUE) {
//                    InterfaceAdmin.pbui_Type_AdminLogonStatus.parseFrom(msg.data)?.let {
//                        loginResult(it)
//                    }
//                }
//            }

            Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE -> {
                val info = InterfaceDevice.pbui_Type_MeetDeviceBaseInfo.parseFrom(msg.data)
                LogUtils.e("设备寄存器 deviceid:${info.deviceid},attribid:${info.attribid},localDeviceId:$localDeviceId")

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}