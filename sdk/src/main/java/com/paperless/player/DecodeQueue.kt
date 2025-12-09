package com.paperless.player

import com.blankj.utilcode.util.LogUtils
import com.paperless.data.FrameData
import java.util.concurrent.LinkedBlockingQueue

/**
 *  @author : Administrator
 *  created on 2025/9/10 10:44
 */
object DecodeQueue {
    //根据资源id存放，jni回调的解码数据，每个资源id对应一个播放窗口
    private val decodeMap: HashMap<Int, LinkedBlockingQueue<FrameData>> = hashMapOf()

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
                PerformanceMonitor.logFrameDropped(frameData.res)
                LogUtils.e("player_log", "添加帧数据失败，队列已满")
            }
        } else {
            // 没有非关键帧可移除，回收当前帧
            FrameDataPool.recycle(frameData)
            PerformanceMonitor.logFrameDropped(frameData.res)
            LogUtils.e("player_log", "队列已满且无非关键帧可移除")
        }
    }

    fun offer(resId: Int, frameData: FrameData): Boolean {
        return getQueue(resId).offer(frameData)
    }

    fun poll(resId: Int): FrameData? {
        return getQueue(resId).poll()
    }

    fun cleanup(resId: Int) {
        getQueue(resId).clear()
    }

}