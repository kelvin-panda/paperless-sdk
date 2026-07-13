package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.sdk.Call

/**
 *  @author : Administrator
 *  created on 2026/7/13 14:29
 */
object CallbackDispatcher : Call.DataChangeCallback {

    private val repositories = mutableMapOf<Int, BaseRepository<*>>()

    fun register(type: Int, repo: BaseRepository<*>) {
        repositories[type] = repo
    }

    fun unregister(type: Int) {
        repositories.remove(type)
    }

    override fun onDataChanged(type: Int, method: Int, data: ByteArray?, dataLen: Int) {
        if (method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
            repositories[type]?.handleNotifyCallback(data)
        } else {
            repositories[type]?.handleCallback(method, data)
        }
    }
}