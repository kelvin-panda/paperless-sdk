package com.paperless.player

import com.paperless.data.FrameData
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 优化后的 FrameData 对象池
 */
object FrameDataPool {
    private const val INITIAL_POOL_SIZE = 20
    private const val MAX_POOL_SIZE = 50
    private const val MAX_CAPACITY_FACTOR = 2 // 最大容量因子

    private val pool = LinkedBlockingQueue<FrameData>(MAX_POOL_SIZE)
    private val createdCount = AtomicInteger(0)
    private val trimThreshold = MAX_POOL_SIZE * 3 / 4 // 缩减阈值

    init {
        // 预填充对象池
        repeat(INITIAL_POOL_SIZE) {
            pool.offer(createNewFrameData())
        }
    }

    fun obtain(): FrameData {
        return pool.poll() ?: createNewFrameData()
    }

    fun recycle(frame: FrameData) {
        frame.clear()

        // 如果池未满，则回收对象
        if (pool.size < MAX_POOL_SIZE) {
            // 尝试放入池中，如果失败则忽略（让GC回收）
            pool.offer(frame)
        } else if (createdCount.get() > MAX_POOL_SIZE * MAX_CAPACITY_FACTOR) {
            // 如果创建过多对象，定期缩减池大小
            trimPool()
        }
        // 否则让GC回收对象
    }

    private fun createNewFrameData(): FrameData {
        createdCount.incrementAndGet()
        return FrameData()
    }

    private fun trimPool() {
        // 移除一些对象以缩减池大小
        while (pool.size > trimThreshold) {
            pool.poll() // 让GC回收
        }
        createdCount.set(pool.size)
    }

    fun getPoolSize(): Int = pool.size
    fun getCreatedCount(): Int = createdCount.get()

    fun clear() {
        pool.clear()
        createdCount.set(0)
    }
}