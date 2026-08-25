# GymCoach ProGuard Rules

# ── Room ─────────────────────────────────────────────────────
-keep class com.gymcoach.app.data.local.entity.** { *; }
-keep class com.gymcoach.app.data.local.dao.** { *; }
-keep class com.gymcoach.app.data.local.database.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * implements androidx.room.RoomDatabase.Callback { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# ── Domain & Repository ──────────────────────────────────────
-keep class com.gymcoach.app.domain.model.** { *; }
-keep class com.gymcoach.app.domain.repository.** { *; }
-keep class com.gymcoach.app.data.repository.** { *; }

# ── Presentation (ViewModels, Screens) ───────────────────────
-keep class com.gymcoach.app.presentation.** { *; }

# ── ML / MediaPipe ───────────────────────────────────────────
-keep class com.gymcoach.app.core.ml.** { *; }
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-keep class com.google.mediapipe.solutions.** { *; }
-keep class com.google.mediapipe.tasks.** { *; }
-dontwarn com.google.mediapipe.**

# ── Timer / Notification ─────────────────────────────────────
-keep class com.gymcoach.app.core.timer.** { *; }
-keep class com.gymcoach.app.core.notification.** { *; }

# ── Hilt ─────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewWithFragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# ── Kotlin Coroutines ────────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }

# ── CameraX ──────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Coil Image Loading ───────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Remove logging in release builds ─────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ── Prevent renaming of source files for cleaner stack traces ─
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
