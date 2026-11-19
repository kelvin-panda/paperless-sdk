package com.xlk.paperless.sdk.onlyoffice

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 生成 ONLYOFFICE Docs API 的初始化配置。
 *
 * 注意：这里在客户端生成 JWT token 仅用于 Demo；正式环境必须由自己的服务端生成，
 * 绝不能把 JWT 密钥下发到客户端。
 */
object OnlyOfficeConfig {

    fun build(
        settings: OnlyOfficeSettings,
        darkTheme: Boolean,
        usePdfEditor: Boolean = false,
        useMobileEditor: Boolean = false
    ): String {
        require(settings.fileUrl.isNotBlank()) { "document URL is empty" }

        val root = JSONObject()
        root.put("documentType", settings.kind.documentType(usePdfEditor))
        // Community Edition 只授权 desktop 编辑器；mobile 需要商业授权。
        root.put("type", if (useMobileEditor) "mobile" else "desktop")
        root.put("width", "100%")
        root.put("height", "100%")

        root.put(
            "document",
            JSONObject().apply {
                put("fileType", settings.fileType)
                put("key", newKey())
                put("title", settings.fileName)
                put("url", settings.fileUrl)
                put(
                    "permissions",
                    JSONObject().apply {
                        put("edit", settings.edit)
                        put("download", settings.download)
                        put("print", settings.print)
                        put("comment", settings.edit)
                        put("review", settings.edit)
                    }
                )
            }
        )

        root.put(
            "editorConfig",
            JSONObject().apply {
                put("mode", settings.mode)
                put("lang", "zh-CN")
                put(
                    "user",
                    JSONObject().apply {
                        put("id", "paperless-android")
                        put("name", settings.userName)
                    }
                )
                settings.callbackUrl?.let { put("callbackUrl", it) }
                put(
                    "customization",
                    JSONObject().apply {
                        put("compactHeader", true)
                        put("toolbarNoTabs", false)
                        put("hideRightMenu", true)
                        put("help", false)
                        put("feedback", false)
                        put("chat", false)
                        put("comments", true)
                        // 显示关闭按钮，需要 JS 侧的 onRequestClose 事件。
                        put("close", true)
                        val hasCallback = settings.callbackUrl != null && settings.edit
                        put("forcesave", hasCallback)
                        put("autosave", hasCallback)
                        put("uiTheme", if (darkTheme) "theme-dark" else "theme-light")
                    }
                )
            }
        )

        if (settings.jwtSecret.isNotBlank()) {
            // token 的 payload 是不含 token/events 的完整配置。
            root.put("token", OnlyOfficeJwt.sign(root.toString(), settings.jwtSecret))
        }
        return root.toString()
    }

    private fun newKey(): String {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        return "paperless-${System.currentTimeMillis()}-$unique"
    }
}

/**
 * 最小化的 HS256 JWT 实现。
 */
object OnlyOfficeJwt {

    private const val HEADER = """{"alg":"HS256","typ":"JWT"}"""

    private val BASE64_URL_TABLE =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()

    fun sign(payload: String, secret: String): String {
        require(secret.isNotEmpty()) { "JWT secret must not be empty" }
        val headerPart = base64Url(HEADER.toByteArray(Charsets.UTF_8))
        val payloadPart = base64Url(payload.toByteArray(Charsets.UTF_8))
        val signingInput = "$headerPart.$payloadPart"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = base64Url(mac.doFinal(signingInput.toByteArray(Charsets.US_ASCII)))
        return "$signingInput.$signature"
    }

    private fun base64Url(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            val combined = (b0 shl 16) or (b1 shl 8) or b2
            out.append(BASE64_URL_TABLE[(combined ushr 18) and 0x3F])
            out.append(BASE64_URL_TABLE[(combined ushr 12) and 0x3F])
            if (i + 1 < bytes.size) out.append(BASE64_URL_TABLE[(combined ushr 6) and 0x3F])
            if (i + 2 < bytes.size) out.append(BASE64_URL_TABLE[combined and 0x3F])
            i += 3
        }
        return out.toString()
    }
}

/**
 * 检查 DocumentServer 的 /healthcheck 和 api.js 是否可用。
 * 需要在子线程调用。
 */
object OnlyOfficeServerProbe {

    private const val USER_AGENT = "PaperlessOnlyOfficeDemo/1.0"

    data class Result(
        val healthOk: Boolean,
        val healthStatus: Int?,
        val apiOk: Boolean,
        val apiStatus: Int?,
        val error: String?
    ) {
        val ok: Boolean get() = healthOk && apiOk
    }

    fun check(rawServerUrl: String, timeoutMs: Int = 8_000): Result {
        val base = OnlyOfficeUrls.normalizeServer(rawServerUrl)
        if (!OnlyOfficeUrls.isHttpUrl(base)) {
            return Result(false, null, false, null, "服务器地址无效")
        }
        val health = get("$base/healthcheck", timeoutMs)
        val api = get("$base/web-apps/apps/api/documents/api.js", timeoutMs)

        val healthOk = health.code in 200..299 &&
                health.body?.trim().equals("true", ignoreCase = true)
        val apiOk = api.code in 200..299 &&
                !api.body.isNullOrBlank() &&
                api.body.contains("DocsAPI")

        return Result(healthOk, health.code, apiOk, api.code, health.error ?: api.error)
    }

    private data class Reply(val code: Int?, val body: String?, val error: String? = null)

    private fun get(url: String, timeoutMs: Int): Reply {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            Reply(code, body?.take(300_000))
        } catch (e: IOException) {
            Reply(null, null, e.message ?: e.javaClass.simpleName)
        } catch (e: Exception) {
            Reply(null, null, e.message ?: e.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }
}
