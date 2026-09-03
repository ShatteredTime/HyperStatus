package moe.evil.hyperstatus.xposed.model

enum class DataActivity(val cli: String, val icon: String) {
    NONE("none", "stat_signal_activity_default_public"),
    IN("in", "stat_signal_activity_in_public"),
    OUT("out", "stat_signal_activity_out_public"),
    INOUT("inout", "stat_signal_activity_inout_public"),
}

data class MobileSignalOverride(
    val level: Int? = null,
    val rat: Rat? = null,
    val activity: DataActivity? = null,
    val roaming: Boolean? = null,
    val inService: Boolean? = null,
    val softSim: Boolean? = null,
    val ims: Boolean? = null,
    val inetCondition: Int? = null,
) {
    val isEmpty
        get() = listOf(
            level,
            rat,
            activity,
            roaming,
            inService,
            softSim,
            ims,
            inetCondition
        ).all { it == null }

    fun overlay(patch: MobileSignalOverride) = MobileSignalOverride(
        level = patch.level ?: level,
        rat = patch.rat ?: rat,
        activity = patch.activity ?: activity,
        roaming = patch.roaming ?: roaming,
        inService = patch.inService ?: inService,
        softSim = patch.softSim ?: softSim,
        ims = patch.ims ?: ims,
        inetCondition = patch.inetCondition ?: inetCondition,
    )
}
