package moe.evil.hyperstatus.xposed.utils

import android.view.View
import android.view.ViewGroup

inline fun <T : Any> View.ancestorValue(transform: (ViewGroup) -> T?): T? {
    var current = parent
    while (current is ViewGroup) {
        transform(current)?.let { return it }
        current = current.parent
    }
    return null
}
