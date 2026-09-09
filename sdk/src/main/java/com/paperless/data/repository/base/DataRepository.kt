package com.paperless.data.repository.base

/**
 * 数据
 * - 设备列表
 * - 会议列表
 * - 会议室列表
 * - 会议室排位信息
 * - 会议室设备
 */
interface DataRepository {
    val type: Int
    fun handleCallback(method: Int, data: ByteArray?)
    fun handleNotifyCallback(type: Int, data: ByteArray?)
    fun query()
    fun destroy()
}
