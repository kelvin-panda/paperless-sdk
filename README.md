[![](https://jitpack.io/v/kelvin-panda/paperless-sdk.svg)](https://jitpack.io/#kelvin-panda/paperless-sdk)

# 依赖

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.kelvin-panda:paperless-sdk:2.5.16")
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

# 调试

播放窗口（悬浮窗播放）的完整链路日志见 [sdk/README.md](sdk/README.md#播放窗口日志链调试用)，
一次播放的所有日志共用会话 ID，可用 `adb logcat -s PlayWin` 一次性抓取
「入口事件 → 窗口创建 → Surface → 解码配置 → 帧渲染 → 停止销毁」全过程。
