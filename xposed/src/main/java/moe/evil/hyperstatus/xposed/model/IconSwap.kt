package moe.evil.hyperstatus.xposed.model

const val OPAQUE = 255
const val SOFT_ALPHA = 128

data class IconTune(
    val scale: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val widen: Boolean = false,
    val padEnd: Float = 0f,
) {
    override fun toString() =
        "scale=$scale dx=$dx dy=$dy anchor=$anchorX/$anchorY widen=$widen padEnd=$padEnd"
}

enum class IconGroup(val cli: String, val tune: IconTune, val intrinsic: Boolean = false) {
    SIGNAL("signal", IconTune(padEnd = 2f)),
    TYPE("type", IconTune(dy = -0.9f, anchorX = 1f, anchorY = 1f, widen = true)),
    ROAM("roam", IconTune()),
    ACTIVITY("activity", IconTune(), intrinsic = true),
}

data class IconSwap(
    val host: String,
    val replacement: String,
    val group: IconGroup,
    val alpha: Int = OPAQUE,
)
