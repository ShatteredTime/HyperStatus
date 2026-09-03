package moe.evil.hyperstatus.xposed.utils

import android.content.Context
import android.content.res.Resources
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.CopyOnWriteArrayList

class HookContext(
    val xposed: XposedInterface,
    val classLoader: ClassLoader,
    val debug: DebugSurface?,
) {
    @Volatile
    var hostContext: Context? = null
        private set

    @Volatile
    var moduleResources: Resources? = null
        private set

    private val pending = CopyOnWriteArrayList<(Context) -> Unit>()

    fun safeOnHostContext(block: (Context) -> Unit) {
        val guarded: (Context) -> Unit = { host -> safe("onHostContext") { block(host) } }
        val existing = hostContext
        if (existing != null) guarded(existing) else pending += guarded
    }

    internal fun bindHostContext(app: Context) {
        if (hostContext != null) return
        hostContext = app
        moduleResources = runCatching {
            app.packageManager.getResourcesForApplication(xposed.moduleApplicationInfo)
        }.getOrNull()
        pending.forEach { it(app) }
        pending.clear()
        debug?.attach(app)
    }
}
