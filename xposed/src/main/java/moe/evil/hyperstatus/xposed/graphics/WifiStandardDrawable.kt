package moe.evil.hyperstatus.xposed.graphics

import android.graphics.Typeface

class WifiStandardDrawable(
    text: String,
    typeface: Typeface,
    textSize: Float,
    private val width: Int,
    private val height: Int,
) : TextDrawable(text, typeface, textSize) {
    private val metrics = paint.fontMetrics

    override val textX = (width - paint.measureText(text)) / 2f

    override val baseline = (height - (metrics.bottom - metrics.top)) / 2f - metrics.top

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height
}
