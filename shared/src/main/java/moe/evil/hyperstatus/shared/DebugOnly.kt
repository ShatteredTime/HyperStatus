package moe.evil.hyperstatus.shared

inline fun <T> debugOnly(value: () -> T?): T? = if (BuildConfig.DEBUG) value() else null
