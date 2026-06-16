package com.paperless.player.controller.listener

/**
 *  @author : Administrator
 *  created on 2025/9/18 17:59
 */
interface ControlCallback {
    /**
     * 进度拖动
     */
    fun seekTo(progress: Int)

    /**
     * 开始或恢复
     */
    fun start()

    /**
     * 暂停播放
     */
    fun pause()

    /**
     * 控制界面点击返回按钮
     */
    fun onBack()

    /**
     * 调整亮度
     */
    fun onBrightnessSlide(percent: Float)

    /**
     * 窗口切换：全屏/半屏
     */
    fun toggleScreen()

    /**
     * 更多菜单项被点击
     * @param itemId 菜单项ID
     */
    fun onMoreMenuItemClick(itemId: Int) {}
}