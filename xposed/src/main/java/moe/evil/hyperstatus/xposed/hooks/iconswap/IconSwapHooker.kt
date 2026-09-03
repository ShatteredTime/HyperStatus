package moe.evil.hyperstatus.xposed.hooks.iconswap

import android.content.Context
import android.content.res.Resources
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import com.highcapable.kavaref.extension.classOf
import moe.evil.hyperstatus.shared.debugOnly
import moe.evil.hyperstatus.xposed.graphics.FittedDrawable
import moe.evil.hyperstatus.xposed.graphics.MiSans
import moe.evil.hyperstatus.xposed.graphics.MobileTypeDrawable
import moe.evil.hyperstatus.xposed.graphics.contentBounds
import moe.evil.hyperstatus.xposed.model.IconGroup
import moe.evil.hyperstatus.xposed.model.IconSwap
import moe.evil.hyperstatus.xposed.model.OPAQUE
import moe.evil.hyperstatus.xposed.utils.DrawableCache
import moe.evil.hyperstatus.xposed.utils.Guard
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.hostIdOrNull
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.moduleDrawable
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong

object IconSwapHooker : Hooker() {
    private class Box(val width: Int, val height: Int, val dst: RectF)

    private class Entry(val label: String?, val swap: IconSwap?, val moduleId: Int) {
        @Volatile
        var box: Box? = null

        @Volatile
        var src: RectF? = null
    }

    private class Binding(
        val host: Resources,
        modules: Resources,
        val entries: SparseArray<Entry>,
        val signalWidth: Int
    ) {
        val cache = DrawableCache(modules)
        val density = host.displayMetrics.density
        val typeface = MiSans.of(modules)
    }

    private class Swapped(val guard: Guard) {
        @Volatile
        var resId = 0
    }

    internal val state = IconSwapState()

    private val issued = Collections.synchronizedMap(WeakHashMap<ImageView, Swapped>())

    private val calls = AtomicLong()

    private val swaps = AtomicLong()

    @Volatile
    private lateinit var binding: Binding

