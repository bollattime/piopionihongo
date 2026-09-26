// Kanji Kartları – uygulama modülü
// Android Studio şablonundaki "plugins" ve SDK satırları farklı yazılmışsa (ör. compileSdk { version = release(36) })
// şablondakini koruyabilirsin; önemli olan değerlerin aşağıdakiyle aynı olması.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.piopionihongo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.piopionihongo.app"
        minSdk = 24
        targetSdk = 36
        // Play'e her yeni yüklemede versionCode'u 1 artır.
        versionCode = 44
        versionName = "0.9"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Google Play satın alma (Premium ve destek). Play, 31 Ağustos 2026'dan itibaren en az 8. sürümü istiyor.
    implementation("com.android.billingclient:billing:9.1.0")
}
// Şablondan gelen başka bağımlılıklara (appcompat, material vb.) ihtiyaç yok; silebilirsin.
