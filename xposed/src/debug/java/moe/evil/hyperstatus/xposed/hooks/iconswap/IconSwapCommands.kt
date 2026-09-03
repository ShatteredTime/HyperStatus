package moe.evil.hyperstatus.xposed.hooks.iconswap

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.float
import moe.evil.hyperstatus.xposed.model.IconGroup

class IconCommand(private val state: IconSwapState) : CoreCliktCommand(name = "icon") {
    private val groups by option("--group", "-g").choice(
        IconGroup.entries.associateBy { it.cli },
        ignoreCase = true
    )
        .multiple()
    private val swap by option().switch("--on" to true, "--off" to false)
    private val reset by option("--reset").flag()
    private val scale by option("--scale").float()
    private val dx by option("--dx").float()
    private val dy by option("--dy").float()
    private val anchorX by option("--anchor-x", "--ax").float()
    private val anchorY by option("--anchor-y", "--ay").float()
    private val widen by option().switch("--widen" to true, "--no-widen" to false)
    private val padEnd by option("--pad-end").float()
    private val textSize by option("--text-size").float()
    private val trace by option("--trace")
    private val noTrace by option("--no-trace").flag()

    override fun run() {
        if (reset) {
            state.reset()
            return
        }
        if (noTrace) state.trace = null
        trace?.let { state.trace = it }
        textSize?.let(state::resize)
        swap?.let(state::enable)
        if (listOf(scale, dx, dy, anchorX, anchorY, widen, padEnd).all { it == null }) return
        state.update(groups.ifEmpty { IconGroup.entries }) {
            it.copy(
                scale = scale ?: it.scale,
                dx = dx ?: it.dx,
                dy = dy ?: it.dy,
                anchorX = anchorX ?: it.anchorX,
                anchorY = anchorY ?: it.anchorY,
                widen = widen ?: it.widen,
                padEnd = padEnd ?: it.padEnd,
            )
        }
    }
}
