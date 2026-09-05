package moe.evil.hyperstatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import moe.evil.hyperstatus.ui.AboutScreen
import moe.evil.hyperstatus.ui.HomeScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        setContent {
            MiuixTheme(controller = remember { ThemeController() }) {
                var about by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = about) { about = false }
                AnimatedContent(
                    targetState = about,
                    transitionSpec = {
                        val slide = tween<IntOffset>(350, easing = FastOutSlowInEasing)
                        val fade = tween<Float>(350, easing = FastOutSlowInEasing)
                        if (targetState) {
                            slideInHorizontally(slide) { it } togetherWith
                                    slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade)
                        } else {
                            slideInHorizontally(slide) { -it / 4 } + fadeIn(fade) togetherWith
                                    slideOutHorizontally(slide) { it }
                        }
                    },
                ) { showAbout ->
                    if (showAbout) {
                        AboutScreen(onBack = { about = false })
                    } else {
                        HomeScreen(XposedFramework.service, onAboutClick = { about = true })
                    }
                }
            }
        }
    }
}
