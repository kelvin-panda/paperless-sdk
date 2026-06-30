package com.paperless.player

import com.paperless.data.FrameData
import java.util.concurrent.LinkedBlockingQueue

/**
 * FrameData 对象池
 */
object FrameDataPool {
    private const val INITIAL_POOL_SIZE = 20
    private const val MAX_POOL_SIZE = 50

    private val pool = LinkedBlockingQueue<FrameData>(MAX_POOL_SIZE)

    init {
        // 预填充对象池
        repeat(INITIAL_POOL_SIZE) {
            pool.offer(FrameData())
        }
    }

    fun obtain(): FrameData {
        return pool.poll() ?: FrameData()
    }

    fun recycle(frame: FrameData) {
        frame.clear()
        pool.offer(frame)
    }

    fun clear() = pool.clear()

    fun getPoolSize(): Int = pool.size
}