package moe.evil.hyperstatus.xposed.graphics

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable

abstract class TextDrawable(
    protected val text: String,
    typeface: Typeface,
    textSize: Float,
) : Drawable() {
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.typeface = typeface
        it.textSize = textSize
        it.color = Color.WHITE
    }

    protected abstract val textX: Float

    protected abstract val baseline: Float

    private var tint: ColorStateList? = null

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun isStateful() = tint?.isStateful == true

    override fun draw(canvas: Canvas) {
        canvas.drawText(text, bounds.left + textX, bounds.top + baseline, paint)
    }

    override fun setTintList(tint: ColorStateList?) {
        this.tint = tint
        onStateChange(state)
    }

    override fun onStateChange(state: IntArray): Boolean {
        val color = tint?.getColorForState(state, Color.WHITE) ?: return false
        if (color == paint.color) return false
        paint.color = color
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }
}
