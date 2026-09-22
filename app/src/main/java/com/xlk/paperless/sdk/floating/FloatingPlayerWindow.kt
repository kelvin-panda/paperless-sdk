package com.xlk.paperless.sdk.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfaceStop
import com.mogujie.tt.protobuf.InterfaceStream
import com.paperless.bus.Bus
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
import com.paperless.sdk.R
import com.paperless.sdk.SUB_TYPE_BITMASK
import com.paperless.sdk.SdkVars
import com.paperless.util.PlayerLog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *  @author : Administrator
 *  created on 2026/7/1 16:20
 */
class FloatingPlayerWindow private constructor(val context: Context){

    companion object {
        private const val MIN_SIZE_RATIO = 1 / 3f
        private const val DRAG_BAR_HEIGHT_DP = 40
        private const val RESIZE_HOTSPOT_SIZE_DP = 36

        //<editor-fold desc="日志链路标签（配合 PlayerLog 使用，过滤：adb logcat -s PlayWin）">
        private const val L_WINDOW = "窗口"
        private const val L_EVENT = "事件"
        private const val L_CONTROL = "控制"
        //</editor-fold>

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FloatingPlayerWindow? = null
        fun getInstance(context: Context): FloatingPlayerWindow {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingPlayerWindow(context.applicationContext).also {
                    INSTANCE = it
                    PlayerLog.i(L_WINDOW, "创建 FloatingPlayerWindow 单例（应用上下文）")
                }
            }
        }

