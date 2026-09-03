package moe.evil.hyperstatus.xposed.debug

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import moe.evil.hyperstatus.shared.DEBUG_ACTION
import moe.evil.hyperstatus.shared.DEBUG_EXTRA_CMD
import moe.evil.hyperstatus.shared.log.HLog
import moe.evil.hyperstatus.xposed.hooks.battery.BatteryCommand
import moe.evil.hyperstatus.xposed.hooks.battery.BatteryPercentHooker
import moe.evil.hyperstatus.xposed.hooks.iconswap.IconCommand
import moe.evil.hyperstatus.xposed.hooks.iconswap.IconSwapHooker
import moe.evil.hyperstatus.xposed.hooks.mobilelayout.LayoutCommand
import moe.evil.hyperstatus.xposed.hooks.mobilelayout.MobileLayoutHooker
import moe.evil.hyperstatus.xposed.hooks.mobilesignal.MobileSignalDebugHooker
import moe.evil.hyperstatus.xposed.hooks.mobilesignal.ResetCommand
import moe.evil.hyperstatus.xposed.hooks.mobilesignal.SignalCommand
import moe.evil.hyperstatus.xposed.hooks.netspeed.NetworkSpeedHooker
import moe.evil.hyperstatus.xposed.hooks.netspeed.SpeedCommand
import moe.evil.hyperstatus.xposed.hooks.statusicon.StatusCommand
import moe.evil.hyperstatus.xposed.hooks.statusicon.StatusIconDebugHooker
import moe.evil.hyperstatus.xposed.hooks.statusicon.StatusIconHooker
import moe.evil.hyperstatus.xposed.hooks.wifi.WifiCommand
import moe.evil.hyperstatus.xposed.hooks.wifi.WifiIconHooker
import moe.evil.hyperstatus.xposed.utils.DebugSurface
import moe.evil.hyperstatus.xposed.utils.safe
import java.util.concurrent.CopyOnWriteArrayList

class DebugChannel : DebugSurface {
    private val log = HLog.of<DebugChannel>()
    private val whitespace = Regex("\\s+")
    private val probes = CopyOnWriteArrayList<Pair<String, () -> String>>()

    override fun probe(name: String, provider: () -> String) {
        probes += name to provider
    }

    override fun attach(context: Context) {
        context.registerReceiver(
            receiver,
            IntentFilter(DEBUG_ACTION),
            Manifest.permission.DUMP,
            null,
            Context.RECEIVER_NOT_EXPORTED,
        )
        log.info { "Registered $DEBUG_ACTION guarded by ${Manifest.permission.DUMP}" }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = safe("DebugChannel") {
            val raw = intent.getStringExtra(DEBUG_EXTRA_CMD)?.trim().orEmpty()
            if (raw.isEmpty()) {
                log.warn { "Empty command (extra '$DEBUG_EXTRA_CMD')" }
                return@safe
            }
            val root = object : CoreCliktCommand(name = "hyperstatus") {
                init {
                    context {
                        echoMessage = { _, message, _, _ -> log.debug { message.toString() } }
                    }
                }

                override fun run() = Unit
            }.subcommands(
                SignalCommand(MobileSignalDebugHooker.state),
                ResetCommand(MobileSignalDebugHooker.state),
                IconCommand(IconSwapHooker.state),
                LayoutCommand(MobileLayoutHooker.state),
                WifiCommand(WifiIconHooker.state),
                StatusCommand(StatusIconHooker.state, StatusIconDebugHooker.state),
                SpeedCommand(NetworkSpeedHooker.state),
                BatteryCommand(BatteryPercentHooker.state),
                DumpCommand(),
            )
            runCatching { root.parse(raw.split(whitespace)) }
                .onSuccess { log.debug { "Ran '$raw'" } }
                .onFailure {
                    if (it !is CliktError) throw it
                    val help = root.getFormattedHelp(it)
                    if (it.printError) log.warn { "Rejected '$raw': ${help ?: it}" }
                    else log.debug { help.orEmpty() }
                }
        }
    }

    private inner class DumpCommand : CoreCliktCommand(name = "dump") {
        override fun run() {
            if (probes.isEmpty()) echo("No diagnostics registered")
            probes.forEach { (name, provider) ->
                echo("[$name] " + runCatching(provider).getOrElse { "Probe failed: $it" })
            }
        }
    }
}
