package moe.evil.hyperstatus.xposed.hooks.mobilesignal

import android.graphics.drawable.Drawable
import android.telephony.ServiceState
import android.telephony.TelephonyDisplayInfo
import android.view.View
import android.widget.ImageView
import com.android.systemui.statusbar.pipeline.mobile.domain.model.SignalIconModel
import com.highcapable.kavaref.extension.classOf
import com.oplus.systemui.statusbar.pipeline.OplusMobileSignalExImpl
import io.github.libxposed.api.XposedInterface
import moe.evil.hyperstatus.shared.HOST_SYSTEMUI
import moe.evil.hyperstatus.xposed.model.MOBILE_VIEW_BINDERS
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.ancestorValue
import moe.evil.hyperstatus.xposed.utils.hostClassOrNull
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.safeIntercept
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

object MobileSignalDebugHooker : Hooker() {
    private class SignalBinding(
        val update: Method,
        val typeView: ImageView?,
        val activityView: ImageView?,
    ) {
        @Volatile
        var model: SignalIconModel.Cellular? = null

        @Volatile
        var realType: Drawable? = null

        @Volatile
        var realActivity: Int? = null
    }

    private class RoamBinding(val update: Method, val space: View) {
        @Volatile
        var real = 0
    }

    internal val state = MobileSignalDebugState()
    private val applyGuard = guard("apply")
    private val nudgeGuard = guard("nudge")

    private val signalBindings =
        Collections.synchronizedMap(WeakHashMap<ImageView, SignalBinding>())

    private val roamBindings = Collections.synchronizedMap(WeakHashMap<ImageView, RoamBinding>())

    private val hostIds = ConcurrentHashMap<String, Int>()

    private fun hostId(ctx: HookContext, type: String, name: String): Int? =
        hostIds["$type/$name"]
            ?: ctx.hostContext?.resources?.getIdentifier(name, type, HOST_SYSTEMUI)
                ?.takeIf { it != 0 }
                ?.also { hostIds["$type/$name"] = it }

