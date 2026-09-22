package com.paperless.player

import com.blankj.utilcode.util.LogUtils
import com.paperless.data.FrameData
import com.paperless.util.PlayerLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 *  @author : Administrator
 *  created on 2025/9/10 10:44
 */
object DecodeQueue {

    private const val L_FRAME = "帧"

    /** 队列溢出日志节流，避免高频告警刷屏 */
    private val overflowLogCounter = PlayerLog.ThrottleCounter()

    //根据资源id存放，jni回调的解码数据，每个资源id对应一个播放窗口
    private val decodeMap: ConcurrentHashMap<Int, LinkedBlockingQueue<FrameData>> = ConcurrentHashMap<Int, LinkedBlockingQueue<FrameData>>()

    private fun getQueue(resId: Int): LinkedBlockingQueue<FrameData> {
        var queue = decodeMap.get(resId)
        if (queue == null) {
            queue = LinkedBlockingQueue<FrameData>(50)
            decodeMap.put(resId, queue)
        }
        return queue
    }

    /**
     * 队列已满，尝试移除最旧的非关键帧
     */
    fun remove(frameData: FrameData) {
        val queue = getQueue(frameData.res)
        var removedNonKeyFrame = false
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val oldFrame = iterator.next()
            if (!oldFrame.checkKeyFrame()) {
                iterator.remove()
                FrameDataPool.recycle(oldFrame)
                removedNonKeyFrame = true
                break
            }
        }

        // 如果移除了非关键帧，再次尝试添加
        if (removedNonKeyFrame) {
            if (!queue.offer(frameData)) {
                // 仍然添加失败，回收帧数据
                FrameDataPool.recycle(frameData)
                logOverflow("添加帧数据失败，队列已满（已丢弃最旧非关键帧后仍失败）", frameData.res)
                LogUtils.d("player_log", "添加帧数据失败，队列已满")
            }
        } else {
            // 没有非关键帧可移除，回收当前帧
            FrameDataPool.recycle(frameData)
            logOverflow("队列已满且无非关键帧可移除，本帧被丢弃", frameData.res)
            LogUtils.d("player_log", "队列已满且无非关键帧可移除")
        }
    }

    /** 队列溢出告警（2 秒最多一条），说明解码消费跟不上推帧速度 */
    private fun logOverflow(msg: String, resId: Int) {
        if (PlayerLog.shouldLog(overflowLogCounter, 2000L)) {
            PlayerLog.w(
                L_FRAME,
                "$msg resId=$resId 当前队列长度=${getSize(resId)}（解码侧消费慢或解码线程已停止，累计告警 ${overflowLogCounter.count} 次）"
            )
        }
    }

    fun offer(resId: Int, frameData: FrameData): Boolean {
        return getQueue(resId).offer(frameData)
    }

    fun poll(resId: Int): FrameData? {
        return getQueue(resId).poll()
    }

    fun getSize(resId: Int): Int {
        return getQueue(resId).size
    }

    fun cleanup(resId: Int) {
        val size = getQueue(resId).size
        getQueue(resId).clear()
        if (size > 0) {
            PlayerLog.i(L_FRAME, "解码队列清理 resId=$resId 丢弃未解码帧数=$size")
        }
    }

}