package com.paperless.bus

import org.greenrobot.eventbus.EventBus

/**
 *  @author : Administrator
 *  created on 2024/12/7 15:38
 */
object Bus {
    fun post(type: Int, method: Int = 0, data: ByteArray? = null) {
        post(EventBusMessage.build { type(type).method(method).data(data) })
    }

    fun postObj(type: Int, obj: Any? = null) {
        post(EventBusMessage.build { type(type).obj(obj) })
    }

    fun postVararg(type: Int, vararg objs: Any) {
        // 加上 * 展开符，将 objs 数组展开为多个参数
        post(EventBusMessage.build { type(type).objs(*objs) })
    }

    fun post(msg: EventBusMessage) {
        EventBus.getDefault().post(msg)
    }

    fun postSticky(msg: EventBusMessage) {
        EventBus.getDefault().postSticky(msg)
    }
}