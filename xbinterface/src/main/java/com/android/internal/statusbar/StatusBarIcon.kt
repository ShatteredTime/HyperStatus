package com.android.internal.statusbar

import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon

open class StatusBarIcon {
    @JvmField
    val icon: Icon = stub()

    @JvmField
    val contentDescription: CharSequence? = stub()

    @JvmField
    var preloadedIcon: Drawable? = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
