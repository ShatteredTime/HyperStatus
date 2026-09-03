package moe.evil.hyperstatus.xposed.graphics

import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import moe.evil.hyperstatus.xposed.model.IconTune
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val CENTER_DP = 1.8f
private const val CAP_DP = 10.7f

class MobileTypeDrawable(
    text: String,
    typeface: Typeface,
    textSize: Float,
    boxWidth: Int,
    private val boxHeight: Int,
    signalWidth: Float,
    dst: RectF,
    density: Float,
    tune: IconTune,
) : TextDrawable(text, typeface, textSize * tune.scale) {
    private val ink = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
    private val ascent = (ink.height() + ink.bottom).toFloat()
    private val inkWidth = ceil(paint.measureText(text))

    private val rightInset = maxOf(
        signalWidth - CENTER_DP * density - inkWidth / 2f,
        signalWidth - CAP_DP * density,
    )

    private val width = maxOf(boxWidth.toFloat(), rightInset + inkWidth).roundToInt()

    override val textX = width - rightInset - inkWidth + tune.dx * density

    override val baseline = verticalOffset(ascent, boxHeight, dst, density, tune) + ascent

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = boxHeight
}
