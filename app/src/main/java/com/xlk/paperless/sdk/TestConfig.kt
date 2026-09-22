package com.xlk.paperless.sdk

/**
 * 示例工程的可调开关
 *
 *  @author : Administrator
 */
object TestConfig {

    /**
     * 悬浮窗播放是否强制全屏（对应 hengxun 的 Macro.isForceFullScreen）
     *
     * - false：按视频源宽高比等比适配，画面不变形（黑边填充）
     * - true ：画面拉伸铺满整个窗口
     *
     * 改这里就能直接对比两种表现，无需重新接线。
     */
    var IS_FORCE_FULL_SCREEN: Boolean = false

    /**
     * 悬浮窗是否允许拖动右下角缩放
     */
    var FLOATING_RESIZE_ENABLE: Boolean = true

    /**
     * 悬浮窗是否显示全屏切换按钮
     */
    var FLOATING_SIZE_TOGGLE_ENABLE: Boolean = true
}
