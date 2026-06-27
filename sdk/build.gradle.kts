plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.paperless.sdk"
    ndkVersion = "28.2.13676358"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                // C++ 编译器参数
                // -frtti: 启用运行时类型信息 (RTTI)，支持 dynamic_cast 和 typeid 操作符
                // -Wl,<options>: 将逗号分隔的 <options> 直接传递给链接器。这是一个重要的高级功能，
                //      比如 -Wl,-z,max-page-size=16384 就是告诉链接器将库的内存对齐（alignment）设置为 16KB
                cppFlags("-frtti -fexceptions")
                abiFilters("armeabi-v7a", "arm64-v8a")

                // CMake 系统参数 通过 -D 定义 CMake 变量来修改 NDK 工具链的行为
                // -DANDROID_STL=c++_shared
                //      选择链接的 C++ 标准库，例如 c++_shared (推荐) 或 c++_static
                // -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON
                //      显式告诉 CMake 支持可变页面大小（NDK r23+ 推荐）
                //      版本较旧（r23 以下），可以用这行替代：-DANDROID_PAGE_SIZE=16384
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON", "-DANDROID_STL=c++_shared")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }


    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    //解决API 28只创建arm64目录，导致找不到库的问题
    splits {
        abi {
            isEnable = true // 启用 ABI 拆分
            reset() // 重置所有配置项到默认状态
            include("armeabi-v7a", "arm64-v8a") //,'arm64-v8a' // 指定要包含的 ABI
            isUniversalApk = true //不生成包含所有 ABI 的单一 APK
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}
//https://github.com/jitpack/android-example
// 使用afterEvaluate确保在项目评估完成后获取组件
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.gitee.xlk_gitee"
                artifactId = "sdk-library"
                //定义：大更新.库更新.java层更新
                // 移除 version 的硬编码，让 JitPack 从 Git Tag 获取
                version = "1.4.4"
                from(components["release"])
            }
        }
    }
}
dependencies {
    api(libs.utilcodex)
    api(libs.eventbus)
    api(files("libs/ini4j-0.5.2.jar"))
    api(files("libs/protobuf-java-3.3.0.jar"))
}

