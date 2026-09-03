package moe.evil.hyperstatus.xposed.hooks.wifi

import android.content.res.Resources
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.ui.binder.IconViewBinder
import com.android.systemui.statusbar.pipeline.wifi.shared.model.WifiNetworkModel
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.makeAccessible
import com.oplus.systemui.statusbar.pipeline.OplusWifiSignalExImpl
import moe.evil.hyperstatus.shared.debugOnly
import moe.evil.hyperstatus.xposed.graphics.MiSans
import moe.evil.hyperstatus.xposed.graphics.WifiStandardDrawable
import moe.evil.hyperstatus.xposed.model.NO_WIFI_OVERRIDE
import moe.evil.hyperstatus.xposed.model.WifiActivity
import moe.evil.hyperstatus.xposed.model.WifiBadge
import moe.evil.hyperstatus.xposed.model.WifiFamily
import moe.evil.hyperstatus.xposed.utils.DrawableCache
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.ancestorValue
import moe.evil.hyperstatus.xposed.utils.hostClassOrNull
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostIdOrNull
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.moduleDrawable
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

private const val EX_COLLECTOR =
    $$"com.oplus.systemui.statusbar.pipeline.OplusWifiSignalExImpl$bindEx$1$1"
private const val HOST_LEVELS = 5
private const val HYPEROS_LEVELS = 4

object WifiIconHooker : Hooker() {
    private class Level(val level: Int, val validated: Boolean)

    private class Binding(val host: Resources, modules: Resources) {
        val combo = host.hostId("id", "wifi_combo")
        val signal = host.hostId("id", "wifi_signal")
        val inout = host.hostId("id", "wifi_inout")
        val left = host.hostId("id", "wifi_left")
        val validated = IntArray(HOST_LEVELS) {
            host.hostId(
                "drawable",
                "stat_signal_wifi_signal_%d".format(it)
            )
        }
        val unvalidated =
            IntArray(HOST_LEVELS) {
                host.hostId(
                    "drawable",
                    "ic_no_internet_wifi_signal_%d".format(it)
                )
            }
        val levels = SparseArray<Level>(HOST_LEVELS * 2).apply {
            repeat(HOST_LEVELS) {
                put(validated[it], Level(it, true))
                put(unvalidated[it], Level(it, false))
            }
        }
        val badges = SparseArray<WifiBadge>(WifiBadge.entries.size).apply {
            WifiBadge.entries.forEach { badge ->
                host.hostIdOrNull("drawable", badge.host)?.let { put(it, badge) }
            }
        }
        val activities = SparseArray<WifiActivity>(WifiActivity.entries.size).apply {
            WifiActivity.entries.forEach { put(host.hostId("drawable", it.host), it) }
        }
        val families = Array(WifiFamily.entries.size) { family ->
            IntArray(HYPEROS_LEVELS) {
                modules.moduleDrawable(
                    WifiFamily.entries[family].asset.format(
                        it
                    )
                )
            }
        }
        val arrows = IntArray(WifiActivity.entries.size) {
            WifiActivity.entries[it].asset?.let(modules::moduleDrawable) ?: 0
        }
        val cache = DrawableCache(modules)
        val typeface = MiSans.of(modules)
        val density = host.displayMetrics.density
    }

    private class Group(val root: ViewGroup, b: Binding) {
        val signal: ImageView? = root.findViewById(b.signal)
        val inout: ImageView? = root.findViewById(b.inout)
        val left: ImageView? = root.findViewById(b.left)
        val inoutGravity = inout?.gravityOrNull() ?: Gravity.NO_GRAVITY
        val leftGravity = left?.gravityOrNull() ?: Gravity.NO_GRAVITY

        @Volatile
        var signalRes = 0

        @Volatile
        var leftRes = 0

        @Volatile
        var inoutRes = 0
    }

    internal val state = WifiIconState()

    private val groups = Collections.synchronizedMap(WeakHashMap<ViewGroup, Group>())

    @Volatile
    private lateinit var binding: Binding

