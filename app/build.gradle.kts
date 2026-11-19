import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// API key không bao giờ commit lên Git: đọc từ local.properties (đã nằm trong .gitignore).
// Thêm dòng sau vào local.properties:  GEMINI_API_KEY=your_key_here
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY", "")
val geminiModel: String = localProperties.getProperty("GEMINI_MODEL", "gemini-3.1-flash-lite")
val mealDbApiKey = localProperties.getProperty("MEAL_DB_API_KEY", "1").trim()
fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

// Private configuration: generated once by scripts/New-ReleaseKeystore.ps1.
val signingProperties = Properties().apply {
    val file = rootProject.file(".signing/release.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun releaseProperty(name: String): String = providers.environmentVariable(name).orNull
    ?: signingProperties.getProperty(name, localProperties.getProperty(name, ""))
val releaseStoreFile = releaseProperty("NUTRISCAN_RELEASE_STORE_FILE")
val releaseStorePassword = releaseProperty("NUTRISCAN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseProperty("NUTRISCAN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseProperty("NUTRISCAN_RELEASE_KEY_PASSWORD")
val releaseSigningReady = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it.isNotBlank() }

android {
    namespace = "vn.edu.vlu.milestone2"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.edu.vlu.milestone2"
        minSdk = 24
        targetSdk = 37
        versionCode = 4
        versionName = "2.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPEN_FOOD_FACTS_BASE_URL", "\"https://world.openfoodfacts.org/\"")
        buildConfigField("String", "MEAL_DB_BASE_URL", "https://www.themealdb.com/api/json/v1/$mealDbApiKey/".asBuildConfigString())
        buildConfigField("String", "GEMINI_BASE_URL", "\"https://generativelanguage.googleapis.com/\"")
        buildConfigField("String", "GEMINI_API_KEY", geminiApiKey.asBuildConfigString())
        buildConfigField("String", "GEMINI_MODEL", geminiModel.asBuildConfigString())
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// Fail rather than accidentally hand in an unsigned or debug-signed Release APK.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val signingConfigured = releaseSigningReady
    doFirst {
        check(signingConfigured) {
            "Release signing is missing. Run scripts/New-ReleaseKeystore.ps1 or configure NUTRISCAN_RELEASE_* privately."
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
    // AndroidX + Material 3
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // MVVM (Milestone 2)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Room (Milestone 07)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network (Milestone 04): Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Image loading (Milestone 04)
    implementation(libs.glide)

    // Firebase Auth + Firestore (Milestone 04 + proposal 3.1)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.security.crypto)
    debugImplementation(libs.leakcanary.android)

    // CameraX + ML Kit + WorkManager (proposal 3.2 / 3.3 / 4.4)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
