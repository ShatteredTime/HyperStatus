package com.oplus.systemui.statusbar.phone

import com.android.internal.statusbar.StatusBarIcon

class OplusStatusBarIcon : StatusBarIcon() {
    @JvmField
    var foreDrawableResId: Int = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
