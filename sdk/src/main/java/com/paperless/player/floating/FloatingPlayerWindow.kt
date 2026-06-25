package com.paperless.player.floating

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
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.blankj.utilcode.util.LogUtils
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
import com.paperless.MemberAdapter
import com.paperless.ProjectAdapter
import com.paperless.bus.EventBusMessage
import com.paperless.bus.SdkBusType
import com.paperless.player.DecodeQueue
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
import com.paperless.sdk.isProjector
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FloatingPlayerWindow private constructor(context: Context) {

    companion object {
        private const val MIN_SIZE_RATIO = 1 / 3f
        private const val DRAG_BAR_HEIGHT_DP = 40
        private const val RESIZE_HOTSPOT_SIZE_DP = 36

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FloatingPlayerWindow? = null
        fun getInstance(context: Context): FloatingPlayerWindow {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingPlayerWindow(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 在 App 退出时调用，避免单例长期持有
        fun destroyInstance() {
            INSTANCE?.onDestroy()
            INSTANCE = null
        }
    }

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var screenPopView: View? = null
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

    // 窗口参数
    private val popLayoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.CENTER
        width = screenSize().x / 3 * 2
        height = screenSize().y / 3 * 2
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
    val tempMembers: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo> = mutableListOf()
    val onlineMembers: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo> = mutableListOf()
    val onlineProjects: MutableList<InterfaceDevice.pbui_Item_DeviceDetailInfo> = mutableListOf()
    val memberAdapter: MemberAdapter = MemberAdapter(onlineMembers)
    var projectAdapter: ProjectAdapter = ProjectAdapter(onlineProjects)

    private var mExitFloatingPlayListener: ExitFloatingPlayListener? = null

    interface ExitFloatingPlayListener {
        fun exitFloatingPlayListener()
    }

    fun setExitFloatingPlayListener(listener: ExitFloatingPlayListener?) {
        mExitFloatingPlayListener = listener
    }

    private var curResId = 0
    private var fullEnabled = true

    // 推荐在 initial() 调用前完成配置
    fun configure(curResId: Int = 0, fullEnabled: Boolean = true) {
        this.curResId = curResId
        this.fullEnabled = fullEnabled
    }

    fun initial() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        delayQueryDeviceTask()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
            //接收的帧数
            SdkBusType.fps -> {
//                val fps = msg.obj as Int
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
                    val subType = it.mediaid and SUB_TYPE_BITMASK
                    if (type == MEDIA_FILE_TYPE_AUDIO
                        || type == MEDIA_FILE_TYPE_VIDEO
                        || type == MEDIA_FILE_TYPE_RECORD
                    ) {
                        if (it.res == 0) {
                            DecodeQueue.cleanup(it.res)
                            val fileName = jni.queryFileName(it.mediaid)
                            showPlayerWindow(true, fileName, isMandatory, it.res)
                        }
                    }
                }
            }
            // 流播放
            Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE -> {
                InterfaceStream.pbui_Type_MeetStreamPlay.parseFrom(msg.data)?.let {
                    val isMandatory =
                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
                    if (it.res == 0) {
                        DecodeQueue.cleanup(it.res)
                        currentDeviceId = it.deviceid
                        currentSubId = it.subid
                        val devName = jni.queryDeviceNameById(it.deviceid)
                        showPlayerWindow(false, devName, isMandatory, it.res)
                    }
                }
            }

            //平台播放进度通知
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE -> {
                InterfacePlaymedia.pbui_Type_PlayPosCb.parseFrom(msg.data)?.let {
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
                        it.resList.find { it == 0 }?.let {
                            LogUtils.e("流播放停止资源通知")
                            delayDismiss()
                        }
                    }
                } else if (msg.method == Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopPlay.parseFrom(msg.data)?.let {
                        LogUtils.i("流播放停止通知: res[${it.res}] createdeviceid[${it.createdeviceid}] triggerid[${it.triggerid}]")
                        if (it.res == 0) {
                            delayDismiss()
                        }
                    }
                }
            }
            // 设备、参会人、设备会议
            Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE,// InterfaceDevice.pbui_Type_MeetDeviceBaseInfo
            Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE,
            Pb_TYPE_MEET_INTERFACE_DEVICEMEETSTATUS_VALUE -> {
                LogUtils.i("busEvent:变更通知更新 ${msg.type}")
                delayQueryDeviceTask()
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var delayQueryDeviceTask = Runnable {
        queryOnlineDev()
    }

    private fun delayQueryDeviceTask() {
        handler.removeCallbacks(delayQueryDeviceTask)
        handler.postDelayed(delayQueryDeviceTask, 1000L)
    }

    private fun queryOnlineDev() {
        tempMembers.clear()
        onlineMembers.clear()
        onlineProjects.clear()
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE))
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE)
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE))
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE)
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM_VALUE))
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM_VALUE)
        if (!jni.checkCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE))
            jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE)
        jni.queryMemberDetail()?.let {
            tempMembers.addAll(it.itemList)
        }
        jni.queryDevice()?.let {
            it.pdevList.filter { it.netstate == 1 }.forEach { dev ->
                if (dev.devcieid.isProjector()) {
                    onlineProjects.add(dev)
                } else {
                    if (dev.devcieid != SdkVars.localDeviceId && dev.facestate == 1 && dev.meetingid == SdkVars.localMeetingId) {
                        tempMembers.find { dev.devcieid == it.devid }?.let {
                            onlineMembers.add(it)
                        }
                    }
                }
            }
        }
        //LogUtils.i("queryOnlineDev: ${onlineMembers.size},${onlineProjects.size}")
        memberAdapter.updateData(onlineMembers)
        projectAdapter.updateData(onlineProjects)
    }

    fun showPlayerWindow(isMedia: Boolean = true, title: String = "", isMandatory: Boolean = false, resid: Int = curResId) {
        LogUtils.i("showPlayerWindow: isMedia=$isMedia,isMandatory=$isMandatory,title=$title")
        hasNewPlay = true
        if (isShowing) {
            updateTitle(title)
            playerControlView?.setupPlayFlag(
                if (isMedia) {
                    if (isMandatory) PlayerControlView.video_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.video_flag
                } else {
                    if (isMandatory) PlayerControlView.stream_flag.or(PlayerControlView.mandatory_flag) else PlayerControlView.stream_flag
                }
            )
        } else {
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
        if (isShowing) return
        playerController = PlayerController(resid, onSurfaceReady = {
            LogUtils.i("FloatingPlayer: Surface ready, decoding started")
        }).apply {
            initialize(surfaceView)   // 绑定 Surface，内部会监听 surfaceCreated
            setPlayerViewResetListener(object : PlayerController.PlayerViewResetListener {
                override fun onPlayerViewReset(width: Int, height: Int) {
                    // 从解码线程回调的，需要切换到主线程
                    handler.post { playerControlView?.resetPlayerViewRenderSize(width, height, screenSize().x, screenSize().y) }
                }
            })
        }
        createFloatingView(surfaceView, title, isMedia, isMandatory)
        windowManager.addView(floatingView, layoutParams)
        isShowing = true
        LogUtils.i("FloatingPlayerWindow shown")
    }

    fun setProgressAndTime(progress: Long, secProgress: Long, currentTime: Long, totalTime: Long, forceChange: Boolean) {
        playerControlView?.setProgressAndTime(progress, secProgress, currentTime, totalTime, forceChange)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView(surfaceView: View, title: String, isMedia: Boolean = true, isMandatory: Boolean = false) {
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
            setFullEnabled(fullEnabled)
            setFloatingMode(true)
            setPlayView(surfaceView)
            setTitle(title)
            preparePlay()
            startPlay()
            setDragWindowTouchListener(dragBarTouchListener) // 拖动顶部标题栏实现拖动窗口
            setSeekGestureEnabled(false) // 禁用屏幕滑动进度调节
        }
        playerControlView?.callback = object : ControlCallback {
            override fun seekTo(progress: Int) {
                jni.mediaPlayPos(0, progress, mutableListOf(SdkVars.localDeviceId), 0, 0)
            }

            override fun start() {
                jni.mediaPlayRecover(0, SdkVars.localDeviceId)
            }

            override fun pause() {
                jni.mediaPlayPause(0, SdkVars.localDeviceId)
            }

            override fun onBack() {
                jni.stopResource(curResId, SdkVars.localDeviceId)
            }

            override fun onLock(locked: Boolean) {
                playerControlView?.setDragWindowTouchListener(dragBarTouchListener, locked)
            }

            override fun toggleScreen() {
                toggleFullscreen()
            }

            override fun onMoreMenuItemClick(itemId: Int) {
                println("onMoreMenuItemClick: $itemId")
                when (itemId) {
                    // 开始同屏
                    1 -> {
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

        // 2. 右下角缩放把手
        val resizeHandle = View(appContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp2px(RESIZE_HOTSPOT_SIZE_DP),
                dp2px(RESIZE_HOTSPOT_SIZE_DP)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
//                bottomMargin = dp2px(8)
//                rightMargin = dp2px(8)
            }
            setBackgroundResource(R.drawable.video_shrink) // 替换为实际图标
            setOnTouchListener(resizeHandleTouchListener)
        }
        root.addView(resizeHandle)

        floatingView = root
    }

    private fun screenPop(start: Boolean) {
        // 移除旧的弹窗
        screenPopView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // 已经移除或未添加，忽略
            }
            screenPopView = null
        }
        val inflate = LayoutInflater.from(appContext).inflate(R.layout.video_screen_pop, null)
        inflate.apply {
            findViewById<TextView>(R.id.tv_title).apply { text = if (start) "开始同屏" else "结束同屏" }
            val cb_force = findViewById<CheckBox>(R.id.cb_force).apply {
                visibility = if (start) View.VISIBLE else View.GONE
            }
            val cb_member = findViewById<CheckBox>(R.id.cb_member)
            val cb_projector = findViewById<CheckBox>(R.id.cb_projector)
            val rv_member = findViewById<RecyclerView>(R.id.rv_member)
            val rv_projector = findViewById<RecyclerView>(R.id.rv_projector)

            memberAdapter.setOnItemCheckedChangeListener(object : MemberAdapter.OnItemCheckedChangeListener {
                override fun onCheckedAll(value: Boolean) {
                    cb_member.isChecked = value
                }
            })
            rv_member.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            rv_member.adapter = memberAdapter
            projectAdapter.setOnItemCheckedChangeListener(object : ProjectAdapter.OnItemCheckedChangeListener {
                override fun onCheckedAll(value: Boolean) {
                    cb_projector.isChecked = value
                }
            })
            rv_projector.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            rv_projector.adapter = projectAdapter
            cb_member.isChecked = memberAdapter.isChooseAll()
            cb_member.setOnClickListener {
                val check = cb_member.isChecked
                cb_member.isChecked = check
                memberAdapter.chooseAll(check)
            }
            cb_projector.isChecked = projectAdapter.isChooseAll()
            cb_projector.setOnClickListener {
                val check = cb_projector.isChecked
                cb_projector.isChecked = check
                projectAdapter.chooseAll(check)
            }

            findViewById<Button>(R.id.btn_ensure).setOnClickListener {
                val ids = memberAdapter.selectedIds
                ids.addAll(projectAdapter.selectedIds)
                LogUtils.i("${if (start) "开始同屏" else "结束同屏"}: ${ids.size}")
                val triggeruserval =
                    if (cb_force.isChecked) InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE else 0
                if (ids.isNotEmpty()) {
                    if (start) {
                        if (currentDeviceId != 0 && currentSubId != 0) {
                            jni.streamPlay(currentDeviceId, currentSubId, 0, ids, 0, triggeruserval)
                        } else if (currentMediaId != 0) {
                            jni.mediaPlay(0, currentMediaId, ids, 0, currentProgress, triggeruserval)
                        }
                    } else {
                        jni.stopResource(0, ids)
                    }
                    windowManager.removeView(screenPopView)
                    screenPopView = null
                }
            }
            findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                windowManager.removeView(screenPopView)
                screenPopView = null
            }
        }
        screenPopView = inflate
        windowManager.addView(screenPopView, popLayoutParams)
    }

    // 拖拽监听器
    @SuppressLint("ClickableViewAccessibility")
    private val dragBarTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragStartX = event.rawX.toInt()
                dragStartY = event.rawY.toInt()
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
                    dragStartX = event.rawX.toInt()
                    dragStartY = event.rawY.toInt()
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
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
                }
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isResizing = false
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
        windowManager.updateViewLayout(floatingView, layoutParams)
    }

    fun hide() {
        floatingView?.visibility = View.GONE
    }

    fun showAgain() {
        floatingView?.visibility = View.VISIBLE
    }

    fun delayDismiss() {
        hasNewPlay = false
        handler.postDelayed({
            LogUtils.i("delayDismiss: hasNewPlay=$hasNewPlay")
            if (!hasNewPlay) {
                // 停止后立马进行播放是无效的，需要延迟
                mExitFloatingPlayListener?.exitFloatingPlayListener()
                dismiss()
            }
        }, 500L)
    }

    private fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        if (isShowing && floatingView != null) {
            // 恢复窗口全屏
            layoutParams.width = screenSize.x
            layoutParams.height = screenSize.y
            layoutParams.x = 0
            layoutParams.y = 0

            windowManager.removeView(floatingView)
            floatingView = null
            isShowing = false
            jni.stopResource(0, SdkVars.localDeviceId)
            LogUtils.i("FloatingPlayerWindow dismissed")
        }

        playerControlView?.callback = null
        playerControlView?.release()
        playerControlView = null
        playerController?.setPlayerViewResetListener(null)
        playerController?.release()
        playerController = null
        memberAdapter.setOnItemCheckedChangeListener(null)
        projectAdapter.setOnItemCheckedChangeListener(null)

        if (screenPopView != null) {
            try {
                windowManager.removeView(screenPopView)
            } catch (e: Exception) {

            }
            screenPopView = null
            LogUtils.i("FloatingPlayerWindow remove screenPopView")
        }

        mExitFloatingPlayListener = null
        currentDeviceId = 0
        currentSubId = 0
        currentMediaId = 0
        currentProgress = 0
    }

    fun updateTitle(title: String) {
        playerControlView?.setTitle(title)
    }

    fun updateFps(fps: Int) {
        playerControlView?.setFps(fps)
    }

    fun getPlayerControlView() = playerControlView

    private fun dp2px(dp: Int) = (dp * appContext.resources.displayMetrics.density).toInt()
    private fun screenSize(): Point {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point
    }

    fun onDestroy() {
        LogUtils.i("onDestroy: ")
        dismiss()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}