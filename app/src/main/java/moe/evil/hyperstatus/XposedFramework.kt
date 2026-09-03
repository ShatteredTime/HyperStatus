package moe.evil.hyperstatus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object XposedFramework {
    var service: XposedService? by mutableStateOf(null)
        private set

    init {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                XposedFramework.service = service
            }

            override fun onServiceDied(service: XposedService) {
                if (XposedFramework.service === service) XposedFramework.service = null
            }
        })
    }
}
