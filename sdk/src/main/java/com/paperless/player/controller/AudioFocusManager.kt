package com.paperless.player.controller

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.paperless.player.controller.listener.AudioFocusListener
import com.paperless.util.Debuger
import java.lang.ref.WeakReference

/**
 * 音频焦点管理器 - 解决内存泄漏和重复请求问题
 * Audio Focus Manager - Solves memory leaks and duplicate request issues
 * Created for GSYVideoPlayer optimization
 */
class AudioFocusManager {
    private var mAudioManagerRef: WeakReference<AudioManager>? = null
    private var mListenerRef: WeakReference<AudioFocusListener>? = null

    private var mHasAudioFocus = false

    /**
     * 标记是否已释放，避免重复操作
     */
    @Volatile
    private var mIsReleased = false

    private val mInternalListener = object : AudioManager.OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            val listener = mListenerRef?.get()
            if (listener == null) {
                // 如果监听器已被回收，说明外部对象已释放，应该放弃音频焦点
                abandonAudioFocusInternal()
                return
            }
            Handler(Looper.getMainLooper()).post { handleAudioFocusChange(focusChange, listener) }
        }
    }

    fun initialize(context: Context, listener: AudioFocusListener) {
        if (mIsReleased) return
        val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManagerRef = WeakReference(audioManager)
        mListenerRef = WeakReference(listener)
    }

    /**
     * 请求音频焦点
     * @return 是否成功请求到音频焦点
     */
    fun requestAudioFocus(): Boolean {
        if (mIsReleased) {
            Debuger.printfWarning(" Cannot request audio focus after release")
            return false
        }

        val audioManager = if (mAudioManagerRef != null) mAudioManagerRef!!.get() else null
        if (audioManager == null) {
            Debuger.printfWarning(" AudioManager is null, cannot request audio focus")
            return false
        }

        if (mHasAudioFocus) {
            Debuger.printfLog("Already has audio focus, skipping request")
            return true
        }

        try {
            val result = audioManager.requestAudioFocus(
                mInternalListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )

            mHasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

            if (mHasAudioFocus) {
                Debuger.printfLog(" Audio focus request granted")
            } else {
                Debuger.printfWarning(" Audio focus request failed with result: " + result)
            }

            return mHasAudioFocus
        } catch (e: Exception) {
            Debuger.printfError(" Exception while requesting audio focus: " + e.message)
            e.printStackTrace()
            return false
        }
    }


    /**
     * 放弃音频焦点
     */
    fun abandonAudioFocus() {
        if (!mHasAudioFocus) {
            Debuger.printfLog(" No audio focus to abandon")
            return
        }

        abandonAudioFocusInternal()
    }

    /**
     * 内部放弃音频焦点方法
     */
    private fun abandonAudioFocusInternal() {
        val audioManager = if (mAudioManagerRef != null) mAudioManagerRef!!.get() else null
        if (audioManager == null) {
            mHasAudioFocus = false
            return
        }
        try {
            val result = audioManager.abandonAudioFocus(mInternalListener)
            mHasAudioFocus = false

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Debuger.printfLog(" Audio focus abandoned successfully")
            } else {
                Debuger.printfWarning("Audio focus abandon failed with result: " + result)
            }
        } catch (e: java.lang.Exception) {
            mHasAudioFocus = false
            Debuger.printfError(" Exception while abandoning audio focus: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * 处理音频焦点变化
     */
    private fun handleAudioFocusChange(
        focusChange: Int,
        listener: AudioFocusListener
    ) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                mHasAudioFocus = true
                try {
                    listener.onAudioFocusGain()
                } catch (e: java.lang.Exception) {
                    Debuger.printfError(" Error in onAudioFocusGain: " + e.message)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                mHasAudioFocus = false
                try {
                    listener.onAudioFocusLoss()
                } catch (e: java.lang.Exception) {
                    Debuger.printfError(" Error in onAudioFocusLoss: " + e.message)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->                 // 暂时失去焦点，不更改mHasAudioFocus状态
                try {
                    listener.onAudioFocusLossTransient()
                } catch (e: java.lang.Exception) {
                    Debuger.printfError(" Error in onAudioFocusLossTransient: " + e.message)
                }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> try {
                listener.onAudioFocusLossTransientCanDuck()
            } catch (e: java.lang.Exception) {
                Debuger.printfError(" Error in onAudioFocusLossTransientCanDuck: " + e.message)
            }

            else -> Debuger.printfWarning(" Unknown audio focus change: $focusChange")
        }
    }

    /**
     * 检查是否拥有音频焦点
     */
    fun hasAudioFocus(): Boolean {
        return mHasAudioFocus
    }

    /**
     * 释放所有资源
     */
    fun release() {
        if (mIsReleased) {
            return  // 避免重复释放
        }

        abandonAudioFocus()

        if (mAudioManagerRef != null) {
            mAudioManagerRef!!.clear()
            mAudioManagerRef = null
        }

        if (mListenerRef != null) {
            mListenerRef!!.clear()
            mListenerRef = null
        }

        mIsReleased = true
        Debuger.printfLog("AudioFocusManager released")
    }


    /**
     * 获取当前音频管理器（用于其他音频操作，如音量控制）
     * @return AudioManager实例，可能为null
     */
    fun getAudioManager(): AudioManager? {
        if (mIsReleased) {
            return null
        }
        return if (mAudioManagerRef != null) mAudioManagerRef!!.get() else null
    }
}