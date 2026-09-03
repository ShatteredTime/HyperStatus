package moe.evil.hyperstatus.xposed.debug

import android.content.SharedPreferences
import moe.evil.hyperstatus.xposed.utils.DebugSurface
import moe.evil.hyperstatus.xposed.utils.Hooker

@Suppress("UNUSED_PARAMETER")
object DebugBootstrap {
    fun hookers(prefs: SharedPreferences?): List<Hooker> = emptyList()
    fun surface(prefs: SharedPreferences?): DebugSurface? = null
}
