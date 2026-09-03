package moe.evil.hyperstatus.xposed.hooks.statusicon

enum class VolteSlot(val cli: String, val host: String, val description: String) {
    PLAIN("plain", "stat_signal_volte", "accessibility_sim_volte"),
    SIM1("sim1", "stat_signal_volte_sim1", "accessibility_sim1_volte"),
    SIM2("sim2", "stat_signal_volte_sim2", "accessibility_sim2_volte"),
    BOTH("both", "stat_signal_volte_sim_both", "accessibility_sim12_volte"),
}

class StatusIconDebugState {
    @Volatile
    var volte: VolteSlot? = null
        private set

    @Volatile
    var onChange: (() -> Unit)? = null

    fun force(slot: VolteSlot?) {
        volte = slot
        onChange?.invoke()
    }
}
