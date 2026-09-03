package com.android.systemui.statusbar.phone.ui

class StatusBarIconControllerImpl {
    fun setIcon(contentDescription: CharSequence, slot: String, resId: Int): Unit = stub()

    fun setIconVisibility(slot: String, visible: Boolean): Unit = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
