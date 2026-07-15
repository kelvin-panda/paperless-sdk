package com.paperless.data.repository.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.mogujie.tt.protobuf.InterfaceBase
import com.paperless.data.repository.base.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

/**
 * 单条数据基类（配置、设备信息等）
 */
abstract class BaseSingleRepository<T>(
    override val type: Int,
    private vararg val additionalTypes: Int    // 附加监听的类型
) : DataRepository {

    protected val _data = MutableLiveData<T?>()
    val data: LiveData<T?> = _data

    fun dataFlow(): Flow<T?> = _data.asFlow()

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        CallbackDispatcher.register(type, this)
        additionalTypes.forEach { CallbackDispatcher.register(it, this) }
    }

    override fun handleCallback(method: Int, data: ByteArray?) {}

    override fun handleNotifyCallback(data: ByteArray?) {
        try {
            InterfaceBase.pbui_MeetNotifyMsg.parseFrom(data)?.let { query() }
        } catch (e: Exception) {
            query()
        }
    }

    abstract override fun query()

    override fun destroy() {
        CallbackDispatcher.unregister(type)
        additionalTypes.forEach { CallbackDispatcher.unregister(it) }
        scope.cancel()
    }
}
