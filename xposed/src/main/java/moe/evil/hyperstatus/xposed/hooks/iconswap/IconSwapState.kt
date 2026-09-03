package moe.evil.hyperstatus.xposed.hooks.iconswap

import moe.evil.hyperstatus.xposed.model.IconGroup
import moe.evil.hyperstatus.xposed.model.IconTune
import java.util.concurrent.ConcurrentHashMap

private const val HYPEROS_TYPE_TEXT_DP = 7.16f

class IconSwapState {
    private val tunes = ConcurrentHashMap<IconGroup, IconTune>()

    @Volatile
    var enabled = true
        private set

    @Volatile
    var textSize = HYPEROS_TYPE_TEXT_DP
        private set

    @Volatile
    var trace: String? = null

    @Volatile
    var onChange: (() -> Unit)? = null

    fun tune(group: IconGroup): IconTune = tunes[group] ?: group.tune

    fun snapshot(): Map<IconGroup, IconTune> = IconGroup.entries.associateWith(::tune)

    fun enable(value: Boolean) {
        enabled = value
        onChange?.invoke()
    }

    fun resize(dp: Float) {
        textSize = dp
        onChange?.invoke()
    }

    fun update(groups: Collection<IconGroup>, block: (IconTune) -> IconTune) {
        groups.forEach { tunes[it] = block(tune(it)) }
        onChange?.invoke()
    }

    fun reset() {
        tunes.clear()
        enabled = true
        trace = null
        textSize = HYPEROS_TYPE_TEXT_DP
        onChange?.invoke()
    }
}
