package com.paperless.data

/**
 *  @author : Administrator
 *  created on 2025/7/7 10:25
 */
data class YuvData(
    val res: Int,
    val w: Int,
    val h: Int,
    val y: ByteArray,
    val u: ByteArray,
    val v: ByteArray
)
