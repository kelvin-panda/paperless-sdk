package com.paperless.data.repository.base

interface DataRepository {
    val type: Int
    fun handleCallback(method: Int, data: ByteArray?)
    fun handleNotifyCallback(data: ByteArray?)
    fun query()
    fun destroy()
}