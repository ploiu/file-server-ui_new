import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class) compilerOptions {
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
                implementation(libs.mockk)
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                // TODO https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html#how-compose-multiplatform-testing-is-different-from-jetpack-compose
                implementation(libs.ui.test)
            }
        }

        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.android.datastore)
            implementation(libs.android.tink)
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
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
            implementation(libs.jetbrains.compose.material.icons)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeVM)
            implementation(libs.material3.desktop)
            implementation(libs.jna.platform)
        }
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
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
    buildFeatures {
        compose = true
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
            packageName = "Ploiu File Server"
            packageVersion = "1.0.1"
            linux {
                modules("jdk.security.auth")
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}

tasks.named("desktopProcessResources") {
    // there is a stupid bug with _something_ in intellij, where running in debug mode makes the buildCredsFfi task hang permanently. ONLY in debug mode
    if (System.getenv("INTELLIJ_RUN") != "true") {
        dependsOn("buildCredsFfi")
    }
}

tasks.register<Exec>("buildCredsFfi") {
    workingDir = projectDir.parentFile
    if (Os.isFamily(Os.FAMILY_UNIX)) {
        executable = "./scripts/buildCredsFfi.sh"
    } else {
        TODO("windows support")
    }
}
