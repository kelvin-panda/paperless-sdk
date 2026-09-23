package com.paperless.sdk

import android.graphics.Point

/**
 *  @author : Administrator
 *  created on 2025/7/22 16:41
 */
class SdkConfig {
    companion object {
        /**
         * 批量操作开关
         */
        var isBatchOperate: Boolean = false

        /**
         * 使用开发中的播放逻辑
         */
        var isUseSdkPlayer: Boolean = false

        /**
         * 解码播放时帧渲染根据视频源的宽高或像素点来限制
         * - 比如如果是4K视频，则帧间隔是60毫秒，至多60毫秒显示一帧
         */
        var isDecodeDiscard: Boolean = true

        /**
         * 解码时超出该宽高的帧时进行跳帧渲染
         */
        var decodeDiscardSize: Point = Point(1920, 1080)

        /**
         * Call / BaseJni 中的高频日志打印开关（默认关闭）
         * - 覆盖：`Call.error_ret`（每次 native 回调返回，实测 ~3.4 次/秒，其中 43% 是 ret=0）、
         *   `Call.callback_method`（~1 次/秒）、`Call` 后台接收包长、队列满、BaseJni 查询结果等共 8 处；
         * - 实测这些日志占依赖方 App 全部日志量的 **56%**，且 `error_ret` 里大量是正常返回，排查价值低；
         * - 需要排查时由依赖方打开（例如跟随 App 的"调试开关"）：
         *   `SdkConfig.logEnable = true`
         */
        var logEnable: Boolean = false

        /**
         * 播放流程调试日志开关（logcat TAG = PlayWin）
         * - 覆盖整条链路：事件入口 → 窗口创建 → Surface → 解码配置 → 首帧 → 帧率 → 等比适配 → 停止销毁
         * - 由依赖方（App）按需打开，默认关闭；打开方式：SdkConfig.playLogEnable = true
         * - 抓取：`adb logcat -c && adb logcat -d -s PlayWin > playwin.log`
         */
        var playLogEnable: Boolean = false

        /**
         * 使用悬浮窗口播放，默认false
         */
        var floatingPlayEnable: Boolean = false

        /**
         * EventBus调用栈打印
         */
        var enabledBusCallStack: Boolean = false
    }
}