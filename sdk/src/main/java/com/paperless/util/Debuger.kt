package com.paperless.util

import android.util.Log

/**
 *  @author : Administrator
 *  created on 2025/9/13 18:32
 */
object Debuger {
    private val tag = "PlayerLog"
    private var enabled = true

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun printfLog(log: String) {
        printfLog(tag, log)
    }

    fun printfLog(tag: String, log: String) {
        if (enabled) {
            if (log.isNotEmpty()) {
                Log.i(tag, log)
            }
        }
    }

    fun printfWarning(log: String) {
        printfWarning(tag, log)
    }

    fun printfWarning(tag: String, log: String) {
        if (enabled) {
            if (log.isNotEmpty()) {
                Log.w(tag, log)
            }
        }
    }

    fun printfError(log: String) {
        printfError(tag, log)
    }

    fun printfError(tag: String, log: String) {
        if (enabled) {
            if (log.isNotEmpty()) {
                Log.e(tag, log)
            }
        }
    }
}