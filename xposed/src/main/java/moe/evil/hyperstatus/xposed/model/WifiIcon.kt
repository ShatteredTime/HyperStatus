package moe.evil.hyperstatus.xposed.model

enum class WifiFamily(val asset: String) {
    NORMAL("stat_sys_wifi_signal_%d_tint"),
    UNAVAILABLE("stat_sys_wifi_signal_unavailable_%d_tint"),
    SLAVE("stat_sys_slave_wifi_signal_%d_tint"),
}

enum class WifiBadge(val host: String, val text: String? = null, val standard: Int? = null) {
    SIX("stat_signal_wifi_6", "6", 6),

    // TODO: HyperOS has no dedicated Wi-Fi 6E badge yet; ColorOS emits stat_signal_wifi_6e when
    //  CustomizeFeatureOption.sIsSupportShowWifi6E is on, so it is rendered as the plain "6" badge for now.
    SIX_E("stat_signal_wifi_6e", "6", 6),
    SEVEN("stat_signal_wifi_7", "7", 7),
    DOUBLE("stat_signal_wifi_double"),
    PASSPOINT("stat_signal_wifi_passpoint"),
    PASSPOINT_HOME("stat_signal_wifi_h_passpoint"),
    PASSPOINT_ROAMING("stat_signal_wifi_r_passpoint"),
}

enum class WifiActivity(val cli: String, val host: String, val asset: String? = null) {
    NONE("none", "stat_signal_activity_wifi_none"),
    IN("in", "stat_signal_activity_wifi_in", "stat_sys_wifi_in_tint"),
    OUT("out", "stat_signal_activity_wifi_out", "stat_sys_wifi_out_tint"),
    INOUT("inout", "stat_signal_activity_wifi_inout", "stat_sys_wifi_inout_tint"),
}

data class WifiOverride(
    val level: Int? = null,
    val validated: Boolean? = null,
    val activity: WifiActivity? = null,
    val standard: Int? = null,
    val double: Boolean? = null,
)

val NO_WIFI_OVERRIDE = WifiOverride()
