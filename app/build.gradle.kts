plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.xlk.paperless.sdk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xlk.paperless.sdk"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    //解决API 28只创建arm64目录，导致找不到库的问题
    splits {
        abi {
            isEnable = true // 启用 ABI 拆分
            reset() // 重置所有配置项到默认状态
            include("armeabi-v7a", "arm64-v8a") //,'arm64-v8a' // 指定要包含的 ABI
            isUniversalApk = true //不生成包含所有 ABI 的单一 APK
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    api(libs.xxPermissions)
    api(libs.eventbus)
    api(libs.utilcodex)
//    implementation("com.gitee.xlk_gitee:paperless_sdk:1.3.11")
    api(project(":sdk"))
}