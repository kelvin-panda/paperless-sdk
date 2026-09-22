package com.paperless.util

import java.util.Locale

/**
 * 播放窗口（悬浮窗播放）统一日志入口。
 *
 * 目的：把「入口事件 → 窗口创建 → Surface → 解码配置 → 帧渲染 → 停止销毁」整条链路的日志
 * 汇总到一个 TAG 下，并用「单次播放会话 ID」串起来，方便按一次播放抓取完整日志：
 *
 * ```shell
 * adb logcat -c && adb logcat -s PlayWin
 * adb logcat -d -s PlayWin > playwin.log
 * ```
 *
 * 日志格式：
 * ```
 * PlayWin: [窗口][S3][4.312s][main] addView 成功 窗口=720x1280
 * PlayWin: [解码][S3][4.418s][VideoDecodeThread-0] 解码器启动成功 mime=video/avc 输出=1920x1080
 * ```
 * - `PlayWin`：logcat TAG，统一过滤用
 * - `[窗口]`：链路环节标签（窗口 / 解码 / 帧 / 渲染 / 控制 / 同屏 / 事件 / Surface …）
 * - `[S3]`：会话 ID（每次真正新建播放窗口 +1），[NO_SESSION] 表示未开窗或不属于任何一次播放
 * - `[4.312s]`：该会话从第一行日志开始的耗时，用来定位「卡在哪一步、这一步花了多久」
 * - `[main]`：线程名，用来确认回调线程（主线程 / JNI 回调线程 / 解码线程）
 *
 * 关闭方式（默认关闭）：[com.paperless.sdk.SdkConfig.playLogEnable] = true 打开；置 false 关闭。
 * 另有编译期总开关 [ENABLE] 与运行期开关 [enable]，二者与 SdkConfig 开关是「与」关系。
 *
 * @author : Administrator
 * created on 2026/7/9
 */
object PlayerLog {

    /**
     * 编译期总开关，需要彻底移除日志时改为 false
     */
    const val ENABLE = true

    /**
     * logcat 统一 TAG 前缀，过滤命令：`adb logcat -s PlayWin`
     *
     * 注意：必须使用不含 `/` 等特殊字符的短 TAG。
     * utilcodex 的 LogUtils 会把「含特殊字符的字符串参数」当成日志正文走块状格式化分支，
     * 导致 TAG 不生效、`adb logcat -s PlayWin` 抓不到日志；因此这里直接用 android.util.Log。
     */
    const val TAG = "PlayWin"

    /**
     * 尚未开窗（没有会话）时使用的会话 ID
     */
    const val NO_SESSION = -1

    /**
     * 运行期开关（保留给历史调用），与 [com.paperless.sdk.SdkConfig.playLogEnable] 是「与」关系
     */
    @Volatile
    var enable: Boolean = true

    /**
     * 单次播放会话自增 ID，0 保留给「未开窗」状态
     */
    private var sessionSeq: Int = 0

    @Volatile
    private var sessionId: Int = NO_SESSION

    /**
     * 当前会话第一行日志的时间戳，用于计算链路耗时
     */
    @Volatile
    private var sessionStart: Long = 0L

    /**
     * 最终开关：编译期 ENABLE、SdkConfig.playLogEnable（依赖方控制，默认 false）、运行期 enable 三者都为真才输出
     */
    private fun isOn() = ENABLE && enable && com.paperless.sdk.SdkConfig.playLogEnable

    //<editor-fold desc="会话管理">

    /**
     * 开启一次新的播放会话（每次真正新建播放窗口时调用），返回新的会话 ID
     */
    @Synchronized
    fun openSession(): Int {
        sessionSeq++
        sessionId = sessionSeq
        sessionStart = System.currentTimeMillis()
        return sessionSeq
    }

    /**
     * 结束当前播放会话，后续日志标记为 [NO_SESSION]
     */
    @Synchronized
    fun closeSession() {
        sessionId = NO_SESSION
        sessionStart = 0L
    }

    /**
     * 当前会话 ID，未开窗时返回 [NO_SESSION]
     */
    fun currentSession(): Int = sessionId

    /**
     * 当前是否有进行中的播放会话
     */
    fun isSessionOpen(): Boolean = sessionId != NO_SESSION

    //</editor-fold>

    //<editor-fold desc="日志级别入口">

    fun v(label: String, msg: String, vararg args: Any?) {
        if (isOn()) print(LEVEL_V, label, msg, args)
    }

    fun d(label: String, msg: String, vararg args: Any?) {
        if (isOn()) print(LEVEL_D, label, msg, args)
    }

    fun i(label: String, msg: String, vararg args: Any?) {
        if (isOn()) print(LEVEL_I, label, msg, args)
    }

    fun w(label: String, msg: String, vararg args: Any?) {
        if (isOn()) print(LEVEL_W, label, msg, args)
    }

