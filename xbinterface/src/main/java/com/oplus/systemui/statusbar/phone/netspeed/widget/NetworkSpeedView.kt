package com.oplus.systemui.statusbar.phone.netspeed.widget

import android.content.Context
import android.graphics.Typeface
import android.widget.FrameLayout
import android.widget.TextView

class NetworkSpeedView(context: Context) : FrameLayout(context) {
    @JvmField
    val mSpeedNumber: TextView? = stub()

    @JvmField
    val mSpeedUnit: TextView? = stub()

    @JvmField
    val mDefaultBoldFont: Typeface = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
