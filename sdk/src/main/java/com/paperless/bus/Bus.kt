package com.paperless.bus

import org.greenrobot.eventbus.EventBus

/**
 *  @author : Administrator
 *  created on 2024/12/7 15:38
 */
object Bus {
    fun post(type: Int) {
        post(type, method = 0, data = null, obj = null)
    }

    fun post(type: Int, obj: Any?) {
        post(type, method = 0, data = null, obj = obj)
    }

    fun post(type: Int, method: Int, data: ByteArray?) {
        post(type, method, data, obj = null)
    }

    fun post(type: Int, method: Int, data: ByteArray?, obj: Any?) {
        EventBus.getDefault().post(EventBusMessage(type, method, data, obj))
    }
}