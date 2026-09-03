package moe.evil.hyperstatus.xposed.hooks.statusicon

import moe.evil.hyperstatus.xposed.model.StatusIconGroup

class StatusIconState {
    @Volatile
    var enabled: Set<StatusIconGroup> = StatusIconGroup.entries.toSet()
        private set

    @Volatile
    var battery: Int? = null
        private set

    @Volatile
    var headsetMic: Boolean? = null
        private set

    @Volatile
    var onChange: (() -> Unit)? = null

    fun enable(groups: Collection<StatusIconGroup>, value: Boolean) {
        enabled = if (value) enabled + groups else enabled - groups.toSet()
        onChange?.invoke()
    }

    fun override(battery: Int?, headsetMic: Boolean?) {
        this.battery = battery
        this.headsetMic = headsetMic
        onChange?.invoke()
    }

    fun reset() {
        enabled = StatusIconGroup.entries.toSet()
        battery = null
        headsetMic = null
        onChange?.invoke()
    }
}
