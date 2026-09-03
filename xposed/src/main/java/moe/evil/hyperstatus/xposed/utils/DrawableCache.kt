package moe.evil.hyperstatus.xposed.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

class DrawableCache(private val res: Resources) {
    private val states = ConcurrentHashMap<Int, Drawable.ConstantState>()

    fun get(id: Int): Drawable =
        (states[id] ?: res.getDrawable(id, null)?.constantState?.also { states[id] = it })
            ?.newDrawable(res) ?: error("Module drawable $id unavailable")
}
