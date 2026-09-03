package moe.evil.hyperstatus.shared.log

class HLog @PublishedApi internal constructor(private val defaultTag: String) {

    @PublishedApi
    internal fun enabled(level: LogLevel) = level >= globalMinLevel

    @PublishedApi
    internal fun emit(level: LogLevel, throwable: Throwable?, message: String) =
        sink.emit(level, "$defaultTag $message", throwable)

    inline fun debug(lazyMessage: () -> String) {
        if (enabled(LogLevel.DEBUG)) emit(LogLevel.DEBUG, null, lazyMessage())
    }

    inline fun info(lazyMessage: () -> String) {
        if (enabled(LogLevel.INFO)) emit(LogLevel.INFO, null, lazyMessage())
    }

    inline fun warn(throwable: Throwable? = null, lazyMessage: () -> String) {
        if (enabled(LogLevel.WARN)) emit(LogLevel.WARN, throwable, lazyMessage())
    }

    inline fun error(throwable: Throwable? = null, lazyMessage: () -> String) {
        if (enabled(LogLevel.ERROR)) emit(LogLevel.ERROR, throwable, lazyMessage())
    }

    companion object {
        @Volatile
        var globalMinLevel = LogLevel.DEBUG

        @Volatile
        var sink: LogSink = AndroidLogSink

        inline fun <reified T : Any> of() = HLog("[${T::class.java.simpleName}]")

        operator fun invoke(tag: String) = HLog("[$tag]")
    }
}
