package com.tolstoy.mitablaturas.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tolstoy.mitablaturas.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Componente de terceros que se distribuye con la app. */
private data class ThirdPartyNotice(
    val name: String,
    @StringRes val role: Int,
    val license: String,
    /** Texto completo de la licencia dentro de `assets/`, o `null` si basta con el enlace. */
    val assetPath: String?,
    val url: String,
)

private val notices = listOf(
    ThirdPartyNotice(
        name = "AlphaTab",
        role = R.string.licenses_role_alphatab,
        license = "Mozilla Public License 2.0",
        assetPath = "alphatab/LICENSE-alphaTab.txt",
        url = "https://github.com/CoderLine/alphaTab",
    ),
    ThirdPartyNotice(
        name = "Bravura",
        role = R.string.licenses_role_bravura,
        license = "SIL Open Font License 1.1",
        assetPath = "alphatab/font/Bravura-OFL.txt",
        url = "https://github.com/steinbergmedia/bravura",
    ),
    ThirdPartyNotice(
        name = "Sonivox SoundFont",
        role = R.string.licenses_role_sonivox,
        license = "Apache License 2.0",
        assetPath = "alphatab/soundfont/LICENSE-sonivox.txt",
        url = "https://android.googlesource.com/platform/external/sonivox",
    ),
    ThirdPartyNotice(
        name = "Android Jetpack, Jetpack Compose, Kotlin, kotlinx.coroutines, Dagger Hilt",
        role = R.string.licenses_role_platform,
        license = "Apache License 2.0",
        assetPath = null,
        url = "https://www.apache.org/licenses/LICENSE-2.0",
    ),
)

@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { context.versionName() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.licenses_back)) }
                Text(
                    text = stringResource(R.string.licenses_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.licenses_app_version, stringResource(R.string.app_name), version),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(notices, key = { it.name }) { notice -> NoticeCard(notice) }
    }
}

@Composable
private fun NoticeCard(notice: ThirdPartyNotice) {
    val context = LocalContext.current
    var expanded by rememberSaveable(notice.name) { mutableStateOf(false) }
    var text by remember(notice.name) { mutableStateOf<String?>(null) }
    if (expanded && notice.assetPath != null) {
        LaunchedEffect(notice.assetPath) { text = context.readAsset(notice.assetPath) }
    }
    Surface(
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(notice.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(notice.role), style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${notice.license} · ${notice.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                Text(
                    text = text ?: stringResource(R.string.licenses_see_link),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else if (notice.assetPath != null) {
                Text(
                    text = stringResource(R.string.licenses_show_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private suspend fun Context.readAsset(path: String): String? = withContext(Dispatchers.IO) {
    try {
        assets.open(path).bufferedReader().use { it.readText() }
    } catch (_: IOException) {
        null
    }
}

private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
