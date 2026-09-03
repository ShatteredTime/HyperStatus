package moe.evil.hyperstatus.xposed.hooks.mobilelayout

import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import moe.evil.hyperstatus.xposed.graphics.contentLeft
import moe.evil.hyperstatus.xposed.model.MOBILE_VIEW_BINDERS
import moe.evil.hyperstatus.xposed.model.SOFT_ALPHA
import moe.evil.hyperstatus.xposed.utils.DrawableCache
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.ancestorValue
import moe.evil.hyperstatus.xposed.utils.hostClassOrNull
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.moduleDrawable
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

object MobileLayoutHooker : Hooker() {
    private class Binding(host: Resources, modules: Resources) {
        val group = host.hostId("id", "mobile_group")
        val signal = host.hostId("id", "mobile_signal")
        val inout = host.hostId("id", "data_inout")
        val roaming = host.hostId("id", "mobile_roaming")
        val large = host.hostId("id", "mobile_roaming_large")
        val space = host.hostId("id", "mobile_roaming_space")
        val softRoma = host.hostId("drawable", "stat_signal_soft_roma_lte")
        val roam = modules.moduleDrawable("stat_sys_data_connected_roam_tint")
        val cache = DrawableCache(modules)
        val density = host.displayMetrics.density
        val hyperInset = cache.get(modules.moduleDrawable("stat_sys_signal_4_tint")).contentLeft()
    }

    private class Group(val root: ViewGroup, b: Binding) {
        val signal: ImageView? = root.findViewById(b.signal)
        val inout: ImageView? = root.findViewById(b.inout)
        val roaming: ImageView? = root.findViewById(b.roaming)
        val large: ImageView? = root.findViewById(b.large)
        val space: View? = root.findViewById(b.space)

        @Volatile
        var roamRes = 0

        @Volatile
        var tint: ColorStateList? = null
    }

    internal val state = MobileLayoutState()

    private val groups = Collections.synchronizedMap(WeakHashMap<ViewGroup, Group>())

    @Volatile
    private lateinit var binding: Binding

    private val inoutGuard = guard("inout")
    private val roamingGuard = guard("roaming")
    private val tintGuard = guard("tint")
    private val applyGuard = guard("apply")

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            val b = Binding(
                host.resources,
                ctx.moduleResources ?: error("Module resources unavailable")
            )
            binding = b
            log.info { "Bound hyperInset=${b.hyperInset} density=${b.density}" }
        }

        MOBILE_VIEW_BINDERS.forEach { fqn ->
            val binder = hostClassOrNull(fqn, ctx.classLoader) ?: return@forEach

            binder.hostMethod("$fqn#updateSignalIcon", optional = true) {
                name { it.endsWith("updateSignalIcon") }
                parameterCount = 2
            }?.safeIntercept(ctx, "$id.updateSignalIcon") { chain ->
                mark("updateSignalIcon")
                chain.proceed().also {
                    inoutGuard.safe {
                        val b = binding
                        groupOf(chain.getArg(0) as ImageView, b)?.let { applyInout(it, b) }
                    }
                }
            }

            binder.hostMethod("$fqn#updateMobileRoaming", optional = true) {
                name { it.endsWith("updateMobileRoaming") }
                parameterCount = 3
            }?.safeIntercept(ctx, "$id.updateMobileRoaming") { chain ->
                mark("updateMobileRoaming")
                chain.proceed().also {
                    roamingGuard.safe {
                        val b = binding
                        val group = groupOf(chain.getArg(0) as ImageView, b) ?: return@safe
                        group.roamRes = chain.getArg(2) as Int
                        applyRoaming(group, b)
                    }
                }
            }

            binder.hostMethod("$fqn#updateTint", optional = true) {
                name { it.endsWith("updateTint") }
                parameterCount = 6
            }?.safeIntercept(ctx, "$id.updateTint") { chain ->
                mark("updateTint")
                chain.proceed().also {
                    tintGuard.safe {
                        if (!state.enabled) return@safe
                        val group = groupOf(chain.getArg(3) as ImageView, binding) ?: return@safe
                        val large = group.large ?: return@safe
                        val color = chain.getArg(5) as Int
                        val last = group.tint
                        if (last != null && last.defaultColor == color) return@safe
                        large.imageTintList =
                            ColorStateList.valueOf(color).also { group.tint = it }
                    }
                }
            }
        }

        state.onChange = {
            val b = binding
            val live = synchronized(groups) { groups.values.toList() }
            live.forEach { group ->
                group.root.post {
                    applyGuard.safe {
                        applyInout(group, b)
                        applyRoaming(group, b)
                    }
                }
            }
            log.debug { "Reapplied ${live.size} group(s), enabled=${state.enabled} inoutDx=${state.inoutDx}" }
        }

        ctx.debug?.probe(id) {
            buildString {
                appendLine("enabled=${state.enabled} inoutDx=${state.inoutDx}dp hyperInset=${binding.hyperInset}")
                appendLine(hitsSummary())
                val live = synchronized(groups) { groups.values.toList() }
                appendLine("Groups (${live.size}):  ")
                live.forEach { g ->
                    val lp = g.inout?.layoutParams as? FrameLayout.LayoutParams
                    appendLine(
                        "shown=${g.root.isShown} roamRes=${g.roamRes} " +
                                "signal=${g.signal?.drawable?.intrinsicWidth}px " +
                                "roaming=${g.roaming?.visibility} space=${g.space?.visibility}:${g.space?.width} " +
                                "large=${g.large?.visibility}:${g.large?.width}x${g.large?.height} " +
                                "inout=${g.inout?.visibility}:${g.inout?.width}x${g.inout?.height} " +
                                "gravity=${lp?.gravity} marginEnd=${lp?.marginEnd}"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun groupOf(view: View, b: Binding): Group? =
        view.ancestorValue { it.takeIf { root -> root.id == b.group } }
            ?.let { root -> groups.getOrPut(root) { Group(root, b) } }

    private fun applyInout(group: Group, b: Binding) {
        val view = group.inout ?: return
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val bars = group.signal?.drawable
        val gravity = (if (state.enabled) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
        val margin = if (state.enabled && bars != null) {
            (bars.intrinsicWidth - bars.contentLeft() + b.hyperInset + (-2f + state.inoutDx) * b.density)
                .roundToInt()
        } else {
            0
        }
        if (lp.gravity == gravity && lp.marginEnd == margin) return
        lp.gravity = gravity
        lp.marginEnd = margin
        view.layoutParams = lp
    }

    private fun applyRoaming(group: Group, b: Binding) {
        val shown = group.roamRes != 0
        val large = group.large
        if (!state.enabled || large == null) {
            val restored = if (shown) View.VISIBLE else View.GONE
            large?.visibility = View.GONE
            group.roaming?.visibility = restored
            group.space?.visibility = restored
            return
        }
        group.roaming?.visibility = View.GONE
        group.space?.visibility = View.GONE
        if (!shown) {
            large.visibility = View.GONE
            return
        }
        large.setImageDrawable(
            b.cache.get(b.roam).let {
                if (group.roamRes == b.softRoma) it.mutate().apply { alpha = SOFT_ALPHA } else it
            },
        )
        large.imageTintList = group.roaming?.imageTintList
        large.visibility = View.VISIBLE
    }
}
