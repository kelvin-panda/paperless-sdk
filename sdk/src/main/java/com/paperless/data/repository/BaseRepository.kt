package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlin.collections.emptyList

/**
 *  @author : Administrator
 *  created on 2026/7/13 14:23
 */
abstract class BaseRepository<T>(private val type: Int) {
    // 子类暴露自己的 LiveData
    protected val _data = MutableLiveData<List<T>>(emptyList())
    val data: LiveData<List<T>> = _data

    // 也可以提供 Flow 版本
    fun dataFlow(): Flow<List<T>> = _data.asFlow()

    init {
        CallbackDispatcher.register(type, this)
    }

    // 由分发器调用，子类实现的方法
    open fun handleCallback(method: Int, data: ByteArray?){}
    abstract fun handleNotifyCallback(data: ByteArray?)

    fun destroy() {
        CallbackDispatcher.unregister(type)
    }
}