package com.saavdhan.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saavdhan.app.R
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.system.deeplink.SettingsDeepLinks
import com.saavdhan.app.system.overlay.OverlayCoach
import com.saavdhan.app.ui.color
import com.saavdhan.app.ui.components.DangerButton
import com.saavdhan.app.ui.components.InfoCard
import com.saavdhan.app.ui.components.PrimaryButton
import com.saavdhan.app.ui.components.RiskChip
import com.saavdhan.app.ui.components.SecondaryButton
import com.saavdhan.app.ui.components.SignalRow
import com.saavdhan.app.ui.explanationRes
import com.saavdhan.app.ui.labelRes
import com.saavdhan.app.ui.scan.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: ScanViewModel,
    packageName: String,
    onBack: () -> Unit,
    onStartCleanup: () -> Unit
) {
    val item = viewModel.find(packageName)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.app?.label ?: stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            // No scan in memory for this package (e.g. process death restored us here directly).
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.detail_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val assessment = item.assessment
        // Coaching messages are built here (composable scope) and used inside click lambdas.
        val coachAccessibility = stringResource(R.string.coach_accessibility, item.app.label)
        val coachDeviceAdmin = stringResource(R.string.coach_device_admin, item.app.label)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RiskChip(
                text = stringResource(assessment.level.labelRes()),
                color = assessment.level.color()
            )
            Text(item.app.label, style = MaterialTheme.typography.headlineSmall)
            Text(
                item.app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (assessment.allowlisted) {
                Spacer(Modifier.height(8.dp))
                InfoCard(text = stringResource(R.string.detail_allowlisted_note))
            }

            // What this app could do
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.detail_what_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(assessment.level.explanationRes()), style = MaterialTheme.typography.bodyLarge)

            // Why it looks risky (the specific red flags)
            if (assessment.signals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.detail_why_title), style = MaterialTheme.typography.titleLarge)
                assessment.signals.forEach { signal ->
                    SignalRow(text = stringResource(signal.labelRes()))
                }
            }

            // Honest note about what Android lets us do
            Spacer(Modifier.height(12.dp))
            InfoCard(text = stringResource(R.string.honesty_note))

            // Optional on-screen helper: only offered when a guided action exists and the overlay
            // permission isn't granted yet.
            val hasGuidedAction = RiskSignal.ACCESSIBILITY in assessment.signals ||
                RiskSignal.DEVICE_ADMIN in assessment.signals
            if (hasGuidedAction && !OverlayCoach.isAvailable(context)) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.enable_helper),
                    onClick = { OverlayCoach.requestPermission(context) }
                )
                Text(
                    stringResource(R.string.enable_helper_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Guided cleanup — the headline action for a flagged app: a reactive, step-by-step
            // walk-through (isolate → strip powers → uninstall → secure accounts).
            if (assessment.level != RiskLevel.LOW) {
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = stringResource(R.string.start_cleanup),
                    onClick = onStartCleanup
                )
            }

            // Actions — the one-tap deep links
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.action_app_info),
                onClick = { SettingsDeepLinks.launch(context, SettingsDeepLinks.appInfo(packageName)) }
            )
            Text(
                stringResource(R.string.action_app_info_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (RiskSignal.ACCESSIBILITY in assessment.signals) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.action_accessibility),
                    onClick = {
                        OverlayCoach.show(context, coachAccessibility) // shows only if permission granted
                        SettingsDeepLinks.launch(context, SettingsDeepLinks.accessibilitySettings(), SettingsDeepLinks.mainSettings())
                    }
                )
                Text(
                    stringResource(R.string.action_accessibility_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (RiskSignal.DEVICE_ADMIN in assessment.signals) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(R.string.action_device_admin),
                    onClick = {
                        OverlayCoach.show(context, coachDeviceAdmin) // shows only if permission granted
                        SettingsDeepLinks.launch(context, SettingsDeepLinks.deviceAdminSettings(), SettingsDeepLinks.mainSettings())
                    }
                )
                Text(
                    stringResource(R.string.action_device_admin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            DangerButton(
                text = stringResource(R.string.action_uninstall),
                onClick = { SettingsDeepLinks.launch(context, SettingsDeepLinks.uninstall(packageName)) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
