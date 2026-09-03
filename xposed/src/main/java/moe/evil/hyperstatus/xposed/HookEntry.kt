package moe.evil.hyperstatus.xposed

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.SharedPreferences
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.toClass
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import moe.evil.hyperstatus.shared.HOST_SYSTEMUI
import moe.evil.hyperstatus.shared.MODULE_PACKAGE
import moe.evil.hyperstatus.shared.PREFS_GROUP
import moe.evil.hyperstatus.shared.PREF_DEDUPE_ERRORS
import moe.evil.hyperstatus.shared.PREF_LOG_LEVEL
import moe.evil.hyperstatus.shared.dedupeErrors
import moe.evil.hyperstatus.shared.log.HLog
import moe.evil.hyperstatus.shared.logLevel
import moe.evil.hyperstatus.xposed.debug.DebugBootstrap
import moe.evil.hyperstatus.xposed.hooks.battery.BatteryPercentHooker
import moe.evil.hyperstatus.xposed.hooks.iconswap.IconSwapHooker
import moe.evil.hyperstatus.xposed.hooks.mobilelayout.MobileLayoutHooker
import moe.evil.hyperstatus.xposed.hooks.netspeed.NetworkSpeedHooker
import moe.evil.hyperstatus.xposed.hooks.statusicon.StatusIconHooker
import moe.evil.hyperstatus.xposed.hooks.wifi.WifiIconHooker
import moe.evil.hyperstatus.xposed.utils.Guard
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.HostClassLoaderBridge
import moe.evil.hyperstatus.xposed.utils.XposedLogSink
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import org.lsposed.hiddenapibypass.HiddenApiBypass

class HookEntry : XposedModule() {
    private val log = HLog.of<HookEntry>()

    private val onPrefsChanged = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            PREF_LOG_LEVEL -> {
                HLog.globalMinLevel = prefs.logLevel
                log.info { "Log level ${HLog.globalMinLevel}" }
            }

            PREF_DEDUPE_ERRORS -> {
                Guard.dedupe = prefs.dedupeErrors
                log.info { "Dedupe errors ${Guard.dedupe}" }
            }
        }
    }

    @Volatile
    private var hooked = false

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        HiddenApiBypass.setHiddenApiExemptions("L")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName == HOST_SYSTEMUI && param.isFirstPackage) {
            installInto(param.defaultClassLoader)
        }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName == HOST_SYSTEMUI) installInto(param.classLoader)
    }

    private fun installInto(classLoader: ClassLoader) {
        if (hooked) return
        hooked = true

        val prefs = runCatching { getRemotePreferences(PREFS_GROUP) }
            .onFailure { log.warn(it) { "Remote preferences unavailable" } }
            .getOrNull()
        HLog.globalMinLevel = prefs.logLevel
        Guard.dedupe = prefs.dedupeErrors
        XposedLogSink.install(this)
        prefs?.registerOnSharedPreferenceChangeListener(onPrefsChanged)
        HostClassLoaderBridge.install(classLoader)

        val ctx = HookContext(this, classLoader, DebugBootstrap.surface(prefs))
        val hookers = DebugBootstrap.hookers(prefs) + listOf(
            IconSwapHooker,
            MobileLayoutHooker,
            WifiIconHooker,
            StatusIconHooker,
            NetworkSpeedHooker,
            BatteryPercentHooker,
        )
        hookers.forEach { it.install(ctx) }
        ctx.safeOnHostContext { host ->
            log.info {
                "HyperStatus ${
                    host.packageManager.getPackageInfo(
                        MODULE_PACKAGE,
                        0
                    ).versionName
                } init on safeOnHostContext!!!"
            }
        }

        runCatching {
            val activityThread = "android.app.ActivityThread".toClass()
            val current = activityThread.getMethod("currentActivityThread").invoke(null)
            activityThread.getMethod("getApplication").invoke(current) as? Context
        }.getOrNull()?.let { ctx.bindHostContext(it) }

        if (ctx.hostContext == null) {
            classOf<Instrumentation>()
                .hostMethod("Instrumentation#callApplicationOnCreate") {
                    name = "callApplicationOnCreate"
                    parameters(classOf<Application>())
                }
                ?.safeIntercept(ctx, "HookEntry.captureHostContext") { chain ->
                    ctx.bindHostContext(chain.getArg(0) as Application)
                    chain.proceed()
                }
        }

        log.info {
            "Hooked $HOST_SYSTEMUI (${hookers.size} hooker(s)) via $frameworkName api=$apiVersion, " +
                    "build=${if (BuildConfig.DEBUG) "debug" else "release"} log=${HLog.globalMinLevel} " +
                    "debugChannel=${ctx.debug != null}"
        }
    }
}