    private val signalGuard = guard("signal")
    private val emitGuard = guard("emit")
    private val renderGuard = guard("render")

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            val b = Binding(
                host.resources,
                ctx.moduleResources ?: error("Module resources unavailable")
            )
            binding = b
            log.info { "Bound density=${b.density} badges=${b.badges.size()}/${WifiBadge.entries.size}" }
        }

        classOf<OplusWifiSignalExImpl>()
            .hostMethod("OplusWifiSignalExImpl#wifiSignalResId") {
                name = "wifiSignalResId"
                parameters(classOf<WifiNetworkModel.Active>())
            }
            ?.safeIntercept(ctx, "$id.wifiSignalResId") { chain ->
                mark("wifiSignalResId")
                val active = chain.getArg(0) as WifiNetworkModel.Active
                if (!state.enabled || active.isValidated) chain.proceed()
                else binding.unvalidated[active.level]
            }

        classOf<IconViewBinder>()
            .hostMethod("IconViewBinder#bind") {
                name = "bind"; parameters(
                classOf<Icon>(),
                classOf<ImageView>()
            )
            }
            ?.safeIntercept(ctx, "$id.bind") { chain ->
                chain.proceed().also {
                    signalGuard.safe {
                        val b = binding
                        val view = chain.getArg(1) as ImageView
                        if (view.id != b.signal) return@safe
                        mark("bind")
                        val group = groupOf(view, b) ?: return@safe
                        group.signalRes = (chain.getArg(0) as? Icon.Resource)?.res
                            ?: error("Wifi signal bound with a non-resource icon")
                        render(group, b)
                    }
                }
            }

        hostClassOrNull(EX_COLLECTOR, ctx.classLoader)?.let { collector ->
            val target = collector.declaredFields.firstOrNull { it.type == classOf<Any>() }
                ?.apply { makeAccessible() }
                ?: error("No captured view field on $EX_COLLECTOR")
            collector.hostMethod("$EX_COLLECTOR#emit") { name = "emit"; parameterCount = 2 }
                ?.safeIntercept(ctx, "$id.emit") { chain ->
                    chain.proceed().also {
                        emitGuard.safe {
                            val b = binding
                            val res = chain.getArg(0) as? Int ?: return@safe
                            val view = target.get(chain.thisObject) as? ImageView ?: return@safe
                            val group = groupOf(view, b) ?: return@safe
                            when (view.id) {
                                b.left -> group.leftRes = res
                                b.inout -> group.inoutRes = res
                                else -> return@safe
                            }
                            mark("emit")
                            render(group, b)
                        }
                    }
                }
        }

        state.onChange = {
            val b = binding
            val live = synchronized(groups) { groups.values.toList() }
            live.forEach { group -> group.root.post { renderGuard.safe { render(group, b) } } }
            log.debug {
                "Rerendered ${live.size} group(s), enabled=${state.enabled} " +
                        "textSize=${state.textSize}dp override=${state.override}"
            }
        }

        ctx.debug?.probe(id) {
            buildString {
                val b = binding
                fun name(res: Int) =
                    runCatching { b.host.getResourceEntryName(res) }.getOrNull() ?: res.toString()
                appendLine("enabled=${state.enabled} textSize=${state.textSize}dp override=${state.override} density=${b.density}")
                appendLine(hitsSummary())
                val live = synchronized(groups) { groups.values.toList() }
                appendLine("Groups (${live.size}):  ")
                live.forEach { g ->
                    val level = b.levels.get(g.signalRes)
                    appendLine(
                        "shown=${g.root.isShown} signal=${name(g.signalRes)} " +
                                "level=${level?.level} validated=${level?.validated} " +
                                "left=${name(g.leftRes)} inout=${name(g.inoutRes)} "
                    )
                    appendLine(
                        "signal=${g.signal?.drawable?.intrinsicWidth}x${g.signal?.drawable?.intrinsicHeight} " +
                                "left=${g.left?.visibility}:${g.left?.width}x${g.left?.height}@${g.left?.gravityOrNull()} " +
                                "inout=${g.inout?.visibility}:${g.inout?.width}x${g.inout?.height}@${g.inout?.gravityOrNull()}"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun View.gravityOrNull() = (layoutParams as? FrameLayout.LayoutParams)?.gravity

    private fun groupOf(view: View, b: Binding): Group? =
        view.ancestorValue { it.takeIf { root -> root.id == b.combo } }
            ?.let { root -> groups.getOrPut(root) { Group(root, b) } }

    private fun render(group: Group, b: Binding) {
        val host = b.levels.get(group.signalRes)
        if (!state.enabled) {
            if (group.signalRes != 0) group.signal?.setImageResource(host?.let { b.validated[it.level] }
                ?: group.signalRes)
            group.left?.restore(group.leftRes, group.leftGravity)
            group.inout?.restore(group.inoutRes, group.inoutGravity)
            return
        }
        val override = debugOnly { state.override } ?: NO_WIFI_OVERRIDE
        val level = override.level ?: host?.level ?: return
        val validated = override.validated ?: host?.validated ?: true
        val badge = when {
            override.double == true -> WifiBadge.DOUBLE
            override.standard != null -> WifiBadge.entries.firstOrNull { it.standard == override.standard }
            else -> b.badges.get(group.leftRes)
        }
        val activity = override.activity ?: b.activities.get(group.inoutRes)
        val family = when {
            badge == WifiBadge.DOUBLE -> WifiFamily.SLAVE
            !validated -> WifiFamily.UNAVAILABLE
            else -> WifiFamily.NORMAL
        }
        group.signal?.setImageDrawable(
            b.cache.get(
                b.families[family.ordinal][minOf(
                    level,
                    HYPEROS_LEVELS - 1
                )]
            )
        )
        group.left?.let { left ->
            val text = badge?.text
            if (text == null) {
                left.visibility = View.GONE
            } else {
                left.setImageDrawable(
                    WifiStandardDrawable(
                        text,
                        b.typeface,
                        state.textSize * b.density,
                        (7.3636f * b.density).roundToInt(),
                        (11.2727f * b.density).roundToInt(),
                    ),
                )
                left.place(Gravity.RIGHT or Gravity.BOTTOM)
                left.visibility = View.VISIBLE
            }
        }
        group.inout?.let { inout ->
            val arrow =
                activity?.let { b.arrows[it.ordinal] }?.takeIf { it != 0 }?.let(b.cache::get)
            if (arrow == null) {
                inout.visibility = View.GONE
            } else {
                inout.setImageDrawable(arrow)
                inout.place((if (badge?.text != null) Gravity.LEFT else Gravity.RIGHT) or Gravity.BOTTOM)
                inout.visibility = View.VISIBLE
            }
        }
    }

    private fun ImageView.restore(res: Int, gravity: Int) {
        if (res == 0) {
            visibility = View.GONE
        } else {
            setImageResource(res)
            visibility = View.VISIBLE
        }
        place(gravity)
    }

    private fun View.place(gravity: Int) {
        val lp = layoutParams as? FrameLayout.LayoutParams ?: return
        if (lp.gravity == gravity) return
        lp.gravity = gravity
        layoutParams = lp
    }
}
