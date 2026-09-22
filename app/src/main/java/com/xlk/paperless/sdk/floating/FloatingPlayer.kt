package com.xlk.paperless.sdk.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEMEETSTATUS_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMember
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfaceStop
import com.mogujie.tt.protobuf.InterfaceStream
import com.paperless.bus.EventBusMessage
import com.paperless.bus.SdkBusType
import com.paperless.player.DecodeQueue
import com.paperless.player.Fps
import com.paperless.player.PlayerController
import com.paperless.player.controller.PlayerControlView
import com.paperless.player.controller.listener.ControlCallback
import com.paperless.sdk.BaseJni
import com.paperless.sdk.MAIN_TYPE_BITMASK
import com.paperless.sdk.MEDIA_FILE_TYPE_AUDIO
import com.paperless.sdk.MEDIA_FILE_TYPE_RECORD
import com.paperless.sdk.MEDIA_FILE_TYPE_VIDEO
import com.paperless.sdk.SUB_TYPE_BITMASK
import com.paperless.sdk.SdkVars
import com.paperless.sdk.isProjector
import com.paperless.util.PlayerLog
import com.xlk.paperless.sdk.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 悬浮窗播放器（由 hengxun 的 com.xlk.paperless.hengxun.server.floating.FloatingPlayer 移植）
 *
 * 与原版的差异：
 * 1. 包名/R 指向 SDK app 模块（com.xlk.paperless.sdk.floating / com.xlk.paperless.sdk.R）
 * 2. `Macro.isForceFullScreen` 改为可配置项 `configure(..., isForceFullScreen = ...)`，
 *    不再依赖 hengxun 的全局宏，方便在示例工程里直接对比 true/false 两种表现
 * 3. 同屏选择弹窗由 EasyFloat 改为原生 WindowManager（少一个三方依赖）
 * 4. 参会人/会场设备列表用内置的轻量 RecyclerView.Adapter（原来在 hengxun 的 MemberAdapter/ProjectAdapter）
 * 5. JniHelper 改为 SDK 的 BaseJni，JNI 调用签名按 SDK 的 BaseJni 适配
 *
 *  @author : Administrator
 *  created on 2026/7/1 14:45
 */
