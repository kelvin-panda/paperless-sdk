package com.paperless.player

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils
import com.paperless.bus.Bus
import com.paperless.bus.SdkBusType

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  @author : Administrator
 *  created on 2026/6/30 9:30
 */
object Fps {

    // 存储每个资源 ID (res) 对应的帧计数
    private val mFpsMap = ConcurrentHashMap<Int, Int>()

    // 使用主线程 Handler 确保 Bus 发送在主线程（通常 Bus 接收者需要主线程）
    private val handler = Handler(Looper.getMainLooper())

    // 运行状态标志（AtomicBoolean 保证可见性和原子性）
    private val isRunning = AtomicBoolean(false)

    // 周期性任务
    private val task = object : Runnable {
        override fun run() {
            // 防止在 stopPost 后被意外唤醒
            if (!isRunning.get()) return

            // 核心优化：遍历 Entry，使用 put 原子地替换为 0 并获取旧值
            for (entry in mFpsMap.entries) {
                val key = entry.key
                // put 操作是原子的，返回旧值，然后立即将 map 中的值置为 0
                val value = mFpsMap.put(key, 0) ?: 0
                if (value > 0) {
                    // 发送 FPS 数据（每秒帧数）
                    Bus.postVararg(type = SdkBusType.fps, value, key)
                }
            }

            // 如果仍在运行，1 秒后再次执行
            if (isRunning.get()) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    /**
     * 开始上报（幂等操作，重复调用不会创建多个任务）
     */
    private fun busPost() {
        // 如果已经运行，直接返回；否则设置为 true 并开始
        if (isRunning.compareAndSet(false, true)) {
            LogUtils.i("busPost")
            // 移除之前残留的任务（安全兜底）
            handler.removeCallbacks(task)
            handler.postDelayed(task, 1000L)
        }
    }

    /**
     * 停止上报并清理数据
     */
    private fun stopPost() {
        LogUtils.i("stopPost")
        // 设置为 false，任务下次循环会自行退出
        isRunning.set(false)
        handler.removeCallbacks(task)
    }

    /**
     * 收到一帧数据时的回调（可能由多个线程并发调用）
     * @param res 资源 ID / 场景 ID
     */
    fun add(res: Int) {
        // 关键优化：使用 merge 实现原子性的 "不存在则设1，存在则+1"
        // 比 contains + get + put 安全得多，不会丢帧
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mFpsMap.merge(res, 1) { oldValue, _ ->
                oldValue + 1
            }
            busPost()
        }
    }

    /**
     * 结束播放时调用
     */
    fun clear(res: Int) {
        mFpsMap.remove(res)
        if(mFpsMap.isEmpty()){
            stopPost()
        }
    }
}