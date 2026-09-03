package moe.evil.hyperstatus.xposed.hooks.statusicon

import android.content.res.Resources
import com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl
import com.highcapable.kavaref.extension.classOf
import moe.evil.hyperstatus.xposed.utils.HookContext
import moe.evil.hyperstatus.xposed.utils.Hooker
import moe.evil.hyperstatus.xposed.utils.hostId
import moe.evil.hyperstatus.xposed.utils.hostMethod
import moe.evil.hyperstatus.xposed.utils.safeIntercept

object StatusIconDebugHooker : Hooker() {
    private class Binding(host: Resources) {
        val slot: String = host.getString(host.hostId("string", "status_bar_volte"))
        val icons = VolteSlot.entries.associateWith {
            host.hostId("drawable", it.host) to host.getString(
                host.hostId(
                    "string",
                    it.description
                )
            )
        }
    }

    internal val state = StatusIconDebugState()

    @Volatile
    private lateinit var binding: Binding

    @Volatile
    private var controller: StatusBarIconControllerImpl? = null

    @Volatile
    private var applying = false

    private val applyGuard = guard("apply")

    override fun safeOnSystemUi(ctx: HookContext) {
        ctx.safeOnHostContext { host ->
            val b = Binding(host.resources)
            binding = b
            log.info { "Bound slot=${b.slot} icon(s)=${b.icons.size}/${VolteSlot.entries.size}" }
        }

        classOf<StatusBarIconControllerImpl>()
            .hostMethod("StatusBarIconControllerImpl#setIconVisibility") {
                name = "setIconVisibility"
                parameters(classOf<String>(), classOf<Boolean>())
            }
            ?.safeIntercept(ctx, "$id.visibility") { chain ->
                if (controller == null) {
                    controller = chain.thisObject as StatusBarIconControllerImpl
                }
                if (applying || state.volte == null) return@safeIntercept chain.proceed()
                if (chain.getArg(0) as String != binding.slot) return@safeIntercept chain.proceed()
                mark("kept")
                chain.proceed(chain.args.toTypedArray().also { it[1] = true })
            }

        state.onChange = applyGuard.wrap {
            val target = controller ?: error("Icon controller not observed yet")
            val b = binding
            val forced = state.volte
            applying = true
            try {
                target.setIconVisibility(b.slot, false)
                forced?.let {
                    val (resId, description) = b.icons.getValue(it)
                    target.setIcon(description, b.slot, resId)
                    target.setIconVisibility(b.slot, true)
                }
            } finally {
                applying = false
            }
            log.debug { "Forced volte=${forced?.cli}" }
        }

        ctx.debug?.probe(id) {
            buildString {
                val b = binding
                appendLine(
                    "volte=${state.volte?.cli} slot=${b.slot} controller=${controller != null}"
                )
                appendLine(hitsSummary())
                appendLine("Variants (${b.icons.size}):")
                b.icons.forEach { (variant, icon) ->
                    appendLine("  ${variant.cli} ${variant.host}=${icon.first} '${icon.second}'")
                }
            }
        }
        log.debug { "Installed" }
    }
}
