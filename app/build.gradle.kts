import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun releaseKeystoreFile(): java.io.File? {
    val path = System.getenv("ANDROID_KEYSTORE_FILE")
        ?: keystoreProperties.getProperty("storeFile")
        ?: "release.keystore"
    val f = rootProject.file(path)
    return f.takeIf { it.isFile }
}

fun propOrEnv(key: String, envName: String): String =
    System.getenv(envName)?.trim().orEmpty().ifBlank {
        keystoreProperties.getProperty(key)?.trim().orEmpty()
    }

android {
    namespace = "com.weightagent.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.weightagent.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 21
        versionName = "0.1.20"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // 与 Android 默认 debug 相同口令；提交在仓库内，使本地 debug / 无 release 时的 release 与 CI APK 同签名
        create("ciDebug") {
            storeFile = rootProject.file("keystores/ci-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        val kf = releaseKeystoreFile()
        val storePwd = propOrEnv("storePassword", "ANDROID_KEYSTORE_PASSWORD")
        val keyPwd = propOrEnv("keyPassword", "ANDROID_KEY_PASSWORD")
        val alias = propOrEnv("keyAlias", "ANDROID_KEY_ALIAS")
        if (kf != null && storePwd.isNotBlank() && keyPwd.isNotBlank() && alias.isNotBlank()) {
            create("release") {
                storeFile = kf
                storePassword = storePwd
                keyAlias = alias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ciDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val kf = releaseKeystoreFile()
            val storePwd = propOrEnv("storePassword", "ANDROID_KEYSTORE_PASSWORD")
            val keyPwd = propOrEnv("keyPassword", "ANDROID_KEY_PASSWORD")
            val alias = propOrEnv("keyAlias", "ANDROID_KEY_ALIAS")
            signingConfig = if (kf != null && storePwd.isNotBlank() && keyPwd.isNotBlank() && alias.isNotBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("ciDebug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.security:security-crypto:1.0.0")

    implementation("com.qcloud.cos:cos-android-nobeacon:5.9.50")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
