import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            
            // Media3 (ExoPlayer) for Android audio
            implementation(libs.bundles.media3)
            
            // NewPipeExtractor for YouTube without API
            implementation(libs.newpipe.extractor)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Networking
            implementation(libs.bundles.ktor)
            
            implementation(libs.kotlinx.coroutines.core)
            
            // Navigation - Voyager
            implementation(libs.bundles.voyager)
            
            // Image Loading - Coil 3
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            
            // DI - Koin (core only for common)
            implementation(libs.koin.core)
            
            // Settings - Multiplatform Settings
            implementation(libs.bundles.settings)

            // Kotlinx Collections Immutable
            implementation(libs.kotlinx.collections.immutable)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            
            // VLCJ for Desktop audio
            implementation(libs.vlcj)
            
            // NewPipeExtractor for YouTube without API
            implementation(libs.newpipe.extractor)
        }
    }
}

android {
    namespace = "org.antidepressants.mp5"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.antidepressants.mp5"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Resolve JNI conflicts (likely between vlcj transitive deps or others)
            pickFirsts += "lib/x86/libc++_shared.so"
            pickFirsts += "lib/x86_64/libc++_shared.so"
            pickFirsts += "lib/armeabi-v7a/libc++_shared.so"
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "android"
            keyAlias = "mp5release"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        // debugImplementation(libs.compose.ui.tooling) // Causing version conflicts with KMP Compose
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "mp5"
            packageVersion = "1.0.0"
            
            buildTypes.release.proguard {
                isEnabled.set(false)
            }
            
            windows {
                menuGroup = "Antidepressants"
                shortcut = true
                // console = true // Disabled due to build failure
            }
        }
    }
}
