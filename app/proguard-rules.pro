-keepattributes SourceFile,LineNumberTable

-keep class moe.evil.hyperstatus.xposed.HookEntry { *; }
-keepnames class moe.evil.hyperstatus.xposed.** { *; }
-keepnames class moe.evil.hyperstatus.shared.** { *; }

-keep class io.github.libxposed.service.** { *; }

-dontwarn io.github.libxposed.api.**
-dontwarn com.android.internal.**
-dontwarn com.android.systemui.**
-dontwarn com.oplus.systemui.**