    /**
     * 错误日志（无异常对象）
     */
    fun e(label: String, msg: String, vararg args: Any?) {
        if (isOn()) print(LEVEL_E, label, msg, args)
    }

    /**
     * 错误日志（带异常），异常会被展开成「类型: 描述 @ 首个业务栈帧」
     */
    fun e(label: String, msg: String, tr: Throwable?) {
        if (isOn()) print(LEVEL_E, label, "$msg 异常=${tr.brief()}", emptyArray())
    }

    /**
     * 链路节点日志（最常用）：在关键步骤前后各打一条，便于定位卡点
     */
    fun step(label: String, msg: String, vararg args: Any?) {
        i(label, msg, *args)
    }

    //</editor-fold>

    //<editor-fold desc="辅助能力">

    /**
     * 低频诊断能力：只有满足「首次 或 距上次记录超过 interval」时才返回 true，
     * 避免逐帧日志刷爆 logcat（例如解码循环、触摸移动）
     *
     * @param counter 计数器对象（每个调用点各自持有）
     * @param interval 最小间隔（毫秒）
     */
    fun shouldLog(counter: ThrottleCounter, interval: Long = 1000L): Boolean {
        val now = System.currentTimeMillis()
        if (now - counter.last >= interval) {
            counter.last = now
            counter.count++
            return true
        }
        return false
    }

    /**
     * 日志节流计数器
     */
    class ThrottleCounter {
        @Volatile
        var last: Long = 0L

        @Volatile
        var count: Int = 0
    }

    /**
     * 打印当前调用栈（用于定位「谁把这个窗口关掉了」这类问题）
     *
     * @param depth 最多打印多少层
     */
    fun stack(label: String, msg: String, depth: Int = 5) {
        if (!isOn()) return
        val frames = Thread.currentThread().stackTrace
            .drop(2)
            .take(depth)
            .joinToString(" <- ") { frame ->
                "${frame.methodName}(${frame.fileName}:${frame.lineNumber})"
            }
        i(label, "$msg 调用栈=$frames")
    }

    /**
     * 异常简要描述：类型 + message + 第一个非日志框架的栈帧
     */
    fun Throwable?.brief(): String {
        val t = this ?: return "null"
        val first = t.stackTrace?.firstOrNull { frame ->
            frame.className?.startsWith("com.paperless") == true ||
                    frame.className?.startsWith("com.xlk") == true
        } ?: t.stackTrace?.firstOrNull()
        val location = first?.let { " @ ${it.fileName}:${it.lineNumber}" } ?: ""
        return "${t.javaClass.simpleName}: ${t.message}$location"
    }

    //</editor-fold>

    //<editor-fold desc="内部实现">

    private const val LEVEL_V = 2
    private const val LEVEL_D = 3
    private const val LEVEL_I = 4
    private const val LEVEL_W = 5
    private const val LEVEL_E = 6

    /**
     * 拼装消息前缀：[窗口][S1][1.234s][main]
     */
    private fun prefix(label: String): String {
        val session = sessionId
        val sessionPart = if (session == NO_SESSION) "S-" else "S$session"
        val start = sessionStart
        val elapsedPart = if (start <= 0L) {
            "-"
        } else {
            String.format(Locale.US, "%.3fs", (System.currentTimeMillis() - start) / 1000.0)
        }
        return "[$label][$sessionPart][$elapsedPart][${threadName()}]"
    }

    private fun print(level: Int, label: String, msg: String, args: Array<out Any?>) {
        val content = try {
            val formatted = if (args.isEmpty()) msg else String.format(Locale.US, msg, *args)
            "${prefix(label)} $formatted"
        } catch (t: Throwable) {
            "${prefix(label)} $msg <日志格式化失败:${t.message}>"
        }
        write(level, content)
    }

    /**
     * 按级别输出到 logcat。
     * 这里刻意不使用 utilcodex 的 LogUtils：它会把带 `/`、`{}` 等字符的参数判定为日志正文，
     * 从而让自定义 TAG 失效（详见类注释）。
     */
    private fun write(level: Int, content: String) {
        when (level) {
            LEVEL_V -> android.util.Log.v(TAG, content)
            LEVEL_D -> android.util.Log.d(TAG, content)
            LEVEL_W -> android.util.Log.w(TAG, content)
            LEVEL_E -> android.util.Log.e(TAG, content)
            else -> android.util.Log.i(TAG, content)
        }
    }

    /**
     * 线程名，超过 20 个字符进行截断，保证单行日志可读
     */
    private fun threadName(): String {
        val name = Thread.currentThread().name ?: "unknown"
        val simple = if (name.startsWith("Thread-")) name.removePrefix("Thread-") else name
        return if (simple.length > 20) simple.substring(0, 20) else simple
    }

    //</editor-fold>
}