    private val redrawTag = "$id.redraw"

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            val hosts = host.resources
            val modules = ctx.moduleResources ?: error("Module resources unavailable")
            val entries = SparseArray<Entry>(MOBILE_TYPE_LABELS.size + MOBILE_ICON_SWAPS.size)
            MOBILE_TYPE_LABELS.forEach { (name, label) ->
                hosts.hostIdOrNull("drawable", name)
                    ?.let { entries.put(it, Entry(label, null, 0)) }
                    ?: log.warn { "Unresolved type icon $name" }
            }
            MOBILE_ICON_SWAPS.forEach { swap ->
                hosts.hostIdOrNull("drawable", swap.host)
                    ?.let {
                        entries.put(it, Entry(null, swap, modules.moduleDrawable(swap.replacement)))
                    }
                    ?: log.warn { "Unresolved host icon ${swap.host}" }
            }
            val signalWidth = MOBILE_ICON_SWAPS.first { it.group == IconGroup.SIGNAL }
                .let { hosts.hostIdOrNull("drawable", it.host) }
                ?.let { host.getDrawable(it)?.intrinsicWidth }
                ?: 0
            val b = Binding(hosts, modules, entries, signalWidth)
            binding = b
            log.info {
                "Mapped ${entries.size()}/${MOBILE_TYPE_LABELS.size + MOBILE_ICON_SWAPS.size} icon(s), " +
                        "signalWidth=$signalWidth, density=${b.density}"
            }
        }

        classOf<ImageView>()
            .hostMethod("ImageView#setImageResource") {
                name = "setImageResource"; parameters(
                classOf<Int>()
            )
            }
            ?.safeIntercept(ctx, "$id.setImageResource") { chain ->
                debugOnly { calls.incrementAndGet() }
                val resId = chain.getArg(0) as Int
                val b = binding
                debugOnly { state.trace }?.let { filter ->
                    runCatching { b.host.getResourceEntryName(resId) }.getOrNull()
                        ?.takeIf { filter in it }
                        ?.let { log.debug { "Trace $it=$resId mapped=${b.entries.indexOfKey(resId) >= 0}" } }
                }
                val entry = b.entries.get(resId) ?: return@safeIntercept chain.proceed()
                val view = chain.thisObject as ImageView
                val drawable = render(entry, resId, view.context, b)
                debugOnly { swaps.incrementAndGet() }
                issued.getOrPut(view) { Swapped(Guard(log, redrawTag)) }.resId = resId
                view.setImageDrawable(drawable)
                null
            }

        state.onChange = {
            val b = binding
            val live = synchronized(issued) { issued.toMap() }
            live.forEach { (view, swapped) ->
                swapped.guard.safe {
                    val resId = swapped.resId
                    val entry = b.entries.get(resId) ?: error("Issued view holds unmapped $resId")
                    view.post {
                        swapped.guard.safe {
                            view.setImageDrawable(render(entry, resId, view.context, b))
                        }
                    }
                }
            }
            log.debug { "Refreshed ${live.size} view(s), enabled=${state.enabled} ${state.snapshot()}" }
        }

        ctx.debug?.probe(id) {
            buildString {
                val b = binding
                appendLine(
                    "enabled=${state.enabled} trace=${state.trace} " +
                            "calls=${calls.get()} swaps=${swaps.get()}"
                )
                appendLine(
                    "mapped=${b.entries.size()}/${MOBILE_TYPE_LABELS.size + MOBILE_ICON_SWAPS.size} " +
                            "textSize=${state.textSize}dp density=${b.density} live=${
                                synchronized(
                                    issued
                                ) { issued.size }
                            }"
                )
                state.snapshot().forEach { (group, tune) -> appendLine("  ${group.cli}: $tune") }
                synchronized(issued) { issued.entries.map { it.key to it.value.resId } }
                    .groupingBy { (view, resId) ->
                        val path = generateSequence(view as View) { it.parent as? View }
                            .take(7)
                            .mapNotNull { runCatching { b.host.getResourceEntryName(it.id) }.getOrNull() }
                            .joinToString(" < ")
                        val entry = b.entries.get(resId)
                        val group =
                            if (entry?.label != null) IconGroup.TYPE.cli else entry?.swap?.group?.cli
                        "$group @ $path"
                    }
                    .eachCount()
                    .forEach { (place, count) -> appendLine("  x$count $place") }
                for (i in 0 until b.entries.size()) {
                    val entry = b.entries.valueAt(i)
                    val swap = entry.swap ?: continue
                    val box = entry.box ?: continue
                    val src = entry.src ?: continue
                    val fitted = b.cache.get(entry.moduleId).let {
                        FittedDrawable(
                            it,
                            box.width,
                            box.height,
                            src,
                            box.dst,
                            b.density,
                            state.tune(swap.group)
                        )
                    }
                    appendLine(
                        "${swap.host} box=${box.width}x${box.height} " +
                                "-> ${fitted.intrinsicWidth}x${fitted.intrinsicHeight} scale=${fitted.scale}  "
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun render(entry: Entry, resId: Int, context: Context, b: Binding): Drawable {
        if (!state.enabled) return context.hostDrawable(resId)
        entry.label?.let { label ->
            val box = entry.box(resId, context)
            return MobileTypeDrawable(
                label,
                b.typeface,
                state.textSize * b.density,
                box.width,
                box.height,
                b.signalWidth + state.tune(IconGroup.SIGNAL).padEnd * b.density,
                box.dst,
                b.density,
                state.tune(IconGroup.TYPE),
            )
        }
        val swap = entry.swap ?: error("Entry for $resId has neither label nor swap")
        val replacement = b.cache.get(entry.moduleId)
            .let { if (swap.alpha == OPAQUE) it else it.mutate().apply { this.alpha = swap.alpha } }
        if (swap.group.intrinsic) return replacement
        val box = entry.box(resId, context)
        val src = entry.src ?: replacement.contentBounds().also {
            entry.src = it
            log.debug { "Fit ${swap.host} box=${box.width}x${box.height} dst=${box.dst} -> ${swap.replacement} src=$it" }
        }
        return FittedDrawable(
            replacement,
            box.width,
            box.height,
            src,
            box.dst,
            b.density,
            state.tune(swap.group)
        )
    }

    private fun Entry.box(resId: Int, context: Context): Box = box
        ?: context.hostDrawable(resId)
            .let { Box(it.intrinsicWidth, it.intrinsicHeight, it.contentBounds()) }
            .also { box = it }

    private fun Context.hostDrawable(resId: Int): Drawable =
        getDrawable(resId) ?: error("Host drawable $resId missing")
}
