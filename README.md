[![](https://jitpack.io/v/kelvin-panda/paperless-sdk.svg)](https://jitpack.io/#kelvin-panda/paperless-sdk)

# 依赖

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.kelvin-panda:paperless-sdk:2.5.29")
}
```

# 使用
调用方`app/build.gradle`配置

```kotlin
    //解决API 28只创建arm64目录，导致找不到库的问题
    splits {
        abi {
            isEnable = true // 启用 ABI 拆分
            reset() // 重置所有配置项到默认状态
            include("armeabi-v7a","arm64-v8a") //,'arm64-v8a' // 指定要包含的 ABI
            isUniversalApk = true //不生成包含所有 ABI 的单一 APK
        }
    }
```

调用`api`前需要进行初始化，比如在`Application`的`onCreate`中调用
```kotlin
override fun onCreate() {
    super.onCreate()
    // 初始化
    Paperless.init()
}
```

之后通过类继承`BaseJni`后使用

```kotlin
object Jni :BaseJni(){
	//自定义父类中没有的接口
}
```

# App Demo 功能

`app` 模块是 SDK 的联调 Demo，除原有播放/缓存/黑名单等入口外，新增：

## 当前会议文件列表

首页点击「查看当前会议文件」：

- 遍历当前会议所有目录，列出：序号、名称、大小、操作
- 「查看」：下载到应用缓存，完成后通过 `FileProvider` 交给系统应用打开
- 「删除」：二次确认后调用 `Jni.delFile(dirId, file)` 从服务器删除，并刷新列表

实现见 [MeetingFilesDialog.kt](app/src/main/java/com/xlk/paperless/sdk/dialog/MeetingFilesDialog.kt) 和
[dialog_meeting_files.xml](app/src/main/res/layout/dialog_meeting_files.xml)。

## ONLYOFFICE 编辑器

首页点击「ONLYOFFICE 编辑器」进入 WebView 编辑器：

- 可配置 DocumentServer 地址、JWT 密钥、文档 URL、文件类型、编辑模式、回调地址和用户名
- 支持 healthcheck / api.js 连通性检测、查看/编辑、文档状态提示、错误诊断和「下载副本」
- 默认本机联调配置：`http://10.248.6.98:8080`、JWT `secret`、
  示例文档 `http://172.17.0.1:8899/sample.docx`
- 文档 URL 必须能被 DocumentServer 访问；编辑保存需要服务端提供 `callbackUrl`
- 客户端生成 JWT 仅用于 Demo，正式环境必须由服务端生成配置和 token

实现见 [OnlyOfficeActivity.kt](app/src/main/java/com/xlk/paperless/sdk/onlyoffice/OnlyOfficeActivity.kt)、
[OnlyOfficeConfig.kt](app/src/main/java/com/xlk/paperless/sdk/onlyoffice/OnlyOfficeConfig.kt)、
[editor.html](app/src/main/assets/onlyoffice/editor.html)。

> ONLYOFFICE DocumentServer 需要独立部署（Docker/WSL 等），不是 SDK 内置服务。
> 本机调试可用 `F:\work\onlyofficedemo\tools\windows\start-onlyoffice.ps1` 启动。

## 库日志开关

首页「库日志开关」复选框会调用 `Call.setLogable(true/false)`，可实时打开/关闭底层 native
日志（默认关闭）。接口说明见 [sdk/README.md](sdk/README.md)。

> `app/src/main/assets/client.ini` 中的服务器地址、`[loghttp]` 和 `task_*` 为本地联调配置，
> 正式使用请按实际环境修改；`[loghttp]` 生产环境应设为 `enable=0` 或删除。

# 调试

播放窗口（悬浮窗播放）的完整链路日志见 [sdk/README.md](sdk/README.md#播放窗口日志链调试用)，
一次播放的所有日志共用会话 ID，可用 `adb logcat -s PlayWin` 一次性抓取
「入口事件 → 窗口创建 → Surface → 解码配置 → 帧渲染 → 停止销毁」全过程。
