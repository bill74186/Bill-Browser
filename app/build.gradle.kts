import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 读取本地或 CI 注入的签名信息；缺失时回退到 debug 签名，确保 release APK 始终可安装
val signingProps = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.bill.browser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bill.browser"
        minSdk = 23          // Android 6.0
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
            // 优先使用环境变量（CI 注入），其次用 signing.properties，最后用 debug keystore
            storeFile = file(System.getenv("KEYSTORE_FILE")
                ?: signingProps.getProperty("storeFile")
                ?: "${android.sdkDirectory}/.android/debug.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: signingProps.getProperty("storePassword")
                ?: "android"
            keyAlias = System.getenv("KEY_ALIAS")
                ?: signingProps.getProperty("keyAlias")
                ?: "androiddebugkey"
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: signingProps.getProperty("keyPassword")
                ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.webkit:webkit:1.10.0")
}
