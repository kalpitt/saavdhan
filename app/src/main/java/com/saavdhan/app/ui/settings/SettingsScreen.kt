package com.saavdhan.app.ui.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saavdhan.app.R
import com.saavdhan.app.i18n.LocaleManager
import com.saavdhan.app.system.battery.BatteryStatus
import com.saavdhan.app.system.deeplink.SettingsDeepLinks
import com.saavdhan.app.system.watchdog.InstalledAppsSnapshot
import com.saavdhan.app.ui.components.InfoCard
import com.saavdhan.app.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: String,
    onChooseLanguage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val batteryExempt = remember { mutableStateOf(BatteryStatus.isExempt(context)) }
    val lastRunMillis = remember { mutableStateOf(InstalledAppsSnapshot.getLastRunMillis(context)) }

    DisposableEffect(Unit) {
        // Check battery status on every screen resume
        val onResume = {
            batteryExempt.value = BatteryStatus.isExempt(context)
            lastRunMillis.value = InstalledAppsSnapshot.getLastRunMillis(context)
        }
        onResume()
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleLarge)

            LanguageOption(
                label = stringResource(R.string.lang_english),
                selected = currentLanguage == LocaleManager.ENGLISH,
                onClick = { onChooseLanguage(LocaleManager.ENGLISH) },
            )
            LanguageOption(
                label = stringResource(R.string.lang_hindi),
                selected = currentLanguage == LocaleManager.HINDI,
                onClick = { onChooseLanguage(LocaleManager.HINDI) },
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.settings_protection_title), style = MaterialTheme.typography.titleLarge)
            BatteryOptimizationCard(
                isExempt = batteryExempt.value,
                lastRunMillis = lastRunMillis.value,
                context = context,
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleLarge)
            InfoCard(text = stringResource(R.string.settings_offline_note))
        }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BatteryOptimizationCard(isExempt: Boolean, lastRunMillis: Long, context: Context) {
    if (isExempt) {
        // Battery optimization is disabled; the app can run in the background.
        InfoCard(text = stringResource(R.string.battery_ok))
    } else {
        // Battery optimization is enabled; warn the user.
        Column {
            InfoCard(text = stringResource(R.string.battery_blocked))
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = stringResource(R.string.battery_fix_button),
                onClick = {
                    SettingsDeepLinks.launch(
                        context,
                        BatteryStatus.settingsIntent(),
                        SettingsDeepLinks.mainSettings(),
                    )
                },
            )
        }
    }

    // Last-check timestamp (shown regardless of exempt status)
    Spacer(Modifier.height(8.dp))
    val lastCheckText = if (lastRunMillis == 0L) {
        stringResource(R.string.watchdog_never_run)
    } else {
        val relative = DateUtils.getRelativeTimeSpanString(lastRunMillis)
        stringResource(R.string.watchdog_last_run, relative)
    }
    Text(
        lastCheckText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
