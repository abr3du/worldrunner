plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// versionCode is the number of commits on HEAD, so every build of a newer main commit can
// update the installed app. Outside a git checkout it falls back to 1.
val commitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

// Test APKs are signed with one shared key so a newer APK installs over an older one.
// The key never lives in the repo: CI decodes it from secrets (see README), and local
// builds without these variables fall back to the machine's own debug key.
val testKeystore = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
val testKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val testKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val testKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasTestSigning = listOf(testKeystore, testKeystorePassword, testKeyAlias, testKeyPassword).all { !it.isNullOrEmpty() }

android {
    namespace = "com.worldrunner.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.worldrunner.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = commitCount.get()
        versionName = "0.1.${commitCount.get()}"
    }

    signingConfigs {
        if (hasTestSigning) {
            create("test") {
                storeFile = file(testKeystore!!)
                storePassword = testKeystorePassword
                keyAlias = testKeyAlias
                keyPassword = testKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasTestSigning) {
                signingConfig = signingConfigs.getByName("test")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.home)
    implementation(projects.feature.teams)
    implementation(projects.feature.standings)
    implementation(projects.feature.profile)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
