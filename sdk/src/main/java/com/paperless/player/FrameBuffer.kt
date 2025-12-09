package com.paperless.player

import com.paperless.data.FrameData
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  @author : Administrator
 *  created on 2025/9/5 17:55
 */
class FrameBuffer {
    private val MAX_BUFFER_SIZE = 45


    private val frameQueue = LinkedBlockingQueue<FrameData>(MAX_BUFFER_SIZE)
    private var isReady = AtomicBoolean(false)
    private var isReleased = AtomicBoolean(false)

    fun setReady() {
        isReady.set(true)
    }

    fun putFrame(frame: FrameData) {
        if (isReleased.get()) {
            FrameDataPool.recycle(frame)
            return
        }

        if (!frameQueue.offer(frame)) {
            // 队列已满，丢弃最旧的一帧
            val discarded = frameQueue.poll()
            discarded?.let { FrameDataPool.recycle(it) }
            frameQueue.offer(frame)
        }
    }

    @Throws(InterruptedException::class)
    fun takeFrame(): FrameData? {
        if (!isReady.get() || isReleased.get()) {
            return null
        }
        return frameQueue.take()
    }

    fun clear() {
        isReleased.set(true)
        while (frameQueue.isNotEmpty()) {
            FrameDataPool.recycle(frameQueue.poll() ?: break)
        }
    }

    fun size(): Int {
        return frameQueue.size
    }
}