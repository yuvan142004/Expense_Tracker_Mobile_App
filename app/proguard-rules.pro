# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools/proguard/proguard-android.txt

# Keep Room entity and DAO classes
-keep class com.example.expensetracker.data.entity.** { *; }
-keep class com.example.expensetracker.data.dao.** { *; }

# Keep WorkManager workers
-keep class com.example.expensetracker.worker.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Suppress warnings for unused classes in release
-dontwarn com.google.errorprone.annotations.**
