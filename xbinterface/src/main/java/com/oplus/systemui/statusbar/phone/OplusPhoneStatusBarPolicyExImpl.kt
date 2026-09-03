package com.oplus.systemui.statusbar.phone

class OplusPhoneStatusBarPolicyExImpl {
    @JvmField
    val bluetoothDownloading: Boolean = stub()

    @JvmField
    val bluetoothUploading: Boolean = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
