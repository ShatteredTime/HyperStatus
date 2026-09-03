package moe.evil.hyperstatus.xposed.utils

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedInterface.HookHandle
import moe.evil.hyperstatus.shared.MODULE_PACKAGE
import moe.evil.hyperstatus.shared.log.HLog
import java.io.Serializable
import java.lang.reflect.Executable

@PublishedApi
internal val safeHookLog = HLog("SafeHook")

class Guard(private val log: HLog, private val tag: String) {
    companion object {
        @Volatile
        var dedupe = true
    }

    private val reported = HashSet<Serializable>()

    inline fun safe(block: () -> Unit) {
        runCatching(block).onFailure { report(it) }
    }

    fun wrap(block: () -> Unit): () -> Unit = { safe(block) }

    @PublishedApi
    @Synchronized
    internal fun report(error: Throwable) {
        if (!dedupe) return log.error(error) { "Failed in $tag" }
        val trace = error.stackTrace
        val site = trace.firstOrNull { it.className.startsWith(MODULE_PACKAGE) }
            ?: trace.firstOrNull() ?: error.javaClass
        if (reported.add(site)) {
            log.error(error) { "Failed in $tag at $site, muting repeats" }
        }
    }
}

fun Executable.safeIntercept(
    ctx: HookContext,
    id: String,
    priority: Int = XposedInterface.PRIORITY_DEFAULT,
    body: (Chain) -> Any?,
): HookHandle? =
    runCatching {
        val guard = Guard(safeHookLog, "$id interceptor, proceeding unhooked")
        ctx.xposed.hook(this)
            .setId(id)
            .setPriority(priority)
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                runCatching { body(chain) }.getOrElse {
                    guard.report(it)
                    chain.proceed()
                }
            }
    }.getOrElse {
        safeHookLog.error(it) { "Failed to install hook $id" }
        null
    }

inline fun safe(tag: String, block: () -> Unit) {
    runCatching(block).onFailure { safeHookLog.error(it) { "Threw in $tag" } }
}
