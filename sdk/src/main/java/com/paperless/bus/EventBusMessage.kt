package com.paperless.bus

/**
 *  @author : Administrator
 *  created on 2025/7/5 17:07
 */
data class EventBusMessage(
    var type: Int = 0,
    var method: Int = 0,
    var data: ByteArray? = null,
    var objects: Any? = null)
