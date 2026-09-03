package com.android.systemui.statusbar.pipeline.wifi.shared.model

abstract class WifiNetworkModel {

    class Active : WifiNetworkModel() {
        @JvmField
        val isValidated: Boolean = stub()

        @JvmField
        val level: Int = stub()
    }
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
