package com.paperless.player.controller

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import com.paperless.player.PlayerController
import com.paperless.player.controller.listener.LockClickListener
import com.paperless.sdk.R
import java.util.Formatter
import java.util.Locale

/**
 *  @author : Administrator
 *  created on 2025/9/13 14:35
 */
abstract class VideoControlView(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0, fullFlag: Boolean = false) :
    FrameLayout(context, attrs), OnClickListener, View.OnTouchListener, SeekBar.OnSeekBarChangeListener {

    //<editor-fold desc="成员变量">

    //手指放下的位置
    protected var mDownPosition: Long = 0

    //手势调节音量的大小
    protected var mGestureDownVolume: Int = 0

    //手势偏差值
    protected var mThreshold: Int = 80

    //手动改变滑动的位置
    protected var mSeekTimePosition: Long = 0

    //手动滑动的起始偏移位置
    protected var mSeekEndOffset: Int = 0

    //退出全屏显示的案件图片
    protected var mShrinkImageRes: Int = -1

    //全屏显示的案件图片
    protected var mEnlargeImageRes: Int = -1

    //触摸显示后隐藏的时间
    protected var mDismissControlTime: Int = 2500

    //触摸的X
    protected var mDownX: Float = 0f

    //触摸的Y
    protected var mDownY: Float = 0f

    //移动的Y
    protected var mMoveY: Float = 0f

    //亮度
    protected var mBrightnessData: Float = -1f

    //触摸滑动进度的比例系数
    protected var mSeekRatio: Float = 1f

    //触摸的是否进度条
    protected var mTouchingProgressBar: Boolean = false

    //是否改变音量
    protected var mChangeVolume: Boolean = false

    //是否改变播放进度
    protected var mChangePosition: Boolean = false

    //触摸显示虚拟按键
    protected var mShowVKey: Boolean = false

    //是否改变亮度
    protected var mBrightness: Boolean = false

    //是否首次触摸
    protected var mFirstTouch: Boolean = false

    //是否隐藏虚拟按键
    protected var mHideKey: Boolean = true

    //是否需要显示流量提示
    protected var mNeedShowWifiTip: Boolean = true

    //是否支持非全屏滑动触摸有效
    protected var mIsTouchWiget: Boolean = true

    //是否支持全屏滑动触摸有效
    protected var mIsTouchWigetFull: Boolean = true

    //是否点击封面播放
    protected var mThumbPlay: Boolean = false

    //播放错误时，是否点击触发重试
    protected var mSurfaceErrorPlay: Boolean = true

    //锁定屏幕点击
    protected var mLockCurScreen: Boolean = false

    //是否需要锁定屏幕
    protected var mNeedLockFull: Boolean = false

    //lazy的setup
    protected var mSetUpLazy: Boolean = false

    //seek touch
    protected var mHadSeekTouch: Boolean = false

    protected var mPostProgress: Boolean = false
    protected var mPostDismiss: Boolean = false
    protected var isShowDragProgressTextOnSeekBar: Boolean = false

    //播放按键
    protected var mStartButton: View? = null

    //封面
    protected var mThumbImageView: View? = null

    //loading view
    protected var mLoadingProgressBar: View? = null

    //进度条
    protected var mProgressBar: SeekBar? = null

    //全屏按键
    protected var mFullscreenButton: ImageView? = null

    //返回按键
    protected var mBackButton: ImageView? = null

    //锁定图标
    protected var mLockScreen: ImageView? = null

    //时间显示
    protected var mCurrentTimeTextView: TextView? = null  //时间显示
    protected var mTotalTimeTextView: TextView? = null

    //title
    protected var mTitleTextView: TextView? = null

    //顶部和底部区域
    protected var mTopContainer: ViewGroup? = null  //顶部和底部区域
    protected var mBottomContainer: ViewGroup? = null

    //封面父布局
    protected var mThumbImageViewLayout: RelativeLayout? = null

    //底部进度条
    protected var mBottomProgressBar: ProgressBar? = null

    //点击锁屏的回调
    protected var mLockClickListener: LockClickListener? = null
    //</editor-fold>

    //<editor-fold desc="播放状态">
    //正常
    val CURRENT_STATE_NORMAL: Int = 0
    //准备中
    val CURRENT_STATE_PREPAREING: Int = 1
    //播放中
    val CURRENT_STATE_PLAYING: Int = 2
    //开始缓冲
    val CURRENT_STATE_PLAYING_BUFFERING_START: Int = 3
    //暂停
    val CURRENT_STATE_PAUSE: Int = 5
    //自动播放结束
    val CURRENT_STATE_AUTO_COMPLETE: Int = 6
    //错误状态
    val CURRENT_STATE_ERROR: Int = 7

    //当前的播放状态
    protected var mCurrentState: Int = -1

    //</editor-fold>

    init {

    }

    protected fun initial() {
        mStartButton = findViewById(R.id.start)
        mTitleTextView = findViewById(R.id.title)
        mBackButton = findViewById(R.id.back)!!
        mFullscreenButton = findViewById(R.id.fullscreen)
        mProgressBar = findViewById(R.id.progress)
        mCurrentTimeTextView = findViewById(R.id.current)
        mTotalTimeTextView = findViewById(R.id.total)
        mBottomContainer = findViewById(R.id.layout_bottom)
        mTopContainer = findViewById(R.id.layout_top)
        mBottomProgressBar = findViewById(R.id.bottom_progressbar)
        mThumbImageViewLayout = findViewById(R.id.thumb)
        mLockScreen = findViewById(R.id.lock_screen)

        mLoadingProgressBar = findViewById(R.id.loading)


        if (isInEditMode) return

        if (mStartButton != null) {
            mStartButton!!.setOnClickListener(this)
        }

        if (mFullscreenButton != null) {
            mFullscreenButton!!.setOnClickListener(this)
            mFullscreenButton!!.setOnTouchListener(this)
        }

        if (mProgressBar != null) {
            mProgressBar!!.setOnSeekBarChangeListener(this)
        }

        if (mBottomContainer != null) {
            mBottomContainer!!.setOnClickListener(this)
        }

        if (mProgressBar != null) {
            mProgressBar!!.setOnTouchListener(this)
        }

        if (mThumbImageViewLayout != null) {
            mThumbImageViewLayout!!.visibility = GONE
            mThumbImageViewLayout!!.setOnClickListener(this)
        }


        if (mBackButton != null) mBackButton!!.setOnClickListener(this)

        if (mLockScreen != null) {
            mLockScreen!!.visibility = GONE
            mLockScreen!!.setOnClickListener(OnClickListener { v ->
                if (mCurrentState === CURRENT_STATE_AUTO_COMPLETE ||
                    mCurrentState === CURRENT_STATE_ERROR
                ) {
                    return@OnClickListener
                }
                lockTouchLogic()
                mLockClickListener?.onClick(v, mLockCurScreen)

            })
        }

        mSeekEndOffset = 100

    }

    /**
     * 处理锁屏屏幕触摸逻辑
     */
    protected fun lockTouchLogic() {
        if (mLockCurScreen) {
            mLockScreen!!.setImageResource(R.drawable.unlock)
            mLockCurScreen = false
        } else {
            mLockScreen!!.setImageResource(R.drawable.lock)
            mLockCurScreen = true
            hideAllWidget()
        }
    }

    protected fun initInflate(context: Context) {
        try {

            View.inflate(context, getLayoutId(), this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    abstract fun getLayoutId(): Int


    /**
     * 双击
     */
    protected var gestureDetector: GestureDetector =
        GestureDetector(getContext().applicationContext, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                touchDoubleUp(e)
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!mChangePosition && !mChangeVolume && !mBrightness) {
                    onClickUiToggle(e)
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                touchLongPress(e)
            }
        })

    /**
     * 双击暂停/播放
     * 如果不需要，重载为空方法即可
     */
    protected fun touchDoubleUp(e: MotionEvent?) {

    }

    /**
     * 长按
     */
    protected fun touchLongPress(e: MotionEvent?) {
    }


    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        showDragProgressTextOnSeekBar(fromUser, progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        TODO("Not yet implemented")
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        TODO("Not yet implemented")
    }

    protected fun showDragProgressTextOnSeekBar(fromUser: Boolean, progress: Int) {
//        if (fromUser && isShowDragProgressTextOnSeekBar) {
//            val duration: Long = getDuration()
//            mCurrentTimeTextView?.text = stringForTime(progress * duration / 100)
//        }
    }

    fun stringForTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        val stringBuilder = StringBuilder()
        val mFormatter = Formatter(stringBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    /************************* 继承之后可自定义ui与显示隐藏  */

    /**
     * 获取视图的控制
     */

    abstract fun getPlayerController(): PlayerController

    protected abstract fun showWifiDialog()

    protected abstract fun showProgressDialog(
        deltaX: Float,
        seekTime: String?, seekTimePosition: Long,
        totalTime: String?, totalTimeDuration: Long
    )

    protected abstract fun dismissProgressDialog()

    protected abstract fun showVolumeDialog(deltaY: Float, volumePercent: Int)

    protected abstract fun dismissVolumeDialog()

    protected abstract fun showBrightnessDialog(percent: Float)

    protected abstract fun dismissBrightnessDialog()

    /**
     * @param e MotionEvent 存在 null 的情况，外部使用需要判空
     * null 时说明不是手动触发而是自动触发的
     */
    protected abstract fun onClickUiToggle(e: MotionEvent?)

    protected abstract fun hideAllWidget()

    protected abstract fun changeUiToNormal()

    protected abstract fun changeUiToPreparingShow()

    protected abstract fun changeUiToPlayingShow()

    protected abstract fun changeUiToPauseShow()

    protected abstract fun changeUiToError()

    protected abstract fun changeUiToCompleteShow()

    protected abstract fun changeUiToPlayingBufferingShow()
}