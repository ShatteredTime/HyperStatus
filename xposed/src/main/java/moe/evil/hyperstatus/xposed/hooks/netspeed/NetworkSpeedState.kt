package moe.evil.hyperstatus.xposed.hooks.netspeed

private const val HYPEROS_SPEED_WEIGHT = 700
private const val HYPEROS_NUMBER_SIZE_DP = 7f
private const val HYPEROS_UNIT_SIZE_DP = 6.4f

class NetworkSpeedState {
    @Volatile
    var enabled = true
        private set

    @Volatile
    var weight = HYPEROS_SPEED_WEIGHT
        private set

    @Volatile
    var numberSize: Float? = HYPEROS_NUMBER_SIZE_DP
        private set

    @Volatile
    var unitSize: Float? = HYPEROS_UNIT_SIZE_DP
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

    fun resize(number: Float?, unit: Float?) {
        numberSize = number
        unitSize = unit
        onChange?.invoke()
    }

    fun reset() {
        enabled = true
        weight = HYPEROS_SPEED_WEIGHT
        numberSize = HYPEROS_NUMBER_SIZE_DP
        unitSize = HYPEROS_UNIT_SIZE_DP
        onChange?.invoke()
    }
}
