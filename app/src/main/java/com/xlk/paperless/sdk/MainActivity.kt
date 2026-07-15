package com.xlk.paperless.sdk

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
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
import com.paperless.bus.EventBusMessage
import com.paperless.bus.SdkBusType
import com.paperless.data.repository.base.DataRepositoryManager
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
import com.paperless.sdk.SdkConfig
import com.paperless.sdk.SdkVars
import com.paperless.sdk.SdkVars.Companion.localDeviceId
import com.paperless.util.IniUtil
import com.xlk.paperless.sdk.databinding.ActivityMainBinding
import com.xlk.paperless.sdk.helper.AppNetworkMonitor
import com.xlk.paperless.sdk.screen.ScreenRecordService
import com.xlk.paperless.sdk.service.ScreenShareService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import kotlin.system.exitProcess
import kotlin.toString

class MainActivity : AppCompatActivity() {
    lateinit var mBinding: ActivityMainBinding
    private var mTvNetworkSpeed: TextView? = null

    var networkMonitor: AppNetworkMonitor? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity()
            exitProcess(0)
        }
    }

    private val applyScreenRecorder = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result != null) {
            if (result.resultCode == RESULT_OK) {

                val intent = Intent(this, ScreenShareService::class.java).apply {
                    action = ScreenShareService.ACTION_START
                    putExtra(ScreenShareService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenShareService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }

//                launchService(result.resultCode, result.data!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        viewEvent()
        EventBus.getDefault().register(this)
        applyPermissions()
        checkUsageStatsPermission()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        Thread {
            initConfigFile()
            Jni.initialization(
                InterfaceMacro.Pb_ProgramType.Pb_MEET_PROGRAM_TYPE_MEETCLIENT_VALUE,
                SdkVars.root_dir + "client.ini", DeviceUtils.getUniqueDeviceId(), 4, 0
            )
        }.start()
    }

    private fun viewEvent() {
        mBinding.apply {
            //修改配置并重启应用
            btnModify.setOnClickListener {
                IniUtil.loadFile(SdkVars.root_dir + "client.ini")
                IniUtil.ip = edtIp.text.toString()
                IniUtil.port = edtPort.text.toString()
                IniUtil.store()
                AppUtils.relaunchApp(true)
            }
            //网络监控
            btnNetwork.setOnClickListener {
                networkSpeedWindow()
            }
            //修改界面状态
            btnPage.setOnClickListener {
                //InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_MemFace_VALUE
                val pageCode = Integer.parseInt(edtPageCode.text.toString())
                Jni.modPageStatus(pageCode)
            }
            //播放页面
            btnPlayPage.setOnClickListener {
                startActivity(Intent(this@MainActivity, ControlViewActivity::class.java))
            }
            //四分屏播放页面
            btnSplit.setOnClickListener {
                startActivity(Intent(this@MainActivity, SplitPlayActivity::class.java))
            }
            //缓存数据
            btnCacheData.setOnClickListener {
                Jni.cache(edtType.text.toString().toInt(), edtCacheId.text.toString().toInt())
            }
            //下载文件
            btnDownloadMedia.setOnClickListener {
                val str = edtMediaId.text.toString()
                val id = Integer.parseInt(str)
                val fileName = Jni.queryFileName(id)
                val filePath = cacheDir.absolutePath + File.separator + fileName
                Jni.downloadFile(id, filePath, "")
            }
            //播放媒体文件
            btnPlayMedia.setOnClickListener {
                val str = edtMediaId.text.toString()
                val id = Integer.parseInt(str)
                val temp = mutableListOf<Int>()
                if (id0.isChecked) temp.add(0)
                if (id1.isChecked) temp.add(1)
                if (id2.isChecked) temp.add(2)
                if (id3.isChecked) temp.add(3)
                if (id4.isChecked) temp.add(4)
                Jni.mediaPlay(
                    temp,
                    id,
                    localDeviceId,
                    0,
                    0,
                    if (cbMandatory.isChecked) InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE else 0
                )
            }
            //播放终端屏幕
            btnStreamPlay.setOnClickListener {
                val temp = mutableListOf<Int>()
                if (id0.isChecked) temp.add(0)
                if (id1.isChecked) temp.add(1)
                if (id2.isChecked) temp.add(2)
                if (id3.isChecked) temp.add(3)
                if (id4.isChecked) temp.add(4)
                val str = edtDeviceId.text.toString()
                val id = Integer.parseInt(str)
                Jni.streamPlay(
                    id,
                    2,
                    temp,
                    localDeviceId,
                    0,
                    if (cbMandatory.isChecked) InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE else 0
                )
            }
            //开始同屏
            btnStartRecord.setOnClickListener {
                val w = Integer.parseInt(edtRecordWidth.text.toString())
                val h = Integer.parseInt(edtRecordHeight.text.toString())
                val bitrate = Integer.parseInt(edtBitrate.text.toString())
                val framerate = Integer.parseInt(edtFramerate.text.toString())
                val i = Integer.parseInt(edtIFrameInterval.text.toString())
                SdkVars.record_width = w
                SdkVars.record_height = h
                SdkVars.bitrate = bitrate * 1000
                SdkVars.frameRate = framerate
                SdkVars.iframeInterval = i

                val dstDevId = Integer.parseInt(edtDstId.text.toString())
                Jni.streamPlay(localDeviceId, 2, resource_id_0, dstDevId)
            }
            //结束同屏
            btnStopRecord.setOnClickListener {
                val dstDevId = Integer.parseInt(edtDstId.text.toString())
                Jni.stopResource(0, dstDevId, resource_id_0, 0)
            }
            //本机黑名单目录
            btnDirBlackList.setOnClickListener {
                val sb = StringBuilder()
                sb.append("无权限目录：")
                Jni.queryDir()?.let {
                    it.itemList.forEach {
                        if (Jni.isNoDirPermission(it.id, SdkVars.localMemberId)) {
                            sb.append("\n").append(it.name.toStringUtf8())
                        }
                    }
                }
                LogUtils.d(sb.toString())
                tvShowBlackList.text = sb.toString()
            }
            //本机黑名单文件
            btnFileBlackList.setOnClickListener {
                val sb = StringBuilder()
                sb.append("无权限文件：")
                Jni.queryDir()?.let {
                    it.itemList.forEach {
                        Jni.queryFile(it.id)?.let {
                            it.forEach {
                                if (Jni.isNoFilePermission(it.mediaid, SdkVars.localMemberId)) {
                                    sb.append("\n").append(it.name.toStringUtf8())
                                }
                            }
                        }
                    }
                }
                LogUtils.d(sb.toString())
                tvShowBlackList.text = sb.toString()
            }
            btnTest.setOnClickListener {
                dataRepositoryListener()
            }
        }
    }

    private fun networkSpeedWindow() {
        if (mTvNetworkSpeed != null) {
            closeNetworkMonitorWindow()
            return
        }
        windowManager!!.defaultDisplay.width
        windowManager!!.defaultDisplay.height
        val metrics = DisplayMetrics()
        windowManager!!.defaultDisplay.getMetrics(metrics)
        val params = WindowManager.LayoutParams()
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // 加上这句话悬浮窗不拦截事件
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) { //8.0新特性
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            params.type = WindowManager.LayoutParams.TYPE_PHONE //总是出现在应用程序窗口之上
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT //总是出现在应用程序窗口之上
        }
        params.format = PixelFormat.RGBA_8888
        params.gravity = Gravity.START or Gravity.TOP
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT
        params.x = 0
        params.y = metrics.heightPixels - params.height
        mTvNetworkSpeed = TextView(this)
        mTvNetworkSpeed!!.setTextColor(Color.argb(200, 255, 255, 255))
        mTvNetworkSpeed!!.setBackgroundColor(Color.argb(50, 0, 0, 0))
        windowManager!!.addView(mTvNetworkSpeed, params)

        networkMonitor = AppNetworkMonitor(this)
        networkMonitor!!.startMonitoring(object : AppNetworkMonitor.NetworkInfoListener {
            override fun onNetworkInfoUpdated(totalBytes: Long, downloadSpeed: Long, uploadSpeed: Long) {
                runOnUiThread {
                    val msg = (/*"总流量: " + AppNetworkMonitor.formatTraffic(totalBytes)
                            + "\n" +*/ "下载: " + AppNetworkMonitor.formatSpeed(downloadSpeed)
                            + "\n" + "上传: " + AppNetworkMonitor.formatSpeed(uploadSpeed))
                    LogUtils.e("onNetworkInfoUpdated: $msg")
                    mTvNetworkSpeed?.text = msg
                }
            }

            override fun onSessionTrafficUpdated(sessionBytes: Long) {
                LogUtils.d(
                    "onSessionTrafficUpdated 本屏流量: " + AppNetworkMonitor.formatTraffic(sessionBytes)
                )
            }
        })
    }

    // 检查并请求权限
    private fun checkUsageStatsPermission() {
        val appOps: AppOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
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

        DataRepositoryManager.init()
        DataRepositoryManager.refreshAll()

//        dataRepositoryListener()
    }

    private fun dataRepositoryListener() {
        DataRepositoryManager.deviceMeetInfoRepository.data.observe(this) {
            SdkVars.localMeetingId = it?.meetingid ?: 0
            SdkVars.localMeetingName = it?.meetingname?.toStringUtf8() ?: ""
            SdkVars.localMemberId = it?.memberid ?: 0
            SdkVars.localMemberName = it?.membername?.toStringUtf8() ?: ""
            SdkVars.localRoomId = it?.roomid ?: 0
            mBinding.tvMemberId.text = "人员id：${SdkVars.localMemberId}"
            mBinding.tvMemberName.text = "人员名称：${SdkVars.localMemberName}"
            mBinding.tvMeetingId.text = "会议id：${SdkVars.localMeetingId}"
            mBinding.tvMeetingName.text = "会议名称：${SdkVars.localMeetingName}"
        }
        DataRepositoryManager.roomRepository.data.observe(this) {
            LogUtils.i("roomRepository: ${it.size}")
        }
        DataRepositoryManager.memberDetailRepository.data.observe(this) {
            LogUtils.i("memberDetailRepository: ${it.size}")
        }
        DataRepositoryManager.deviceInfoRepository.data.observe(this) {
            LogUtils.i("deviceInfoRepository: ${it.size}")
        }
        DataRepositoryManager.meetingInfoRepository.data.observe(this) {
            LogUtils.i("meetingInfoRepository: ${it.size}")
        }
        DataRepositoryManager.agendaRepository.data.observe(this) {
            LogUtils.i("agendaRepository: ${it.size}")
        }
        DataRepositoryManager.bulletinRepository.data.observe(this) {
            LogUtils.i("bulletinRepository: ${it.size}")
        }
        DataRepositoryManager.directoryRepository.data.observe(this) {
            LogUtils.i("directoryRepository: ${it.size}")
        }
        DataRepositoryManager.directoryFileRepository.data.observe(this){
            LogUtils.i("directoryFileRepository: ${it.size}")
        }
        DataRepositoryManager.voteRepository.data.observe(this) {
            LogUtils.i("voteRepository: ${it.size}")
        }
        DataRepositoryManager.signInRepository.data.observe(this) {
            LogUtils.i("signInRepository: ${it.size}")
        }
        DataRepositoryManager.adminRepository.data.observe(this) {
            LogUtils.i("adminRepository: ${it.size}")
        }
        DataRepositoryManager.peopleRepository.data.observe(this) {
            LogUtils.i("peopleRepository: ${it.size}")
        }
        DataRepositoryManager.memberPermissionRepository.data.observe(this) {
            LogUtils.i("memberPermissionRepository: ${it.size}")
        }
        DataRepositoryManager.tableCardRepository.data.observe(this) {
            LogUtils.i("tableCardRepository: ${it.size}")
        }
        DataRepositoryManager.functionConfigRepository.data.observe(this) {
            LogUtils.i("functionConfigRepository: ${it.size}")
        }
        DataRepositoryManager.newVoteRepository.data.observe(this) {
            LogUtils.i("newVoteRepository: ${it.size}")
        }

        DataRepositoryManager.interfaceConfigRepository.data.observe(this) {
            LogUtils.i("interfaceConfigRepository: data:${it?.size}")
        }
        DataRepositoryManager.interfaceConfigRepository.mainFilePath.observe(this) {
            LogUtils.i("interfaceConfigRepository: mainFilePath:${it}")
        }
        DataRepositoryManager.interfaceConfigRepository.subFilePath.observe(this) {
            LogUtils.i("interfaceConfigRepository: subFilePath:${it}")
        }
        DataRepositoryManager.interfaceConfigRepository.logoFilePath.observe(this) {
            LogUtils.i("interfaceConfigRepository: logoFilePath:${it}")
        }
        DataRepositoryManager.interfaceConfigRepository.bulletinBgFilePath.observe(this) {
            LogUtils.i("interfaceConfigRepository: bulletinBgFilePath:${it}")
        }
        DataRepositoryManager.interfaceConfigRepository.bulletinLogoFilePath.observe(this) {
            LogUtils.i("interfaceConfigRepository: bulletinLogoFilePath:${it}")
        }
        DataRepositoryManager.interfaceConfigRepository.companyName.observe(this) {
            LogUtils.i("interfaceConfigRepository: companyName:${it}")
        }
        //本机设备名称
        DataRepositoryManager.deviceInfoRepository.devName.observe(this) {
            LogUtils.i("deviceInfoRepository: devName:${it}")
            mBinding.tvDevName.text = it ?: ""
        }
        //本机在线状态
        DataRepositoryManager.deviceInfoRepository.online.observe(this) {
            LogUtils.i("deviceInfoRepository: online:${it}")
            mBinding.tvOnline.text = if (it) "在线" else "离线"
        }
    }

    private suspend fun repositoryFlow() {
        DataRepositoryManager.roomRepository.dataFlow().collect {
            LogUtils.i("repositoryFlow: roomRepository:${it.size}")
        }
        DataRepositoryManager.memberDetailRepository.dataFlow().collect {
            LogUtils.i("repositoryFlow: memberDetailRepository:${it.size}")
        }
    }

    private fun applyPermissions() {
        XXPermissions.with(this)
            .permission(PermissionLists.getReadPhoneStatePermission())
            .permission(PermissionLists.getPostNotificationsPermission())
            .permission(PermissionLists.getRecordAudioPermission())
            .permission(PermissionLists.getSystemAlertWindowPermission())
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
            runOnUiThread {
                mBinding.edtIp.setText(ip)
                mBinding.edtPort.setText(port)
            }
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
                                mBinding.tvDevId.text = "$code(0x${Integer.toHexString(code)})"
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
            // 媒体播放
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE -> {
                if (SdkConfig.floatingPlayEnable) return
                InterfacePlaymedia.pbui_Type_MeetMediaPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    val type = it.mediaid and MAIN_TYPE_BITMASK.toInt()
                    val subType = it.mediaid and SUB_TYPE_BITMASK
                    if (type == MEDIA_FILE_TYPE_AUDIO
                        || type == MEDIA_FILE_TYPE_VIDEO
                        || type == MEDIA_FILE_TYPE_RECORD
                    ) {
                        DecodeQueue.cleanup(it.res)
                        if (it.res == 0) {
                            ControlViewActivity.jump(this, Bundle().apply {
                                putInt("type", Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE)
                                putBoolean("isMandatory", isMandatory)
                                putInt("createdeviceid", it.createdeviceid)
                                putInt("mediaid", it.mediaid)
                            })
                        }
                    }
                }
            }
            // 流播放
            Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE -> {
                if (SdkConfig.floatingPlayEnable) return
                InterfaceStream.pbui_Type_MeetStreamPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    DecodeQueue.cleanup(it.res)
                    if (it.res == 0) {
                        ControlViewActivity.jump(this, Bundle().apply {
                            putInt("type", Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE)
                            putBoolean("isMandatory", isMandatory)
                            putInt("createdeviceid", it.createdeviceid)
                            putInt("deviceid", it.deviceid)
                            putInt("subid", it.subid)
                        })
                    }
                }
            }

            SdkBusType.capture_start -> {
                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                applyScreenRecorder.launch(mediaProjectionManager.createScreenCaptureIntent())
            }

            SdkBusType.capture_stop -> {
//                mRecordService?.stopRecording()
//                stopService(Intent(this, ForegroundService::class.java))
                stopService(Intent(this, ScreenShareService::class.java).apply {
                    setAction(ScreenShareService.ACTION_STOP)
                })
            }

            SdkBusType.floating_same_play_progress -> {
                val pos = msg.obj as Int
                LogUtils.i("busEvent: 同步进度：$pos")
            }

            SdkBusType.floating_start_screen_share -> {
                LogUtils.i("busEvent: 同屏 ${msg.objs?.size}")
                msg.objs?.forEachIndexed { index, any ->
                    LogUtils.i("busEvent: index:$index , any:${any as Int}")
                }
            }

            SdkBusType.floating_stop_screen_share -> {}
        }
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

    private fun closeNetworkMonitorWindow() {
        windowManager?.removeView(mTvNetworkSpeed)
        mTvNetworkSpeed = null
        networkMonitor?.stopMonitoring()
        networkMonitor = null
    }

    override fun onDestroy() {
        stopService(Intent(this, ScreenShareService::class.java).apply {
            setAction(ScreenShareService.ACTION_STOP)
        })
        super.onDestroy()
        closeNetworkMonitorWindow()
    }

    override fun onBackPressed() {

    }
}
