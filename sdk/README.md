
# 播放窗口日志链（调试用）

悬浮窗播放涉及的链路比较长（平台事件 → 窗口创建 → Surface → 解码器配置 → 帧渲染 → 停止销毁），
任意一环出问题都表现为"窗口不出来 / 黑屏 / 卡住 / 异常关闭"。为此给整条链路加了统一日志入口
`com.paperless.util.PlayerLog`，一次播放的所有日志共用一个会话 ID。

## 抓取日志

全部播放链路日志都在 `PlayWin` 前缀下：

```shell
# 实时查看整条播放链路
adb logcat -c && adb logcat -s PlayWin

# 导出一份完整日志（推荐：出问题后直接发给开发定位）
adb logcat -d -s PlayWin > playwin.log

# 只看某一次播放（会话 ID 见日志中的 S 编号）
adb logcat -d -s PlayWin | findstr "S3"
```

## 日志格式

```
PlayWin: [窗口][S3][4.312s][main] addView 成功 窗口=720x1280
PlayWin: [解码][S3][4.418s][VideoDecodeThread-0] 解码器启动成功 耗时=63ms 总耗时(自解码线程启动)=112ms，等待首帧输出
PlayWin: [帧][S3][4.501s][VideoDecodeThread-0] 首帧已渲染（解码器配置完成到首帧耗时=83ms）
```

| 字段 | 含义 |
| --- | --- |
| `PlayWin` | logcat TAG，统一过滤用 |
| `[窗口]` | 链路环节：`事件` `窗口` `Surface` `解码` `帧` `渲染` `控制` `同屏` |
| `[S3]` | 播放会话 ID，每次真正新建播放窗口 +1；`[S-]` 表示未开窗（如 App 启动期的日志） |
| `[4.312s]` | 距该会话第一条日志的耗时，用来定位卡在哪一步、这一步花了多久 |
| `[main]` | 线程名：`main` 主线程、`VideoDecodeThread-N` 解码线程、其他为 JNI 回调线程 |

## 链路环节与关键日志

| 环节 | 关键日志 | 说明 |
| --- | --- | --- |
| 事件入口 | `收到媒体播放通知 res=…` / `收到流播放通知 res=…` | 平台推送的播放指令，含强制播放标记；`res != 0` 会被明确记为"忽略" |
| 窗口创建 | `===== 播放窗口创建开始 … 会话=S3` … `===== 播放窗口创建完成` | 起止成对，包含窗口尺寸/位置/type/flags、设备上下文、`addView` 结果 |
| 复用窗口 | `复用已有播放窗口，仅更新标题与播放标记` | 第二次播放指令未新建窗口，只改标题和播放标记 |
| Surface | `surfaceCreated: Surface 有效=…` / `surfaceDestroyed: Surface 被销毁` | Surface 回调，`surfaceDestroyed` 会触发解码资源释放 |
| 解码准备 | `首帧/关键帧触发解码器配置 上报尺寸=… 变化项=[…]` | 说明为何重新配置解码器（未配置/宽高变化/旋转变化/mime变化） |
| 解码配置 | `configureCodec: 解码器启动成功 耗时=…` | 含 `isSupported`、上报尺寸、旋转、显示尺寸与比例 |
| 首帧 | `首帧已渲染（解码器配置完成到首帧耗时=…）` | **有没有出画面的分水岭**；此条不出现即"解码/渲染没起来" |
| 帧率 | `帧率上报：每秒帧数 resId=0 FPS=25` | 上游是否持续推帧的直观指标 |
| 等比适配 | `resetPlayerViewRenderSize: 视频源=… 窗口上限=… 适配后=…` | 画面拉伸/黑边的定位点（含缩放系数与两个比例） |
| 交互 | `用户点击暂停/继续播放/拖动进度/退出播放 …` | 每个操作都记录了随之下发的 JNI 调用与参数 |
| 同屏 | `开始同屏确认: 勾选目标数=… 目标=[…]` | 同屏目标列表、剔除同视频源、实际调用的 `mediaPlay/streamPlay` |
| 停止销毁 | `===== 播放窗口销毁开始 会话=S3` … `销毁完成` + `dismiss 调用来源` | 起止成对并打印调用栈，解决"窗口莫名消失" |

## 常见问题定位

| 现象 | 先看这几条 |
| --- | --- |
| 窗口完全不出来 | 有 `收到…播放通知` 但无 `播放窗口创建开始` → 事件被 `res != 0` 过滤；有 `创建开始` 无 `addView 成功` → 悬浮窗权限问题（日志会打印异常） |
| 窗口出来但黑屏 | 无 `surfaceCreated` → Surface 未创建；有 `解码器启动成功` 无 `首帧已渲染` → 解码无输出；`解码线程持续取不到帧` → 上游没推帧 |
| 画面拉伸/有黑边 | `resetPlayerViewRenderSize` 的"视频源/窗口上限/适配后"三个尺寸，以及 `applyPlayerViewRenderSize: 视频源尺寸未知，跳过适配` |
| 窗口自己消失 | `收到…停止通知` → `已安排 500ms 后延迟销毁` → `延迟销毁到期`；`dismiss 调用来源` 会打印调用栈 |
| 卡顿/丢帧 | `帧率上报：…FPS=`；`队列已满且无非关键帧可移除，本帧被丢弃`；`本轮解码未渲染新画面` |

## 开关与约定

- 开关（**默认关闭**）：`SdkConfig.playLogEnable = true` 打开；另保留 `PlayerLog.enable`（运行期）与 `PlayerLog.ENABLE`（编译期），三者是「与」关系。
  ```kotlin
  // 依赖方 App 启动处，需要排查播放问题时打开
  SdkConfig.playLogEnable = true
  ```
- 低频约束：逐帧、触摸移动、队列溢出等高频日志均已节流（默认 1~2 秒一条），不会刷爆 logcat。
- 新增链路日志时统一用 `PlayerLog.i("环节标签", "…")`，需要会话 / 耗时 / 线程前缀时不要再手写 `LogUtils`。
- 兼容：原 `LogUtils` 日志全部保留，新链路日志只做追加，不影响既有排查习惯。

### 高频回调日志开关（`SdkConfig.logEnable`，**默认已关闭**）

`Call` / `BaseJni` 里有 8 处跟随**每次 native 回调**打印的日志，实测占依赖方 App 全部日志量的 **56%**，
其中 `Call.error_ret` 有很大比例是 `ret=0`（正常返回），排查价值低，因此**默认关闭**：

| 位置 | 内容 | 实测频率 |
| --- | --- | --- |
| `Call.error_ret` | `error_ret：type=…,method=…,ret=…` | ~3.4 次/秒（43% 是 ret=0） |
| `Call.callback_method` | `callback_method：type=…,method=…,datalen=…` | ~1 次/秒 |
| `Call` | 后台接收包长、队列已满移除最旧帧 | 随收包频率 |
| `BaseJni` | 查询设备硬件信息成功/失败、参会人权限查询失败、参会人颜色 | 随调用频率 |

```kotlin
// 依赖方按需打开（建议跟随自己的"调试开关"，默认保持 false）
SdkConfig.logEnable = true
```

> 关闭它**不影响任何功能**，只是不再打印上述日志；需要排查回调/权限问题时再打开。

### 2.5.13
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