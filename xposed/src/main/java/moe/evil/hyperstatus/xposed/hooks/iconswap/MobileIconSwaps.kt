package moe.evil.hyperstatus.xposed.hooks.iconswap

import moe.evil.hyperstatus.xposed.model.IconGroup
import moe.evil.hyperstatus.xposed.model.IconSwap
import moe.evil.hyperstatus.xposed.model.OPAQUE
import moe.evil.hyperstatus.xposed.model.Rat
import moe.evil.hyperstatus.xposed.model.SOFT_ALPHA

private fun swaps(
    group: IconGroup,
    vararg pairs: Pair<String, String>,
    alpha: Int = OPAQUE,
) = pairs.map { (host, replacement) -> IconSwap(host, replacement, group, alpha) }

val MOBILE_ICON_SWAPS: List<IconSwap> = swaps(
    IconGroup.SIGNAL,
    "stat_signal_signal_lte_single_0" to "stat_sys_signal_0_tint",
    "stat_signal_signal_lte_single_1" to "stat_sys_signal_1_tint",
    "stat_signal_signal_lte_single_2" to "stat_sys_signal_2_tint",
    "stat_signal_signal_lte_single_3" to "stat_sys_signal_3_tint",
    "stat_signal_signal_lte_single_4" to "stat_sys_signal_4_tint",
    "stat_signal_noservice_lte" to "stat_sys_signal_null_tint",
    "stat_signal_signal_null_lte" to "stat_sys_signal_null_tint",
) + swaps(
    IconGroup.SIGNAL,
    "stat_signal_soft_signal_0" to "stat_sys_signal_0_tint",
    "stat_signal_soft_signal_1" to "stat_sys_signal_1_tint",
    "stat_signal_soft_signal_2" to "stat_sys_signal_2_tint",
    "stat_signal_soft_signal_3" to "stat_sys_signal_3_tint",
    "stat_signal_soft_signal_4" to "stat_sys_signal_4_tint",
    "stat_signal_soft_noservice" to "stat_sys_signal_null_tint",
    alpha = SOFT_ALPHA,
) + swaps(
    IconGroup.ROAM,
    "stat_signal_roma_lte" to "stat_sys_data_connected_roam_tint",
) + swaps(
    IconGroup.ROAM,
    "stat_signal_soft_roma_lte" to "stat_sys_data_connected_roam_tint",
    alpha = SOFT_ALPHA,
) + swaps(
    IconGroup.ACTIVITY,
    "stat_signal_activity_default_public" to "stat_sys_signal_data_left_tint",
    "stat_signal_activity_in_public" to "stat_sys_signal_in_left_tint",
    "stat_signal_activity_out_public" to "stat_sys_signal_out_left_tint",
    "stat_signal_activity_inout_public" to "stat_sys_signal_inout_left_tint",
) + swaps(
    IconGroup.ACTIVITY,
    "stat_signal_activity_soft_default_public" to "stat_sys_signal_data_left_tint",
    "stat_signal_activity_soft_in_public" to "stat_sys_signal_in_left_tint",
    "stat_signal_activity_soft_out_public" to "stat_sys_signal_out_left_tint",
    "stat_signal_activity_soft_inout_public" to "stat_sys_signal_inout_left_tint",
    alpha = SOFT_ALPHA,
)

val MOBILE_TYPE_LABELS: Map<String, String> =
    Rat.entries.associate { it.dataTypeIcon to it.label } + mapOf(
        "stat_signal_connected_big_4g" to "4G",
        "stat_signal_connected_big_4g_plus" to "4G+",
        "stat_signal_connected_big_5g" to "5G",
        "stat_signal_connected_soft_g_lte_big" to "G",
        "stat_signal_connected_soft_e_lte_big" to "E",
        "stat_signal_connected_soft_2g_lte_big" to "2G",
        "stat_signal_connected_soft_3g_lte_big" to "3G",
        "stat_signal_connected_soft_3gp_lte_big" to "3G+",
        "stat_signal_connected_soft_h_lte_big" to "H",
        "stat_signal_connected_soft_h_p_lte_big" to "H+",
        "stat_signal_connected_soft_4g_lte_big" to "4G",
        "stat_signal_connected_soft_4gp_lte_big" to "4G+",
        "stat_signal_connected_soft_lte_big" to "LTE",
        "stat_signal_connected_soft_lte_plus_big" to "LTE+",
        "stat_signal_connected_soft_5g" to "5G",
    )
