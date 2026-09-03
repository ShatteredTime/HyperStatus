package moe.evil.hyperstatus.xposed.utils

import android.content.res.Resources
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.FieldCondition
import com.highcapable.kavaref.condition.MethodCondition
import com.highcapable.kavaref.extension.toClassOrNull
import moe.evil.hyperstatus.shared.HOST_SYSTEMUI
import moe.evil.hyperstatus.shared.MODULE_PACKAGE
import moe.evil.hyperstatus.shared.log.HLog
import java.lang.reflect.Field
import java.lang.reflect.Method

private val log = HLog("HostRef")

fun hostClassOrNull(name: String, loader: ClassLoader): Class<*>? =
    name.toClassOrNull(loader) ?: null.also { log.error { "Class not found: $name" } }

@Suppress("UNCHECKED_CAST")
fun Class<*>.hostMethod(
    label: String,
    optional: Boolean = false,
    condition: MethodCondition<Any>.() -> Unit,
): Method? =
    (this as Class<Any>).resolve().optional(silent = true).firstMethodOrNull(condition)?.self
        ?: null.also {
            if (optional) log.warn { "Method not found: $label" }
            else log.error { "Method not found: $label" }
        }

@Suppress("UNCHECKED_CAST")
fun Class<*>.hostField(label: String, condition: FieldCondition<Any>.() -> Unit): Field? =
    (this as Class<Any>).resolve().optional(silent = true).firstFieldOrNull(condition)?.self
        ?: null.also { log.error { "Field not found: $label" } }

fun Resources.hostIdOrNull(type: String, name: String): Int? =
    getIdentifier(name, type, HOST_SYSTEMUI).takeIf { it != 0 }

fun Resources.hostId(type: String, name: String): Int =
    hostIdOrNull(type, name) ?: error("Unresolved $type/$name")

fun Resources.moduleDrawable(name: String): Int =
    getIdentifier(
        name,
        "drawable",
        MODULE_PACKAGE
    ).also { if (it == 0) error("Module drawable $name missing") }
