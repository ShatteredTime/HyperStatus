package com.oplus.systemui.statusbar.pipeline.battery.ui.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable

class HorizontalBatteryContentDrawable : Drawable() {

    fun setTextTypeface(typeface: Typeface?): Unit = stub()

    override fun draw(canvas: Canvas): Unit = stub()

    override fun setAlpha(alpha: Int): Unit = stub()

    override fun setColorFilter(colorFilter: ColorFilter?): Unit = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = stub()
}

private fun stub(): Nothing =
    throw NotImplementedError("Xbinterface stub; HostClassLoaderBridge not installed")
