plugins {
    id("com.android.application")
}

android {
    namespace = "com.leox.marketwidget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.leox.marketwidget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 故意保持零第三方依赖：网络用 HttpURLConnection，JSON 用平台自带 org.json
}
