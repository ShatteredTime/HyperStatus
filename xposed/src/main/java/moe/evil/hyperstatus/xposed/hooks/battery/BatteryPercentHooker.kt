package moe.evil.hyperstatus.xposed.hooks.battery

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.makeAccessible
import com.oplus.systemui.statusbar.pipeline.battery.ui.drawable.HorizontalBatteryContentDrawable
import moe.evil.hyperstatus.shared.debugOnly
import moe.evil.hyperstatus.xposed.graphics.MiSans
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.hostField
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.lang.reflect.Field
import java.util.Collections
import java.util.WeakHashMap

private const val PAINT_FIELD = "percentInPaint"

object BatteryPercentHooker : Hooker() {
    private class Binding(host: Resources, val modules: Resources) {
        val hostSize = host.getDimension(host.hostId("dimen", "battery_percent_in_text_size"))
        val density = host.displayMetrics.density
    }

    internal val state = BatteryPercentState()

    private val drawables =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<HorizontalBatteryContentDrawable, Boolean>()))

    @Volatile
    private lateinit var binding: Binding

    @Volatile
    private var paintField: Field? = null

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            binding = Binding(
                host.resources,
                ctx.moduleResources ?: error("Module resources unavailable")
            )
            log.info { "Bound hostSize=${binding.hostSize}px" }
            refresh()
        }

        val drawableClass = classOf<HorizontalBatteryContentDrawable>()

        paintField = debugOnly {
            drawableClass.hostField("HorizontalBatteryContentDrawable#$PAINT_FIELD") {
                name = PAINT_FIELD
            }?.apply { makeAccessible() }
        }

        drawableClass
            .hostMethod("HorizontalBatteryContentDrawable#setTextTypeface") {
                name = "setTextTypeface"
                parameters(classOf<Typeface>())
            }
            ?.safeIntercept(ctx, "$id.typeface") { chain ->
                val drawable = chain.thisObject as HorizontalBatteryContentDrawable
                drawables += drawable
                mark("typeface")
                val b = binding
                debugOnly {
                    drawable.paint()?.textSize =
                        state.size?.takeIf { state.enabled }?.times(b.density) ?: b.hostSize
                }
                if (!state.enabled) return@safeIntercept chain.proceed()
                mark("swapped")
                chain.proceed(arrayOf(MiSans.of(b.modules, state.weight)))
            }

        state.onChange = ::refresh

        ctx.debug?.probe(id) {
            buildString {
                appendLine("enabled=${state.enabled} weight=${state.weight} size=${state.size}")
                appendLine(hitsSummary())
                val live = synchronized(drawables) { drawables.toList() }
                val ours = MiSans.of(binding.modules, state.weight)
                appendLine("Drawables (${live.size}):  ")
                live.forEach { drawable ->
                    val paint = drawable.paint()
                    appendLine(
                        "bounds=${drawable.bounds} textSize=${paint?.textSize}px " +
                                "misans=${paint?.typeface != null && paint.typeface == ours}"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun HorizontalBatteryContentDrawable.paint() = paintField?.get(this) as? Paint

    private fun refresh() = synchronized(drawables) { drawables.toList() }.forEach { drawable ->
        drawable.setTextTypeface(null)
        drawable.invalidateSelf()
    }
}
