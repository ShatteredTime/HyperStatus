package moe.evil.hyperstatus.xposed.utils

import android.annotation.SuppressLint
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.makeAccessible
import moe.evil.hyperstatus.shared.log.HLog
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("DiscouragedPrivateApi")
object HostClassLoaderBridge {
    private val hostPrefixes = arrayOf("com.android.systemui.", "com.oplus.systemui.")

    private val installed = AtomicBoolean(false)

    private val log = HLog.of<HostClassLoaderBridge>()

    private val parentField: Field by lazy {
        classOf<ClassLoader>().getDeclaredField("parent").apply { makeAccessible() }
    }

    fun install(host: ClassLoader) {
        if (!installed.compareAndSet(false, true)) return
        val moduleLoader = javaClass.classLoader ?: run {
            installed.set(false)
            log.warn { "Not installed: module ClassLoader is null" }
            return
        }
        runCatching {
            val bridge = BridgeClassLoader(host, moduleLoader.parent, hostPrefixes)
            parentField.set(moduleLoader, bridge)
            log.info { "Installed, prefixes=${hostPrefixes.joinToString()}" }
        }.onFailure {
            installed.set(false)
            log.warn(it) { "Install failed, stub types stay unbridged" }
        }
    }

    private class BridgeClassLoader(
        private val host: ClassLoader,
        fallback: ClassLoader?,
        private val prefixes: Array<String>,
    ) : ClassLoader(fallback) {
        private val hostMiss = ConcurrentHashMap.newKeySet<String>()

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name !in hostMiss && prefixes.any(name::startsWith)) {
                runCatching { return host.loadClass(name) }.onFailure { hostMiss += name }
            }
            return super.loadClass(name, resolve)
        }
    }
}
