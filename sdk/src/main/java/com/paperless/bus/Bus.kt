package com.paperless.bus

import org.greenrobot.eventbus.EventBus

/**
 *  @author : Administrator
 *  created on 2024/12/7 15:38
 */
object Bus {
    fun post(type: Int,obj: Any? = null, method: Int = 0, data: ByteArray? = null ) {
        EventBus.getDefault().post(EventBusMessage(type, method, data, obj))
    }

    fun postSticky(type: Int, method: Int = 0, data: ByteArray? = null, obj: Any? = null) {
        EventBus.getDefault().postSticky(EventBusMessage(type, method, data, obj))
    }
}