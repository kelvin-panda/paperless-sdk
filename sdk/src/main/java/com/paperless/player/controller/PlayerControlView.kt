package com.paperless.player.controller

import android.app.Activity
import android.app.Dialog
import android.content.Context
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.paperless.player.ENDownloadView
import com.paperless.player.ENPlayView
import com.paperless.player.controller.listener.ControlCallback
import com.paperless.sdk.R
import com.paperless.util.CommonUtil
import com.paperless.util.CommonUtil.stringForTime
import java.util.Locale
import kotlin.math.abs

class PlayerControlView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), View.OnClickListener,
    SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    companion object {

        /**
         * 播放视频文件模式
         * - 所有功能都可使用
         * - 音量调节、进度调节、亮度调节 则依赖相关开关项：mEnableVolumeGesture、mEnableSeekGesture、mEnableBrightnessGesture
         */
        val video_flag = 0x1        // 二进制 0001

        /**
         * 播放流媒体模式
         * - 流媒体无法暂停播放、开始播放、拖动进度，底部进度模块和播放模块不可响应，且必须隐藏
         * - - 音量调节、亮度调节 则依赖相关开关项：mEnableVolumeGesture、mEnableBrightnessGesture
         */
        val stream_flag = 0x2       // 二进制 0010

        /**
         * 强制播放模式
         * - 只可显示标题、fps和full全屏按钮
         * - 退出强制播放模式时回退到相应的其它模式下的表现
         */
        val mandatory_flag = 0x4    // 二进制 0100
    }

    private var curPlayFlag = video_flag
    private var isMandatory = false
    private var isStream = false
    private var isVideo = false

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    var callback: ControlCallback? = null

    private var mEnableSeekGesture = false      // 是否允许进度滑动
    private var mDismissControlTime: Long = 5_000L
    private var mPostDismiss: Boolean = false

    // 悬浮窗模式：禁用所有 Dialog
    private var mFloatingMode = false

    private var mEnableFull = false // 是否展示全屏按钮

    // Dialog 相关
    private var mProgressDialog: Dialog? = null
    private var mDialogProgressBar: ProgressBar? = null
    private var mDialogSeekTime: TextView? = null
    private var mDialogTotalTime: TextView? = null
    private var mDialogIcon: ImageView? = null

    //<editor-fold desc="手势相关变量">
    private var mSeekTimePosition: Long = 80L
    private var mThreshold: Int = 80
    private var mSeekEndOffset: Int = 0
    private var mDownPosition: Long = 0
    private var mCurPosition: Long = 0
    private var mTotalPosition: Long = 0
    private var mDownX: Float = 0f
    private var mDownY: Float = 0f
    private var mMoveY: Float = 0f
    private var mSeekRatio: Float = 1f
    private var mTouchingProgressBar: Boolean = false
    private var mChangePosition: Boolean = false
    private var mShowVKey: Boolean = false
    private var mFirstTouch: Boolean = false

    //</editor-fold>

    //<editor-fold desc="控件">
    // 控件
    private var mTopContainer: ViewGroup? = null
    private var mBackButton: ImageView? = null
    private var mTitleTextView: TextView? = null
    private var mFpsTextView: TextView? = null
    private var mFullImageView: ImageView? = null
    private var mStartScreen: View? = null
    private var mStopScreen: View? = null
    private var mTextureViewContainer: ViewGroup? = null
    private var mThumbImageViewLayout: RelativeLayout? = null
    private var mThumbImageView: View? = null
    private var mBottomContainer: ViewGroup? = null
    private var mCurrentTimeTextView: TextView? = null
    private var mProgressBar: SeekBar? = null
    private var mTotalTimeTextView: TextView? = null
    private var mStartButton: View? = null
    private var mLoadingProgressBar: View? = null
    private var mBottomProgressBar: ProgressBar? = null

    private var mBtnPlay: Button? = null
    private var mBtnCapture: Button? = null
    private var mBtnSameScreen: Button? = null
    private var mBtnStop: Button? = null

    //</editor-fold>

    // 播放状态
    private val CURRENT_STATE_NORMAL = 0
    private val CURRENT_STATE_PREPAREING = 1
    private val CURRENT_STATE_PLAYING = 2
    private val CURRENT_STATE_PAUSE = 3
    private val CURRENT_STATE_AUTO_COMPLETE = 4
    private val CURRENT_STATE_ERROR = 5
    private var mCurrentState: Int = -1

    // 缩放平移相关
    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mVideoView: View? = null
    private var mMinScale = 0.5f
    private var mMaxScale = 3.0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mIsPanning = false
    private var mActivePointerId = MotionEvent.INVALID_POINTER_ID

    init {
        mScreenWidth = ScreenUtils.getScreenWidth() // context.resources.displayMetrics.widthPixels
        mScreenHeight = ScreenUtils.getScreenHeight() // context.resources.displayMetrics.heightPixels
        LogUtils.i("播放界面宽高: [$mScreenWidth] [$mScreenHeight]")
        initInflate(context)
        initView()
        viewEvent()
        mScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        mTextureViewContainer?.clipChildren = false
    }

    fun setFullEnabled(enabled: Boolean) {
        mEnableFull = enabled
        mFullImageView?.visibility = if (enabled) VISIBLE else GONE
    }

    /**
     * 设置悬浮窗模式（例如用于悬浮窗播放）
     * 在悬浮窗模式下，会禁用亮度调节和所有 Dialog，避免因 Activity 依赖导致崩溃
     */
    fun setFloatingMode(enable: Boolean) {
        mFloatingMode = enable
        if (enable) {
            LogUtils.i("PlayerControlView 已切换到悬浮窗模式，将禁用亮度调节和弹窗")
        }
    }

    /**
     * 设置是否允许进度滑动（左右滑动调节播放进度）
     * @param enabled true 允许，false 禁用
     */
    fun setSeekGestureEnabled(enabled: Boolean) {
        mEnableSeekGesture = enabled
    }

    private fun viewEvent() {
        mStartButton?.setOnClickListener(this)
        mTextureViewContainer?.setOnClickListener(this)
        mTextureViewContainer?.setOnTouchListener(this)
        mProgressBar?.setOnSeekBarChangeListener(this)
        mProgressBar?.setOnTouchListener(this)
        mBottomContainer?.setOnClickListener(this)
        mThumbImageViewLayout?.setOnClickListener(this)
        mBtnPlay?.setOnClickListener(this)
        mBtnCapture?.setOnClickListener(this)
        mBtnSameScreen?.setOnClickListener(this)
        mBtnStop?.setOnClickListener(this)
        if (mThumbImageView != null && mThumbImageViewLayout != null) {
            mThumbImageViewLayout?.removeAllViews()
            resolveThumbImage(mThumbImageView!!)
        }
        mBackButton?.setOnClickListener(this)
        mStartScreen?.setOnClickListener(this)
        mStopScreen?.setOnClickListener(this)
        mFullImageView?.setOnClickListener(this)
    }

    private fun initView() {
        mTopContainer = findViewById(R.id.layout_top)
        mBackButton = findViewById(R.id.back)
        mTitleTextView = findViewById(R.id.title)
        mFpsTextView = findViewById(R.id.fps)
        mFullImageView = findViewById<ImageView>(R.id.full)?.apply {
            visibility = if (mEnableFull) VISIBLE else GONE
        }
        mStartScreen = findViewById(R.id.startScreen)
        mStopScreen = findViewById(R.id.stopScreen)
        mTextureViewContainer = findViewById(R.id.surface_container)
        mThumbImageViewLayout = findViewById<RelativeLayout?>(R.id.thumb)?.apply { visibility = INVISIBLE }
        mBottomContainer = findViewById<ViewGroup?>(R.id.layout_bottom)?.apply {
            visibility = if (isVideo && !isMandatory) VISIBLE else INVISIBLE
        }
        mCurrentTimeTextView = findViewById(R.id.current)
        mProgressBar = findViewById(R.id.progress)
        mTotalTimeTextView = findViewById(R.id.total)
        mStartButton = findViewById<View?>(R.id.start)?.apply {
            visibility = if (isVideo && !isMandatory) VISIBLE else INVISIBLE
        }
        mLoadingProgressBar = findViewById(R.id.loading)
        mBottomProgressBar = findViewById<ProgressBar?>(R.id.bottom_progressbar)?.apply {
            visibility = if (isVideo) VISIBLE else INVISIBLE
        }
    }

    private fun initInflate(context: Context) {
        inflate(context, R.layout.video_control_layout, this)
    }

    private fun isPlayingMandatory() = (curPlayFlag.and(mandatory_flag) == mandatory_flag)
    private fun isPlayingStream() = (curPlayFlag.and(stream_flag) == stream_flag)
    private fun isPlayingVideo() = (curPlayFlag.and(video_flag) == video_flag)

    fun setupPlayFlag(flag: Int) {
        curPlayFlag = flag
        isMandatory = isPlayingMandatory()
        isStream = isPlayingStream()
        isVideo = isPlayingVideo()
        LogUtils.i("setupPlayFlag: flag=$flag,isVideo=${isVideo},isStream=${isStream},isMandatory=${isMandatory}")
        setupMandatoryStatus()
    }

    private fun setupMandatoryStatus() {
        if (isMandatory) {
            // 显示顶部栏（标题、FPS、全屏）
            setViewShowState(mTopContainer, VISIBLE)

            setViewShowState(mBottomContainer, INVISIBLE)
            setViewShowState(mStartButton, INVISIBLE)
            setViewShowState(mBottomProgressBar, INVISIBLE)
            setViewShowState(mLoadingProgressBar, INVISIBLE)
            setViewShowState(mThumbImageViewLayout, INVISIBLE)

            setViewShowState(mBackButton, GONE)
            setViewShowState(mStartScreen, GONE)
            setViewShowState(mStopScreen, GONE)

            // 禁用所有手势调节
            mEnableSeekGesture = false
        } else {
            // 顶部强制状态下隐藏的进行恢复
            setViewShowState(mBackButton, VISIBLE)
            setViewShowState(mStartScreen, VISIBLE)
            setViewShowState(mStopScreen, VISIBLE)

            // 根据播放video的模式恢复其它ui
            setViewShowState(mBottomContainer, if (isVideo) VISIBLE else INVISIBLE)
            setViewShowState(mBottomProgressBar, if (isVideo) VISIBLE else INVISIBLE)
            setViewShowState(mStartButton, if (isVideo) VISIBLE else INVISIBLE)
        }
    }

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
        setStateAndUi(CURRENT_STATE_PREPAREING)
        startDismissControlViewTimer()
    }

    fun startPlay() {
        setStateAndUi(CURRENT_STATE_PLAYING)
        startDismissControlViewTimer()
    }

    fun setPlayView(view: View) {
        mVideoView = view
        mTextureViewContainer?.addView(
            view, RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            })
    }

    fun resetPlayerViewRenderSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int) {
        var newWidth = 0
        var newHeight = 0
        LogUtils.e("resetPlayerViewRenderSize 视频源:$width x $height,最大宽高:$maxWidth x $maxHeight")
        //视频源是横屏
        if (width > height) {
            //根据播放区域宽进行适配新高度
            newHeight = height * maxWidth / width
            newWidth = maxWidth
            if (newHeight > maxHeight) {
                //适配后进行计算后的高度太高了，则改成进行基于高度的适配
                newHeight = maxHeight
                newWidth = width * newHeight / height
            }
        } else {
            //视频源是竖屏
            newHeight = maxHeight
            newWidth = width * newHeight / height
        }
        LogUtils.e("resetPlayerViewRenderSize 适配后宽高:$newWidth x $newHeight")
        mVideoView?.layoutParams = RelativeLayout.LayoutParams(newWidth, newHeight)
            .apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
        mVideoView?.invalidate()
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.surface_container -> {
                    startDismissControlViewTimer()
                }

                R.id.full -> {
                    callback?.toggleScreen()
                }

                else -> {
                    if (isMandatory) return // 强制播放时某些按钮点击无效
                    when (it.id) {
                        R.id.start -> clickStartIcon()
                        R.id.back -> callback?.onBack()
                        R.id.more -> callback?.onMoreMenuItemClick(0)
                        R.id.startScreen -> callback?.onMoreMenuItemClick(1)
                        R.id.stopScreen -> callback?.onMoreMenuItemClick(2)

                        else -> {}
                    }
                }

            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val x = event!!.x
        val y = event.y
        when (v!!.id) {
            R.id.surface_container -> {
                // 让缩放检测器优先处理
                mScaleGestureDetector?.onTouchEvent(event)

                // 如果在缩放中 或 已经缩放，进入缩放/平移模式
                if (mScaleGestureDetector?.isInProgress == true || (mVideoView?.scaleX ?: 1f) != 1f) {
                    handleScaleAndPanTouch(event)
                    return true
                }

                // 未缩放时的原有逻辑
                val view = mVideoView
                view?.let {
                    it.translationX = 0f
                    it.translationY = 0f
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> touchSurfaceDown(x, y)
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = x - mDownX
                        val deltaY = y - mDownY
                        val absDeltaX = abs(deltaX)
                        val absDeltaY = abs(deltaY)
                        if (!mChangePosition) {
                            touchSurfaceMoveFullLogic(absDeltaX, absDeltaY)
                        }
                        touchSurfaceMove(deltaX, deltaY, y)
                    }

                    MotionEvent.ACTION_UP -> {
                        LogUtils.e("触摸抬起: mChangePosition[$mChangePosition]")
                        startDismissControlViewTimer()
                        touchSurfaceUp()
                    }
                }
                gestureDetector.onTouchEvent(event)
            }

            R.id.progress -> {
                if (isMandatory) return true   // 强制播放时直接消费事件，不处理
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> cancelDismissControlViewTimer()
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
        gestureDetector.onTouchEvent(event)  // 支持双击
        // 注意：缩放检测器已在外部调用过，这里不再重复调用

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
                if (mScaleGestureDetector?.isInProgress == true) return true
                if (pointerCount == 1) {
                    val index = event.findPointerIndex(mActivePointerId)
                    if (index == MotionEvent.INVALID_POINTER_ID) return true
                    val x = event.getX(index)
                    val y = event.getY(index)
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY
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
        // 强制播放禁止所有滑动调节
        if (isMandatory) {
            mChangePosition = false
            mFirstTouch = false
        } else {
            mChangePosition = false
            mFirstTouch = true
        }
    }

    private fun touchSurfaceMove(deltaX: Float, deltaY: Float, y: Float) {
        val curWidth = mScreenWidth
        val curHeight = mScreenHeight
        if (mChangePosition) {
            if (!isVideo) return // 媒体文件模式才可操作
            val totalTimeDuration: Long = mTotalPosition
            mSeekTimePosition = (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio).toInt().toLong()
            if (mSeekTimePosition < 0) mSeekTimePosition = 0
            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition = totalTimeDuration
            val seekTime: String = stringForTime(mSeekTimePosition)
            val totalTime: String = stringForTime(totalTimeDuration)
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration)
        }
    }

    private fun touchSurfaceUp() {
        if (mChangePosition) {
            val duration: Long = mTotalPosition
            val progress = mSeekTimePosition * 100 / (if (duration == 0L) 1 else duration)
            mBottomProgressBar?.progress = progress.toInt()
        }
        mTouchingProgressBar = false
        dismissProgressDialog()
        if (mChangePosition && (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE)) {
            val duration: Long = mTotalPosition
            val progress = mSeekTimePosition * 100 / (if (duration == 0L) 1 else duration)
            callback?.seekTo(progress.toInt())
            mProgressBar?.progress = progress.toInt()
        }
    }

    private fun touchSurfaceMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {
        // 强制播放：禁止进度、音量、亮度手势
        if (isMandatory) {
            mChangePosition = false
            return
        }
        val curWidth = mScreenWidth
        if (absDeltaX > mThreshold || absDeltaY > mThreshold) {
            if (absDeltaX >= mThreshold) {
                // 进度调节：需要判断是否允许
                if (mEnableSeekGesture) {
                    val screenWidth: Int = CommonUtil.getScreenWidth(context)
                    if (abs((screenWidth - mDownX).toDouble()) > mSeekEndOffset) {
                        mChangePosition = true
                        mDownPosition = mCurPosition
                    } else {
                        mShowVKey = true
                    }
                }
            } else {
                val screenHeight: Int = CommonUtil.getScreenHeight(context)
                val noEnd: Boolean = abs((screenHeight - mDownY).toDouble()) > mSeekEndOffset
                if (mFirstTouch) {
                    // 悬浮窗模式下禁用亮度调节，判断是否允许
                    mFirstTouch = false
                }
                mShowVKey = !noEnd
            }
        }
    }

    private fun showProgressDialog(
        deltaX: Float,
        seekTime: String,
        seekTimePosition: Long,
        totalTime: String,
        totalTimeDuration: Long
    ) {
        if (mFloatingMode) return  // 悬浮窗模式下不显示进度弹窗
        if (mProgressDialog == null && context is Activity) {
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
                x = 0
                y = 0
            }
        }
        mProgressDialog?.let {
            if (!it.isShowing) it.show()
            mDialogSeekTime?.text = seekTime
            mDialogTotalTime?.text = "/ $totalTime"
            if (totalTimeDuration > 0) {
                val toInt = (seekTimePosition * 100 / totalTimeDuration).toInt()
                mDialogProgressBar?.progress = toInt
            }
            mDialogIcon?.setBackgroundResource(if (deltaX > 0) R.drawable.video_forward_icon else R.drawable.video_backward_icon)
        }
    }

    private fun dismissProgressDialog() {
        mProgressDialog?.dismiss(); mProgressDialog = null
    }

    private var dismissControlTask: Runnable = object : Runnable {
        override fun run() {
            if (mCurrentState != CURRENT_STATE_NORMAL && mCurrentState != CURRENT_STATE_ERROR && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                hideAllWidget()
                if (mPostDismiss) postDelayed(this, mDismissControlTime)
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

    private fun clickStartIcon() {
        if (!isVideo) return // 媒体文件模式才可操作
        // 强制播放时，当前正在播放则不允许暂停
        if (isMandatory && mCurrentState == CURRENT_STATE_PLAYING) return
        updateStartImage()
        when (mCurrentState) {
            CURRENT_STATE_NORMAL, CURRENT_STATE_ERROR -> setStateAndUi(CURRENT_STATE_PREPAREING)
            CURRENT_STATE_PLAYING -> {
                setStateAndUi(CURRENT_STATE_PAUSE)
                callback?.pause()
                mBtnPlay?.text = "播放"
            }

            CURRENT_STATE_PAUSE -> {
                setStateAndUi(CURRENT_STATE_PLAYING)
                mBtnPlay?.text = "暂停"
                callback?.start()
            }

            CURRENT_STATE_AUTO_COMPLETE -> setStateAndUi(CURRENT_STATE_PREPAREING)
        }
    }

    private fun updateStartImage() {
        if (mStartButton is ENPlayView) {
            val enPlayView = mStartButton as ENPlayView
            enPlayView.setDuration(500)
            if (mCurrentState == CURRENT_STATE_PLAYING) enPlayView.play()
            else enPlayView.pause()
        }
    }

    private fun setStateAndUi(state: Int) {
        mCurrentState = state
        when (state) {
            CURRENT_STATE_NORMAL -> {
                changeUiToNormal(); cancelDismissControlViewTimer()
            }

            CURRENT_STATE_PREPAREING -> {
                changeUiToPreparingShow(); startDismissControlViewTimer()
            }

            CURRENT_STATE_PLAYING -> {
                changeUiToPlayingShow(); startDismissControlViewTimer()
            }

            CURRENT_STATE_PAUSE -> {
                changeUiToPauseShow(); cancelDismissControlViewTimer()
            }

            else -> {}
        }
    }

    private fun changeUiToPauseShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.reset()
        updateStartImage()
        requestLayout()
    }

    private fun changeUiToPlayingShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.reset()
        updateStartImage()
        requestLayout()
    }

    private fun changeUiToPreparingShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
        setViewShowState(mLoadingProgressBar, VISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.let {
            if (it.currentState == ENDownloadView.STATE_PRE) it.start()
        }
        requestLayout()
    }

    private fun changeUiToNormal() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        updateStartImage()
        (mLoadingProgressBar as? ENDownloadView)?.reset()
        requestLayout()
    }

    fun setDragWindowTouchListener(listener: OnTouchListener, locked: Boolean = false) {
        if (locked) {
            mTopContainer?.setOnTouchListener(null)
            setOnTouchListener(listener)
        } else {
            setOnTouchListener(null)
            mTopContainer?.setOnTouchListener(listener)
        }
    }

    fun setTitle(title: String) {
        mTitleTextView?.text = title
    }

    fun setFps(fps: Int) {
        mFpsTextView?.text = String.format(Locale.getDefault(), "%d f/s", fps)
    }

    fun setProgressAndTime(progress: Long, secProgress: Long, currentTime: Long, totalTime: Long, forceChange: Boolean) {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) return
        mCurPosition = currentTime
        mTotalPosition = totalTime
        if (progress > 0 || forceChange) mProgressBar?.progress = progress.toInt()
        setSecondaryProgress(secProgress)
        mTotalTimeTextView?.text = stringForTime(totalTime)
        if (currentTime > 0L) mCurrentTimeTextView?.text = stringForTime(currentTime)
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

    private fun changeUiToCompleteShow() {
        setViewShowState(mTopContainer, VISIBLE)
        setViewShowState(mBottomContainer, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.reset()
        updateStartImage()
    }

    private fun changeUiToCompleteClear() {
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, if (isVideo && !isMandatory) VISIBLE else INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, VISIBLE)
        setViewShowState(mBottomProgressBar, if (isVideo) VISIBLE else INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.reset()
        updateStartImage()
    }

    private fun changeUiToPauseClear() {
        changeUiToClear(); setViewShowState(mBottomProgressBar, if (isVideo) VISIBLE else INVISIBLE)
    }

    private fun changeUiToPrepareingClear() {
        changeUiToClear()
    }

    private fun changeUiToPlayingClear() {
        changeUiToClear(); setViewShowState(mBottomProgressBar, if (isVideo) VISIBLE else INVISIBLE)
    }

    private fun changeUiToClear() {
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
        setViewShowState(mLoadingProgressBar, INVISIBLE)
        setViewShowState(mThumbImageViewLayout, INVISIBLE)
        setViewShowState(mBottomProgressBar, INVISIBLE)
        (mLoadingProgressBar as? ENDownloadView)?.reset()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if (isMandatory) return
        callback?.seekTo(seekBar?.progress!!)
    }

    private fun hideAllWidget() {
        setViewShowState(mBottomContainer, INVISIBLE)
        setViewShowState(mTopContainer, INVISIBLE)
        setViewShowState(mBottomProgressBar, if (isVideo) VISIBLE else INVISIBLE)
        setViewShowState(mStartButton, INVISIBLE)
    }

    private fun setViewShowState(view: View?, visibility: Int) {
        view?.visibility = visibility
    }

    private var gestureDetector: GestureDetector =
        GestureDetector(context.applicationContext, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                touchDoubleUp(e)
                resetVideoScale()
                LogUtils.i("onDoubleTap: ${mTopContainer?.top} ${mTopContainer?.bottom}")
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // 点击后会有大约300毫秒的时间用来确认这次点击不是双击的第一击，所以回调会比 onClick 晚大约 300ms，感官上UI不能及时的显示出来
                onClickUiToggle(e)
                LogUtils.i("onDoubleTap: ${mTopContainer?.top} ${mTopContainer?.bottom}")
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e); touchLongPress(e)
            }
        })

    private fun resetVideoScale() {
        mVideoView?.let {
            it.scaleX = 1f
            it.scaleY = 1f
            it.translationX = 0f
            it.translationY = 0f
            it.pivotX = (it.width / 2).toFloat()
            it.pivotY = (it.height / 2).toFloat()
        }
    }

    private fun touchLongPress(e: MotionEvent) {}
    private fun onClickUiToggle(e: MotionEvent) {
        // 强制播放模式下，仅切换顶部标题栏（标题、FPS、全屏按钮）
        if (isMandatory) {
            if (mTopContainer?.visibility == VISIBLE) {
                setViewShowState(mTopContainer, INVISIBLE)
            } else {
                setViewShowState(mTopContainer, VISIBLE)
            }
            startDismissControlViewTimer() // 重新计时，稍后自动隐藏
            return
        }
        when (mCurrentState) {
            CURRENT_STATE_PREPAREING -> if (mBottomContainer?.visibility == VISIBLE) changeUiToPrepareingClear() else changeUiToPreparingShow()
            CURRENT_STATE_PLAYING -> if (mBottomContainer?.visibility == VISIBLE) changeUiToPlayingClear() else changeUiToPlayingShow()
            CURRENT_STATE_PAUSE -> if (mBottomContainer?.visibility == VISIBLE) changeUiToPauseClear() else changeUiToPauseShow()
            CURRENT_STATE_AUTO_COMPLETE -> if (mBottomContainer?.visibility == VISIBLE) changeUiToCompleteClear() else changeUiToCompleteShow()
        }
    }

    private fun touchDoubleUp(e: MotionEvent) {
        clickStartIcon()
    }

    /**
     * 释放播放控制相关资源，包括音频焦点管理器、缩放检测器、视图引用等。
     * 外部（如悬浮窗）在销毁时需调用此方法。
     */
    fun release() {
        // 取消定时器
        cancelDismissControlViewTimer()
        // 清理弹窗
        dismissProgressDialog()
        // 清空视图引用
        mVideoView = null
        mScaleGestureDetector = null
        // 移除所有回调（若有 Handler 任务可在此取消）
        removeCallbacks(dismissControlTask)
        LogUtils.i("PlayerControlView released")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    private fun clampTranslation() {
        val view = mVideoView ?: return
        val scale = view.scaleX
        if (scale <= 1.0f) {
            view.translationX = 0f
            view.translationY = 0f
            return
        }
        val parent = view.parent as? ViewGroup ?: return
        val parentWidth = parent.width
        val parentHeight = parent.height
        val scaledWidth = view.width * scale
        val scaledHeight = view.height * scale
        val maxTranslateX = (scaledWidth - parentWidth) / 2
        val maxTranslateY = (scaledHeight - parentHeight) / 2
        view.translationX = view.translationX.coerceIn(-maxTranslateX, maxTranslateX)
        view.translationY = view.translationY.coerceIn(-maxTranslateY, maxTranslateY)
    }

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val view = mVideoView ?: return false
            val oldScale = view.scaleX
            val newScale = (oldScale * detector.scaleFactor).coerceIn(mMinScale, mMaxScale)
            if (newScale == oldScale) return true
            val focusX = detector.focusX
            val focusY = detector.focusY
            val deltaScale = newScale - oldScale
            val pivotX = view.pivotX
            val pivotY = view.pivotY
            view.translationX += (focusX - pivotX) * deltaScale / oldScale
            view.translationY += (focusY - pivotY) * deltaScale / oldScale
            view.scaleX = newScale
            view.scaleY = newScale
            clampTranslation()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector) = true
        override fun onScaleEnd(detector: ScaleGestureDetector) {}
    }
}