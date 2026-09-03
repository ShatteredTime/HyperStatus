package com.android.systemui.common.shared.model

abstract class Icon {

    class Resource : Icon() {
        @JvmField
        val res: Int = stub()
    }
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
