package com.xlk.paperless.sdk

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.mogujie.tt.protobuf.InterfaceBase
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEFACESHOW_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEVALIDATE_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_READY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfaceStream
import com.paperless.bus.BusType
import com.paperless.bus.EventBusMessage
import com.paperless.player.DecodeQueue
import com.paperless.sdk.Call
import com.paperless.sdk.MAIN_TYPE_BITMASK
import com.paperless.sdk.MEDIA_FILE_TYPE_AUDIO
import com.paperless.sdk.MEDIA_FILE_TYPE_RECORD
import com.paperless.sdk.MEDIA_FILE_TYPE_VIDEO
import com.paperless.sdk.Protocol
import com.paperless.sdk.Protocol.Companion.resource_id_0
import com.paperless.sdk.ProtocolTool
import com.paperless.sdk.SUB_TYPE_BITMASK
import com.paperless.sdk.SdkVars
import com.paperless.sdk.SdkVars.Companion.localDeviceId
import com.paperless.util.IniUtil
import com.xlk.paperless.sdk.service.ForegroundService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var edtIp: EditText
    lateinit var edtPort: EditText
    lateinit var tvOnline: TextView
    lateinit var tvDevId: TextView
    lateinit var tvDevName: TextView
    lateinit var tvMemberName: TextView
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
            exitProcess(0)
        }
    }

    private val applyScreenRecorder = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result != null) {
            if (result.resultCode == RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(this@MainActivity, ForegroundService::class.java)
                        .apply {
                            putExtra("intent_extra_code", result.resultCode)
                            putExtra("intent_extra_data", result.data)
                        })
                } else {
                    startService(Intent(this@MainActivity, ForegroundService::class.java)
                        .apply {
                            putExtra("intent_extra_code", result.resultCode)
                            putExtra("intent_extra_data", result.data)
                        })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        EventBus.getDefault().register(this)
        applyPermissions()
        edtIp = findViewById<EditText>(R.id.edtIp)
        edtPort = findViewById<EditText>(R.id.edtPort)
        findViewById<Button>(R.id.btnModify).setOnClickListener {
            IniUtil.loadFile(SdkVars.root_dir + "client.ini")
            IniUtil.ip = edtIp.text.toString()
            IniUtil.port = edtPort.text.toString()
            AppUtils.relaunchApp(true)
        }
        tvOnline = findViewById<TextView>(R.id.tvOnline)
        tvDevId = findViewById<TextView>(R.id.tvDevId)
        tvDevName = findViewById<TextView>(R.id.tvDevName)
        tvMemberName = findViewById<TextView>(R.id.tvMemberName)
        val id_0 = findViewById<CheckBox>(R.id.id_0)
        val id_1 = findViewById<CheckBox>(R.id.id_1)
        val id_2 = findViewById<CheckBox>(R.id.id_2)
        val id_3 = findViewById<CheckBox>(R.id.id_3)
        val id_4 = findViewById<CheckBox>(R.id.id_4)
        val edt_device_id = findViewById<EditText>(R.id.edt_device_id)
        val edt_media_id = findViewById<EditText>(R.id.edt_media_id)
        findViewById<Button>(R.id.btnPlayPage).setOnClickListener {
            startActivity(Intent(this, ControlViewActivity::class.java))
        }
        val edt_page_code = findViewById<EditText>(R.id.edt_page_code)
        findViewById<Button>(R.id.btn_page).setOnClickListener {
            //InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_MemFace_VALUE
            val pageCode = Integer.parseInt(edt_page_code.text.toString())
            Jni.modPageStatus(pageCode)
        }
        findViewById<Button>(R.id.btn_split).setOnClickListener {
            startActivity(Intent(this, SplitPlayActivity::class.java))
        }
        //播放媒体文件
        findViewById<Button>(R.id.btn_play_media).setOnClickListener {
            val str = edt_media_id.text.toString()
            val id = Integer.parseInt(str)
            val temp = mutableListOf<Int>()
            if (id_0.isChecked) temp.add(0)
            if (id_1.isChecked) temp.add(1)
            if (id_2.isChecked) temp.add(2)
            if (id_3.isChecked) temp.add(3)
            if (id_4.isChecked) temp.add(4)
            Jni.mediaPlay(temp, id, localDeviceId)
        }
        findViewById<Button>(R.id.btn_stream_play).setOnClickListener {
            val temp = mutableListOf<Int>()
            if (id_0.isChecked) temp.add(0)
            if (id_1.isChecked) temp.add(1)
            if (id_2.isChecked) temp.add(2)
            if (id_3.isChecked) temp.add(3)
            if (id_4.isChecked) temp.add(4)
            val str = edt_device_id.text.toString()
            val id = Integer.parseInt(str)
            Jni.streamPlay(id, 2, temp, localDeviceId)
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initConfigFile()
        Jni.initialization(
            InterfaceMacro.Pb_ProgramType.Pb_MEET_PROGRAM_TYPE_MEETCLIENT_VALUE,
            SdkVars.root_dir + "client.ini", DeviceUtils.getUniqueDeviceId(), 4, 0
        )
        val edt_dst_id = findViewById<EditText>(R.id.edt_dst_id)
        findViewById<Button>(R.id.btn_start_record).setOnClickListener {
            val dstDevId = Integer.parseInt(edt_dst_id.text.toString())
            Jni.streamPlay(localDeviceId, 2, resource_id_0, dstDevId)
        }
        findViewById<Button>(R.id.btn_stop_record).setOnClickListener {
            val dstDevId = Integer.parseInt(edt_dst_id.text.toString())
            Jni.stopResource(0, dstDevId, resource_id_0, 0)
        }
    }

    private fun afterSysInitial() {
        Call.initAndCapture(0, Protocol.channel_screen)
        Call.initAndCapture(0, Protocol.channel_camera)
        Jni.initialResource(SdkVars.screen_width, SdkVars.screen_height, 0)
        Jni.initialResource(SdkVars.screen_width, SdkVars.screen_height, 1)
        Jni.initialResource(SdkVars.screen_width, SdkVars.screen_height, 2)
        Jni.initialResource(SdkVars.screen_width, SdkVars.screen_height, 3)
        Jni.initialResource(SdkVars.screen_width, SdkVars.screen_height, 4)
        //修改本机界面状态
        Jni.modPageStatus(InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_MainFace_VALUE)
        queryDeviceMeetInfo()
    }

    private fun queryDeviceMeetInfo() {
        var memberName = ""
        Jni.queryDeviceMeetInfo()?.let {
            memberName = it.membername.toStringUtf8()
        }
        tvMemberName.text = memberName
    }

    private fun applyPermissions() {
        XXPermissions.with(this)
            .permission(PermissionLists.getReadPhoneStatePermission())
            .permission(PermissionLists.getPostNotificationsPermission())
            .permission(PermissionLists.getRecordAudioPermission())
            .request { grantedList, deniedList ->

            }
    }

    private fun initConfigFile() {
        val currentTimeMillis = System.currentTimeMillis()
        FileUtils.createOrExistsDir(SdkVars.root_dir)
        val exists = FileUtils.isFileExists(SdkVars.root_dir + "client.ini")
        if (!exists) {
            ResourceUtils.copyFileFromAssets("client.ini", SdkVars.root_dir + "client.ini")
        }
        val file = File(SdkVars.root_dir + "client.ini")
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
                        + ",bitrate=" + SdkVars.bitrate
                        + ",frameRate=" + SdkVars.frameRate
                        + ",iframeInterval=" + SdkVars.iframeInterval
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
            IniUtil.configDir = SdkVars.root_dir
            IniUtil.mediaDir = SdkVars.root_dir + "mediadir" + File.separator
            IniUtil.store()
            edtIp.setText(ip)
            edtPort.setText(port)
        }
        FileUtils.delete(SdkVars.root_dir + "client.dev")
        ResourceUtils.copyFileFromAssets("client.dev", SdkVars.root_dir + "client.dev")
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
                                tvDevId.text = "$code(0x${Integer.toHexString(code)})"
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
                    afterSysInitial()
                }
            }
            // 设备寄存器
            Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE -> {
                val info = InterfaceDevice.pbui_Type_MeetDeviceBaseInfo.parseFrom(msg.data)
                LogUtils.e("设备寄存器 deviceid:${info.deviceid},attribid:${info.attribid},localDeviceId:$localDeviceId")
                //寄存器id 0:net status  50:res status  63:base info
                if (info.deviceid == localDeviceId) {
                    if (info.attribid == 0) {
                        updateOnLineStatus()
                    } else if (info.attribid == 63) {
                        updateDeviceName()
                    }
                }
            }
            // 媒体播放
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE -> {
                InterfacePlaymedia.pbui_Type_MeetMediaPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_MEETFILE_PUSH_FLAG_FORCEMODE_VALUE
                    val type = it.mediaid and MAIN_TYPE_BITMASK.toInt()
                    val subType = it.mediaid and SUB_TYPE_BITMASK
                    if (type == MEDIA_FILE_TYPE_AUDIO
                        || type == MEDIA_FILE_TYPE_VIDEO
                        || type == MEDIA_FILE_TYPE_RECORD
                    ) {
                        DecodeQueue.cleanup(it.res)
                        if (it.res == 0) {
                            startActivity(Intent(this, ControlViewActivity::class.java).apply {
                                putExtra("type", Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE)
                                putExtra("isMandatory", isMandatory)
                                putExtra("createdeviceid", it.createdeviceid)
                                putExtra("mediaid", it.mediaid)
                            })
                        }
                    }
                }
            }
            // 流播放
            Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE -> {
                InterfaceStream.pbui_Type_MeetStreamPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_MEETFILE_PUSH_FLAG_FORCEMODE_VALUE
                    DecodeQueue.cleanup(it.res)
                    if (it.res == 0) {
                        startActivity(Intent(this, ControlViewActivity::class.java).apply {
                            putExtra("type", Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE)
                            putExtra("isMandatory", isMandatory)
                            putExtra("createdeviceid", it.createdeviceid)
                            putExtra("deviceid", it.deviceid)
                            putExtra("subid", it.subid)
                        })
                    }
                }
            }
            //设备会议信息
            Pb_TYPE_MEET_INTERFACE_DEVICEFACESHOW_VALUE->{
                queryDeviceMeetInfo()
            }

            BusType.capture_start -> {
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                applyScreenRecorder.launch(mediaProjectionManager.createScreenCaptureIntent())
            }

            BusType.capture_stop -> {
                stopService(Intent(this, ForegroundService::class.java))
            }
        }
    }

    private fun updateDeviceName() {
        tvDevName.text = Jni.queryDeviceNameById(localDeviceId)
    }

    private fun updateOnLineStatus() {
        tvOnline.text = if (Jni.isOnline(localDeviceId)) "在线" else "离线"
    }

    override fun onStart() {
        super.onStart()
        LogUtils.i("onStart: ")
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onStop() {
        super.onStop()
        LogUtils.i("onStop: ")
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}