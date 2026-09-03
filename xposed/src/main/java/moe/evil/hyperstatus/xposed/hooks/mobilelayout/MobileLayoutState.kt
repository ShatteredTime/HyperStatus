package moe.evil.hyperstatus.xposed.hooks.mobilelayout

class MobileLayoutState {
    @Volatile
    var enabled = true
        private set

    @Volatile
    var inoutDx = 0f
        private set

    @Volatile
    var onChange: (() -> Unit)? = null

    fun enable(value: Boolean) {
        enabled = value
        onChange?.invoke()
    }

    fun shift(dx: Float) {
        inoutDx = dx
        onChange?.invoke()
    }

    fun reset() {
        enabled = true
        inoutDx = 0f
        onChange?.invoke()
    }
}
