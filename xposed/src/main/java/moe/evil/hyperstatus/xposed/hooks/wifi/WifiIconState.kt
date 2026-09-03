package moe.evil.hyperstatus.xposed.hooks.wifi

import moe.evil.hyperstatus.xposed.model.NO_WIFI_OVERRIDE
import moe.evil.hyperstatus.xposed.model.WifiOverride

private const val HYPEROS_STANDARD_TEXT_DP = 5.8182f

class WifiIconState {
    @Volatile
    var enabled = true
        private set

    @Volatile
    var textSize = HYPEROS_STANDARD_TEXT_DP
        private set

    @Volatile
    var override = NO_WIFI_OVERRIDE
        private set

    @Volatile
    var onChange: (() -> Unit)? = null

    fun enable(value: Boolean) {
        enabled = value
        onChange?.invoke()
    }

    fun resize(dp: Float) {
        textSize = dp
        onChange?.invoke()
    }

    fun update(block: (WifiOverride) -> WifiOverride) {
        override = block(override)
        onChange?.invoke()
    }

    fun reset() {
        enabled = true
        textSize = HYPEROS_STANDARD_TEXT_DP
        override = NO_WIFI_OVERRIDE
        onChange?.invoke()
    }
}
