package com.android.systemui.statusbar

import android.content.Context
import android.widget.ImageView
import com.android.internal.statusbar.StatusBarIcon

open class StatusBarIconView(context: Context) : ImageView(context) {

    fun getSlot(): String? = stub()

    fun getStatusBarIcon(): StatusBarIcon? = stub()

    fun updateDrawable(force: Boolean): Boolean = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