        // 在 App 退出时调用，避免单例长期持有
        fun destroyInstance() {
            PlayerLog.i(L_WINDOW, "销毁 FloatingPlayerWindow 单例")
            INSTANCE?.onDestroy()
            INSTANCE = null
        }
    }

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        format = PixelFormat.TRANSLUCENT
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
    private var jni = BaseJni()

    // 当前播放信息
    var currentDeviceId = 0
    var currentSubId = 0
    var currentMediaId = 0
    var currentProgress = 0

    //<editor-fold desc="日志链路辅助（不参与业务逻辑）">
    /** 待执行的延迟销毁任务，收到新的播放请求时需要撤销 */
    private var pendingDismiss: Runnable? = null

    /** 拖拽/缩放过程日志节流 */
    private val dragLogCounter = PlayerLog.ThrottleCounter()
    private val resizeLogCounter = PlayerLog.ThrottleCounter()

    /** 当前是否持有播放窗口资源 */
    private fun inPlaySession() = isShowing || playerController != null || playerControlView != null
    //</editor-fold>

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

    // 按比例缩放
    private var scaleProportionally = true

    // 推荐在 initial() 调用前完成配置
    fun configure(
        curResId: Int = 0,
        sizeToggleEnabled: Boolean = true,
        resizeHandleEnabled: Boolean = true,
        scaleProportionally: Boolean = true
    ) {
        this.curResId = curResId
        this.sizeToggleEnabled = sizeToggleEnabled
        this.resizeHandleEnabled = resizeHandleEnabled
        this.scaleProportionally = scaleProportionally
        PlayerLog.i(
            L_WINDOW,
            "configure 播放窗口配置: curResId=$curResId 窗口大小切换=$sizeToggleEnabled " +
                    "右下角缩放=$resizeHandleEnabled 等比适配=$scaleProportionally " +
                    "屏幕=${screenSize.x}x${screenSize.y}"
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
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
                    it.mediaid and SUB_TYPE_BITMASK
                    PlayerLog.i(
                        L_EVENT,
                        "收到媒体播放通知 res=${it.res} mediaid=${it.mediaid} 类型=$type " +
                                "强制播放=$isMandatory triggeruserval=${it.triggeruserval}"
                    )
                    if (type == MEDIA_FILE_TYPE_AUDIO
                        || type == MEDIA_FILE_TYPE_VIDEO
                        || type == MEDIA_FILE_TYPE_RECORD
                    ) {
                        if (it.res == 0) {
                            DecodeQueue.cleanup(it.res)
                            val fileName = jni.queryFileName(it.mediaid)
                            PlayerLog.i(L_EVENT, "媒体播放走播放窗口 文件名=$fileName mediaId=${it.mediaid}")
                            showPlayerWindow(true, fileName, isMandatory, it.res)
                        } else {
                            PlayerLog.w(L_EVENT, "媒体播放资源 res=${it.res} 不属于当前窗口(curResId=$curResId)，忽略")
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
                                "强制播放=$isMandatory triggeruserval=${it.triggeruserval}"
                    )
                    if (it.res == 0) {
                        DecodeQueue.cleanup(it.res)
                        currentDeviceId = it.deviceid
                        currentSubId = it.subid
                        val devName = jni.queryDeviceNameById(it.deviceid)
                        PlayerLog.i(L_EVENT, "流播放走播放窗口 设备名=$devName deviceId=${it.deviceid} subId=${it.subid}")
                        showPlayerWindow(false, devName, isMandatory, it.res)
                    } else {
                        PlayerLog.w(L_EVENT, "流播放资源 res=${it.res} 不属于当前窗口(curResId=$curResId)，忽略")
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
                            currentProgress = it.sec
                            setProgressAndTime(
                                it.per.toLong(), it.per.toLong(),
                                it.sec * 1000L, curTotalMs * 1L, false
                            )
                        }
                    }
                }
            }
            //流播放
            Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE -> {
                if (msg.method == Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopResWork.parseFrom(msg.data)?.let {
                        PlayerLog.i(L_EVENT, "收到流播放停止资源通知(CLOSE) res列表=${it.resList}")
                        it.resList.forEach { resId ->
                            LogUtils.e("流播放停止资源通知 $resId")
                            if (resId == 0) {
                                delayDismiss()
                            }
                            Fps.clear(resId)
                        }
                    }
                } else if (msg.method == Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopPlay.parseFrom(msg.data)?.let {
                        PlayerLog.i(
                            L_EVENT,
                            "收到流播放停止通知(NOTIFY) res=${it.res} createdeviceid=${it.createdeviceid} triggerid=${it.triggerid}"
                        )
                        LogUtils.i("流播放停止通知: res[${it.res}] createdeviceid[${it.createdeviceid}] triggerid[${it.triggerid}]")
                        if (it.res == 0) {
                            Fps.clear(it.res)
                            delayDismiss()
                        }
                    }
                }
            }
        }
    }

    fun showPlayerWindow(isMedia: Boolean = true, title: String = "", isMandatory: Boolean = false, resid: Int = curResId) {
        PlayerLog.i(
            L_WINDOW,
            "showPlayerWindow 请求: 媒体=$isMedia 标题=$title 强制播放=$isMandatory " +
                    "resid=$resid 当前窗口显示中=$isShowing"
        )
        hasNewPlay = true
        // 新的播放请求到来，撤销上一次「延迟销毁」
        pendingDismiss?.let {
            handler.removeCallbacks(it)
            pendingDismiss = null
            PlayerLog.i(L_WINDOW, "showPlayerWindow: 已撤销上一次待执行的延迟销毁任务（新播放到来）")
        }
        if (isShowing) {
            PlayerLog.i(L_WINDOW, "复用已有播放窗口，仅更新标题与播放标记")
            updateTitle(title)
            playerControlView?.setupPlayFlag(
                if (isMedia) {
                    if (isMandatory) PlayerControlView.video_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.video_flag
                } else {
                    if (isMandatory) PlayerControlView.stream_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.stream_flag
                }
            )
        } else {
            PlayerLog.step(L_WINDOW, "窗口未显示，开始创建新的播放会话")
            val surfaceView = SurfaceView(appContext)
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
        val session = PlayerLog.openSession()
        PlayerLog.i(
            L_WINDOW,
            "===== 播放窗口创建开始 会话=S$session 类型=${if (isMedia) "媒体文件" else "流媒体"} " +
                    "标题=$title 强制播放=$isMandatory resid=$resid 等比适配=$scaleProportionally"
        )
        PlayerLog.i(
            L_WINDOW,
            "窗口参数: 尺寸=${layoutParams.width}x${layoutParams.height} 位置=(${layoutParams.x},${layoutParams.y}) " +
                    "type=${layoutParams.type} flags=${layoutParams.flags} format=${layoutParams.format}"
        )
        playerController = PlayerController(resid, onSurfaceReady = {
            PlayerLog.i(L_WINDOW, "Surface 已就绪，解码即将开始（窗口层回调）")
            LogUtils.i("FloatingPlayer: Surface ready, decoding started")
        }).apply {
            initialize(surfaceView)   // 绑定 Surface，内部会监听 surfaceCreated
        }
        PlayerLog.i(L_WINDOW, "PlayerController 已创建并绑定 SurfaceView resId=$resid")
        if (scaleProportionally) {
            playerController?.setPlayerViewResetListener(object : PlayerController.PlayerViewResetListener {
                override fun onPlayerViewReset(width: Int, height: Int) {
                    PlayerLog.i(
                        L_WINDOW,
                        "收到视频源尺寸回调（解码线程）：${width}x$height，" +
                                "窗口=${screenSize().x}x${screenSize().y}，切主线程做等比适配"
                    )
                    // 从解码线程回调的，需要切换到主线程
                    handler.post {
                        playerControlView?.resetPlayerViewRenderSize(width, height, screenSize().x, screenSize().y)
                    }
                }
            })
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
            "===== 播放窗口创建完成 会话=S$session isShowing=$isShowing，等待 Surface 创建与首帧渲染"
        )
    }

    fun setProgressAndTime(progress: Long, secProgress: Long, currentTime: Long, totalTime: Long, forceChange: Boolean) {
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
        val root = FrameLayout(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 半透明背景，更美观
            setBackgroundColor(0xDD000000.toInt())
        }

        // 1. PlayerControlView（视频播放控制层）
        playerControlView = PlayerControlView(appContext).apply {
            setupPlayFlag(
                if (isMedia) {
                    if (isMandatory) PlayerControlView.video_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.video_flag
                } else {
                    if (isMandatory) PlayerControlView.stream_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.stream_flag
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
                PlayerLog.i(L_CONTROL, "用户拖动进度到 $progress% → mediaPlayPos + 通知同屏进度")
                Bus.postObj(type = SdkBusType.floating_same_play_progress, progress)
                jni.mediaPlayPos(0, progress, mutableListOf(SdkVars.localDeviceId), 0, 0)
            }

            override fun start() {
                PlayerLog.i(L_CONTROL, "用户点击继续播放 → mediaPlayRecover")
                jni.mediaPlayRecover(0, SdkVars.localDeviceId)
            }

            override fun pause() {
                PlayerLog.i(L_CONTROL, "用户点击暂停 → mediaPlayPause")
                jni.mediaPlayPause(0, SdkVars.localDeviceId)
            }

            override fun onBack() {
                PlayerLog.i(L_CONTROL, "用户点击退出播放 → stopResource(res=$curResId)")
                jni.stopResource(curResId, SdkVars.localDeviceId)
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
                        PlayerLog.i(
                            L_CONTROL,
                            "开始同屏：交由 Activity 处理 videoSource=(deviceId=$currentDeviceId," +
                                    "subId=$currentSubId,mediaId=$currentMediaId,progress=$currentProgress)"
                        )
                        Bus.postVararg(
                            type = SdkBusType.floating_start_screen_share,
                            currentDeviceId,
                            currentSubId,
                            currentMediaId,
                            currentProgress
                        )
                    }
                    // 结束同屏
                    2 -> {
                        PlayerLog.i(L_CONTROL, "结束同屏：交由 Activity 处理")
                        Bus.post(SdkBusType.floating_stop_screen_share)
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
            val resizeHandle = View(appContext).apply {
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
                PlayerLog.i(L_CONTROL, "开始拖动窗口 起点=($dragStartX,$dragStartY) 位置=(${layoutParams.x},${layoutParams.y})")
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
                        PlayerLog.d(L_CONTROL, "拖动中 位移=($dx,$dy) 位置=(${layoutParams.x},${layoutParams.y})")
                    }
                    dragStartX = event.rawX.toInt()
                    dragStartY = event.rawY.toInt()
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                PlayerLog.i(L_CONTROL, "结束拖动 最终位置=(${layoutParams.x},${layoutParams.y})")
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
                    "开始缩放窗口 起点=($resizeStartX,$resizeStartY) 起始尺寸=${resizeStartWidth}x$resizeStartHeight"
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
                        PlayerLog.d(L_CONTROL, "缩放中 位移=($dx,$dy) 窗口尺寸=${newWidth}x$newHeight")
                    }
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isResizing = false
                PlayerLog.i(L_CONTROL, "结束缩放 最终尺寸=${layoutParams.width}x${layoutParams.height}")
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
        pendingDismiss?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            pendingDismiss = null
            PlayerLog.i(
                L_WINDOW,
                "延迟销毁到期(500ms) hasNewPlay=$hasNewPlay isShowing=$isShowing " +
                        "解码已就绪=${playerController?.isCodecReady}"
            )
            LogUtils.i("delayDismiss: hasNewPlay=$hasNewPlay")
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
        if (isShowing && floatingView != null) {
            // 恢复窗口全屏
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
            LogUtils.i("FloatingPlayerWindow dismissed")
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
        currentDeviceId = 0
        currentSubId = 0
        currentMediaId = 0
        currentProgress = 0
        PlayerLog.i(L_WINDOW, "===== 播放窗口销毁完成，播放状态已复位")
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

    private fun dp2px(dp: Int) = (dp * appContext.resources.displayMetrics.density).toInt()
    private fun screenSize(): Point {
        val point = Point().apply {
            x = ScreenUtils.getScreenWidth()
            y = ScreenUtils.getScreenHeight()
        }
//        windowManager.defaultDisplay.getSize(point)

        return point
    }

    fun onDestroy() {
        PlayerLog.i(
            L_WINDOW,
            "onDestroy: 释放播放窗口 会话=S${PlayerLog.currentSession()} isShowing=$isShowing " +
                    "floatingView=${floatingView != null} playerController=${playerController != null}"
        )
        LogUtils.i("onDestroy: ")
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