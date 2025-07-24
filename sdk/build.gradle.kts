plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.paperless.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags("-frtti -fexceptions")
                //,"arm64-v8a"
                abiFilters("armeabi-v7a")
                //arguments '-DANDROID_STL=c++_shared'
                //arguments '-DANDROID_STL=gnu_stl'
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }


    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
            jni.setSrcDirs(emptyList<String>())
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
            include("armeabi-v7a") //,'arm64-v8a' // 指定要包含的 ABI
            isUniversalApk = false //不生成包含所有 ABI 的单一 APK
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
                version = "1.0.7"
                from(components["release"])
            }
        }
    }
}
dependencies {
    api("com.blankj:utilcodex:1.31.1")
    api("org.greenrobot:eventbus:3.3.1")
    api(files("libs/ini4j-0.5.2.jar"))
    api(files("libs/protobuf-java-3.3.0.jar"))
}

