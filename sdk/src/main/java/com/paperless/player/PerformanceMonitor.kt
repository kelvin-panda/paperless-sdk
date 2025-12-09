package com.paperless.player

import com.blankj.utilcode.util.LogUtils
import java.util.concurrent.ConcurrentHashMap

/**
 *  @author : Administrator
 *  created on 2025/9/8 14:09
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    private val frameCounters = ConcurrentHashMap<Int, FrameCounter>()

    class FrameCounter(val resId: Int) {
        private var totalFrames = 0L
        private var droppedFrames = 0L
        private var receivedFrames = 0L
        private var lastUpdateTime = System.currentTimeMillis()
        private var lastReceivedTime = System.currentTimeMillis()

        /**
         * 性能日志打印间隔秒数
         */
        private val second = 5

        fun incrementTotal() {
            totalFrames++
            if (System.currentTimeMillis() - lastUpdateTime > second * 1000) {
                logStats()
                reset()
            }
        }

        fun incrementReceived() {
            receivedFrames++
            if (System.currentTimeMillis() - lastReceivedTime > second * 1000) {
                logReceived()
                droppedFrames = 0
                receivedFrames = 0
                lastReceivedTime = System.currentTimeMillis()
            }
        }

        fun incrementDropped() {
            droppedFrames++
        }

        private fun logStats() {
            val fps = totalFrames / second // second秒内的平均帧率
            LogUtils.d(TAG, "resId: $resId FPS: $fps")
        }

        private fun logReceived() {
            LogUtils.d(
                TAG,
                "resId: $resId Received frames: $receivedFrames , Dropped frames: ${droppedFrames > 0} $droppedFrames"
            )
        }

        fun reset() {
            totalFrames = 0
            lastUpdateTime = System.currentTimeMillis()
        }
    }

    private fun getCounter(resId: Int): FrameCounter {
        return frameCounters.getOrPut(resId) { FrameCounter(resId) }
    }

    fun logFrameReceived(resId: Int) {
        if (LogConfig.ENABLED) {
            getCounter(resId).incrementReceived()
        }
    }

    fun logFrameDecoded(resId: Int) {
        if (LogConfig.ENABLED) {
            getCounter(resId).incrementTotal()
        }
    }

    fun logFrameDropped(resId: Int) {
        if (LogConfig.ENABLED) {
            getCounter(resId).incrementDropped()
        }
    }

    fun cleanup(resId: Int) {
        frameCounters.remove(resId)
    }
}