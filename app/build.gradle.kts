import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Release signing: reads keystore path + passwords from local.properties
// (git-ignored) or environment variables (CI).
//
// NOTE: use an explicit import + bare `Properties()` here. A fully-qualified
// inline `java.util.Properties()` does NOT compile in Gradle Kotlin DSL:
// once the Java plugin is applied, the bare identifier `java` binds to the
// JavaPluginExtension project accessor on the implicit receiver, shadowing
// the java.* package namespace ("Unresolved reference: util").
val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.gymcoach.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gymcoach.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // F-BUILD-1 fix: removed empty CMake stub. The app has no JNI/native
        // code. The cpp/ directory with gymcoach.cpp and CMakeLists.txt was
        // scaffolded and never implemented. Keeping it added build complexity,
        // NDK dependency, and packaged a do-nothing libgymcoach.so into the APK.
        // ndk.abiFilters removed along with the CMake block.
    }

    signingConfigs {
        create("release") {
            // Priority: environment variables (CI) > local.properties (dev)
            // NOTE (F-BUILD-2): Release builds require either KEYSTORE_PATH,
            // KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD env vars (CI) or
            // the equivalent entries in local.properties. bundleRelease will
            // fail with FileNotFoundException if neither source is set and
            // keystore/release.jks does not exist locally.
            storeFile = file(
                System.getenv("KEYSTORE_PATH")
                    ?: keystoreProperties.getProperty("KEYSTORE_PATH", "keystore/release.jks")
            )
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProperties.getProperty("KEY_ALIAS", "gymcoach")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val relKeystore = signingConfigs.getByName("release").storeFile
            signingConfig = if (relKeystore != null && relKeystore.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // NOTE: no `room { schemaDirectory = ... }` block here. Applying it
    // requires the androidx.room Gradle plugin, which is not present in
    // gradle/libs.versions.toml [plugins]. Schema export is handled by the
    // ksp { arg("room.schemaLocation", ...) } block above.
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // MediaPipe
    implementation(libs.mediapipe.tasks.vision)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Coil
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.coroutines.android)
    testImplementation(libs.coroutines.test)
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
