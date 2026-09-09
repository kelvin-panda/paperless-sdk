package com.paperless.data.repository.base

/**
 * 无数据、纯回调的基类
 */
abstract class BaseEventRepository(
    override val type: Int,
    private vararg val additionalTypes: Int    // 附加监听的类型
) : DataRepository {
    init {
        CallbackDispatcher.register(type, this)
        additionalTypes.forEach { CallbackDispatcher.register(it, this) }
    }

    override fun handleCallback(method: Int, data: ByteArray?) {}

    override fun handleNotifyCallback(type: Int, data: ByteArray?) {}

    override fun query() {} // 无数据可查，空实现

    override fun destroy() {
        CallbackDispatcher.unregister(type, this)
        additionalTypes.forEach { CallbackDispatcher.unregister(it, this) }
    }
}

