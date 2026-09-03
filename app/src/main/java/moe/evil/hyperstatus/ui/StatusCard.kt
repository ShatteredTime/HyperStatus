package moe.evil.hyperstatus.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.libxposed.service.XposedService
import moe.evil.hyperstatus.BuildConfig
import moe.evil.hyperstatus.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun StatusCard(service: XposedService?) {
    val framework = remember(service) {
        service?.let { runCatching { it.frameworkName to it.apiVersion }.getOrNull() }
    }
    if (framework == null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            BasicComponent(
                title = stringResource(R.string.status_not_activated),
                summary = stringResource(R.string.status_not_activated_summary),
                startAction = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_error_outline),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onBackground,
                    )
                },
            )
        }
        return
    }
    val (name, api) = framework
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        colors = CardDefaults.defaultColors(
            color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFAE4),
        ),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_outline),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp),
                    tint = Color(0xFF36D167),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 10.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = stringResource(R.string.status_framework, name, api),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 14.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.status_activated),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(
                            R.string.status_version,
                            "${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE.uppercase()})",
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
