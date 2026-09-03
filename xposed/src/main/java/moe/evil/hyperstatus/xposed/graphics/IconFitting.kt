package moe.evil.hyperstatus.xposed.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import moe.evil.hyperstatus.xposed.model.IconTune
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

private val contentLefts = ConcurrentHashMap<Drawable.ConstantState, Float>()

fun Drawable.contentBounds(): RectF {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val previous = Rect(bounds)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    bounds = previous

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()

    var left = width
    var top = height
    var right = 0
    var bottom = 0
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if (pixels[row + x] ushr 24 == 0) continue
            if (x < left) left = x
            if (x >= right) right = x + 1
            if (y < top) top = y
            bottom = y + 1
        }
    }
    return if (left >= right) RectF(0f, 0f, 1f, 1f)
    else RectF(
        left / width.toFloat(),
        top / height.toFloat(),
        right / width.toFloat(),
        bottom / height.toFloat(),
    )
}

fun Drawable.contentLeft(): Float = (this as? FittedDrawable)?.contentLeft
    ?: constantState?.let { contentLefts.getOrPut(it) { contentBounds().left * intrinsicWidth } }
    ?: (contentBounds().left * intrinsicWidth)

fun verticalOffset(
    contentHeight: Float,
    boxHeight: Int,
    dst: RectF,
    density: Float,
    tune: IconTune,
) = dst.top * boxHeight + tune.dy * density +
        tune.anchorY * (dst.height() * boxHeight - contentHeight)

class FittedDrawable(
    inner: Drawable,
    boxWidth: Int,
    private val boxHeight: Int,
    private val src: RectF,
    dst: RectF,
    density: Float,
    tune: IconTune,
) : DrawableWrapper(inner) {
    private val innerWidth = inner.intrinsicWidth.toFloat()
    private val innerHeight = inner.intrinsicHeight.toFloat()
    private val srcWidth = innerWidth * src.width()
    private val srcHeight = innerHeight * src.height()

    val scale = when {
        srcWidth <= 0f || srcHeight <= 0f -> 1f
        else -> (tune.scale * dst.height() * boxHeight / srcHeight).let {
            if (tune.widen) minOf(it, boxHeight / srcHeight)
            else minOf(it, boxWidth / srcWidth, boxHeight / srcHeight)
        }
    }

    private val dstWidth = dst.width() * boxWidth
    private val extra = (scale * srcWidth - dstWidth).coerceAtLeast(0f).takeIf { tune.widen } ?: 0f
    private val width = (boxWidth + extra + tune.padEnd * density).roundToInt()
    private val offsetY = verticalOffset(scale * srcHeight, boxHeight, dst, density, tune)

    val contentLeft = dst.left * boxWidth + tune.dx * density +
            if (extra > 0f) 0f else tune.anchorX * (dstWidth - scale * srcWidth)

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = boxHeight

    override fun onBoundsChange(bounds: Rect) {
        val inner = drawable ?: return
        val left = bounds.left + contentLeft - scale * innerWidth * src.left
        val top = bounds.top + offsetY - scale * innerHeight * src.top
        inner.setBounds(
            left.roundToInt(),
            top.roundToInt(),
            (left + scale * innerWidth).roundToInt(),
            (top + scale * innerHeight).roundToInt(),
        )
    }
}
