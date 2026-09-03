package com.android.systemui.statusbar.pipeline.mobile.domain.model

interface SignalIconModel {

    class Cellular : SignalIconModel {
        @JvmField
        val phoneId: Int = stub()
    }
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