    override fun safeOnSystemUi(ctx: HookContext) {
        classOf<OplusMobileSignalExImpl>()
            .hostMethod("OplusMobileSignalExImpl#getCurrentIconId") {
                name = "getCurrentIconId"
                parameterCount = 7
            }?.let { method ->
                val types = method.parameterTypes
                val ints = types.indices.filter { types[it] == classOf<Int>() }
                val flags = types.indices.filter { types[it] == classOf<Boolean>() }
                if (ints.size != 3 || flags.size != 4) error("Unexpected getCurrentIconId signature ${types.contentToString()}")
                val (slot, level, inet) = ints
                val (inService, softSim, roaming, ims) = flags
                method.safeIntercept(ctx, "$id.currentIcon") { chain ->
                    mark("getCurrentIconId")
                    val ov =
                        state.of(chain.getArg(slot) as Int) ?: return@safeIntercept chain.proceed()
                    val args = chain.args.toTypedArray()
                    ov.level?.let { args[level] = it }
                    ov.inetCondition?.let { args[inet] = it }
                    ov.inService?.let { args[inService] = it }
                    ov.softSim?.let { args[softSim] = it }
                    ov.roaming?.let { args[roaming] = it }
                    ov.ims?.let { args[ims] = it }
                    chain.proceed(args)
                }
            }

        classOf<OplusMobileSignalExImpl>()
            .hostMethod("OplusMobileSignalExImpl#getIconKeyEx") {
                name = "getIconKeyEx"
                parameters(
                    classOf<Int>(),
                    classOf<TelephonyDisplayInfo>(),
                    classOf<ServiceState>(),
                    classOf<Int>()
                )
            }
            ?.safeIntercept(ctx, "$id.iconKey") { chain ->
                mark("getIconKeyEx")
                state.of(chain.getArg(0) as Int)?.rat?.iconKey ?: chain.proceed()
            }

        MOBILE_VIEW_BINDERS.forEach { fqn ->
            val binder = hostClassOrNull(fqn, ctx.classLoader) ?: return@forEach
            binder.hostMethod("$fqn#updateSignalIcon", optional = true) {
                name { it.endsWith("updateSignalIcon") }
                parameterCount = 2
            }?.let { update ->
                update.safeIntercept(ctx, "$id.updateSignalIcon") { chain ->
                    mark("updateSignalIcon")
                    chain.proceed().also {
                        applyGuard.safe {
                            val view = chain.getArg(0) as ImageView
                            val model = chain.getArg(1) as SignalIconModel.Cellular
                            val binding = signalBindings.getOrPut(view) {
                                SignalBinding(
                                    update,
                                    sibling(ctx, view, "mobile_type"),
                                    sibling(ctx, view, "data_inout"),
                                )
                            }
                            binding.model = model
                            val override = state.of(model.phoneId)
                            binding.typeView?.let { typeView ->
                                val forced =
                                    override?.rat?.let { hostId(ctx, "drawable", it.dataTypeIcon) }
                                when {
                                    forced != null -> {
                                        if (binding.realType == null) binding.realType =
                                            typeView.drawable
                                        typeView.setImageResource(forced)
                                    }

                                    binding.realType != null -> {
                                        typeView.setImageDrawable(binding.realType)
                                        binding.realType = null
                                    }
                                }
                            }
                            val activityView = binding.activityView ?: return@safe
                            val forced =
                                override?.activity?.let { hostId(ctx, "drawable", it.icon) }
                            when {
                                forced != null -> {
                                    if (binding.realActivity == null) binding.realActivity =
                                        activityView.visibility
                                    activityView.setImageResource(forced)
                                    activityView.visibility = View.VISIBLE
                                }

                                binding.realActivity != null -> {
                                    activityView.visibility = binding.realActivity ?: View.GONE
                                    binding.realActivity = null
                                }
                            }
                        }
                    }
                }
            }
            binder.hostMethod("$fqn#updateMobileRoaming", optional = true) {
                name { it.endsWith("updateMobileRoaming") }
                parameterCount = 3
            }?.let { update ->
                update.safeIntercept(
                    ctx,
                    "$id.updateMobileRoaming",
                    XposedInterface.PRIORITY_HIGHEST
                ) { chain ->
                    mark("updateMobileRoaming")
                    val roaming = chain.getArg(0) as ImageView
                    roamBindings.getOrPut(roaming) { RoamBinding(update, chain.getArg(1) as View) }
                        .real = chain.getArg(2) as Int
                    if (state.isEmpty) return@safeIntercept chain.proceed()
                    val forced = forcedRoam(ctx, roaming) ?: return@safeIntercept chain.proceed()
                    chain.proceed(chain.args.toTypedArray().also { it[2] = forced })
                }
            }
        }

        state.onChange = {
            val signals = synchronized(signalBindings) { signalBindings.toMap() }
            signals.forEach { (view, b) ->
                val model = b.model ?: return@forEach
                view.post { nudgeGuard.safe { b.update.invoke(null, view, model) } }
            }
            val roams = synchronized(roamBindings) { roamBindings.toMap() }
            roams.forEach { (view, b) ->
                view.post { nudgeGuard.safe { b.update.invoke(null, view, b.space, b.real) } }
            }
            log.debug { "Nudged ${signals.size} signal + ${roams.size} roam view(s), overrides=${state.snapshot()}" }
        }

        ctx.debug?.probe(id) {
            buildString {
                val snapshot = synchronized(signalBindings) { signalBindings.toMap() }
                val roams = synchronized(roamBindings) { roamBindings.toMap() }
                val slots = snapshot.values.mapNotNull { it.model?.phoneId }.distinct().sorted()
                appendLine("Overrides: ${state.snapshot()}")
                appendLine("Live slots (use as --card): $slots")
                appendLine(hitsSummary())
                appendLine("Bindings (${snapshot.size} signal, ${roams.size} roam):")
                snapshot.forEach { (view, b) ->
                    val name = runCatching {
                        ctx.hostContext?.resources?.getResourceEntryName(view.id)
                    }.getOrNull() ?: view.id.toString()
                    appendLine(
                        "  slot=${b.model?.phoneId} $name@${System.identityHashCode(view)} " +
                                "shown=${view.isShown} type=${b.typeView != null} saved=${b.realType != null}"
                    )
                }
                roams.forEach { (view, b) ->
                    appendLine(
                        "  roam@${System.identityHashCode(view)} real=${b.real} forced=${
                            forcedRoam(
                                ctx,
                                view
                            )
                        }"
                    )
                }
            }
        }
        log.debug { "Installed" }
    }

    private fun forcedRoam(ctx: HookContext, roaming: ImageView): Int? {
        val signal = sibling(ctx, roaming, "mobile_signal") ?: return null
        val slot = signalBindings[signal]?.model?.phoneId ?: return null
        val ov = state.of(slot) ?: return null
        val roam = ov.roaming ?: return null
        if (!roam) return 0
        return hostId(
            ctx,
            "drawable",
            if (ov.softSim == true) "stat_signal_soft_roma_lte" else "stat_signal_roma_lte"
        )
    }

    private fun sibling(ctx: HookContext, view: ImageView, name: String): ImageView? {
        val id = hostId(ctx, "id", name) ?: return null
        return view.ancestorValue { it.findViewById(id) }
    }
}
