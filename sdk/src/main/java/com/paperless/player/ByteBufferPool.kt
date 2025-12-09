package com.paperless.player

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * ByteBuffer 对象池，用于复用直接内存缓冲区
 */
object ByteBufferPool {
    private const val INITIAL_POOL_SIZE = 10
    private const val MAX_POOL_SIZE = 30
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024 // 1MB

    private val bufferQueue = ConcurrentLinkedQueue<ByteBuffer>()
    private val createdCount = AtomicInteger(0)

    init {
        // 预填充池
        repeat(INITIAL_POOL_SIZE) {
            bufferQueue.offer(createBuffer(DEFAULT_BUFFER_SIZE))
        }
    }

    /**
     * 获取一个ByteBuffer，如果池中没有则创建新的
     */
    fun obtain(size: Int = DEFAULT_BUFFER_SIZE): ByteBuffer {
        return bufferQueue.poll()?.let { buffer ->
            if (buffer.capacity() >= size) {
                buffer.clear() // 准备重用
                buffer
            } else {
                // 缓冲区太小，创建新的并回收旧的
                recycle(buffer)
                createBuffer(size)
            }
        } ?: createBuffer(size)
    }

    /**
     * 回收ByteBuffer
     */
    fun recycle(buffer: ByteBuffer) {
        if (bufferQueue.size < MAX_POOL_SIZE) {
            bufferQueue.offer(buffer)
        }
        // 如果池已满，让GC回收
    }

    private fun createBuffer(size: Int): ByteBuffer {
        createdCount.incrementAndGet()
        return ByteBuffer.allocateDirect(size)
    }

    fun getPoolSize(): Int = bufferQueue.size
    fun getCreatedCount(): Int = createdCount.get()

    fun clear() {
        bufferQueue.clear()
        createdCount.set(0)
    }
}