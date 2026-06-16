package com.paperless.player.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.blankj.utilcode.util.LogUtils
import com.paperless.player.PlayerController
import com.paperless.player.controller.PlayerControlView
import com.paperless.player.controller.listener.ControlCallback
import com.paperless.sdk.BaseJni
import com.paperless.sdk.R
import com.paperless.sdk.SdkVars

class FloatingPlayerWindow(private val context: Context) {

    companion object {
        private const val MIN_SIZE_RATIO = 1 / 3f
        private const val DRAG_BAR_HEIGHT_DP = 40
        private const val RESIZE_HOTSPOT_SIZE_DP = 36
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var playerControlView: PlayerControlView? = null
    private var playerController: PlayerController? = null
    private var isShowing = false

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

    fun show(surfaceView: SurfaceView, title: String) {
        if (isShowing) return
        playerController = PlayerController(0, onSurfaceReady = {
            LogUtils.i("FloatingPlayer: Surface ready, decoding started")
        }).apply {
            initialize(surfaceView)   // 绑定 Surface，内部会监听 surfaceCreated
        }
        createFloatingView(surfaceView, title)
        windowManager.addView(floatingView, layoutParams)
        isShowing = true
        LogUtils.i("FloatingPlayerWindow shown")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView(surfaceView: View, title: String) {
        // 根布局：FrameLayout，所有子视图叠加
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 半透明背景，更美观
            setBackgroundColor(0xDD000000.toInt())
        }

        // 1. PlayerControlView（视频播放控制层）
        playerControlView = PlayerControlView(context).apply {
            setFloatingMode(true)
            setPlayView(surfaceView)
            setTitle(title)
            preparePlay()
            startPlay()
            setDragWindowTouchListener(dragBarTouchListener) //拖动标题栏
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
                dismiss()
            }

            override fun onBrightnessSlide(percent: Float) {

            }

            override fun toggleScreen() {
                toggleFullscreen()
            }


            override fun onMoreMenuItemClick(itemId: Int) {
                println("onMoreMenuItemClick: $itemId")
                when (itemId) {
                    1 -> {
                        surfaceView.post {
                            val bitmap = createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
//                            PixelCopy.request(surfaceView, bitmap, listener = object : PixelCopy.OnPixelCopyFinishedListener {
//                                override fun onPixelCopyFinished(copyResult: Int) {
//                                    if (copyResult == PixelCopy.SUCCESS) {
//
//                                    }
//                                }
//                            }, Handler(Looper.getMainLooper()))
                        }
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
        val resizeHandle = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp2px(RESIZE_HOTSPOT_SIZE_DP),
                dp2px(RESIZE_HOTSPOT_SIZE_DP)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
//                bottomMargin = dp2px(8)
//                rightMargin = dp2px(8)
            }
//            setBackgroundResource(android.R.drawable.ic_menu_rotate) // 替换为实际图标
            setBackgroundResource(R.drawable.video_shrink) // 替换为实际图标
            setOnTouchListener(resizeHandleTouchListener)
        }
        root.addView(resizeHandle)

        floatingView = root
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

    fun dismiss() {
        if (isShowing && floatingView != null) {
            playerControlView?.release()
            windowManager.removeView(floatingView)
            floatingView = null
            playerControlView = null
            isShowing = false
            BaseJni().stopResource(0, SdkVars.localDeviceId)
            LogUtils.i("FloatingPlayerWindow dismissed")
        }
    }

    fun updateTitle(title: String) {
        playerControlView?.setTitle(title)
    }

    fun updateFps(fps: Int) {
        playerControlView?.setFps(fps)
    }

    fun getPlayerControlView() = playerControlView

    private fun dp2px(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
    private fun screenSize(): Point {
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        return point
    }
}