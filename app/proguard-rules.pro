# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep the Room database entities and DAOs
-keep class com.gymcoach.app.data.local.entity.** { *; }
-keep class com.gymcoach.app.data.local.dao.** { *; }
-keep class com.gymcoach.app.data.local.database.** { *; }

# Keep the domain models
-keep class com.gymcoach.app.domain.model.** { *; }

# Keep the repository interfaces and implementations
-keep class com.gymcoach.app.domain.repository.** { *; }
-keep class com.gymcoach.app.data.repository.** { *; }

# Keep the ViewModels
-keep class com.gymcoach.app.presentation.** { *; }

# Keep the FormAnalyzer
-keep class com.gymcoach.app.core.ml.** { *; }

# Keep the FormAnalyzer
-keep class com.gymcoach.app.core.timer.** { *; }

# Keep Room auto-generated classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomDatabase.Callback { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class com.gymcoach.app.** { *; }

# Keep Coil
-keep class coil.** { *; }
-keep class com.github.bumptech.glide.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep MediaPipe
-keep class com.google.mediapipe.** { *; }

# Keep Coil
-keep class coil.** { *; }