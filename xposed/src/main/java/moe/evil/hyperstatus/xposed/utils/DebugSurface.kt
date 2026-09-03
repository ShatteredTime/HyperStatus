package moe.evil.hyperstatus.xposed.utils

import android.content.Context

interface DebugSurface {
    fun attach(context: Context)

    fun probe(name: String, provider: () -> String)
}
