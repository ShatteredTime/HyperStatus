package moe.evil.hyperstatus.xposed.hooks.battery

private const val HYPEROS_DIGIT_WEIGHT = 620

class BatteryPercentState {
    @Volatile
    var enabled = true
        private set

    @Volatile
    var weight = HYPEROS_DIGIT_WEIGHT
        private set

    @Volatile
    var size: Float? = null
        private set

    @Volatile
    var onChange: (() -> Unit)? = null

    fun enable(value: Boolean) {
        enabled = value
        onChange?.invoke()
    }

    fun weigh(value: Int) {
        weight = value
        onChange?.invoke()
    }

    fun resize(dp: Float?) {
        size = dp
        onChange?.invoke()
    }

    fun reset() {
        enabled = true
        weight = HYPEROS_DIGIT_WEIGHT
        size = null
        onChange?.invoke()
    }
}
