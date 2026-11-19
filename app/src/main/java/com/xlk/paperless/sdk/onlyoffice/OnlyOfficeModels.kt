package com.xlk.paperless.sdk.onlyoffice

import android.net.Uri
import java.util.Locale

/**
 * ONLYOFFICE Docs API 中的编辑器类型。
 *
 * PDF 默认按 word 打开以兼容旧版 DocumentServer；新版可改为 pdf。
 */
enum class OnlyOfficeDocumentKind {
    WORD,
    CELL,
    SLIDE,
    PDF;

    fun documentType(usePdfEditor: Boolean = false): String = when (this) {
        WORD -> "word"
        CELL -> "cell"
        SLIDE -> "slide"
        PDF -> if (usePdfEditor) "pdf" else "word"
    }

    companion object {
        fun fromFileType(fileType: String?): OnlyOfficeDocumentKind? =
            when (fileType?.lowercase(Locale.ROOT)) {
                "doc", "docm", "docx", "dot", "dotm", "dotx", "epub", "fb2", "fodt",
                "hml", "htm", "html", "hwp", "hwpx", "md", "mht", "mhtml", "odt", "ott",
                "pages", "rtf", "stw", "sxw", "txt", "wps", "wpt", "xml" -> WORD

                "csv", "et", "ett", "fods", "numbers", "ods", "ots", "sxc",
                "xls", "xlsb", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> CELL

                "dps", "dpt", "fodp", "key", "odg", "odp", "otp", "pot", "potm",
                "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "sxi" -> SLIDE

                "djvu", "ofd", "oxps", "pdf", "xps" -> PDF

                else -> null
            }
    }
}

/**
 * 打开一次 ONLYOFFICE 编辑器所需的全部参数。
 *
 * [callbackBase] 是基础回调地址，实际会追加 `name=文件名`。
 */
data class OnlyOfficeSettings(
    val serverUrl: String,
    val jwtSecret: String,
    val userName: String,
    val fileUrl: String,
    val fileType: String,
    val edit: Boolean,
    val download: Boolean,
    val print: Boolean,
    val key: String,
    val callbackBase: String
) {
    val kind: OnlyOfficeDocumentKind
        get() = OnlyOfficeDocumentKind.fromFileType(fileType) ?: OnlyOfficeDocumentKind.WORD

    val mode: String
        get() = if (edit) "edit" else "view"

    val fileName: String
        get() = OnlyOfficeUrls.fileName(fileUrl, fileType)

    val callbackUrl: String?
        get() = callbackBase
            .takeIf { it.isNotBlank() }
            ?.let { OnlyOfficeUrls.withNameParameter(it, fileName) }
}

/**
 * URL 与文件名处理，逻辑与 ONLYOFFICE Demo 保持一致。
 */
object OnlyOfficeUrls {

    private val ILLEGAL_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    /** 补全默认 scheme，并去掉末尾斜杠。 */
    fun normalizeServer(raw: String): String {
        var value = raw.trim()
        if (value.isEmpty()) return ""
        if (!value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true)
        ) {
            value = "http://$value"
        }
        return value.trimEnd('/')
    }

    fun isHttpUrl(raw: String): Boolean {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return false
        return (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
                !uri.host.isNullOrBlank()
    }

    fun extensionOf(rawUrl: String): String? {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return null
        return extensionOfName(uri.lastPathSegment?.substringAfterLast('/') ?: return null)
    }

    fun extensionOfName(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext.takeIf { it.isNotEmpty() && it.length <= 8 && it.all(Char::isLetterOrDigit) }
    }

    /**
     * `document.title`、回调的 `name` 参数和下载文件名。
     * URL 没有扩展名时（例如 `.../download?fileid=123`）使用 `document.<fileType>`。
     */
    fun fileName(rawUrl: String, fileType: String): String {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull()
        val queryName = uri?.let { parsed ->
            listOf("filename", "name", "file")
                .firstNotNullOfOrNull { key -> parsed.getQueryParameter(key)?.takeIf(String::isNotBlank) }
        }
        val segment = uri?.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
        val candidate = when {
            segment != null && extensionOfName(segment) != null -> segment
            queryName != null -> queryName
            else -> "document.$fileType"
        }
        val withExtension = if (extensionOfName(candidate) == null) "$candidate.$fileType" else candidate
        return sanitizeFileName(withExtension)
    }

    fun sanitizeFileName(name: String): String =
        name.replace(ILLEGAL_FILE_NAME_CHARS, "_").trim().trim('.').take(120).ifBlank { "document" }

    /** 与网页控制台一致：基础回调地址追加 `name` 参数。 */
    fun withNameParameter(baseCallbackUrl: String, fileName: String): String {
        val uri = runCatching { Uri.parse(baseCallbackUrl.trim()) }.getOrNull()
            ?: return baseCallbackUrl.trim()
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames.forEach { key ->
            if (key != "name") {
                uri.getQueryParameters(key).forEach { value -> builder.appendQueryParameter(key, value) }
            }
        }
        builder.appendQueryParameter("name", fileName)
        return builder.build().toString()
    }

    fun sameHost(a: String, b: String): Boolean {
        val hostA = runCatching { Uri.parse(a).host }.getOrNull()
        val hostB = runCatching { Uri.parse(b).host }.getOrNull()
        return hostA != null && hostA == hostB
    }
}
