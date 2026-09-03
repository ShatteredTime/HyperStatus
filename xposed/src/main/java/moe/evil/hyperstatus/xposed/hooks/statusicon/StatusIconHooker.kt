package moe.evil.hyperstatus.xposed.hooks.statusicon

import android.content.res.Resources
import android.graphics.drawable.Icon
import android.util.SparseArray
import android.view.View
import com.android.internal.statusbar.StatusBarIcon
import com.android.systemui.statusbar.StatusBarIconView
import com.highcapable.kavaref.extension.classOf
import com.oplus.systemui.statusbar.phone.OplusPhoneStatusBarPolicyExImpl
import com.oplus.systemui.statusbar.phone.OplusStatusBarIcon
import com.oplus.systemui.statusbar.widget.StatIconView
import moe.evil.hyperstatus.shared.HOST_SYSTEMUI
import moe.evil.hyperstatus.shared.debugOnly
import moe.evil.hyperstatus.xposed.graphics.RowDrawable
import moe.evil.hyperstatus.xposed.model.BluetoothTransfer
import moe.evil.hyperstatus.xposed.model.StatusIconGroup
import moe.evil.hyperstatus.xposed.utils.DrawableCache
import moe.evil.hyperstatus.xposed.utils.Guard
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.moduleDrawable
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.util.Collections
import java.util.WeakHashMap

object StatusIconHooker : Hooker() {
    private class Plan(val group: StatusIconGroup, val assets: IntArray)

    private class Binding(val host: Resources, modules: Resources) {
        val plans = SparseArray<Plan>(STATUS_ICON_SWAPS.size).apply {
            STATUS_ICON_SWAPS.forEach { swap ->
                put(
                    host.hostId("drawable", swap.host),
                    Plan(
                        swap.group,
                        IntArray(swap.assets.size) { modules.moduleDrawable(swap.assets[it]) })
                )
            }
        }
        val batteryHosts =
            IntArray(HOST_BT_BATTERY_LEVELS) { host.hostId("drawable", hostBatteryIcon(it)) }
        val transferHost = host.hostId("drawable", BT_TRANSFER_HOST)
        val headphones: String = host.getString(host.hostId("string", HEADPHONES_DESCRIPTION))
        val cache = DrawableCache(modules)
    }

    private class Issued(val guard: Guard) {
        @Volatile
        var hostId = 0
    }

    internal val state = StatusIconState()

    private val issued = Collections.synchronizedMap(WeakHashMap<StatusBarIconView, Issued>())

    @Volatile
    private lateinit var binding: Binding

    @Volatile
    private var transfer = BluetoothTransfer.INOUT

