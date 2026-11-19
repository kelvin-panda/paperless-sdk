package com.xlk.paperless.sdk.onlyoffice

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.xlk.paperless.sdk.BuildConfig
import com.xlk.paperless.sdk.databinding.ActivityOnlyofficeBinding
import com.xlk.paperless.sdk.databinding.DialogOnlyofficeSettingsBinding
import org.json.JSONObject
import java.util.Locale

/**
 * 在 WebView 中嵌入 ONLYOFFICE 编辑器。
 *
 * 页面 HTML 通过 [WebView.loadDataWithBaseURL] 加载，base URL 设为 DocumentServer 地址，
 * 这样页面与 api.js / 编辑器 iframe 同源，避免混合内容与跨域问题。
 *
 * 文档修改的保存依赖服务端 callbackUrl；没有 callbackUrl 时可在工具栏点“下载副本”。
 */
class OnlyOfficeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlyofficeBinding
    private lateinit var prefs: OnlyOfficePrefs

    private var settings: OnlyOfficeSettings? = null
    private var configJson: String = ""
    private var serverUrl: String = ""

    private var ready = false
    private var modified = false
    private var hasUserChanges = false
    private var errorShown = false

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var settingsDialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper())

    private val hideToastRunnable = Runnable {
        binding.statusToast.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { binding.statusToast.visibility = View.GONE }
            .start()
    }

    private val loadTimeoutRunnable = Runnable {
        if (!ready && !errorShown) {
            showError("编辑器加载超时，请检查 DocumentServer 地址、api.js 是否可访问，以及文档 URL 是否对服务器可达。")
        }
    }

    private val html: String by lazy {
        assets.open("onlyoffice/editor.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileChooserCallback ?: return@registerForActivityResult
        fileChooserCallback = null
        if (result.resultCode != RESULT_OK) {
            callback.onReceiveValue(null)
            return@registerForActivityResult
        }
        val data = result.data
        val clipData = data?.clipData
        val uris: Array<Uri> = when {
            clipData != null && clipData.itemCount > 0 ->
                Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }

            data?.data != null -> arrayOf(data.data!!)
            else -> emptyArray()
        }
        callback.onReceiveValue(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlyofficeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = OnlyOfficePrefs(this)

        binding.btnBack.setOnClickListener { confirmExit() }
        binding.btnReload.setOnClickListener { loadEditor() }
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnErrorSettings.setOnClickListener { showSettingsDialog() }
        binding.btnRetry.setOnClickListener { loadEditor() }
        binding.btnDownload.setOnClickListener { requestDownloadAs() }
        binding.errorPanel.isClickable = true

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExit()
            }
        })

        setupWebView()
        loadEditor()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        settingsDialog?.takeIf { it.isShowing }?.dismiss()
        binding.webView.removeJavascriptInterface(BRIDGE_NAME)
        (binding.webView.parent as? ViewGroup)?.removeView(binding.webView)
        binding.webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------
    // WebView
    // ---------------------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString PaperlessOnlyOffice/1.0"
        }

        binding.webView.addJavascriptInterface(EditorBridge(), BRIDGE_NAME)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                return when (uri.scheme?.lowercase(Locale.ROOT)) {
                    "http", "https" -> {
                        if (serverUrl.isNotBlank() && OnlyOfficeUrls.sameHost(uri.toString(), serverUrl)) {
                            false
                        } else {
                            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            true
                        }
                    }

                    "intent" -> {
                        runCatching {
                            startActivity(
                                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        true
                    }

                    "mailto", "tel" -> {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        true
                    }

                    else -> false
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    showError("网络错误：${error.description} (${error.errorCode})")
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.url.toString().contains("/api/documents/api.js")) {
                    showError("api.js 请求失败：HTTP ${errorResponse.statusCode}")
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                sslHandler: SslErrorHandler,
                error: SslError
            ) {
                sslHandler.cancel()
                showError("DocumentServer 的 HTTPS 证书校验失败：${error.url}")
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progress.visibility =
                    if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                Log.d(TAG, "console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                return true
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    fileChooserLauncher.launch(fileChooserParams.createIntent())
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    false
                }
            }
        }

        binding.webView.setDownloadListener { url, _, _, _, _ ->
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                downloadFromUrl(url, null)
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------

    private fun loadEditor() {
        val current = prefs.load()
        if (!OnlyOfficeUrls.isHttpUrl(current.serverUrl)) {
            showError("还没有配置 DocumentServer 地址，请先点“设置”填写。")
            return
        }
        if (!OnlyOfficeUrls.isHttpUrl(current.fileUrl)) {
            showError("文档 URL 无效，请先在“设置”里填写一个 http/https 地址。")
            return
        }

        settings = current
        serverUrl = current.serverUrl
        configJson = try {
            OnlyOfficeConfig.build(current, isNightMode())
        } catch (e: Exception) {
            showError("生成编辑器配置失败：${e.message}")
            return
        }

        ready = false
        modified = false
        hasUserChanges = false
        errorShown = false
        handler.removeCallbacks(loadTimeoutRunnable)
        binding.progress.visibility = View.VISIBLE
        binding.errorPanel.visibility = View.GONE
        binding.statusToast.visibility = View.GONE
        binding.tvTitle.text = current.fileName
        binding.tvSubtitle.text = if (current.edit) {
            "正在加载编辑器…（编辑模式）"
        } else {
            "正在加载编辑器…（查看模式）"
        }

        // base URL 使用 DocumentServer，页面与 api.js / 编辑器同源。
        val baseUrl = current.serverUrl.trimEnd('/') + "/"
        binding.webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
        handler.postDelayed(loadTimeoutRunnable, 45_000)
    }

    private fun showError(message: String) {
        if (isFinishing || isDestroyed) return
        errorShown = true
        ready = false
        handler.removeCallbacks(loadTimeoutRunnable)
        handler.removeCallbacks(hideToastRunnable)
        binding.progress.visibility = View.GONE
        binding.statusToast.visibility = View.GONE
        binding.tvSubtitle.text = "加载失败"
        binding.tvErrorMessage.text = buildString {
            append(message)
            append("\n\n排查建议：")
            append("\n1. 手机浏览器打开 <DocumentServer>/healthcheck 是否返回 true")
            append("\n2. <DocumentServer>/web-apps/apps/api/documents/api.js 是否可访问")
            append("\n3. 文档 URL 是否能被 DocumentServer 访问（不是手机能访问就行）")
            append("\n4. JWT 密钥是否与服务器配置一致")
        }
        binding.errorPanel.visibility = View.VISIBLE
    }

    private fun showStatusToast(text: String, durationMs: Long = 2_500) {
        if (isFinishing || isDestroyed) return
        handler.removeCallbacks(hideToastRunnable)
        val toast = binding.statusToast
        toast.animate().cancel()
        toast.text = text
        toast.visibility = View.VISIBLE
        toast.alpha = 0f
        toast.animate().alpha(1f).setDuration(150).start()
        handler.postDelayed(hideToastRunnable, durationMs)
    }

    // ---------------------------------------------------------------------------------
    // Editor events
    // ---------------------------------------------------------------------------------

    private fun handleEvent(name: String, payload: String) {
        if (isFinishing || isDestroyed) return
        val json = runCatching { JSONObject(payload) }.getOrNull()
        val data = json?.opt("data")
        LogUtils.d(TAG, "event=$name payload=$payload")

        when (name) {
            "onAppReady" -> {
                binding.tvSubtitle.text = "编辑器正在启动…"
            }

            "onDocumentReady" -> {
                ready = true
                errorShown = false
                handler.removeCallbacks(loadTimeoutRunnable)
                binding.progress.visibility = View.GONE
                binding.errorPanel.visibility = View.GONE
                binding.tvSubtitle.text = if (settings?.edit == true) {
                    "已就绪（编辑模式）"
                } else {
                    "已就绪（查看模式）"
                }
                showStatusToast("✅ 文档已就绪")
            }

            "onDocumentStateChange" -> {
                val isModified = (data as? Boolean) ?: json?.optBoolean("data", false) ?: false
                modified = isModified
                if (isModified) {
                    hasUserChanges = true
                    binding.tvSubtitle.text = "文档已修改"
                    showStatusToast("📝 文档已修改")
                } else {
                    binding.tvSubtitle.text = "更改已保存"
                    if (hasUserChanges) {
                        hasUserChanges = false
                        showStatusToast("✅ 更改已保存")
                    }
                }
            }

            "onError" -> {
                val errorObject = data as? JSONObject
                val code = errorObject?.optInt("errorCode", 0) ?: 0
                val description = errorObject?.optString("errorDescription").orEmpty()
                showError(formatEditorError(code, description))
            }

            "onWarning" -> {
                val warning = when (data) {
                    is String -> data
                    is JSONObject -> data.optString("errorDescription")
                    else -> ""
                }
                if (warning.isNotBlank()) showStatusToast(warning, 4_000)
            }

            "onRequestClose" -> confirmExit()

            "onDownloadAs" -> {
                val file = data as? JSONObject
                val url = file?.optString("url").orEmpty()
                if (url.isNotBlank()) {
                    downloadFromUrl(url, file?.optString("fileType"))
                }
            }

            "onOutdatedVersion" -> {
                showStatusToast("编辑器版本已更新，正在重新加载…", 2_000)
                loadEditor()
            }

            "onInfo" -> Unit
            else -> Unit
        }
    }

    private fun formatEditorError(code: Int, description: String): String {
        val hint = when (code) {
            -1 -> "DocumentServer 内部错误，请查看服务器日志。"
            -2 -> "文档 key 冲突或已过期，点“重新加载”即可生成新的 key。"
            -3 -> "文档下载失败，请检查文档 URL 和服务端网络。"
            -4 -> "DocumentServer 无法下载文档 URL。请确认该 URL 对 DocumentServer 所在容器/机器可达。"
            -8 -> "文件类型与 documentType 不匹配，或 JWT token 校验失败（密钥不一致）。"
            else -> ""
        }
        return buildString {
            append("编辑器错误")
            if (code != 0) append(" [").append(code).append("]")
            if (description.isNotBlank()) append("：").append(description)
            if (hint.isNotBlank()) append("\n\n").append(hint)
        }
    }

    // ---------------------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------------------

    private fun showSettingsDialog() {
        if (settingsDialog?.isShowing == true) return

        val dialogBinding = DialogOnlyofficeSettingsBinding.inflate(LayoutInflater.from(this))
        val current = prefs.load()
        dialogBinding.edtServer.setText(current.serverUrl)
        dialogBinding.edtSecret.setText(current.jwtSecret)
        dialogBinding.edtFileUrl.setText(current.fileUrl)
        dialogBinding.edtCallback.setText(current.callbackBase)
        dialogBinding.edtUser.setText(current.userName)
        dialogBinding.cbEdit.isChecked = current.edit

        val typeOptions = listOf("自动", "docx", "xlsx", "pptx", "pdf", "doc", "xls", "ppt", "txt")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spType.adapter = adapter
        val typeIndex = typeOptions.indexOfFirst { it.equals(current.fileType, true) }
        dialogBinding.spType.setSelection(if (typeIndex >= 0) typeIndex else 0)

        dialogBinding.btnDefaults.setOnClickListener {
            dialogBinding.edtServer.setText(OnlyOfficePrefs.DEFAULT_SERVER)
            dialogBinding.edtSecret.setText(OnlyOfficePrefs.DEFAULT_SECRET)
            dialogBinding.edtFileUrl.setText(OnlyOfficePrefs.DEFAULT_FILE_URL)
            dialogBinding.spType.setSelection(typeOptions.indexOf(OnlyOfficePrefs.DEFAULT_FILE_TYPE))
            dialogBinding.edtCallback.setText("")
            dialogBinding.edtUser.setText(OnlyOfficePrefs.DEFAULT_USER_NAME)
            dialogBinding.cbEdit.isChecked = false
            dialogBinding.tvDialogError.visibility = View.GONE
            dialogBinding.tvTestResult.visibility = View.GONE
        }

        dialogBinding.btnTest.setOnClickListener {
            testConnection(dialogBinding)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("ONLYOFFICE 设置")
            .setView(dialogBinding.root)
            .setPositiveButton("保存并打开", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newSettings = readSettings(dialogBinding) ?: return@setOnClickListener
                prefs.save(newSettings)
                dialog.dismiss()
                loadEditor()
            }
        }
        dialog.setOnDismissListener {
            if (settingsDialog === dialog) settingsDialog = null
        }
        settingsDialog = dialog
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.80f).toInt(),
            (resources.displayMetrics.heightPixels * 0.96f).toInt()
        )
    }

    private fun readSettings(binding: DialogOnlyofficeSettingsBinding): OnlyOfficeSettings? {
        binding.tvDialogError.visibility = View.GONE

        val server = OnlyOfficeUrls.normalizeServer(binding.edtServer.text?.toString().orEmpty())
        if (!OnlyOfficeUrls.isHttpUrl(server)) {
            return showDialogError(binding, "DocumentServer 地址无效，请填写 http:// 或 https:// 地址")
        }

        val fileUrl = binding.edtFileUrl.text?.toString().orEmpty().trim()
        if (!OnlyOfficeUrls.isHttpUrl(fileUrl)) {
            return showDialogError(binding, "文档 URL 无效，必须是 http:// 或 https:// 地址")
        }

        val selectedType = binding.spType.selectedItem?.toString().orEmpty()
        val fileType = if (selectedType.isBlank() || selectedType == "自动") {
            OnlyOfficeUrls.extensionOf(fileUrl)
        } else {
            selectedType.lowercase(Locale.ROOT)
        }
        if (fileType == null || OnlyOfficeDocumentKind.fromFileType(fileType) == null) {
            return showDialogError(binding, "无法识别文件类型，请选择“自动”或在文件名中带上扩展名")
        }

        val callback = binding.edtCallback.text?.toString().orEmpty().trim()
        if (callback.isNotEmpty() && !OnlyOfficeUrls.isHttpUrl(callback)) {
            return showDialogError(binding, "回调地址无效，必须是 http:// 或 https:// 地址")
        }

        val user = binding.edtUser.text?.toString().orEmpty().trim()
            .ifBlank { OnlyOfficePrefs.DEFAULT_USER_NAME }

        return OnlyOfficeSettings(
            serverUrl = server,
            jwtSecret = binding.edtSecret.text?.toString().orEmpty().trim(),
            userName = user,
            fileUrl = fileUrl,
            fileType = fileType,
            edit = binding.cbEdit.isChecked,
            download = false,
            print = false,
            key = "",
            callbackBase = callback,
        )
    }

    private fun showDialogError(
        binding: DialogOnlyofficeSettingsBinding,
        message: String
    ): OnlyOfficeSettings? {
        binding.tvDialogError.text = message
        binding.tvDialogError.visibility = View.VISIBLE
        return null
    }

    private fun testConnection(binding: DialogOnlyofficeSettingsBinding) {
        val server = OnlyOfficeUrls.normalizeServer(binding.edtServer.text?.toString().orEmpty())
        if (!OnlyOfficeUrls.isHttpUrl(server)) {
            showDialogError(binding, "请先填写有效的 DocumentServer 地址")
            return
        }
        binding.tvDialogError.visibility = View.GONE
        binding.btnTest.isEnabled = false
        binding.btnTest.text = "测试中…"
        binding.tvTestResult.visibility = View.VISIBLE
        binding.tvTestResult.text = "正在检测 $server …"

        Thread {
            val result = OnlyOfficeServerProbe.check(server)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                binding.btnTest.isEnabled = true
                binding.btnTest.text = "测试连接"
                binding.tvTestResult.text = buildString {
                    append(if (result.ok) "✓ 连接成功" else "✗ 连接失败")
                    append("\nhealthcheck：")
                    append(if (result.healthOk) "✓" else "✗")
                    result.healthStatus?.let { append(" (HTTP ").append(it).append(")") }
                    append("\napi.js：")
                    append(if (result.apiOk) "✓" else "✗")
                    result.apiStatus?.let { append(" (HTTP ").append(it).append(")") }
                    result.error?.let { append("\n错误：").append(it) }
                }
            }
        }.start()
    }

    // ---------------------------------------------------------------------------------
    // Download / exit / helpers
    // ---------------------------------------------------------------------------------

    private fun requestDownloadAs() {
        val script = "(function(){try{" +
                "return !!(window.OOAndroidDemo&&window.OOAndroidDemo.downloadAs());" +
                "}catch(e){return false;}})()"
        binding.webView.evaluateJavascript(script) { result ->
            if (result != "true") {
                showStatusToast("编辑器尚未就绪，无法下载副本")
            }
        }
    }

    private fun downloadFromUrl(url: String, fileType: String?) {
        val absoluteUrl = if (url.startsWith("/")) serverUrl + url else url
        val extension = fileType
            ?.takeIf { it.isNotBlank() }
            ?.lowercase(Locale.ROOT)
            ?: settings?.fileType
            ?: "docx"
        val baseName = settings?.fileName?.substringBeforeLast('.') ?: "document"
        val fileName = OnlyOfficeUrls.sanitizeFileName("$baseName.$extension")

        try {
            val request = DownloadManager.Request(Uri.parse(absoluteUrl)).apply {
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            showStatusToast("开始下载：$fileName", 3_000)
        } catch (e: Exception) {
            LogUtils.e(TAG, "下载副本失败：${e.message}")
            showStatusToast("下载失败：${e.message}")
        }
    }

    private fun confirmExit() {
        if (!modified) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("退出编辑器")
            .setMessage(
                if (settings?.callbackUrl == null) {
                    "文档有未保存的修改，退出后这些修改会丢失。确定退出吗？"
                } else {
                    "文档有未保存的修改，退出前请确认服务端已保存。确定退出吗？"
                }
            )
            .setPositiveButton("退出") { _, _ -> finish() }
            .setNegativeButton("继续编辑", null)
            .show()
    }

    private fun isNightMode(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    inner class EditorBridge {

        @JavascriptInterface
        fun getEditorConfig(): String = configJson

        @JavascriptInterface
        fun getServerUrl(): String = serverUrl

        @JavascriptInterface
        fun isDarkTheme(): Boolean = isNightMode()

        @JavascriptInterface
        fun onEvent(name: String, payload: String) {
            runOnUiThread { handleEvent(name, payload) }
        }

        @JavascriptInterface
        fun reportError(source: String, message: String) {
            runOnUiThread { showError("$source：$message") }
        }
    }

    companion object {
        private const val TAG = "PaperlessOnlyOffice"
        private const val BRIDGE_NAME = "OOAndroid"

        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_FILE_TYPE = "extra_file_type"
        private const val EXTRA_FILE_NAME = "extra_file_name"
        private const val EXTRA_EDIT = "extra_edit"
        private const val EXTRA_CALLBACK = "extra_callback"

        fun newIntent(context: Context): Intent =
            Intent(context, OnlyOfficeActivity::class.java)
    }
}
