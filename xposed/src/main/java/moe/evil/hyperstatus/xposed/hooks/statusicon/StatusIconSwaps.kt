package moe.evil.hyperstatus.xposed.hooks.statusicon

import moe.evil.hyperstatus.xposed.model.StatusIconGroup
import moe.evil.hyperstatus.xposed.model.StatusIconSwap

const val HOST_BT_BATTERY_LEVELS = 10
const val HEADSET_HOST = "stat_sys_headset"
const val HEADPHONES_DESCRIPTION = "accessibility_status_bar_headphones"
const val BT_TRANSFER_HOST = "stat_sys_bluetooth_acquiring_anim"
private const val BT_CONNECTED = "stat_sys_data_bluetooth_connected_tint"

private fun swap(host: String, group: StatusIconGroup, vararg assets: String) =
    StatusIconSwap(host, group, assets.toList())

fun hostBatteryIcon(level: Int) = "stat_bt_battery_black_${if (level <= 2) 0 else level}"

val STATUS_ICON_SWAPS = listOf(
    swap("stat_sys_alarm", StatusIconGroup.ALARM, "stat_sys_alarm_tint"),
    swap("stat_sys_location", StatusIconGroup.LOCATION, "stat_sys_gps_on_tint"),
    swap("stat_signal_volte", StatusIconGroup.VOLTE, "stat_sys_signal_volte_tint"),
    swap("stat_signal_volte_sim1", StatusIconGroup.VOLTE, "stat_sys_signal_volte_sim1_tint"),
    swap("stat_signal_volte_sim2", StatusIconGroup.VOLTE, "stat_sys_signal_volte_sim2_tint"),
    swap(
        "stat_signal_volte_sim_both",
        StatusIconGroup.VOLTE,
        "stat_sys_signal_volte_sim_both_tint"
    ),
    swap("stat_sys_ringer_vibrate", StatusIconGroup.RINGER, "stat_sys_ringer_vibrate_tint"),
    swap(
        "stat_sys_flavor_one_ringer_vibrate",
        StatusIconGroup.RINGER,
        "stat_sys_ringer_vibrate_tint"
    ),
    swap("stat_sys_ringer_silent", StatusIconGroup.RINGER, "stat_sys_ringer_silent_tint"),
    swap(
        HEADSET_HOST,
        StatusIconGroup.HEADSET,
        "stat_sys_headset_tint",
        "stat_sys_headset_without_mic_tint"
    ),
    swap("stat_sys_data_bluetooth", StatusIconGroup.BLUETOOTH, "stat_sys_data_bluetooth_tint"),
    swap("stat_sys_data_bluetooth_connected", StatusIconGroup.BLUETOOTH, BT_CONNECTED),
    swap("stat_sys_data_bluetooth_connected_ing", StatusIconGroup.BLUETOOTH, BT_CONNECTED),
    swap(
        BT_TRANSFER_HOST,
        StatusIconGroup.BLUETOOTH,
        "stat_sys_data_bluetooth_in_tint",
        "stat_sys_data_bluetooth_out_tint",
        "stat_sys_data_bluetooth_inout_tint",
    ),
) + (0 until HOST_BT_BATTERY_LEVELS).distinctBy(::hostBatteryIcon).map { level ->
    swap(
        hostBatteryIcon(level),
        StatusIconGroup.BLUETOOTH,
        BT_CONNECTED,
        "stat_sys_bluetooth_handsfree_battery_${
            if (level <= 2) 1 else minOf(
                (level + 1) / 2,
                5
            )
        }_tint",
    )
}
