
### 2.5.12
#### 库更新
   - 添加pdf签名推送相关API
#### 新增
1. `com.paperless.data.repository.base.DataRepoManager`
   - 添加数据的监听管理：以实现调用方随时取的数据都是服务器中最新的数据
#### 修复
1. 双指缩放时的崩溃异常
   - 缩放范围算成空区间后 coerceIn 直接抛异常：`java.lang.IllegalArgumentException: Cannot coerce value to an empty range: maximum -99.9035 is less than minimum 99.9035.`
   - 对应`com.paperless.player.controller.PlayerControlView`中做修改
    ```kotlin
    view.translationX = if (maxTranslateX > 0f) {
        view.translationX.coerceIn(-maxTranslateX, maxTranslateX)
    } else {
        0f
    }
    view.translationY = if (maxTranslateY > 0f) {
        view.translationY.coerceIn(-maxTranslateY, maxTranslateY)
    } else {
        0f
    }
    ```

1.4.11
1. 使用`DecodeQueue`存取帧数据
2. 删除`decodeMap`相关数据

1.3.19
1. 优化悬浮窗播放控件

1.3.15
1. 上线悬浮窗窗口播放

1.3.10
1.`InterfaceCommondata.java`编码格式修正为`UTF-8`

1.3.9
1.`PlayerControlView`自定义播放器添加双指缩放的功能
2.`SdkConfig`添加相关配置参数

1.3.8
1. 添加新版本的会议统计
2. 添加复合全局自定义数据
3. 复合会议自定义数据
4. 添加升级包进度通知

1.3.3
- 更新64位`FFmpeg`库，解决找不到`OPUS`的问题
- 注释无效开发中的代码