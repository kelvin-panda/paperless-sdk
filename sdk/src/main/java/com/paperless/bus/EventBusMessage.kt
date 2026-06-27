package com.paperless.bus

class EventBusMessage(
    val type: Int = 0,
    val method: Int = 0,
    val data: ByteArray? = null,
    val obj: Any? = null,
    val objs: Array<out Any>? = null     // 存储为数组，使用方式：msg.objs?.get(0) as Int
) {
    class Builder {
        private var type: Int = 0
        private var method: Int = 0
        private var data: ByteArray? = null
        private var obj: Any? = null
        private var objs: Array<out Any>? = null

        fun type(type: Int) = apply { this.type = type }
        fun method(method: Int) = apply { this.method = method }
        fun data(data: ByteArray?) = apply { this.data = data }
        fun obj(obj: Any?) = apply { this.obj = obj }
        fun objs(vararg items: Any) = apply { this.objs = items }

        fun build() = EventBusMessage(type, method, data, obj, objs)
    }

    // 便捷的工厂方法（可选）
    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    override fun toString(): String {
        return "EventBusMessage(type=$type, method=$method, data=${data?.contentToString()}, obj=$obj, objs=${objs?.contentToString()})"
    }

}