    private val redrawTag = "$id.redraw"

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            val b = Binding(
                host.resources,
                ctx.moduleResources ?: error("Module resources unavailable")
            )
            binding = b
            log.info { "Bound ${b.plans.size()}/${STATUS_ICON_SWAPS.size} icon(s)" }
        }

        classOf<StatusBarIconView>()
            .hostMethod("StatusBarIconView#getIcon") {
                name = "getIcon"; parameters(classOf<StatusBarIcon>())
            }
            ?.safeIntercept(ctx, "$id.getIcon") { chain ->
                val b = binding
                val view = chain.thisObject as StatusBarIconView
                val icon = chain.getArg(0) as StatusBarIcon
                val hostId = icon.icon.hostId() ?: return@safeIntercept chain.proceed()
                val plan = b.plans.get(hostId) ?: return@safeIntercept chain.proceed()
                mark("getIcon")
                if (plan.group !in state.enabled) {
                    if (icon.preloadedIcon is RowDrawable) icon.preloadedIcon = null
                    return@safeIntercept chain.proceed()
                }
                val assets = when (plan.group) {
                    StatusIconGroup.HEADSET -> {
                        val mic = debugOnly { state.headsetMic }
                            ?: (icon.contentDescription?.toString() != b.headphones)
                        intArrayOf(plan.assets[if (mic) 0 else 1])
                    }

                    StatusIconGroup.BLUETOOTH -> when {
                        hostId == b.transferHost -> intArrayOf(plan.assets[transfer.ordinal])
                        else -> debugOnly { state.battery }
                            ?.let { b.plans.get(b.batteryHosts[it])?.assets }
                            ?: plan.assets
                    }

                    StatusIconGroup.RINGER, StatusIconGroup.ALARM, StatusIconGroup.LOCATION,
                    StatusIconGroup.VOLTE -> plan.assets
                }
                val drawable = RowDrawable(assets.map { b.cache.get(it) })
                issued.getOrPut(view) { Issued(Guard(log, redrawTag)) }.hostId = hostId
                mark("swapped")
                drawable
            }

        classOf<OplusPhoneStatusBarPolicyExImpl>()
            .hostMethod("OplusPhoneStatusBarPolicyExImpl#getBluetoothIconRes") {
                name = "getBluetoothIconRes"
                emptyParameters()
            }
            ?.safeIntercept(ctx, "$id.transfer") { chain ->
                mark("transfer")
                val policy = chain.thisObject as OplusPhoneStatusBarPolicyExImpl
                val next = when {
                    policy.bluetoothDownloading && policy.bluetoothUploading -> BluetoothTransfer.INOUT
                    policy.bluetoothDownloading -> BluetoothTransfer.IN
                    policy.bluetoothUploading -> BluetoothTransfer.OUT
                    else -> transfer
                }
                if (next != transfer) {
                    transfer = next
                    val host = binding.transferHost
                    refresh(synchronized(issued) { issued.filterValues { it.hostId == host } })
                }
                chain.proceed()
            }

        classOf<StatIconView>()
            .hostMethod("StatIconView#set") { name = "set"; parameters(classOf<StatusBarIcon>()) }
            ?.safeIntercept(ctx, "$id.set") { chain ->
                val icon = chain.getArg(0) as? OplusStatusBarIcon
                val group = icon?.icon?.hostId()?.let { binding.plans.get(it) }?.group
                if (icon != null && icon.foreDrawableResId != -1 && group != null && group in state.enabled) {
                    mark("foreDrawable")
                    icon.foreDrawableResId = -1
                }
                chain.proceed()
            }

        state.onChange = {
            val live = synchronized(issued) { issued.toMap() }
            refresh(live)
            log.debug {
                "Refreshed ${live.size} view(s), enabled=${state.enabled.map { it.cli }} " +
                        "battery=${state.battery} headsetMic=${state.headsetMic}"
            }
        }

        ctx.debug?.probe(id) {
            buildString {
                val b = binding
                fun name(res: Int) =
                    runCatching { b.host.getResourceEntryName(res) }.getOrNull() ?: res.toString()
                appendLine(
                    "enabled=${state.enabled.map { it.cli }} battery=${state.battery} headsetMic=${state.headsetMic} " +
                            "transfer=$transfer mapped=${b.plans.size()}/${STATUS_ICON_SWAPS.size}"
                )
                appendLine(hitsSummary())
                val live = synchronized(issued) { issued.entries.map { it.key to it.value.hostId } }
                appendLine("Views (${live.size}): ")
                live.forEach { (view, hostId) ->
                    val path = generateSequence(view as View) { it.parent as? View }
                        .take(6)
                        .mapNotNull { name(it.id).takeIf { name -> name != it.id.toString() } }
                        .joinToString(" < ")
                    appendLine(
                        "${view.getSlot()} shown=${view.isShown} host=${name(hostId)} " +
                                "drawable=${view.drawable?.intrinsicWidth}x${view.drawable?.intrinsicHeight} @ $path"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun Icon.hostId(): Int? =
        if (type == Icon.TYPE_RESOURCE && resPackage == HOST_SYSTEMUI) resId else null

    private fun refresh(views: Map<StatusBarIconView, Issued>) = views.forEach { (view, slot) ->
        view.post {
            slot.guard.safe {
                view.getStatusBarIcon()
                    ?.let { if (it.preloadedIcon is RowDrawable) it.preloadedIcon = null }
                view.updateDrawable(true)
            }
        }
    }
}
