package moe.evil.hyperstatus.xposed.graphics

import android.content.res.Resources
import android.graphics.Typeface
import moe.evil.hyperstatus.shared.log.HLog
import java.util.concurrent.ConcurrentHashMap

private const val MISANS_ASSET = "fonts/MiSansVF-statusbar.ttf"
const val MISANS_MIN_WEIGHT = 150
const val MISANS_MAX_WEIGHT = 700
const val MISANS_STATUS_WEIGHT = 660

object MiSans {
    private val log = HLog.of<MiSans>()
    private val cache = ConcurrentHashMap<Int, Typeface>()

    fun of(modules: Resources, weight: Int = MISANS_STATUS_WEIGHT): Typeface =
        cache.getOrPut(weight) {
            runCatching {
                Typeface.Builder(modules.assets, MISANS_ASSET)
                    .setFontVariationSettings("'wght' $weight")
                    .build()
            }.getOrNull() ?: Typeface.create(Typeface.DEFAULT, weight, false).also {
                log.error { "MiSans not loaded from $MISANS_ASSET, falling back to system weight $weight" }
            }
        }
}
