package com.paperless.player.controller.listener

/**
 *  @author : Administrator
 *  created on 2025/9/19 17:26
 */
interface AudioFocusListener {
    fun onAudioFocusGain()
    fun onAudioFocusLoss()
    fun onAudioFocusLossTransient()
    fun onAudioFocusLossTransientCanDuck()
}