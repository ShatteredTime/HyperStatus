package moe.evil.hyperstatus.xposed.utils

import moe.evil.hyperstatus.shared.debugOnly
import moe.evil.hyperstatus.shared.log.HLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

abstract class Hooker {
    val id: String = javaClass.simpleName.removeSuffix("Hooker")

    protected val log = HLog(id)

    private val hits = ConcurrentHashMap<String, AtomicLong>()

    private val installGuard = Guard(log, "$id::onSystemUi")

    protected fun guard(tag: String) = Guard(log, "$id.$tag")

    fun install(ctx: HookContext) = installGuard.safe { safeOnSystemUi(ctx) }

    protected fun mark(key: String) {
        debugOnly {
            (hits[key]
                ?: hits.computeIfAbsent(key) { AtomicLong().also { log.debug { "First hit: $key" } } })
                .incrementAndGet()
        }
    }

    protected fun hitsSummary() =
        "Hits: " + hits.entries.joinToString { "${it.key}=${it.value.get()}" }.ifEmpty { "none" }

    protected abstract fun safeOnSystemUi(ctx: HookContext)
}
