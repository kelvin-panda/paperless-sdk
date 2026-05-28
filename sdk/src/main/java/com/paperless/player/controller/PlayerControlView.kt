package com.paperless.player.controller

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.paperless.player.ENDownloadView
import com.paperless.player.ENPlayView
import com.paperless.player.controller.listener.AudioFocusListener
import com.paperless.player.controller.listener.ControlCallback
import com.paperless.sdk.R
import com.paperless.util.CommonUtil
import com.paperless.util.CommonUtil.stringForTime
import java.util.Locale
import kotlin.math.abs

data class MoreMenuItem(
    val id: Int,           // 菜单项唯一标识
    val title: String,     // 菜单项文本
    val iconRes: Int = 0   // 菜单项图标资源ID，0表示无图标
)

/**
 *  @author : Administrator
 *  created on 2025/9/18 10:03
 */
class PlayerControlView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), View.OnClickListener,
    SeekBar.OnSeekBarChangeListener, View.OnTouchListener, AudioFocusListener {

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    var callback: ControlCallback? = null

    //是否展示控制UI
    var showControlView = true

    //是否展示播放时间UI
    var showTimeView = true

    //音频焦点管理器
    private var mAudioFocusManager: AudioFocusManager? = null

    //触摸显示后隐藏的时间
    private var mDismissControlTime: Long = 2500L
    private var mPostDismiss: Boolean = false

    //锁定屏幕点击
    private var mLockCurScreen: Boolean = false

    //右上角的菜单
    private var mMoreMenuItems: List<MoreMenuItem> = emptyList()

    //<editor-fold desc="弹窗">
    //触摸进度dialog
    private var mProgressDialog: Dialog? = null

    //音量dialog
    private var mVolumeDialog: Dialog? = null

    //亮度dialog
    private var mBrightnessDialog: Dialog? = null

    //触摸进度条的progress
    private var mDialogProgressBar: ProgressBar? = null
    private var mDialogSeekTime: TextView? = null
    private var mDialogTotalTime: TextView? = null
    private var mDialogIcon: ImageView? = null

    //音量进度条的progress
    private var mDialogVolumeProgressBar: ProgressBar? = null

    //亮度文本
    private var mBrightnessDialogTv: TextView? = null

    //</editor-fold>

    //<editor-fold desc="拖动">

    //手动改变滑动的位置
    private var mSeekTimePosition: Long = 80L

    //手势偏差值
    private var mThreshold: Int = 80

    //手动滑动的起始偏移位置
    private var mSeekEndOffset: Int = 0

    //手指放下的位置 单位毫秒
    private var mDownPosition: Long = 0

    //当前播放 单位毫秒
    private var mCurPosition: Long = 0

    //总时长 单位毫秒
    private var mTotalPosition: Long = 0

    //手势调节音量的大小
    private var mGestureDownVolume: Int = 0

    //触摸的X
    private var mDownX: Float = 0f

    //触摸的Y
    private var mDownY: Float = 0f

    //移动的Y
    private var mMoveY: Float = 0f

    //触摸滑动进度的比例系数
    private var mSeekRatio: Float = 1f

    //触摸的是否进度条
    private var mTouchingProgressBar: Boolean = false

    //是否改变音量
    private var mChangeVolume: Boolean = false

    //是否改变播放进度
    private var mChangePosition: Boolean = false

    //触摸显示虚拟按键
    private var mShowVKey: Boolean = false

    //是否改变亮度
    private var mBrightness: Boolean = false

    //是否首次触摸
    private var mFirstTouch: Boolean = false
    //</editor-fold>

    //<editor-fold desc="控件">

    //顶部和底部区域
    private var mTopContainer: ViewGroup? = null

    //返回按键
    private var mBackButton: ImageView? = null

    //title
    private var mTitleTextView: TextView? = null

    //fps
    private var mFpsTextView: TextView? = null

    //更多按键
    private var mMoreButton: ImageView? = null

    // 渲染控件父类
    private var mTextureViewContainer: ViewGroup? = null

    //封面父布局
    private var mThumbImageViewLayout: RelativeLayout? = null

    //封面
    private var mThumbImageView: View? = null

    //锁定图标
    private var mLockScreen: ImageView? = null

    private var mBottomContainer: ViewGroup? = null

    //时间显示
    private var mCurrentTimeTextView: TextView? = null

    //进度条
    private var mProgressBar: SeekBar? = null
    private var mTotalTimeTextView: TextView? = null

    //播放按键
    private var mStartButton: View? = null

    //loading view
    private var mLoadingProgressBar: View? = null

    //底部进度条
    private var mBottomProgressBar: ProgressBar? = null

    //</editor-fold>

    //<editor-fold desc="播放状态">
    private val CURRENT_STATE_NORMAL = 0
    private val CURRENT_STATE_PREPAREING = 1
    private val CURRENT_STATE_PLAYING = 2
    private val CURRENT_STATE_PAUSE = 3
    private val CURRENT_STATE_AUTO_COMPLETE = 4
    private val CURRENT_STATE_ERROR = 5

    //当前的播放状态
    private var mCurrentState: Int = -1
    //</editor-fold>

    //<editor-fold desc="缩放平移相关">
    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mVideoView: View? = null           // 当前播放视图（SurfaceView 或 VideoGLSurfaceView）
    private var mMinScale = 0.5f                   // 最小缩放比例
    private var mMaxScale = 3.0f                   // 最大缩放比例
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mIsPanning = false                 // 是否正在平移
    private var mActivePointerId = MotionEvent.INVALID_POINTER_ID  // 用于单指平移的手指ID
    //</editor-fold>

    init {
        mScreenWidth = context.resources.displayMetrics.widthPixels
        mScreenHeight = context.resources.displayMetrics.heightPixels
        LogUtils.i("播放界面宽高: [$mScreenWidth] [$mScreenHeight]")
        initInflate(context)
        initView()
        viewEvent()
        //初始化音频焦点管理器
        initAudioFocusManager()
    }

    /**
     * 设置更多菜单的菜单项列表
     * @param items 菜单项列表
     */
    fun setMoreMenuItems(items: List<MoreMenuItem>) {
        mMoreMenuItems = items
    }

    private fun viewEvent() {
        mStartButton?.setOnClickListener(this)
        mTextureViewContainer?.setOnClickListener(this)
        mTextureViewContainer?.setOnTouchListener(this)
        mProgressBar?.setOnSeekBarChangeListener(this)
        mProgressBar?.setOnTouchListener(this)
        mBottomContainer?.setOnClickListener(this)
        mThumbImageViewLayout?.setOnClickListener(this)

        if (mThumbImageView != null && mThumbImageViewLayout != null) {
            mThumbImageViewLayout?.removeAllViews()
            resolveThumbImage(mThumbImageView!!)
        }
        mBackButton?.setOnClickListener(this)
        mMoreButton?.setOnClickListener(this)
        mLockScreen?.setOnClickListener(this)
    }

    private fun initView() {
        mTopContainer = findViewById(R.id.layout_top)
        mBackButton = findViewById(R.id.back)
        mTitleTextView = findViewById(R.id.title)
        mFpsTextView = findViewById(R.id.fps)
        mMoreButton = findViewById(R.id.more)

        mTextureViewContainer = findViewById(R.id.surface_container)

        mThumbImageViewLayout = findViewById<RelativeLayout?>(R.id.thumb)?.apply { visibility = INVISIBLE }

        mBottomContainer =
            findViewById<ViewGroup?>(R.id.layoutBottom)?.apply {
                visibility = if (showControlView) VISIBLE else INVISIBLE
            }
        findViewById<LinearLayout?>(R.id.layoutTime)?.apply {
            visibility = if (showTimeView) VISIBLE else INVISIBLE
        }
        mCurrentTimeTextView = findViewById(R.id.current)
        mProgressBar = findViewById(R.id.progress)
        mTotalTimeTextView = findViewById(R.id.total)

        mStartButton =
            findViewById<View?>(R.id.start)?.apply { visibility = if (showControlView) VISIBLE else INVISIBLE }
        mLoadingProgressBar = findViewById(R.id.loading)
        mLockScreen = findViewById<ImageView?>(R.id.lock_screen)?.apply { visibility = INVISIBLE }

        mBottomProgressBar = findViewById<ProgressBar?>(R.id.bottom_progressbar)?.apply {
            visibility = if (showControlView) VISIBLE else INVISIBLE
        }
    }

    private fun initInflate(context: Context) {
        inflate(context, R.layout.video_control_layout, this)
    }

    //<editor-fold desc="音频焦点管理器">
    private fun initAudioFocusManager() {
        if (mAudioFocusManager == null) {
            mAudioFocusManager = AudioFocusManager()
        }
        mAudioFocusManager!!.initialize(context, this)
        // 初始化双指缩放检测器
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        // 允许容器内的视图超出边界显示（使缩放后的视图不被裁剪）
        mTextureViewContainer?.clipChildren = false
    }

    private fun getAudioManager(): AudioManager? {
        return mAudioFocusManager?.getAudioManager()
    }

    private fun releaseAudioFocusManager() {
        if (mAudioFocusManager != null) {
            mAudioFocusManager!!.release()
            mAudioFocusManager = null
        }
    }
    //</editor-fold>

    private fun resolveThumbImage(thumb: View) {
        mThumbImageViewLayout?.let {
            it.removeAllViews()
            it.addView(thumb)
            thumb.layoutParams = it.layoutParams.apply {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.MATCH_PARENT
            }
        }
    }

    fun preparePlay() {
        //请求音频焦点
        mAudioFocusManager?.requestAudioFocus()
        setStateAndUi(CURRENT_STATE_PREPAREING)
        startDismissControlViewTimer()
    }

    fun startPlay() {
        setStateAndUi(CURRENT_STATE_PLAYING)
        startDismissControlViewTimer()
    }

    fun setPlayView(view: View) {
        mVideoView = view   // 保存引用
        mTextureViewContainer?.addView(
            view, RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            })
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.start -> {//开始图标
                    clickStartIcon()
                }

                R.id.surface_container -> {//渲染器
                    //显示或隐藏控制图标
                    LogUtils.e("播放器点击: ")
                    startDismissControlViewTimer()
                }

                R.id.back -> {//返回
                    callback?.onBack()
                }

                R.id.more -> {//更多
                    showMoreMenu()
                }

                R.id.lock_screen -> {
                    if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE || mCurrentState == CURRENT_STATE_ERROR) {
                        return
                    }
                    lockTouchLogic()
                }

                else -> {}
            }
        }
    }

    private fun showMoreMenu() {
        if (mMoreMenuItems.isEmpty()) return

        val popupMenu = PopupMenu(context, mMoreButton)
        // 使图标在菜单中显示（API < 28 需要）
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
            try {
                val field = PopupMenu::class.java.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuPopupHelper = field.get(popupMenu)
                val setForceShowIcon =
                    menuPopupHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setForceShowIcon.invoke(menuPopupHelper, true)
            } catch (e: Exception) {
                // 忽略反射失败
            }
        }

        // 动态添加菜单项
        mMoreMenuItems.forEach { item ->
            val menuItem = popupMenu.menu.add(0, item.id, 0, item.title)
            if (item.iconRes != 0) {
                menuItem.setIcon(item.iconRes)
            }
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            callback?.onMoreMenuItemClick(menuItem.itemId)
            true
        }
        popupMenu.show()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (mLockCurScreen) {
            onClickUiToggle(event!!)
            startDismissControlViewTimer()
            return true
        }
        val x = event!!.x
        val y = event.y
        when (v!!.id) {
            R.id.surface_container -> {
                // 必须让缩放检测器处理事件，以便检测双指缩放
                mScaleGestureDetector?.onTouchEvent(event)

                // 如果正在缩放 或者 画面已经被缩放，则进入缩放/平移模式
                if (mScaleGestureDetector?.isInProgress == true || (mVideoView?.scaleX ?: 1f) != 1f) {
                    handleScaleAndPanTouch(event)
                    return true
                }

                val view = mVideoView
                // 确保 scale=1 时复位平移
                view?.let {
                    it.translationX = 0f
                    it.translationY = 0f
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchSurfaceDown(x, y)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = x - mDownX
                        val deltaY = y - mDownY
                        val absDeltaX = abs(deltaX.toDouble()).toFloat()
                        val absDeltaY = abs(deltaY.toDouble()).toFloat()

                        if (!mChangePosition && !mChangeVolume && !mBrightness) {
                            touchSurfaceMoveFullLogic(absDeltaX, absDeltaY)
                        }

                        touchSurfaceMove(deltaX, deltaY, y)
                    }

                    MotionEvent.ACTION_UP -> {
                        LogUtils.e(
                            "触摸抬起: mChangePosition[$mChangePosition] mChangeVolume[$mChangeVolume] mBrightness[$mBrightness]"
                        )
                        startDismissControlViewTimer()

                        touchSurfaceUp()
                    }
                }
                gestureDetector.onTouchEvent(event)
            }
            // 拖动时处理取消隐藏界面
            R.id.progress -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelDismissControlViewTimer()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        var vpdown = parent
                        while (vpdown != null) {
                            vpdown.requestDisallowInterceptTouchEvent(true)
                            vpdown = vpdown.parent
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        startDismissControlViewTimer()
                        var vpup = parent
                        while (vpup != null) {
                            vpup.requestDisallowInterceptTouchEvent(false)
                            vpup = vpup.parent
                        }
                    }
                }
            }
        }
        return false
    }

    private fun handleScaleAndPanTouch(event: MotionEvent): Boolean {
        // 让手势检测器也能工作（支持双击重置）
        gestureDetector.onTouchEvent(event)

        val view = mVideoView ?: return true
        val action = event.actionMasked
        val pointerCount = event.pointerCount

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = event.getPointerId(0)
                mLastTouchX = event.getX(0)
                mLastTouchY = event.getY(0)
                mIsPanning = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果正在缩放，不处理平移
                if (mScaleGestureDetector?.isInProgress == true) {
                    return true
                }

                // 单指平移（修复：去掉 !mIsPanning 条件）
                if (pointerCount == 1) {
                    val index = event.findPointerIndex(mActivePointerId)
                    if (index == MotionEvent.INVALID_POINTER_ID) return true

                    val x = event.getX(index)
                    val y = event.getY(index)
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY

                    // 移动超过阈值才激活平移模式
                    if (!mIsPanning && (abs(dx) > 10 || abs(dy) > 10)) {
                        mIsPanning = true
                    }

                    if (mIsPanning) {
                        view.translationX += dx
                        view.translationY += dy
                        clampTranslation()
                    }

                    mLastTouchX = x
                    mLastTouchY = y
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsPanning = false
                mActivePointerId = MotionEvent.INVALID_POINTER_ID
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val newPointerIndex = event.actionIndex
                mActivePointerId = event.getPointerId(newPointerIndex)
                mLastTouchX = event.getX(newPointerIndex)
                mLastTouchY = event.getY(newPointerIndex)
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        mActivePointerId = event.getPointerId(newPointerIndex)
                        mLastTouchX = event.getX(newPointerIndex)
                        mLastTouchY = event.getY(newPointerIndex)
                    } else {
                        mActivePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
                return true
            }
        }
        return true
    }

    private fun touchSurfaceDown(x: Float, y: Float) {
        mTouchingProgressBar = true
        mDownX = x
        mDownY = y
        mMoveY = 0f
        mChangeVolume = false
        mChangePosition = false
        mBrightness = false
        mFirstTouch = true
    }

    private fun touchSurfaceMove(deltaX: Float, deltaY: Float, y: Float) {
        val curWidth = mScreenWidth
        val curHeight = mScreenHeight
        if (mChangePosition) {
            if (!showControlView) {
                //不可显示控制相关的UI，所以不处理进度拖动
                return
            }
            val totalTimeDuration: Long = mTotalPosition
            mSeekTimePosition = (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio).toInt().toLong()
            if (mSeekTimePosition < 0) mSeekTimePosition = 0
            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition = totalTimeDuration

            val seekTime: String = stringForTime(mSeekTimePosition)
            val totalTime: String = stringForTime(totalTimeDuration)
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration)
        } else if (mChangeVolume) {
            val newDeltaY = -deltaY
            getAudioManager()?.let {
                val max = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val deltaV = (max * newDeltaY * 3 / curHeight).toInt()
                it.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0)
                val volumePercent = (mGestureDownVolume * 100 / max + newDeltaY * 3 * 100 / curHeight).toInt()
                showVolumeDialog(-newDeltaY, volumePercent)
            }
        } else if (mBrightness) {
            if (Math.abs(deltaY) > mThreshold) {
                val percent = (-deltaY / curHeight)
                onBrightnessSlide(percent)
                mDownY = y
            }
        }
    }

    private fun touchSurfaceUp() {
        if (mChangePosition) {
            val duration: Long = mTotalPosition
            val progress = mSeekTimePosition * 100 / (if (duration == 0L) 1 else duration)
            if (mBottomProgressBar != null) mBottomProgressBar!!.progress = progress.toInt()
        }

        mTouchingProgressBar = false
        dismissProgressDialog()
        dismissVolumeDialog()
        dismissBrightnessDialog()
        if (mChangePosition && (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE)) {
            val duration: Long = mTotalPosition
            val progress = mSeekTimePosition * 100 / (if (duration == 0L) 1 else duration)
            callback?.seekTo(progress.toInt())
            if (mProgressBar != null) {
                mProgressBar!!.progress = progress.toInt()
            }
        } /*else if (mBrightness) {
            callback?.onTouchScreenSeekLight()
        } else if (mChangeVolume) {
            callback?.onTouchScreenSeekVolume()
        }*/
    }

    private fun touchSurfaceMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {
        val curWidth = mScreenWidth
        if (absDeltaX > mThreshold || absDeltaY > mThreshold) {
            if (absDeltaX >= mThreshold) {//调整进度
                //防止全屏虚拟按键
                val screenWidth: Int = CommonUtil.getScreenWidth(context)
                if (abs((screenWidth - mDownX).toDouble()) > mSeekEndOffset) {
                    mChangePosition = true
                    mDownPosition = mCurPosition
                } else {
                    mShowVKey = true
                }
            } else {
                val screenHeight: Int = CommonUtil.getScreenHeight(context)
                val noEnd: Boolean = abs((screenHeight - mDownY).toDouble()) > mSeekEndOffset
                if (mFirstTouch) { //调整亮度
                    mBrightness = (mDownX < curWidth * 0.5f) && noEnd
                    mFirstTouch = false
                }
                if (!mBrightness) {//调整音量
                    mChangeVolume = noEnd
                    val audioManager: AudioManager? = mAudioFocusManager?.getAudioManager()
                    if (audioManager != null) {
                        mGestureDownVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    }
                }
                mShowVKey = !noEnd
            }
        }
    }

    private fun onBrightnessSlide(percent: Float) {
        //callback?.onBrightnessSlide(percent)
        val act = context as Activity
        var brightness = act.window.attributes.screenBrightness
        if (brightness <= 0.00f) {
            brightness = 0.5f
        } else if (brightness < 0.01f) {
            brightness = 0.01f
        }
        val lpa = act.window.attributes
        lpa.screenBrightness = brightness + percent
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f
        }
        showBrightnessDialog(lpa.screenBrightness)
        act.window.attributes = lpa
    }

    private fun showBrightnessDialog(percent: Float) {
        if (mBrightnessDialog == null) {
            val localView = LayoutInflater.from(context).inflate(R.layout.video_brightness, null)
            mBrightnessDialogTv = localView.findViewById(R.id.app_video_brightness)
            mBrightnessDialog = Dialog(context, R.style.video_style_dialog_progress).apply {
                setContentView(localView)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                window!!.decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                window!!.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }
            val attributes = mBrightnessDialog!!.window!!.attributes
            mBrightnessDialog!!.window!!.attributes = attributes.apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                width = mScreenWidth
                width = mScreenHeight
                val location = IntArray(2)
                getLocationOnScreen(location)
                x = location[0]
                y = location[1]
            }
        }
        if (!mBrightnessDialog!!.isShowing) {
            mBrightnessDialog!!.show()
        }
        mBrightnessDialogTv?.text = "${(percent * 100).toInt()} %"
    }

    private fun showVolumeDialog(deltaY: Float, volumePercent: Int) {
        if (mVolumeDialog == null) {
            val localView = LayoutInflater.from(context).inflate(R.layout.video_volume_dialog, null)
            mDialogVolumeProgressBar = localView.findViewById(R.id.volume_progressbar)
            mVolumeDialog = Dialog(context, R.style.video_style_dialog_progress).apply {
                setContentView(localView)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                window!!.setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            }
            val attributes = mVolumeDialog!!.window!!.attributes
            mVolumeDialog!!.window!!.attributes = attributes.apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                width = mScreenWidth
                width = mScreenHeight
                val location = IntArray(2)
                getLocationOnScreen(location)
                x = location[0]
                y = location[1]
            }
        }
        if (!mVolumeDialog!!.isShowing) {
            mVolumeDialog!!.show()
        }
        mDialogVolumeProgressBar?.progress = volumePercent
    }

    private fun showProgressDialog(
        deltaX: Float,
        seekTime: String,
        seekTimePosition: Long,
        totalTime: String,
        totalTimeDuration: Long
    ) {
        if (mProgressDialog == null) {
            val localView = LayoutInflater.from(context).inflate(R.layout.video_progress_dialog, null)
            mDialogProgressBar = localView.findViewById(R.id.duration_progressbar)
            mDialogSeekTime = localView.findViewById(R.id.tv_current)
            mDialogTotalTime = localView.findViewById(R.id.tv_duration)
            mDialogIcon = localView.findViewById(R.id.duration_image_tip)
            mProgressDialog = Dialog(context, R.style.video_style_dialog_progress).apply {
                setContentView(localView)
                window!!.addFlags(Window.FEATURE_ACTION_BAR)
                window!!.addFlags(32)
                window!!.addFlags(16)
                window!!.setLayout(mScreenWidth, mScreenHeight)
            }
            val attributes = mProgressDialog!!.window!!.attributes
            mProgressDialog!!.window!!.attributes = attributes.apply {
                gravity = Gravity.TOP
                width = mScreenWidth
                width = mScreenHeight
                val location = IntArray(2)
                getLocationOnScreen(location)
                x = location[0]
                y = location[1]
            }
        }
        if (!mProgressDialog!!.isShowing) {
            mProgressDialog!!.show()
        }
        mDialogSeekTime?.text = seekTime
        mDialogTotalTime?.text = "/ $totalTime"
        if (totalTimeDuration > 0) {
            val toInt = (seekTimePosition * 100 / totalTimeDuration).toInt()
            LogUtils.i("showProgressDialog: 设置进度[$toInt]")
            mDialogProgressBar?.progress = toInt
        }
        mDialogIcon?.setBackgroundResource(if (deltaX > 0) R.drawable.video_forward_icon else R.drawable.video_backward_icon)
    }


    private fun dismissBrightnessDialog() {
        if (mBrightnessDialog != null) {
            mBrightnessDialog!!.dismiss()
            mBrightnessDialog = null
        }
    }

    private fun dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
            mProgressDialog = null
        }
    }

    private fun dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog!!.dismiss()
            mVolumeDialog = null
        }
    }


    //<editor-fold desc="控制UI操作后定时隐藏">

    private var dismissControlTask: Runnable = object : Runnable {
        override fun run() {
            if (mCurrentState != CURRENT_STATE_NORMAL && mCurrentState != CURRENT_STATE_ERROR && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                hideAllWidget()
                setViewShowState(mLockScreen, GONE)
                if (mPostDismiss) {
                    postDelayed(this, mDismissControlTime)
                }
            }
        }
    }

    private fun startDismissControlViewTimer() {
        cancelDismissControlViewTimer()
        mPostDismiss = true
        postDelayed(dismissControlTask, mDismissControlTime)
    }

    private fun cancelDismissControlViewTimer() {
        mPostDismiss = false
        removeCallbacks(dismissControlTask)
    }

    //</editor-fold>

    // 开始或暂停
    private fun clickStartIcon() {
        if (!showControlView) return
        updateStartImage()
        if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR) {
            //开始播放
            setStateAndUi(CURRENT_STATE_PREPAREING)//更新ui
        } else if (mCurrentState == CURRENT_STATE_PLAYING) {
            //暂停播放
            setStateAndUi(CURRENT_STATE_PAUSE)//更新ui
            callback?.pause()
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            //恢复播放
            setStateAndUi(CURRENT_STATE_PLAYING)//更新ui
            callback?.start()
        } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            //播放结束，再次播放
            setStateAndUi(CURRENT_STATE_PREPAREING)//更新ui
        }
    }

    private fun lockTouchLogic() {
        if (mLockCurScreen) {
            mLockScreen!!.setImageResource(R.drawable.unlock)
            mLockCurScreen = false
        } else {
            mLockScreen!!.setImageResource(R.drawable.lock)
            mLockCurScreen = true
            hideAllWidget()
        }
    }

    private fun updateStartImage() {
        LogUtils.i("updateStartImage: $mCurrentState")
        if (mStartButton is ENPlayView) {
            val enPlayView: ENPlayView = mStartButton as ENPlayView
            enPlayView.setDuration(500)
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                enPlayView.play()
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                enPlayView.pause()
            } else {
                enPlayView.pause()
            }
        }
    }

    private fun setStateAndUi(state: Int) {
        mCurrentState = state
        resolveUIState(state)
    }

    private fun resolveUIState(state: Int) {
        when (state) {
            CURRENT_STATE_NORMAL -> {
                changeUiToNormal()
                cancelDismissControlViewTimer()
            }

            CURRENT_STATE_PREPAREING -> {
                changeUiToPreparingShow()
                startDismissControlViewTimer()
            }

            CURRENT_STATE_PLAYING -> {
                changeUiToPlayingShow()
                startDismissControlViewTimer()
            }

            CURRENT_STATE_PAUSE -> {
                changeUiToPauseShow()
                cancelDismissControlViewTimer()
            }

            CURRENT_STATE_AUTO_COMPLETE -> {}
            CURRENT_STATE_ERROR -> {}
        }
    }

    //<editor-fold desc="根据状态变化UI">

    private fun changeUiToPauseShow() {
        if (mLockCurScreen) {
            setViewShowState(mLockScreen, VISIBLE)
            return
        }
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, VISIBLE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
        updateStartImage()
    }

    private fun changeUiToPlayingShow() {
        if (mLockCurScreen) {
            setViewShowState(mLockScreen, VISIBLE)
            return
        }

        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, VISIBLE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
        updateStartImage()
    }

    private fun changeUiToPreparingShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
        setViewShowState(mLoadingProgressBar, VISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, GONE)

        if (mLoadingProgressBar is ENDownloadView) {
            val enDownloadView = mLoadingProgressBar as ENDownloadView
            if (enDownloadView.currentState == ENDownloadView.STATE_PRE) {
                (mLoadingProgressBar as ENDownloadView).start()
            }
        }
    }

    private fun changeUiToNormal() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, VISIBLE)

        updateStartImage()
        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
    }

    //</editor-fold>

    //<editor-fold desc="底部进度">

    fun setTitle(title: String) {
        mTitleTextView?.text = title
    }

    fun setFps(fps: Int) {
        mFpsTextView?.text = String.format(Locale.getDefault(), "%d f/s", fps)
    }

    fun setProgressAndTime(progress: Long, secProgress: Long, currentTime: Long, totalTime: Long, forceChange: Boolean) {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return
        }
        mCurPosition = currentTime
        mTotalPosition = totalTime
        if (progress > 0 || forceChange) {
            mProgressBar?.progress = progress.toInt()
        }

        // 获取控制内核当前播放百分比
        setSecondaryProgress(secProgress)
        mTotalTimeTextView?.text = stringForTime(totalTime)
        if (currentTime > 0L) {
            mCurrentTimeTextView?.text = stringForTime(currentTime)
        }
        mBottomProgressBar?.let {
            if (progress > 0 || forceChange) {
                it.progress = progress.toInt()
                setSecondaryProgress(progress)
            }
        }
    }

    private fun setSecondaryProgress(secProgress: Long) {
        if (secProgress == 0L) return
        mProgressBar?.secondaryProgress = secProgress.toInt()
        mBottomProgressBar?.secondaryProgress = secProgress.toInt()
    }

    //</editor-fold>

    private fun changeUiToCompleteShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, VISIBLE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
        updateStartImage()
    }

    private fun changeUiToCompleteClear() {
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mLockScreen, VISIBLE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
        updateStartImage()
    }

    private fun changeUiToPauseClear() {
        changeUiToClear()
        setViewShowState(mBottomProgressBar, if (showControlView) VISIBLE else INVISIBLE)
    }

    private fun changeUiToPrepareingClear() {
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, GONE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
    }

    private fun changeUiToPlayingClear() {
        changeUiToClear()
        setViewShowState(mBottomProgressBar, if (showControlView) VISIBLE else INVISIBLE)
    }

    private fun changeUiToClear() {
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        setViewShowState(mLockScreen, GONE)

        if (mLoadingProgressBar is ENDownloadView) {
            (mLoadingProgressBar as ENDownloadView).reset()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        callback?.seekTo(seekBar?.progress!!)
    }

    private fun hideAllWidget() {
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomProgressBar, if (showControlView) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
    }

    private fun setViewShowState(view: View?, visibility: Int) {
        view?.visibility = visibility
    }

    // 双击
    private var gestureDetector: GestureDetector =
        GestureDetector(getContext().applicationContext, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                touchDoubleUp(e)
                // 重置缩放
                resetVideoScale()
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onClickUiToggle(e)
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                touchLongPress(e)
            }
        })

    private fun resetVideoScale() {
        mVideoView?.let {
            it.scaleX = 1f
            it.scaleY = 1f
            it.translationX = 0f
            it.translationY = 0f
            // 将 pivot 重置为视图中心
            it.pivotX = (it.width / 2).toFloat()
            it.pivotY = (it.height / 2).toFloat()
        }
    }

    // 长按
    private fun touchLongPress(e: MotionEvent) {
    }

    // 点击触摸显示和隐藏逻辑
    private fun onClickUiToggle(e: MotionEvent) {
        LogUtils.e("mCurrentState: $mCurrentState,mLockCurScreen:$mLockCurScreen")
        if (mLockCurScreen) {
            setViewShowState(mLockScreen, VISIBLE)
            return
        }
        if (mCurrentState == CURRENT_STATE_PREPAREING) {
            if (mBottomContainer != null) {
                if (mBottomContainer!!.visibility == VISIBLE) {
                    changeUiToPrepareingClear()
                } else {
                    changeUiToPreparingShow()
                }
            }
        } else if (mCurrentState == CURRENT_STATE_PLAYING) {
            if (mBottomContainer != null) {
                if (mBottomContainer!!.visibility == VISIBLE) {
                    changeUiToPlayingClear()
                } else {
                    changeUiToPlayingShow()
                }
            }
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            if (mBottomContainer != null) {
                if (mBottomContainer!!.visibility == VISIBLE) {
                    changeUiToPauseClear()
                } else {
                    changeUiToPauseShow()
                }
            }
        } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (mBottomContainer != null) {
                if (mBottomContainer!!.visibility == VISIBLE) {
                    changeUiToCompleteClear()
                } else {
                    changeUiToCompleteShow()
                }
            }
        }
    }

    // 双击
    private fun touchDoubleUp(e: MotionEvent) {
        clickStartIcon()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismissVolumeDialog()
        dismissBrightnessDialog()
        cancelDismissControlViewTimer()
        //释放音频焦点管理器资源
        releaseAudioFocusManager()
        mVideoView = null
        mScaleGestureDetector = null
    }

    override fun onAudioFocusGain() {
        TODO("Not yet implemented")
    }

    override fun onAudioFocusLoss() {
        TODO("Not yet implemented")
    }

    override fun onAudioFocusLossTransient() {
        TODO("Not yet implemented")
    }

    override fun onAudioFocusLossTransientCanDuck() {
        TODO("Not yet implemented")
    }

    private fun clampTranslation() {
        val view = mVideoView ?: return
        val scale = view.scaleX
        if (scale <= 1.0f) {
            // 如果未放大，无需平移限制，直接复位
            view.translationX = 0f
            view.translationY = 0f
            return
        }

        // 获取父容器宽高
        val parent = view.parent as? ViewGroup ?: return
        val parentWidth = parent.width
        val parentHeight = parent.height

        // 计算缩放后的视图尺寸
        val scaledWidth = view.width * scale
        val scaledHeight = view.height * scale

        // 计算最大可平移范围
        val maxTranslateX = (scaledWidth - parentWidth) / 2
        val maxTranslateY = (scaledHeight - parentHeight) / 2

        // 限制平移量
        var tx = view.translationX
        var ty = view.translationY
        tx = tx.coerceIn(-maxTranslateX, maxTranslateX)
        ty = ty.coerceIn(-maxTranslateY, maxTranslateY)

        view.translationX = tx
        view.translationY = ty
    }

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val view = mVideoView ?: return false

            // 当前缩放比例
            val oldScale = view.scaleX
            var newScale = oldScale * detector.scaleFactor
            newScale = newScale.coerceIn(mMinScale, mMaxScale)

            if (newScale == oldScale) return true

            // 计算焦点在父容器中的坐标
            val focusX = detector.focusX
            val focusY = detector.focusY

            // 为了以手指为中心缩放，需要同步调整 translation
            // 保持焦点对应的内容点不变
            val deltaScale = newScale - oldScale
            // 注意：默认 pivot 是视图中心 (width/2, height/2)，这里我们需要根据 pivot 调整 translation
            val pivotX = view.pivotX
            val pivotY = view.pivotY

            view.translationX += (focusX - pivotX) * deltaScale / oldScale
            view.translationY += (focusY - pivotY) * deltaScale / oldScale

            view.scaleX = newScale
            view.scaleY = newScale

            // 限制平移范围，防止画面移出可视区域过多
            clampTranslation()

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // 可以留空，或用于记录最后状态
        }

    }

}