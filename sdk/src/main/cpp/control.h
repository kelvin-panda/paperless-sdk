//
// Created by xlk on 2021/9/27.
// logable控制日志打印
// 在需要的地方添加： #include "control.h"
//

// 日志打印开关（运行时可修改，默认关闭）
// 由 jni_wallet_netinit.cpp 中的 g_logable 保存实际值，
// java 端可通过 Call.setLogable(true/false) 动态开关日志打印。
#ifndef logable
extern volatile bool g_logable;
#define logable g_logable
#endif

// 对应java接口操作类
#ifndef main_interface_class_path_name
#define main_interface_class_path_name  "com/paperless/sdk/Call"
#endif