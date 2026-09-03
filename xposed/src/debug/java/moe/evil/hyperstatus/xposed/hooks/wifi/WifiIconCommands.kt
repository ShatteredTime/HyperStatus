package moe.evil.hyperstatus.xposed.hooks.wifi

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import moe.evil.hyperstatus.xposed.model.WifiActivity
import moe.evil.hyperstatus.xposed.model.WifiBadge
import moe.evil.hyperstatus.xposed.model.WifiOverride

class WifiCommand(private val state: WifiIconState) : CoreCliktCommand(name = "wifi") {
    private val swap by option().switch("--on" to true, "--off" to false)
    private val reset by option("--reset").flag()
    private val clear by option("--clear").flag()
    private val textSize by option("--text-size").float()
    private val level by option("--level").int().restrictTo(0, 4)
    private val validated by option().switch("--validated" to true, "--no-validated" to false)
    private val activity by option("--activity").choice(
        WifiActivity.entries.associateBy { it.cli },
        ignoreCase = true
    )
    private val standard by option("--standard").choice(
        mapOf("0" to 0) +
                WifiBadge.entries.mapNotNull { badge -> badge.standard?.let { it.toString() to it } })
    private val double by option().switch("--double" to true, "--no-double" to false)

    override fun run() {
        if (reset) {
            state.reset()
            return
        }
        if (clear) state.update { WifiOverride() }
        textSize?.let(state::resize)
        swap?.let(state::enable)
        if (listOf(level, validated, activity, standard, double).all { it == null }) return
        state.update {
            it.copy(
                level = level ?: it.level,
                validated = validated ?: it.validated,
                activity = activity ?: it.activity,
                standard = standard ?: it.standard,
                double = double ?: it.double,
            )
        }
    }
}
