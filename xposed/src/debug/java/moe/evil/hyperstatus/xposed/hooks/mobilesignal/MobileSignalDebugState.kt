package moe.evil.hyperstatus.xposed.hooks.mobilesignal

import moe.evil.hyperstatus.xposed.model.MobileSignalOverride
import java.util.concurrent.ConcurrentHashMap

class MobileSignalDebugState {
    private val overrides = ConcurrentHashMap<Int, MobileSignalOverride>()

    @Volatile
    var onChange: (() -> Unit)? = null

    val isEmpty
        get() = overrides.isEmpty()

    fun of(slot: Int): MobileSignalOverride? = overrides[slot]

    fun snapshot(): Map<Int, MobileSignalOverride> = overrides.toMap()

    fun update(slot: Int, block: (MobileSignalOverride) -> MobileSignalOverride) {
        val next = block(overrides[slot] ?: MobileSignalOverride())
        if (next.isEmpty) overrides.remove(slot) else overrides[slot] = next
        onChange?.invoke()
    }

    fun clear(slot: Int) {
        overrides.remove(slot)
        onChange?.invoke()
    }

    fun clearAll() {
        overrides.clear()
        onChange?.invoke()
    }
}
