
[![](https://jitpack.io/v/com.gitee.xlk_gitee/paperless_sdk.svg)](https://jitpack.io/#com.gitee.xlk_gitee/paperless_sdk)


调用方`app/build.gradle`配置

```kotlin
    //解决API 28只创建arm64目录，导致找不到库的问题
    splits {
        abi {
            isEnable = true // 启用 ABI 拆分
            reset() // 重置所有配置项到默认状态
            include("armeabi-v7a") //,'arm64-v8a' // 指定要包含的 ABI
            isUniversalApk = false //不生成包含所有 ABI 的单一 APK
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

