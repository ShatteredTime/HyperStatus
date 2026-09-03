package moe.evil.hyperstatus.shared

import android.content.SharedPreferences
import moe.evil.hyperstatus.shared.log.LogLevel

const val PREFS_GROUP = "settings"

const val PREF_LOG_LEVEL = "log_level"

const val PREF_DEBUG_CHANNEL = "debug_channel"

const val PREF_DEDUPE_ERRORS = "dedupe_errors"

val SharedPreferences?.logLevel: LogLevel
    get() {
        val name = this?.getString(PREF_LOG_LEVEL, null)
        return LogLevel.entries.firstOrNull { it.name == name }
            ?: if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
    }

val SharedPreferences?.debugChannel: Boolean
    get() = this?.getBoolean(PREF_DEBUG_CHANNEL, true) ?: true

val SharedPreferences?.dedupeErrors: Boolean
    get() = this?.getBoolean(PREF_DEDUPE_ERRORS, true) ?: true
