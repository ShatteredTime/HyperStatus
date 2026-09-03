package moe.evil.hyperstatus.xposed.hooks.netspeed

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import moe.evil.hyperstatus.xposed.graphics.MISANS_MAX_WEIGHT
import moe.evil.hyperstatus.xposed.graphics.MISANS_MIN_WEIGHT

class SpeedCommand(private val state: NetworkSpeedState) : CoreCliktCommand(name = "speed") {
    private val swap by option().switch("--on" to true, "--off" to false)
    private val reset by option("--reset").flag()
    private val clear by option("--clear").flag()
    private val weight by option("--weight").int().restrictTo(MISANS_MIN_WEIGHT, MISANS_MAX_WEIGHT)
    private val numberSize by option("--number-size").float()
    private val unitSize by option("--unit-size").float()

    override fun run() {
        if (reset) {
            state.reset()
            return
        }
        swap?.let(state::enable)
        weight?.let(state::weigh)
        if (clear) state.resize(null, null)
        if (numberSize == null && unitSize == null) return
        state.resize(numberSize ?: state.numberSize, unitSize ?: state.unitSize)
    }
}
