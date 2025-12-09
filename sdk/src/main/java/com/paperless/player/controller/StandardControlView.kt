package com.paperless.player.controller

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.paperless.player.PlayerController
import com.paperless.sdk.R

/**
 *  @author : Administrator
 *  created on 2025/9/15 17:42
 */
class StandardControlView(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
    VideoControlView(context, attrs, defStyleAttr, true) {


    //亮度dialog
    protected var mBrightnessDialog: Dialog? = null

    //音量dialog
    protected var mVolumeDialog: Dialog? = null

    //触摸进度dialog
    protected var mProgressDialog: Dialog? = null

    //触摸进度条的progress
    protected var mDialogProgressBar: ProgressBar? = null

    //音量进度条的progress
    protected var mDialogVolumeProgressBar: ProgressBar? = null

    //亮度文本
    protected var mBrightnessDialogTv: TextView? = null

    //触摸移动显示文本
    protected var mDialogSeekTime: TextView? = null

    //触摸移动显示全部时间
    protected var mDialogTotalTime: TextView? = null

    //触摸移动方向icon
    protected var mDialogIcon: ImageView? = null

    protected var mBottomProgressDrawable: Drawable? = null

    protected var mBottomShowProgressDrawable: Drawable? = null

    protected var mBottomShowProgressThumbDrawable: Drawable? = null

    protected var mVolumeProgressDrawable: Drawable? = null

    protected var mDialogProgressBarDrawable: Drawable? = null

    protected var mDialogProgressHighLightColor: Int = -11

    protected var mDialogProgressNormalColor: Int = -11

    override fun getLayoutId(): Int = R.layout.video_layout_standard
    override fun getPlayerController(): PlayerController {
        TODO("Not yet implemented")
    }

    override fun showWifiDialog() {
        TODO("Not yet implemented")
    }

    override fun showProgressDialog(
        deltaX: Float,
        seekTime: String?,
        seekTimePosition: Long,
        totalTime: String?,
        totalTimeDuration: Long
    ) {
        TODO("Not yet implemented")
    }

    override fun dismissProgressDialog() {
        TODO("Not yet implemented")
    }

    override fun showVolumeDialog(deltaY: Float, volumePercent: Int) {
        TODO("Not yet implemented")
    }

    override fun dismissVolumeDialog() {
        TODO("Not yet implemented")
    }

    override fun showBrightnessDialog(percent: Float) {
        TODO("Not yet implemented")
    }

    override fun dismissBrightnessDialog() {
        TODO("Not yet implemented")
    }

    override fun onClickUiToggle(e: MotionEvent?) {
        TODO("Not yet implemented")
    }

    override fun hideAllWidget() {
        TODO("Not yet implemented")
    }

    override fun changeUiToNormal() {
        TODO("Not yet implemented")
    }

    override fun changeUiToPreparingShow() {
        TODO("Not yet implemented")
    }

    override fun changeUiToPlayingShow() {
        TODO("Not yet implemented")
    }

    override fun changeUiToPauseShow() {
        TODO("Not yet implemented")
    }

    override fun changeUiToError() {
        TODO("Not yet implemented")
    }

    override fun changeUiToCompleteShow() {
        TODO("Not yet implemented")
    }

    override fun changeUiToPlayingBufferingShow() {
        TODO("Not yet implemented")
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismissVolumeDialog()
        dismissBrightnessDialog()
    }

    //<editor-fold desc="视图id">

    /**
     * 触摸进度dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected fun getProgressDialogLayoutId(): Int {
        return R.layout.video_progress_dialog
    }

    /**
     * 触摸进度dialog的进度条id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected fun getProgressDialogProgressId(): Int {
        return R.id.duration_progressbar
    }

    /**
     * 触摸进度dialog的当前时间文本
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected fun getProgressDialogCurrentDurationTextId(): Int {
        return R.id.tv_current
    }

    /**
     * 触摸进度dialog全部时间文本
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected fun getProgressDialogAllDurationTextId(): Int {
        return R.id.tv_duration
    }

    /**
     * 触摸进度dialog的图片id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected fun getProgressDialogImageId(): Int {
        return R.id.duration_image_tip
    }

    /**
     * 音量dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showVolumeDialog方法
     */
    protected fun getVolumeLayoutId(): Int {
        return R.layout.video_volume_dialog
    }

    /**
     * 音量dialog的百分比进度条 id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showVolumeDialog方法
     */
    protected fun getVolumeProgressId(): Int {
        return R.id.volume_progressbar
    }


    /**
     * 亮度dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showBrightnessDialog方法
     */
    protected fun getBrightnessLayoutId(): Int {
        return R.layout.video_brightness
    }

    /**
     * 亮度dialog的百分比text id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showBrightnessDialog方法
     */
    protected fun getBrightnessTextId(): Int {
        return R.id.app_video_brightness
    }

    //</editor-fold>
}