package moe.evil.hyperstatus.xposed.debug

import android.content.SharedPreferences
import moe.evil.hyperstatus.shared.debugChannel
import moe.evil.hyperstatus.xposed.hooks.mobilesignal.MobileSignalDebugHooker
import moe.evil.hyperstatus.xposed.hooks.statusicon.StatusIconDebugHooker
import moe.evil.hyperstatus.xposed.utils.DebugSurface
import moe.evil.hyperstatus.xposed.utils.Hooker

object DebugBootstrap {
    fun hookers(prefs: SharedPreferences?): List<Hooker> =
        if (prefs.debugChannel) listOf(MobileSignalDebugHooker, StatusIconDebugHooker)
        else emptyList()

    fun surface(prefs: SharedPreferences?): DebugSurface? =
        if (prefs.debugChannel) DebugChannel() else null
}