class FloatingPlayer private constructor(val context: Context) {
    companion object {
        private const val MIN_SIZE_RATIO = 1 / 3f
        private const val DRAG_BAR_HEIGHT_DP = 40
        private const val RESIZE_HOTSPOT_SIZE_DP = 36
        private const val TAG = "FloatingPlayer"

        //<editor-fold desc="日志链路标签（配合 PlayerLog 使用，过滤：adb logcat -s PlayWin）">
        private const val L_WINDOW = "窗口"
        private const val L_EVENT = "事件"
        private const val L_CONTROL = "控制"
        private const val L_SCREEN = "同屏"
        private const val L_RENDER = "渲染"
        //</editor-fold>

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FloatingPlayer? = null

        fun getInstance(context: Context): FloatingPlayer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingPlayer(context.applicationContext).also {
                    INSTANCE = it
                    PlayerLog.i(L_WINDOW, "创建 FloatingPlayer 单例（应用上下文）")
                }
            }
        }

        // 在 App 退出时调用，避免单例长期持有
        fun destroyInstance() {
            PlayerLog.i(L_WINDOW, "销毁 FloatingPlayer 单例")
            INSTANCE?.onDestroy()
            INSTANCE = null
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var floatingView: View? = null
    private var playerControlView: PlayerControlView? = null
    private var playerController: PlayerController? = null
    private var isShowing = false
    private var hasNewPlay = false

    // 窗口参数
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.OPAQUE
        gravity = Gravity.TOP or Gravity.START

        width = screenSize().x
        height = screenSize().y
        x = 0
        y = 0
    }

    private val screenSize: Point by lazy { screenSize() }

    // 半屏尺寸
    private var halfWidth = (screenSize.x * 2 / 3).coerceAtLeast((screenSize.x * MIN_SIZE_RATIO).toInt())
    private var halfHeight = (screenSize.y * 2 / 3).coerceAtLeast((screenSize.y * MIN_SIZE_RATIO).toInt())
    private var isFullscreen = true

    // 触摸状态
    private var isDragging = false
    private var isResizing = false
    private var dragStartX = 0
    private var dragStartY = 0
    private var resizeStartWidth = 0
    private var resizeStartHeight = 0
    private var resizeStartX = 0
    private var resizeStartY = 0
    private val jni = BaseJni()

    // 当前播放信息
    var currentDeviceId = 0
    var currentSubId = 0
    var currentMediaId = 0
    var currentProgress = 0
    var currentSec = 0

    private var mExitFloatingPlayListener: ExitFloatingPlayListener? = null

    interface ExitFloatingPlayListener {
        fun exitFloatingPlayListener()
    }

    fun setExitFloatingPlayListener(listener: ExitFloatingPlayListener?) {
        mExitFloatingPlayListener = listener
    }

    private var curResId = 0

    // 窗口大小切换开关
    private var sizeToggleEnabled = true

    // 右下角拖动缩放窗口
    private var resizeHandleEnabled = true

    // 按比例缩放（= !isForceFullScreen）
    private var scaleProportionally = true

    /**
     * 是否强制全屏播放（对应 hengxun 的 Macro.isForceFullScreen）
     * - true：画面拉伸铺满窗口，不做等比适配
     * - false：按视频源宽高比等比适配，窗口/全屏切换时重新计算
     */
    private var isForceFullScreen = false

    // 解码器回调中的视频显示尺寸（已包含旋转方向）
    @Volatile
    private var videoDisplayWidth = 0

    @Volatile
    private var videoDisplayHeight = 0

    // 同屏选择弹窗
    private var screenPopView: View? = null
    private var screenPopShown = false

    //<editor-fold desc="日志链路辅助（不参与业务逻辑）">
    /** 上一次已结束的播放会话 ID，便于在窗口销毁后回溯是哪一次播放 */
    private var lastSessionId = PlayerLog.NO_SESSION

    /** 当前窗口实例的编号，同一个单例内每开一次窗 +1，用于区分「同一个 session 内的多次 show」 */
    private var windowSeq = 0

    /** 拖拽/缩放过程日志节流 */
    private val dragLogCounter = PlayerLog.ThrottleCounter()
    private val resizeLogCounter = PlayerLog.ThrottleCounter()

    /** 设备/人员变更通知日志节流（平台会短时间连发上百条，必须限制） */
    private val deviceNotifyLogCounter = PlayerLog.ThrottleCounter()

    /** 当前是否处于播放会话中（窗口需要或正在显示） */
    private fun inPlaySession() = isShowing || playerController != null || playerControlView != null
    //</editor-fold>

    val tempMembers: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo> = mutableListOf()
    val onlineMembers: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo> = mutableListOf()
    val onlineProjects: MutableList<InterfaceDevice.pbui_Item_DeviceDetailInfo> = mutableListOf()
    val memberAdapter: MemberAdapter = MemberAdapter(onlineMembers)
    var projectAdapter: ProjectAdapter = ProjectAdapter(onlineProjects)
    val toggleDevIds: MutableList<Int> = mutableListOf()

    // 推荐在 initial() 调用前完成配置
    fun configure(
        curResId: Int = 0,
        sizeToggleEnabled: Boolean = true,
        resizeHandleEnabled: Boolean = true,
        scaleProportionally: Boolean = true,
        isForceFullScreen: Boolean = false
    ) {
        this.curResId = curResId
        this.sizeToggleEnabled = sizeToggleEnabled
        this.resizeHandleEnabled = resizeHandleEnabled
        // 兼容两种用法：既可以直接指定 scaleProportionally，也可以用 isForceFullScreen 反推
        this.scaleProportionally = if (isForceFullScreen) false else scaleProportionally
        this.isForceFullScreen = isForceFullScreen
        PlayerLog.i(
            L_WINDOW,
            "configure 播放窗口配置: curResId=$curResId 窗口大小切换=$sizeToggleEnabled " +
                    "右下角缩放=$resizeHandleEnabled 等比适配=$scaleProportionally " +
                    "强制全屏=$isForceFullScreen 屏幕=${screenSize.x}x${screenSize.y}"
        )
        LogUtils.i(
            TAG,
            "configure: curResId=$curResId,sizeToggle=$sizeToggleEnabled," +
                    "resizeHandle=$resizeHandleEnabled,scaleProportionally=$scaleProportionally," +
                    "isForceFullScreen=$isForceFullScreen"
        )
    }

    fun initial() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
            PlayerLog.i(L_EVENT, "initial: 已注册 EventBus 播放事件监听")
        } else {
            PlayerLog.w(L_EVENT, "initial: EventBus 已注册，跳过重复注册")
        }
    }

    private var delayQueryDeviceTask = Runnable { queryOnlineDev() }

    private fun delayQueryDeviceTask() {
        handler.removeCallbacks(delayQueryDeviceTask)
        handler.postDelayed(delayQueryDeviceTask, 1000L)
    }

    /** 待执行的延迟销毁任务，收到新的播放请求时需要撤销 */
    private var pendingDismiss: Runnable? = null

    private fun queryOnlineDev() {
        tempMembers.clear()
        onlineMembers.clear()
        onlineProjects.clear()
        if (!jni.checkCache(Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE)) {
            jni.cache(Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE)
        }
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE)) {
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE)
        }
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM_VALUE)) {
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM_VALUE)
        }
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE)) {
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE)
        }
        jni.queryMemberDetail()?.let {
            tempMembers.addAll(it.itemList)
        }
        jni.queryDevice()?.let {
            it.pdevList.filter { dev -> dev.netstate == 1 }.forEach { dev ->
                if (dev.devcieid.isProjector()) {
                    onlineProjects.add(dev)
                } else {
                    if (dev.devcieid != SdkVars.localDeviceId && dev.facestate == 1 &&
                        dev.meetingid == SdkVars.localMeetingId
                    ) {
                        tempMembers.find { dev.devcieid == it.devid }?.let {
                            onlineMembers.add(it)
                        }
                    }
                }
            }
        }
        memberAdapter.updateData(onlineMembers)
        projectAdapter.updateData(onlineProjects)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
            // 设备、参会人、设备会议
            Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE,
            Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE,
            Pb_TYPE_MEET_INTERFACE_DEVICEMEETSTATUS_VALUE -> {
                if (PlayerLog.shouldLog(deviceNotifyLogCounter, 2000L)) {
                    PlayerLog.d(
                        L_EVENT,
                        "收到设备/人员变更通知 type=${msg.type}（2 秒内累计 ${deviceNotifyLogCounter.count} 次，已节流），" +
                                "1 秒后刷新同屏列表"
                    )
                }
                LogUtils.i(TAG, "busEvent:变更通知更新 ${msg.type}")
                delayQueryDeviceTask()
            }
            //接收的帧数
            SdkBusType.fps -> {
                val fps = msg.objs?.get(0) as Int
                val resId = msg.objs?.get(1) as Int
                if (resId == curResId) {
                    updateFps(fps)
                }
            }
            // 媒体播放
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE -> {
                InterfacePlaymedia.pbui_Type_MeetMediaPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    val type = it.mediaid and MAIN_TYPE_BITMASK.toInt()
                    PlayerLog.i(
                        L_EVENT,
                        "收到媒体播放通知 res=${it.res} mediaid=${it.mediaid} 类型=$type " +
                                "强制播放(平台弹窗)=$isMandatory triggeruserval=${it.triggeruserval}"
                    )
                    if (type == MEDIA_FILE_TYPE_AUDIO
                        || type == MEDIA_FILE_TYPE_VIDEO
                        || type == MEDIA_FILE_TYPE_RECORD
                    ) {
                        if (it.res == 0) {
                            DecodeQueue.cleanup(it.res)
                            currentMediaId = it.mediaid
                            val fileName = jni.queryFileName(it.mediaid)
                            PlayerLog.i(
                                L_EVENT,
                                "媒体播放走播放窗口 res=${it.res} 文件名=$fileName mediaId=${it.mediaid}"
                            )
                            showPlayerWindow(true, fileName, isMandatory, it.res)
                        } else {
                            PlayerLog.w(
                                L_EVENT,
                                "媒体播放资源 res=${it.res} 不属于当前播放窗口(curResId=$curResId)，忽略"
                            )
                        }
                    } else {
                        PlayerLog.d(L_EVENT, "媒体类型 $type 不需要播放窗口，忽略")
                    }
                }
            }
            // 流播放
            Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE -> {
                InterfaceStream.pbui_Type_MeetStreamPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    PlayerLog.i(
                        L_EVENT,
                        "收到流播放通知 res=${it.res} deviceid=${it.deviceid} subid=${it.subid} " +
                                "强制播放(平台弹窗)=$isMandatory triggeruserval=${it.triggeruserval}"
                    )
                    if (it.res == 0) {
                        DecodeQueue.cleanup(it.res)
                        currentDeviceId = it.deviceid
                        currentSubId = it.subid
                        val devName = jni.queryDeviceNameById(it.deviceid)
                        PlayerLog.i(
                            L_EVENT,
                            "流播放走播放窗口 res=${it.res} 设备名=$devName deviceId=${it.deviceid} subId=${it.subid}"
                        )
                        showPlayerWindow(false, devName, isMandatory, it.res)
                    } else {
                        PlayerLog.w(
                            L_EVENT,
                            "流播放资源 res=${it.res} 不属于当前播放窗口(curResId=$curResId)，忽略"
                        )
                    }
                }
            }

            //平台播放进度通知
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE -> {
                InterfacePlaymedia.pbui_Type_PlayPosCb.parseFrom(msg.data)?.let {
                    PlayerLog.d(
                        L_EVENT,
                        "收到平台播放进度 resId=${it.resId} mediaId=${it.mediaId} 进度=${it.per}% 秒=${it.sec} 状态=${it.status}"
                    )
                    if (it.resId == 0) {
                        currentMediaId = it.mediaId
                        updateTitle(jni.queryFileName(it.mediaId))
                        val curTotalMs = jni.queryFileVideoTime(it.mediaId)
                        if (it.status == 0) {
                            currentProgress = it.per
                            currentSec = it.sec
                            setProgressAndTime(
                                it.per.toLong(), it.per.toLong(),
                                it.sec * 1000L, curTotalMs * 1L, false
                            )
                        }
                    }
                }
            }
            //流播放停止
            Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE -> {
                if (msg.method == Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopResWork.parseFrom(msg.data)?.let {
                        PlayerLog.i(L_EVENT, "收到流播放停止资源通知(CLOSE) res列表=${it.resList}")
                        it.resList.forEach { resId ->
                            LogUtils.e(TAG, "流播放停止资源通知 $resId")
                            Fps.clear(resId)
                            if (resId == 0) {
                                delayDismiss()
                            }
                        }
                    }
                } else if (msg.method == Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopPlay.parseFrom(msg.data)?.let {
                        PlayerLog.i(
                            L_EVENT,
                            "收到流播放停止通知(NOTIFY) res=${it.res} createdeviceid=${it.createdeviceid} " +
                                    "triggerid=${it.triggerid}"
                        )
                        LogUtils.i(
                            TAG,
                            "流播放停止通知: res[${it.res}] createdeviceid[${it.createdeviceid}] triggerid[${it.triggerid}]"
                        )
                        if (it.res == 0) {
                            Fps.clear(it.res)
                            delayDismiss()
                        }
                    }
                } else {
                    PlayerLog.d(L_EVENT, "收到停止播放相关通知 method=${msg.method}（未处理的停止类型）")
                }
            }
        }
    }

    fun showPlayerWindow(
        isMedia: Boolean = true,
        title: String = "",
        isMandatory: Boolean = false,
        resid: Int = curResId
    ) {
        PlayerLog.i(
            L_WINDOW,
            "showPlayerWindow 请求: #${windowSeq + 1} 媒体=$isMedia 标题=$title 强制播放=$isMandatory " +
                    "resid=$resid 当前窗口显示中=$isShowing"
        )
        hasNewPlay = true
        // 新的播放请求到来，撤销上一次「延迟销毁」，避免刚开的窗口被上一条停止通知关掉
        if (pendingDismiss != null) {
            handler.removeCallbacks(pendingDismiss!!)
            pendingDismiss = null
            PlayerLog.i(L_WINDOW, "showPlayerWindow: 已撤销上一次待执行的延迟销毁任务（新播放到来）")
        }
        if (isShowing) {
            // 复用已有窗口：只更新标题与播放模式标记
            if (!isMedia && currentDeviceId != 0 && currentSubId != 0) {
                // 流播放切换时同步记录，便于日志核对
                PlayerLog.i(
                    L_WINDOW,
                    "复用已有播放窗口，切换为流播放 deviceId=$currentDeviceId subId=$currentSubId"
                )
            } else {
                PlayerLog.i(L_WINDOW, "复用已有播放窗口，仅更新标题与播放标记")
            }
            updateTitle(title)
            playerControlView?.setupPlayFlag(
                if (isMedia) {
                    if (isMandatory) {
                        PlayerControlView.video_flag.or(PlayerControlView.mandatory_flag)
                    } else {
                        PlayerControlView.video_flag
                    }
                } else {
                    if (isMandatory) {
                        PlayerControlView.stream_flag.or(PlayerControlView.mandatory_flag)
                    } else {
                        PlayerControlView.stream_flag
                    }
                }
            )
        } else {
            PlayerLog.step(L_WINDOW, "窗口未显示，开始创建新的播放会话")
            delayQueryDeviceTask()
            val surfaceView = SurfaceView(context)
            PlayerLog.d(L_WINDOW, "已创建 SurfaceView 实例，交给 show() 绑定解码器")
            show(surfaceView, title, isMedia, isMandatory, resid)
        }
    }

    fun show(
        surfaceView: SurfaceView,
        title: String,
        isMedia: Boolean = true,
        isMandatory: Boolean = false,
        resid: Int = curResId
    ) {
        if (isShowing) {
            PlayerLog.w(L_WINDOW, "show: 窗口已在显示中，忽略本次调用")
            return
        }
        // 开启新的日志会话：本条链路上的所有日志都会带上同一个 Sxx 标记
        val session = PlayerLog.openSession()
        windowSeq++
        PlayerLog.i(
            L_WINDOW,
            "===== 播放窗口创建开始 #$windowSeq 会话=S$session 类型=${if (isMedia) "媒体文件" else "流媒体"} " +
                    "标题=$title 强制播放=$isMandatory resid=$resid " +
                    "等比适配=$scaleProportionally 强制全屏=$isForceFullScreen"
        )
        PlayerLog.i(
            L_WINDOW,
            "窗口参数: 尺寸=${layoutParams.width}x${layoutParams.height} 位置=(${layoutParams.x},${layoutParams.y}) " +
                    "type=${layoutParams.type} flags=${layoutParams.flags} format=${layoutParams.format}"
        )
        PlayerLog.i(
            L_WINDOW,
            "设备上下文: localDeviceId=${SdkVars.localDeviceId} localMeetingId=${SdkVars.localMeetingId} " +
                    "currentMediaId=$currentMediaId currentDeviceId=$currentDeviceId currentSubId=$currentSubId"
        )
        playerController = PlayerController(resid, onSurfaceReady = {
            PlayerLog.i(L_WINDOW, "Surface 已就绪，解码即将开始（窗口层回调）")
            LogUtils.i(TAG, "Surface ready, decoding started")
        }).apply {
            initialize(surfaceView)   // 绑定 Surface，内部会监听 surfaceCreated
        }
        PlayerLog.i(L_WINDOW, "PlayerController 已创建并绑定 SurfaceView resId=$resid")
        // 强制全屏时拉伸铺满；否则按视频源宽高比适配
        val scaleToFit = !isForceFullScreen
        if (scaleToFit) {
            playerController?.setPlayerViewResetListener(object : PlayerController.PlayerViewResetListener {
                override fun onPlayerViewReset(width: Int, height: Int) {
                    videoDisplayWidth = width
                    videoDisplayHeight = height
                    PlayerLog.i(
                        L_WINDOW,
                        "收到视频源尺寸回调（解码线程）：${width}x$height，" +
                                "当前窗口=${layoutParams.width}x${layoutParams.height}，等待布局完成后再适配"
                    )
                    LogUtils.i(TAG, "onPlayerViewReset 视频源=${width}x$height")
                    // 解码线程回调；必须等主线程把 SurfaceView 改成等比尺寸后再继续配置解码器，
                    // 否则前几帧会先以整屏 Surface 渲染，缩放区域之后可能留下透明区域。
                    val latch = CountDownLatch(1)
                    handler.post {
                        val target = playerControlView
                        if (target == null) {
                            PlayerLog.w(L_WINDOW, "布局等待：playerControlView 为空，直接放行解码")
                            latch.countDown()
                        } else {
                            val observer = target.viewTreeObserver
                            val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    observer.removeOnGlobalLayoutListener(this)
                                    PlayerLog.d(L_WINDOW, "布局完成，放行解码线程做等比适配")
                                    latch.countDown()
                                }
                            }
                            observer.addOnGlobalLayoutListener(layoutListener)
                            applyPlayerViewRenderSize()
                            handler.postDelayed({
                                observer.removeOnGlobalLayoutListener(layoutListener)
                            }, 600L)
                        }
                    }
                    if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                        PlayerLog.e(L_WINDOW, "等待等比缩放布局超时(500ms)，解码将继续（画面可能短暂铺满整屏）")
                        LogUtils.e(TAG, "等待等比缩放布局超时")
                    }
                }
            })
        } else {
            PlayerLog.i(L_WINDOW, "强制全屏模式：不做等比适配，画面拉伸铺满窗口")
        }
        createFloatingView(surfaceView, title, isMedia, isMandatory)
        try {
            windowManager.addView(floatingView, layoutParams)
            PlayerLog.i(
                L_WINDOW,
                "addView 成功 窗口=${layoutParams.width}x${layoutParams.height}"
            )
        } catch (e: Exception) {
            PlayerLog.e(L_WINDOW, "addView 失败（多因缺少悬浮权限或窗口已存在），回滚本次创建的播放资源", e)
            // 回滚：避免半初始化状态导致后续播放请求被 isShowing / playerController 挡住
            playerControlView?.callback = null
            playerControlView?.release()
            playerControlView = null
            playerController?.setPlayerViewResetListener(null)
            playerController?.release()
            playerController = null
            floatingView = null
            isShowing = false
            PlayerLog.closeSession()
            throw e
        }
        isShowing = true
        PlayerLog.i(
            L_WINDOW,
            "===== 播放窗口创建完成 #$windowSeq 会话=S$session isShowing=$isShowing，" +
                    "等待 Surface 创建与首帧渲染"
        )
    }

    fun setProgressAndTime(
        progress: Long,
        secProgress: Long,
        currentTime: Long,
        totalTime: Long,
        forceChange: Boolean
    ) {
        playerControlView?.setProgressAndTime(progress, secProgress, currentTime, totalTime, forceChange)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView(
        surfaceView: View,
        title: String,
        isMedia: Boolean = true,
        isMandatory: Boolean = false
    ) {
        PlayerLog.i(
            L_WINDOW,
            "开始构建播放窗口视图树 标题=$title 媒体=$isMedia 强制播放=$isMandatory " +
                    "窗口大小切换=$sizeToggleEnabled 右下角缩放=$resizeHandleEnabled"
        )
        // 根布局：FrameLayout，所有子视图叠加
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 等比缩放出现黑边时必须使用不透明黑色，避免透出下层界面
            setBackgroundColor(0xFF000000.toInt())
        }

        // 1. PlayerControlView（视频播放控制层）
        playerControlView = PlayerControlView(context).apply {
            setupPlayFlag(
                if (isMedia) {
                    if (isMandatory) {
                        PlayerControlView.video_flag.or(PlayerControlView.mandatory_flag)
                    } else {
                        PlayerControlView.video_flag
                    }
                } else {
                    if (isMandatory) {
                        PlayerControlView.stream_flag.or(PlayerControlView.mandatory_flag)
                    } else {
                        PlayerControlView.stream_flag
                    }
                }
            )
            setFullEnabled(sizeToggleEnabled)
            setFloatingMode(true)
            setPlayView(surfaceView)
            setTitle(title)
            preparePlay()
            startPlay()
            setDragWindowTouchListener(dragBarTouchListener) // 拖动顶部标题栏实现拖动窗口
        }
        PlayerLog.i(L_WINDOW, "PlayerControlView 构建完成并切换到播放态（悬浮窗模式）")
        playerControlView?.callback = object : ControlCallback {
            override fun seekTo(progress: Int) {
                PlayerLog.i(
                    L_CONTROL,
                    "用户拖动进度到 $progress% → 请求平台同步 mediaPlayPos(res=0, devIds=$toggleDevIds)"
                )
                if (!toggleDevIds.contains(SdkVars.localDeviceId)) {
                    toggleDevIds.add(SdkVars.localDeviceId)
                }
                jni.mediaPlayPos(0, progress, toggleDevIds, 0, 0)
            }

            override fun start() {
                PlayerLog.i(L_CONTROL, "用户点击继续播放 → mediaPlayRecover(res=0, devIds=$toggleDevIds)")
                if (!toggleDevIds.contains(SdkVars.localDeviceId)) {
                    toggleDevIds.add(SdkVars.localDeviceId)
                }
                jni.mediaPlayRecover(0, toggleDevIds)
            }

            override fun pause() {
                PlayerLog.i(L_CONTROL, "用户点击暂停 → mediaPlayPause(res=0, devIds=$toggleDevIds)")
                if (!toggleDevIds.contains(SdkVars.localDeviceId)) {
                    toggleDevIds.add(SdkVars.localDeviceId)
                }
                jni.mediaPlayPause(0, toggleDevIds)
            }

            override fun onBack() {
                PlayerLog.i(
                    L_CONTROL,
                    "用户点击退出播放 → stopResource(res=$curResId, devIds=$toggleDevIds)"
                )
                if (!toggleDevIds.contains(SdkVars.localDeviceId)) {
                    toggleDevIds.add(SdkVars.localDeviceId)
                }
                jni.stopResource(curResId, toggleDevIds)
            }

            override fun onLock(locked: Boolean) {
                PlayerLog.i(L_CONTROL, "窗口锁定状态切换 locked=$locked")
                playerControlView?.setDragWindowTouchListener(dragBarTouchListener, locked)
            }

            override fun toggleScreen() {
                PlayerLog.i(L_CONTROL, "用户点击全屏/窗口切换按钮")
                toggleFullscreen()
            }

            override fun onMoreMenuItemClick(itemId: Int) {
                PlayerLog.i(L_CONTROL, "用户点击更多菜单 itemId=$itemId")
                when (itemId) {
                    // 开始同屏
                    1 -> {
                        if (!jni.checkMemberPermission(
                                InterfaceMacro.Pb_MemberPermissionPropertyID.Pb_memperm_sscreen_VALUE
                            )
                        ) {
                            PlayerLog.w(L_SCREEN, "开始同屏被拒绝：当前成员无同屏权限")
                            ToastUtils.showLong("无权限，请申请权限后再试")
                            return
                        }
                        screenPop(true)
                    }
                    // 结束同屏
                    2 -> {
                        screenPop(false)
                    }
                }
            }
        }
        root.addView(
            playerControlView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        if (resizeHandleEnabled) {
            // 2. 右下角缩放把手
            val resizeHandle = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dp2px(RESIZE_HOTSPOT_SIZE_DP),
                    dp2px(RESIZE_HOTSPOT_SIZE_DP)
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                }
                setBackgroundResource(R.drawable.video_shrink)
                setOnTouchListener(resizeHandleTouchListener)
            }
            root.addView(resizeHandle)
        }

        floatingView = root
        PlayerLog.i(
            L_WINDOW,
            "播放窗口视图树构建完成: PlayerControlView(MATCH_PARENT) + " +
                    "缩放把手=${if (resizeHandleEnabled) "${dp2px(RESIZE_HOTSPOT_SIZE_DP)}px" else "未启用"}"
        )
    }

    // 拖拽监听器
    @SuppressLint("ClickableViewAccessibility")
    private val dragBarTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragStartX = event.rawX.toInt()
                dragStartY = event.rawY.toInt()
                PlayerLog.i(
                    L_CONTROL,
                    "开始拖动窗口 起点=($dragStartX,$dragStartY) 窗口位置=(${layoutParams.x},${layoutParams.y})"
                )
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.rawX.toInt() - dragStartX
                    val dy = event.rawY.toInt() - dragStartY
                    layoutParams.x += dx
                    layoutParams.y += dy
                    clampWindowPosition()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    if (PlayerLog.shouldLog(dragLogCounter, 500L)) {
                        PlayerLog.d(
                            L_CONTROL,
                            "拖动中 位移=($dx,$dy) 窗口位置=(${layoutParams.x},${layoutParams.y})"
                        )
                    }
                    dragStartX = event.rawX.toInt()
                    dragStartY = event.rawY.toInt()
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                PlayerLog.i(
                    L_CONTROL,
                    "结束拖动 action=${if (event.action == MotionEvent.ACTION_UP) "UP" else "CANCEL"} " +
                            "最终位置=(${layoutParams.x},${layoutParams.y})"
                )
                true
            }

            else -> false
        }
    }

    // 缩放监听器
    @SuppressLint("ClickableViewAccessibility")
    private val resizeHandleTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isResizing = true
                resizeStartWidth = layoutParams.width
                resizeStartHeight = layoutParams.height
                resizeStartX = event.rawX.toInt()
                resizeStartY = event.rawY.toInt()
                PlayerLog.i(
                    L_CONTROL,
                    "开始缩放窗口 起点=($resizeStartX,$resizeStartY) " +
                            "起始尺寸=${resizeStartWidth}x$resizeStartHeight " +
                            "最小尺寸=${minWindowWidth()}x${minWindowHeight()}"
                )
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isResizing) {
                    val dx = event.rawX.toInt() - resizeStartX
                    val dy = event.rawY.toInt() - resizeStartY
                    var newWidth = (resizeStartWidth + dx).coerceAtLeast(minWindowWidth())
                    var newHeight = (resizeStartHeight + dy).coerceAtLeast(minWindowHeight())
                    newWidth = newWidth.coerceAtMost(screenSize.x)
                    newHeight = newHeight.coerceAtMost(screenSize.y)
                    layoutParams.width = newWidth
                    layoutParams.height = newHeight
                    isFullscreen = false
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    if (PlayerLog.shouldLog(resizeLogCounter, 500L)) {
                        PlayerLog.d(
                            L_CONTROL,
                            "缩放中 位移=($dx,$dy) 窗口尺寸=${newWidth}x$newHeight"
                        )
                    }
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isResizing = false
                PlayerLog.i(
                    L_CONTROL,
                    "结束缩放 最终尺寸=${layoutParams.width}x${layoutParams.height}，" +
                            "重新按视频源尺寸适配画面"
                )
                handler.post { applyPlayerViewRenderSize() }
                true
            }

            else -> false
        }
    }

    private fun minWindowWidth() = (screenSize.x * MIN_SIZE_RATIO).toInt()
    private fun minWindowHeight() = (screenSize.y * MIN_SIZE_RATIO).toInt()

    private fun clampWindowPosition() {
        val maxX = screenSize.x - layoutParams.width
        val maxY = screenSize.y - layoutParams.height
        layoutParams.x = layoutParams.x.coerceIn(0, maxX)
        layoutParams.y = layoutParams.y.coerceIn(0, maxY)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            layoutParams.width = screenSize.x
            layoutParams.height = screenSize.y
            layoutParams.x = 0
            layoutParams.y = 0
        } else {
            layoutParams.width = halfWidth
            layoutParams.height = halfHeight
            layoutParams.x = (screenSize.x - halfWidth) / 2
            layoutParams.y = (screenSize.y - halfHeight) / 2
        }
        PlayerLog.i(
            L_CONTROL,
            "窗口大小切换 → ${if (isFullscreen) "全屏" else "半屏/窗口"} " +
                    "尺寸=${layoutParams.width}x${layoutParams.height} 位置=(${layoutParams.x},${layoutParams.y})"
        )
        windowManager.updateViewLayout(floatingView, layoutParams)
        applyPlayerViewRenderSize()
    }

    /**
     * 按视频源宽高比重新计算播放子 View 的渲染尺寸。
     * - isForceFullScreen = true 时不处理（画面拉伸铺满窗口）
     * - 未拿到视频源尺寸（视频还没解析出宽高）时不处理
     */
    private fun applyPlayerViewRenderSize() {
        if (isForceFullScreen) {
            PlayerLog.d(L_RENDER, "applyPlayerViewRenderSize: 强制全屏模式，跳过等比适配")
            return
        }
        val sourceWidth = videoDisplayWidth
        val sourceHeight = videoDisplayHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            PlayerLog.w(
                L_RENDER,
                "applyPlayerViewRenderSize: 视频源尺寸未知(${sourceWidth}x$sourceHeight)，跳过适配" +
                        "（通常是还没收到解码器尺寸回调）"
            )
            LogUtils.e(TAG, "applyPlayerViewRenderSize: 视频源尺寸未知，跳过适配")
            return
        }
        PlayerLog.i(
            L_RENDER,
            "applyPlayerViewRenderSize: 视频源=${sourceWidth}x$sourceHeight 窗口=${layoutParams.width}x${layoutParams.height}"
        )
        LogUtils.i(
            TAG,
            "applyPlayerViewRenderSize: 视频源=${sourceWidth}x$sourceHeight," +
                    "窗口=${layoutParams.width}x${layoutParams.height}"
        )
        playerControlView?.resetPlayerViewRenderSize(
            sourceWidth,
            sourceHeight,
            layoutParams.width,
            layoutParams.height
        )
    }

    fun hide() {
        PlayerLog.i(L_WINDOW, "hide: 隐藏播放窗口（播放继续，未释放解码资源）")
        floatingView?.visibility = View.GONE
    }

    fun showAgain() {
        PlayerLog.i(L_WINDOW, "showAgain: 重新显示播放窗口")
        floatingView?.visibility = View.VISIBLE
    }

    fun delayDismiss() {
        hasNewPlay = false
        // 同一次播放内重复调用时，先取消上一个待执行任务，避免叠加多个销毁任务
        pendingDismiss?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingDismiss = null
            PlayerLog.i(
                L_WINDOW,
                "延迟销毁到期(500ms) hasNewPlay=$hasNewPlay isShowing=$isShowing " +
                        "解码已就绪=${playerController?.isCodecReady}"
            )
            LogUtils.i(TAG, "delayDismiss: hasNewPlay=$hasNewPlay")
            if (!hasNewPlay) {
                // 停止后立马进行播放是无效的，需要延迟
                mExitFloatingPlayListener?.exitFloatingPlayListener()
                dismiss()
            } else {
                PlayerLog.i(L_WINDOW, "延迟销毁被新的播放请求取消，窗口保持显示")
            }
        }
        pendingDismiss = task
        PlayerLog.i(L_WINDOW, "已安排 500ms 后延迟销毁播放窗口（等待可能马上到来的新播放请求）")
        handler.postDelayed(task, 500L)
    }

    private fun dismiss() {
        if (!inPlaySession()) {
            PlayerLog.w(L_WINDOW, "dismiss: 当前没有播放窗口资源，忽略本次销毁（可能是重复调用）")
            return
        }
        PlayerLog.i(
            L_WINDOW,
            "===== 播放窗口销毁开始 会话=S${PlayerLog.currentSession()} isShowing=$isShowing " +
                    "floatingView=${floatingView != null} 当前资源: mediaId=$currentMediaId " +
                    "deviceId=$currentDeviceId subId=$currentSubId 进度=$currentProgress"
        )
        PlayerLog.stack(L_WINDOW, "dismiss 调用来源", 5)
        handler.removeCallbacksAndMessages(null)
        pendingDismiss = null
        dismissScreenPop()
        if (isShowing && floatingView != null) {
            // 恢复默认窗口全屏
            layoutParams.width = screenSize.x
            layoutParams.height = screenSize.y
            layoutParams.x = 0
            layoutParams.y = 0

            try {
                windowManager.removeView(floatingView)
                PlayerLog.i(L_WINDOW, "removeView 成功，悬浮窗已从 WindowManager 移除")
            } catch (e: Exception) {
                PlayerLog.e(L_WINDOW, "removeView 失败（窗口可能已被系统移除）", e)
            }
            floatingView = null
            isShowing = false
            jni.stopResource(0, SdkVars.localDeviceId)
            PlayerLog.i(L_WINDOW, "已通知平台停止资源 stopResource(res=0, devId=${SdkVars.localDeviceId})")
            LogUtils.i(TAG, "dismissed")
        } else {
            PlayerLog.w(
                L_WINDOW,
                "dismiss: 窗口未处于显示状态（isShowing=$isShowing floatingView=${floatingView != null}），" +
                        "仅释放播放器资源"
            )
        }

        playerControlView?.callback = null
        playerControlView?.release()
        playerControlView = null
        playerController?.setPlayerViewResetListener(null)
        playerController?.release()
        playerController = null
        PlayerLog.i(L_WINDOW, "播放控制层与解码器资源已释放")

        mExitFloatingPlayListener = null

        //同屏数据重置
        toggleDevIds.clear()
        currentDeviceId = 0
        currentSubId = 0
        currentMediaId = 0
        currentProgress = 0
        currentSec = 0
        videoDisplayWidth = 0
        videoDisplayHeight = 0
        lastSessionId = PlayerLog.currentSession()
        PlayerLog.i(
            L_WINDOW,
            "===== 播放窗口销毁完成（本次会话=S$lastSessionId），播放状态与同屏数据已复位"
        )
        PlayerLog.closeSession()
    }

    fun updateTitle(title: String) {
        PlayerLog.d(L_WINDOW, "updateTitle: $title")
        playerControlView?.setTitle(title)
    }

    fun updateFps(fps: Int) {
        playerControlView?.setFps(fps)
    }

    fun getPlayerControlView() = playerControlView

    //<editor-fold desc="同屏选择弹窗（原 EasyFloat 实现改为原生 WindowManager）">

    private val screenPopParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.OPAQUE
            gravity = Gravity.CENTER
            width = ScreenUtils.getScreenWidth() / 3 * 2
            height = ScreenUtils.getScreenHeight() / 3 * 2
            x = 0
            y = 0
        }
    }

    private fun screenPop(isStart: Boolean = true) {
        PlayerLog.i(
            L_SCREEN,
            "打开同屏选择弹窗 操作=${if (isStart) "开始同屏" else "结束同屏"} " +
                    "当前弹窗已显示=$screenPopShown 在线人员=${onlineMembers.size} 在线会场设备=${onlineProjects.size}"
        )
        if (screenPopShown) {
            dismissScreenPop()
        }
        val content = LayoutInflater.from(context)
            .inflate(R.layout.layout_floating_screen_pop, null, false)

        content.findViewById<TextView>(R.id.tv_title).text =
            if (isStart) "开始同屏" else "结束同屏"

        val cbForce = content.findViewById<CheckBox>(R.id.cb_force).apply {
            visibility = if (isStart) View.VISIBLE else View.INVISIBLE
        }
        val cbMember = content.findViewById<CheckBox>(R.id.cb_member)
        val cbProjector = content.findViewById<CheckBox>(R.id.cb_projector)
        val rvMember = content.findViewById<RecyclerView>(R.id.rv_member)
        val rvProjector = content.findViewById<RecyclerView>(R.id.rv_projector)

        rvMember.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        memberAdapter.setOnItemCheckedChangeListener(object : MemberAdapter.OnItemCheckedChangeListener {
            override fun onCheckedAll(value: Boolean) {
                cbMember.isChecked = value
            }
        })
        rvMember.adapter = memberAdapter

        projectAdapter.setOnItemCheckedChangeListener(object : ProjectAdapter.OnItemCheckedChangeListener {
            override fun onCheckedAll(value: Boolean) {
                cbProjector.isChecked = value
            }
        })
        rvProjector.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rvProjector.adapter = projectAdapter

        cbMember.isChecked = memberAdapter.isChooseAll()
        cbMember.setOnClickListener {
            val check = cbMember.isChecked
            cbMember.isChecked = check
            PlayerLog.d(L_SCREEN, "同屏人员全选切换 =$check")
            memberAdapter.chooseAll(check)
        }
        cbProjector.isChecked = projectAdapter.isChooseAll()
        cbProjector.setOnClickListener {
            val check = cbProjector.isChecked
            cbProjector.isChecked = check
            PlayerLog.d(L_SCREEN, "同屏会场设备全选切换 =$check")
            projectAdapter.chooseAll(check)
        }
        content.findViewById<Button>(R.id.btn_ensure).apply {
            text = "确认"
            setOnClickListener {
                val ids = memberAdapter.selectedIds
                ids.addAll(projectAdapter.selectedIds)
                PlayerLog.i(
                    L_SCREEN,
                    "${if (isStart) "开始同屏" else "结束同屏"}确认: 勾选目标数=${ids.size} " +
                            "目标=$ids 强制播放=${cbForce.isChecked} 当前视频源: deviceId=$currentDeviceId " +
                            "subId=$currentSubId mediaId=$currentMediaId progress=$currentProgress"
                )
                LogUtils.i(TAG, "${if (isStart) "开始同屏" else "结束同屏"}: ${ids.size}")
                val triggeruserval =
                    if (cbForce.isChecked) {
                        InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    } else {
                        0
                    }
                if (ids.isNotEmpty()) {
                    if (isStart) {
                        if (currentDeviceId != 0 && currentSubId != 0) {
                            if (ids.contains(currentDeviceId)) {
                                ids.removeAt(ids.indexOf(currentDeviceId))
                                PlayerLog.i(L_SCREEN, "视频源与同屏目标相同,进行剔除")
                                LogUtils.i(TAG, "视频源与同屏目标相同,进行剔除")
                                if (ids.isEmpty()) {
                                    ToastUtils.showShort("视频源与同屏目标相同")
                                }
                            }
                            toggleDevIds.addAll(ids.filter { it !in toggleDevIds })
                            PlayerLog.i(
                                L_SCREEN,
                                "调用 streamPlay 开始流同屏 srcDevice=$currentDeviceId srcSub=$currentSubId " +
                                        "目标=$ids 触发方式=$triggeruserval 累计同屏设备=$toggleDevIds"
                            )
                            jni.streamPlay(
                                currentDeviceId,
                                currentSubId,
                                mutableListOf(0),
                                ids,
                                0,
                                triggeruserval
                            )
                        } else if (currentMediaId != 0) {
                            toggleDevIds.addAll(ids.filter { it !in toggleDevIds })
                            PlayerLog.i(
                                L_SCREEN,
                                "调用 mediaPlay 开始媒体同屏 mediaId=$currentMediaId 目标=$ids " +
                                        "起始进度=$currentProgress 触发方式=$triggeruserval 累计同屏设备=$toggleDevIds"
                            )
                            jni.mediaPlay(
                                0,
                                currentMediaId,
                                ids,
                                0,
                                currentProgress,
                                triggeruserval
                            )
                            //为了同步，调用一次进度
                            jni.mediaPlayPos(0, currentProgress, toggleDevIds, 0, 0)
                            PlayerLog.i(L_SCREEN, "已调用 mediaPlayPos 同步一次进度=$currentProgress")
                        } else {
                            PlayerLog.w(L_SCREEN, "开始同屏失败：既没有流视频源也没有媒体视频源")
                        }
                    } else {
                        toggleDevIds.clear()
                        PlayerLog.i(L_SCREEN, "调用 stopResource 结束同屏 目标=$ids")
                        jni.stopResource(0, ids)
                    }
                    dismissScreenPop()
                } else {
                    PlayerLog.w(L_SCREEN, "未勾选任何同屏目标，忽略本次确认")
                }
            }
        }
        content.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            PlayerLog.i(L_SCREEN, "用户取消同屏选择弹窗")
            dismissScreenPop()
        }

        screenPopView = content
        if (!screenPopShown) {
            windowManager.addView(content, screenPopParams)
            screenPopShown = true
            PlayerLog.i(
                L_SCREEN,
                "同屏选择弹窗已显示 尺寸=${screenPopParams.width}x${screenPopParams.height} " +
                        "type=${screenPopParams.type}"
            )
        }
    }

    private fun dismissScreenPop() {
        val view = screenPopView ?: return
        screenPopView = null
        if (screenPopShown) {
            screenPopShown = false
            try {
                windowManager.removeView(view)
                PlayerLog.i(L_SCREEN, "同屏选择弹窗已移除")
            } catch (e: Exception) {
                PlayerLog.e(L_SCREEN, "dismissScreenPop 移除弹窗失败", e)
                LogUtils.e(TAG, "dismissScreenPop 移除弹窗失败", e)
            }
        }
    }
    //</editor-fold>

    private fun dp2px(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()

    private fun screenSize(): Point {
        return Point().apply {
            x = ScreenUtils.getScreenWidth()
            y = ScreenUtils.getScreenHeight()
        }
    }

    fun onDestroy() {
        PlayerLog.i(
            L_WINDOW,
            "onDestroy: 释放播放窗口 会话=S${PlayerLog.currentSession()} isShowing=$isShowing " +
                    "floatingView=${floatingView != null} playerController=${playerController != null}"
        )
        LogUtils.i(TAG, "onDestroy: ")
        // 无论是否处于播放会话中，都要确保窗口被移除，避免 WindowManager 泄漏
        if (isShowing || floatingView != null || inPlaySession()) {
            dismiss()
        }
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
            PlayerLog.i(L_EVENT, "onDestroy: 已注销 EventBus 监听")
        }
        PlayerLog.closeSession()
    }
}
