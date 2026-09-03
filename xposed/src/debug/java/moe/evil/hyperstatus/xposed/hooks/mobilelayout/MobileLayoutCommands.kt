package moe.evil.hyperstatus.xposed.hooks.mobilelayout

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.float

class LayoutCommand(private val state: MobileLayoutState) : CoreCliktCommand(name = "layout") {
    private val swap by option().switch("--on" to true, "--off" to false)
    private val reset by option("--reset").flag()
    private val inoutDx by option("--inout-dx").float()

    override fun run() {
        if (reset) {
            state.reset()
            return
        }
        swap?.let(state::enable)
        inoutDx?.let(state::shift)
    }
}
