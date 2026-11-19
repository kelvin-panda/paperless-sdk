package com.xlk.paperless.sdk.onlyoffice

import android.content.Context
import kotlin.String

/**
 * ONLYOFFICE 演示参数持久化。
 *
 * 默认值指向本机已经部署过的 DocumentServer（WSL2 + Docker），
 * 用户可在“设置”里改成自己的服务器。
 */
class OnlyOfficePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, DEFAULT_SERVER).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SERVER, OnlyOfficeUrls.normalizeServer(value)).apply()
        }

    var jwtSecret: String
        get() = prefs.getString(KEY_SECRET, DEFAULT_SECRET).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SECRET, value.trim()).apply()
        }

    var userName: String
        get() = prefs.getString(KEY_USER, DEFAULT_USER_NAME).orEmpty().ifBlank { DEFAULT_USER_NAME }
        set(value) {
            prefs.edit().putString(KEY_USER, value.trim().ifBlank { DEFAULT_USER_NAME }).apply()
        }

    var fileUrl: String
        get() = prefs.getString(KEY_FILE_URL, DEFAULT_FILE_URL).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_FILE_URL, value.trim()).apply()
        }

    var fileType: String
        get() = prefs.getString(KEY_FILE_TYPE, DEFAULT_FILE_TYPE).orEmpty().ifBlank { DEFAULT_FILE_TYPE }
        set(value) {
            prefs.edit().putString(KEY_FILE_TYPE, value.trim().lowercase()).apply()
        }

    var edit: Boolean
        get() = prefs.getBoolean(KEY_EDIT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_EDIT, value).apply()
        }

    var download: Boolean
        get() = prefs.getBoolean(KEY_DOWNLOAD, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DOWNLOAD, value).apply()
        }

    var printEnabled: Boolean
        get() = prefs.getBoolean(KEY_PRINT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PRINT, value).apply()
        }

    var key: String
        get() = prefs.getString(KEY_UNIQUE_KEY, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_UNIQUE_KEY, value.trim()).apply()
        }

    var callbackBase: String
        get() = prefs.getString(KEY_CALLBACK, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_CALLBACK, value.trim()).apply()
        }

    val hasServer: Boolean
        get() = serverUrl.isNotBlank()

    fun load(): OnlyOfficeSettings = OnlyOfficeSettings(
        serverUrl = serverUrl,
        jwtSecret = jwtSecret,
        userName = userName,
        fileUrl = fileUrl,
        fileType = fileType,
        edit = edit,
        download = download,
        print = printEnabled,
        key= key,
        callbackBase = callbackBase,
    )

    fun save(settings: OnlyOfficeSettings) {
        serverUrl = settings.serverUrl
        jwtSecret = settings.jwtSecret
        userName = settings.userName
        fileUrl = settings.fileUrl
        fileType = settings.fileType
        edit = settings.edit
        download = settings.download
        printEnabled = settings.print
        callbackBase = settings.callbackBase
    }

    fun resetToLocalDemo() {
        serverUrl = DEFAULT_SERVER
        jwtSecret = DEFAULT_SECRET
        userName = DEFAULT_USER_NAME
        fileUrl = DEFAULT_FILE_URL
        fileType = DEFAULT_FILE_TYPE
        edit = false
        download = false
        printEnabled = false
        callbackBase = ""
    }

    companion object {
        /** 本机演示服务器；改成你们自己的 DocumentServer 即可。 */
        const val DEFAULT_SERVER = "http://10.248.6.98:8080"
        const val DEFAULT_SECRET = "secret"
        const val DEFAULT_USER_NAME = "Paperless Android"
        const val DEFAULT_FILE_URL = "http://172.17.0.1:8899/sample.docx"
        const val DEFAULT_FILE_TYPE = "docx"

        private const val NAME = "onlyoffice_demo"
        private const val KEY_SERVER = "server_url"
        private const val KEY_SECRET = "jwt_secret"
        private const val KEY_USER = "user_name"
        private const val KEY_FILE_URL = "file_url"
        private const val KEY_FILE_TYPE = "file_type"
        private const val KEY_EDIT = "edit_mode"
        private const val KEY_DOWNLOAD = "download_mode"
        private const val KEY_PRINT = "print_mode"
        private const val KEY_UNIQUE_KEY = "UNIQUE_KEY"
        private const val KEY_CALLBACK = "callback_url"
    }
}
