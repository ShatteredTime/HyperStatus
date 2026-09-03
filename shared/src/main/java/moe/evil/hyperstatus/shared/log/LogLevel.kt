package moe.evil.hyperstatus.shared.log

import android.util.Log

enum class LogLevel(val priority: Int) {
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
    OFF(Log.ASSERT),
}
