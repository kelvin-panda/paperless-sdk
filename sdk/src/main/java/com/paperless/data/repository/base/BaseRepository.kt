package com.paperless.data.repository.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.mogujie.tt.protobuf.InterfaceBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

/**
 * 列表数据基类（成员、会议等）
 */
abstract class BaseRepository<T>(
    override val type: Int,
    private vararg val additionalTypes: Int    // 附加监听的类型
) : DataRepository {

    protected val _data = MutableLiveData<List<T>>(emptyList())
    val data: LiveData<List<T>> = _data

    fun dataFlow(): Flow<List<T>> = _data.asFlow()

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        CallbackDispatcher.register(type, this)
        additionalTypes.forEach { CallbackDispatcher.register(it, this) }
    }

    override fun handleCallback(method: Int, data: ByteArray?) {}

    override fun handleNotifyCallback(type: Int, data: ByteArray?) {
        try {
            InterfaceBase.pbui_MeetNotifyMsg.parseFrom(data)?.let { query() }
        } catch (e: Exception) {
            // 非InterfaceBase.pbui_MeetNotifyMsg类型的需要子类自己实现
            query()
        }
    }

    override fun query() {}

    override fun destroy() {
        CallbackDispatcher.unregister(type, this)
        additionalTypes.forEach { CallbackDispatcher.unregister(it, this) }
        scope.cancel()
    }
}


