package moe.evil.hyperstatus.shared.log

const val LOG_TAG = "hyperstatus"

fun interface LogSink {
    fun emit(level: LogLevel, message: String, throwable: Throwable?)
}
