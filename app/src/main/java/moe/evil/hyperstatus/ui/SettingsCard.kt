package moe.evil.hyperstatus.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import moe.evil.hyperstatus.BuildConfig
import moe.evil.hyperstatus.R
import moe.evil.hyperstatus.shared.PREFS_GROUP
import moe.evil.hyperstatus.shared.PREF_DEBUG_CHANNEL
import moe.evil.hyperstatus.shared.PREF_DEDUPE_ERRORS
import moe.evil.hyperstatus.shared.PREF_LOG_LEVEL
import moe.evil.hyperstatus.shared.debugChannel
import moe.evil.hyperstatus.shared.dedupeErrors
import moe.evil.hyperstatus.shared.log.LogLevel
import moe.evil.hyperstatus.shared.logLevel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

private val LOG_LEVEL_NAMES = LogLevel.entries.map { it.name }

@Composable
fun SettingsCard(service: XposedService?) {
    val packageManager = LocalContext.current.packageManager
    val launcher = remember {
        ComponentName(
            BuildConfig.APPLICATION_ID,
            "${BuildConfig.APPLICATION_ID}.Launcher"
        )
    }
    var iconHidden by remember {
        mutableStateOf(packageManager.getComponentEnabledSetting(launcher) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }
    val prefs =
        remember(service) { service?.let { runCatching { it.getRemotePreferences(PREFS_GROUP) }.getOrNull() } }
    var logLevel by remember(prefs) { mutableStateOf(prefs.logLevel) }
    var dedupeErrors by remember(prefs) { mutableStateOf(prefs.dedupeErrors) }
    var debugChannel by remember(prefs) { mutableStateOf(prefs.debugChannel) }
    Card(modifier = Modifier.fillMaxWidth()) {
        OverlayDropdownPreference(
            items = LOG_LEVEL_NAMES,
            selectedIndex = logLevel.ordinal,
            title = stringResource(R.string.log_level),
            summary = stringResource(R.string.log_level_summary),
            enabled = prefs != null,
            onSelectedIndexChange = { index ->
                logLevel = LogLevel.entries[index]
                prefs?.edit { putString(PREF_LOG_LEVEL, logLevel.name) }
            },
        )
        SwitchPreference(
            checked = iconHidden,
            onCheckedChange = { hide ->
                packageManager.setComponentEnabledSetting(
                    launcher,
                    if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                )
                iconHidden = hide
            },
            title = stringResource(R.string.hide_launcher_icon),
        )
        SwitchPreference(
            checked = dedupeErrors,
            onCheckedChange = { enabled ->
                dedupeErrors = enabled
                prefs?.edit { putBoolean(PREF_DEDUPE_ERRORS, enabled) }
            },
            title = stringResource(R.string.dedupe_errors),
            summary = stringResource(R.string.dedupe_errors_summary),
            enabled = prefs != null,
        )
        if (BuildConfig.DEBUG) {
            SwitchPreference(
                checked = debugChannel,
                onCheckedChange = { enabled ->
                    debugChannel = enabled
                    prefs?.edit { putBoolean(PREF_DEBUG_CHANNEL, enabled) }
                },
                title = stringResource(R.string.debug_channel),
                summary = stringResource(R.string.debug_channel_summary),
                enabled = prefs != null,
            )
        }
    }
}
