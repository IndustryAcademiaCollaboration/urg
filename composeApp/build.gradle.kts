import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    //DB
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            implementation("com.google.mediapipe:tasks-genai:0.10.35")
            implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
            implementation("ai.djl.huggingface:tokenizers:0.33.0")
            implementation("ai.djl.android:tokenizer-native:0.33.0")
            implementation(files("libs/sherpa-onnx-static-link-onnxruntime-1.13.2.aar"))
            //DB
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            //DB
            implementation(libs.sqldelight.coroutines)
        }
        //DB
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.urg.edge"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.urg.edge"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/libonnxruntime.so"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
//DB
sqldelight {
    linkSqlite.set(true)
    databases {
        create("UrgDatabase") {
            packageName.set("com.urg.edge.database")
        }
    }
}

