import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        val commonTest by getting {
            dependencies {
                implementation(libs.junit.jupiter.api)
                implementation("io.mockk:mockk:1.14.2")
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                // TODO https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html#how-compose-multiplatform-testing-is-different-from-jetpack-compose
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.okhttp)
            implementation(libs.retrofit)
            implementation(libs.retrofit.jackson)
            implementation(libs.kotlin.logging.jvm)
            implementation(libs.slf4j.simple)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)
            implementation(libs.squareup.okhttp.tls)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.retrofit.kotlinx.converter)
            implementation(libs.compose.navigation)
            implementation(libs.kotlin.result)
            implementation(libs.kotlin.result.coroutines)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)
            implementation("org.jetbrains.compose.material3:material3-desktop:1.8.1")
            implementation(libs.jetbrains.compose.material.icons)
        }
    }

}

android {
    namespace = "dev.ploiu.file_server_ui_new"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ploiu.file_server_ui_new"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "dev.ploiu.file_server_ui_new.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.ploiu.file_server_ui_new"
            packageVersion = "1.0.0"
        }
    }
}
