package moe.evil.hyperstatus.xposed.graphics

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

class RowDrawable(private val children: List<Drawable>) : Drawable(), Drawable.Callback {
    private val width = children.sumOf { it.intrinsicWidth }
    private val height = children.maxOf { it.intrinsicHeight }

    init {
        children.forEach { it.callback = this }
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun isStateful() = children.any { it.isStateful }

    override fun mutate(): Drawable = also { children.forEach { it.mutate() } }

    override fun onBoundsChange(bounds: Rect) {
        var left = bounds.left
        children.forEach {
            val top = bounds.top + (bounds.height() - it.intrinsicHeight) / 2
            it.setBounds(left, top, left + it.intrinsicWidth, top + it.intrinsicHeight)
            left += it.intrinsicWidth
        }
    }

    override fun onStateChange(state: IntArray) =
        children.fold(false) { changed, child -> child.setState(state) || changed }

    override fun draw(canvas: Canvas) = children.forEach { it.draw(canvas) }

    override fun setAlpha(alpha: Int) = children.forEach { it.alpha = alpha }

    override fun setColorFilter(colorFilter: ColorFilter?) =
        children.forEach { it.colorFilter = colorFilter }

    override fun setTintList(tint: ColorStateList?) = children.forEach { it.setTintList(tint) }

    override fun setTintBlendMode(blendMode: BlendMode?) =
        children.forEach { it.setTintBlendMode(blendMode) }

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) =
        scheduleSelf(what, `when`)

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)
}
