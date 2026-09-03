package moe.evil.hyperstatus.shared.log

import android.util.Log

object AndroidLogSink : LogSink {
    override fun emit(level: LogLevel, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(LOG_TAG, message, throwable)
            LogLevel.INFO -> Log.i(LOG_TAG, message, throwable)
            LogLevel.WARN -> Log.w(LOG_TAG, message, throwable)
            LogLevel.ERROR -> Log.e(LOG_TAG, message, throwable)
            LogLevel.OFF -> Unit
        }
    }
}
