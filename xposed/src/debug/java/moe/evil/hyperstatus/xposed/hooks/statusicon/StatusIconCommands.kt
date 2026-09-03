package moe.evil.hyperstatus.xposed.hooks.statusicon

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import moe.evil.hyperstatus.xposed.model.StatusIconGroup

class StatusCommand(
    private val state: StatusIconState,
    private val debug: StatusIconDebugState,
) : CoreCliktCommand(name = "status") {
    private val groups by option(
        "--group",
        "-g"
    ).choice(StatusIconGroup.entries.associateBy { it.cli }, ignoreCase = true)
        .multiple()
    private val swap by option().switch("--on" to true, "--off" to false)
    private val reset by option("--reset").flag()
    private val clear by option("--clear").flag()
    private val battery by option("--battery").int().restrictTo(0, HOST_BT_BATTERY_LEVELS - 1)
    private val headset by option().switch("--mic" to true, "--no-mic" to false)
    private val volte by option("--volte").choice(
        VolteSlot.entries.associateBy { it.cli },
        ignoreCase = true
    )
    private val noVolte by option("--no-volte").flag()

    override fun run() {
        if (reset) {
            state.reset()
            debug.force(null)
            return
        }
        if (noVolte) debug.force(null)
        volte?.let(debug::force)
        swap?.let { state.enable(groups.ifEmpty { StatusIconGroup.entries }, it) }
        if (clear) state.override(null, null)
        if (battery == null && headset == null) return
        state.override(battery ?: state.battery, headset ?: state.headsetMic)
    }
}
