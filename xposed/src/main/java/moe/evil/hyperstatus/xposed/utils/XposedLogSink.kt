package moe.evil.hyperstatus.xposed.utils

import android.util.Log
import io.github.libxposed.api.XposedInterface
import moe.evil.hyperstatus.shared.log.AndroidLogSink
import moe.evil.hyperstatus.shared.log.HLog
import moe.evil.hyperstatus.shared.log.LOG_TAG
import moe.evil.hyperstatus.shared.log.LogLevel
import moe.evil.hyperstatus.shared.log.LogSink

class XposedLogSink private constructor(private val xposed: XposedInterface) : LogSink {
    override fun emit(level: LogLevel, message: String, throwable: Throwable?) =
        try {
            xposed.log(level.priority, LOG_TAG, message, throwable)
        } catch (error: Throwable) {
            HLog.sink = AndroidLogSink
            AndroidLogSink.emit(level, message, throwable)
            AndroidLogSink.emit(LogLevel.WARN, "$PREFIX Channel lost, back on logcat", error)
        }

    companion object {
        private const val PREFIX = "[XposedLogSink]"

        fun install(xposed: XposedInterface) = runCatching {
            Log.i(LOG_TAG, "$PREFIX Attaching to Xposed log channel!!!")
            xposed.log(LogLevel.INFO.priority, LOG_TAG, "$PREFIX Attached", null)
        }.onSuccess { HLog.sink = XposedLogSink(xposed) }
            .onFailure {
                AndroidLogSink.emit(
                    LogLevel.WARN,
                    "$PREFIX Unavailable, staying on logcat",
                    it,
                )
            }
    }
}
