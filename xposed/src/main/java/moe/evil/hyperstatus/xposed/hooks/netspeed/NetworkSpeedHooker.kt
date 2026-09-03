package moe.evil.hyperstatus.xposed.hooks.netspeed

import android.content.res.Resources
import android.util.TypedValue
import android.widget.TextView
import com.highcapable.kavaref.extension.classOf
import com.oplus.systemui.statusbar.phone.netspeed.NetworkSpeedIconState
import com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView
import moe.evil.hyperstatus.xposed.graphics.MiSans
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.util.Collections
import java.util.WeakHashMap

private const val HOST_UNIT_SUFFIX = "/S"
private const val HYPEROS_UNIT_SUFFIX = "/s"

object NetworkSpeedHooker : Hooker() {
    private class Binding(host: Resources, val modules: Resources) {
        val density = host.displayMetrics.density
        val numberSize =
            host.getDimensionPixelSize(host.hostId("dimen", "network_speed_number_text_size"))
        val unitSize =
            host.getDimensionPixelSize(host.hostId("dimen", "network_speed_unit_text_size"))
        val numberShift =
            host.getDimension(
                host.hostId(
                    "dimen",
                    "network_speed_number_margin_bottom"
                )
            ) / 2 - 4.6f * density
        val unitShift =
            3.8f * density - host.getDimension(
                host.hostId(
                    "dimen",
                    "network_speed_unit_margin_top"
                )
            ) / 2
    }

    internal val state = NetworkSpeedState()

    private val views =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<NetworkSpeedView, Boolean>()))

    @Volatile
    private lateinit var binding: Binding

    private val inflateGuard = guard("inflate")
    private val paramsGuard = guard("params")
    private val unitGuard = guard("unit")
    private val refreshGuard = guard("refresh")

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            binding = Binding(
                host.resources,
                ctx.moduleResources ?: error("Module resources unavailable")
            )
            log.info {
                "Bound number=${binding.numberSize}px unit=${binding.unitSize}px " +
                        "shift=${binding.numberShift}/${binding.unitShift}"
            }
            refresh()
        }

        val speedView = classOf<NetworkSpeedView>()

        speedView.hostMethod("NetworkSpeedView#onFinishInflate") {
            name = "onFinishInflate"; emptyParameters()
        }
            ?.safeIntercept(ctx, "$id.inflate") { chain ->
                chain.proceed().also {
                    inflateGuard.safe {
                        val view = chain.thisObject as NetworkSpeedView
                        mark("inflate")
                        views += view
                        apply(view)
                    }
                }
            }

        speedView.hostMethod("NetworkSpeedView#updateSpeedNumberParams") {
            name = "updateSpeedNumberParams"; emptyParameters()
        }
            ?.safeIntercept(ctx, "$id.params") { chain ->
                chain.proceed().also {
                    paramsGuard.safe {
                        mark("params")
                        apply(chain.thisObject as NetworkSpeedView)
                    }
                }
            }

        speedView.hostMethod("NetworkSpeedView#applyNetworkState") {
            name = "applyNetworkState"
            parameters(classOf<NetworkSpeedIconState>())
        }
            ?.safeIntercept(ctx, "$id.unit") { chain ->
                chain.proceed().also {
                    unitGuard.safe {
                        val unit = (chain.thisObject as NetworkSpeedView).speedUnit
                        unitText(unit.text)?.let {
                            mark("unit")
                            unit.text = it
                        }
                    }
                }
            }

        state.onChange = refreshGuard.wrap(::refresh)

        ctx.debug?.probe(id) {
            buildString {
                appendLine(
                    "enabled=${state.enabled} weight=${state.weight} " +
                            "numberSize=${state.numberSize} unitSize=${state.unitSize}"
                )
                appendLine(hitsSummary())
                val live = synchronized(views) { views.toList() }
                appendLine("Views (${live.size}): ")
                live.forEach { view ->
                    appendLine(
                        "'${view.speedNumber.text}' '${view.speedUnit.text}' shown=${view.isShown} " +
                                "number=${view.speedNumber.textSize}px unit=${view.speedUnit.textSize}px " +
                                "misans=${view.speedNumber.typeface != view.mDefaultBoldFont}"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun unitText(text: CharSequence): CharSequence? = when {
        state.enabled && text.endsWith(HOST_UNIT_SUFFIX) -> text.removeSuffix(HOST_UNIT_SUFFIX)
            .toString() + HYPEROS_UNIT_SUFFIX

        !state.enabled && text.endsWith(HYPEROS_UNIT_SUFFIX) -> text.removeSuffix(
            HYPEROS_UNIT_SUFFIX
        ).toString() + HOST_UNIT_SUFFIX

        else -> null
    }

    private fun refresh() = synchronized(views) { views.toList() }.forEach(::apply)

    private val NetworkSpeedView.speedNumber: TextView
        get() = mSpeedNumber ?: error("NetworkSpeedView has no mSpeedNumber")

    private val NetworkSpeedView.speedUnit: TextView
        get() = mSpeedUnit ?: error("NetworkSpeedView has no mSpeedUnit")

    private fun apply(view: NetworkSpeedView) {
        val b = binding
        val number = view.speedNumber
        val unit = view.speedUnit
        val on = state.enabled
        val typeface = if (on) MiSans.of(b.modules, state.weight) else view.mDefaultBoldFont
        number.typeface = typeface
        unit.typeface = typeface
        number.size(state.numberSize, b.numberSize, b.density)
        unit.size(state.unitSize, b.unitSize, b.density)
        number.translationY = if (on) b.numberShift else 0f
        unit.translationY = if (on) b.unitShift else 0f
        unitText(unit.text)?.let { unit.text = it }
    }

    private fun TextView.size(dp: Float?, hostPx: Int, density: Float) =
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            dp?.takeIf { state.enabled }?.times(density) ?: hostPx.toFloat()
        )
}
