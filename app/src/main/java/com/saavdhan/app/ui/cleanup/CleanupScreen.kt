package com.saavdhan.app.ui.cleanup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saavdhan.app.R
import com.saavdhan.app.domain.cleanup.CleanupStep
import com.saavdhan.app.domain.cleanup.CleanupStepId
import com.saavdhan.app.domain.cleanup.StepStatus
import com.saavdhan.app.system.deeplink.SettingsDeepLinks
import com.saavdhan.app.system.overlay.OverlayCoach
import com.saavdhan.app.ui.components.InfoCard
import com.saavdhan.app.ui.components.SecondaryButton
import com.saavdhan.app.ui.theme.RiskCritical
import com.saavdhan.app.ui.theme.RiskHigh
import com.saavdhan.app.ui.theme.RiskLow

/**
 * The guided-cleanup checklist. It re-reads the phone's live state every time the screen resumes
 * (e.g. when the user comes back from a system Settings screen), so steps tick themselves off as
 * the user actually does them — no "mark complete" button to get wrong. The reactive logic lives
 * in the pure [com.saavdhan.app.domain.cleanup.CleanupEngine]; this file only renders it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    viewModel: CleanupViewModel,
    packageName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Start once for this package…
    LaunchedEffect(packageName) { viewModel.start(packageName) }

    // …and re-check on every ON_RESUME so returning from Settings updates the checklist.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val plan = viewModel.plan
    val label = viewModel.appLabel

    // Coach messages must be resolved in composable scope, then used inside click lambdas.
    val coachAccessibility = stringResource(R.string.coach_accessibility, label)
    val coachDeviceAdmin = stringResource(R.string.coach_device_admin, label)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cleanup_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (plan == null) return@Column // first frame, before the live state is read

            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.headlineSmall)
            }

            // Celebrate the moment the threat is gone, before the "secure your accounts" step.
            if (plan.threatRemoved) {
                Text(
                    stringResource(R.string.cleanup_done_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = RiskLow,
                )
                InfoCard(text = stringResource(R.string.cleanup_done_desc))
                Spacer(Modifier.height(4.dp))
            }

            // The reactive checklist.
            plan.steps.forEach { step ->
                StepCard(
                    step = step,
                    onAction = actionFor(step.id, packageName, context, coachAccessibility, coachDeviceAdmin),
                )
            }

            // The app resists normal removal (still installed + still Device Admin) → escalate.
            if (plan.showSafeModeEscalation) {
                Spacer(Modifier.height(4.dp))
                WarningCard(
                    title = stringResource(R.string.safe_mode_title),
                    body = stringResource(R.string.safe_mode_desc),
                )
            }

            // Quiet last-resort, only while the app is still on the phone.
            if (!plan.threatRemoved) {
                WarningCard(
                    title = stringResource(R.string.factory_reset_title),
                    body = stringResource(R.string.factory_reset_desc),
                    actionLabel = stringResource(R.string.factory_reset_action),
                    onAction = { SettingsDeepLinks.launch(context, SettingsDeepLinks.mainSettings()) },
                )
            }

            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = stringResource(R.string.cleanup_recheck),
                onClick = { viewModel.refresh() },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** One step row. DONE = compact + green check; CURRENT = expanded card with its action; PENDING = dimmed. */
@Composable
private fun StepCard(step: CleanupStep, onAction: (() -> Unit)?) {
    val title = stringResource(step.id.titleRes())
    when (step.status) {
        StepStatus.DONE -> StepHeader(
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RiskLow) },
            title = title,
            trailing = stringResource(R.string.status_done),
        )

        StepStatus.PENDING -> StepHeader(
            icon = {
                Icon(
                    Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            title = title,
            dimmed = true,
        )

        StepStatus.CURRENT -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.RadioButtonChecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.status_current),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(step.id.descRes()), style = MaterialTheme.typography.bodyLarge)
                onAction?.let {
                    val actionLabelRes = step.id.actionRes()
                    if (actionLabelRes != null) {
                        SecondaryButton(text = stringResource(actionLabelRes), onClick = it)
                    }
                }
                step.id.hintRes()?.let { hint ->
                    Text(
                        stringResource(hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Compact one-line row for DONE / PENDING steps. */
@Composable
private fun StepHeader(
    icon: @Composable () -> Unit,
    title: String,
    trailing: String? = null,
    dimmed: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = RiskLow,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** A prominent warning/escalation card (Safe Mode, factory reset). */
@Composable
private fun WarningCard(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskHigh.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Filled.RadioButtonChecked,
                    contentDescription = null,
                    tint = RiskHigh,
                    modifier = Modifier.size(20.dp),
                )
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Text(body, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null && onAction != null) {
                SecondaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

// --- Step → string/action mappings (kept here, in the UI layer) ---

private fun CleanupStepId.titleRes(): Int = when (this) {
    CleanupStepId.ISOLATE -> R.string.step_isolate_title
    CleanupStepId.DISABLE_ACCESSIBILITY -> R.string.step_accessibility_title
    CleanupStepId.REMOVE_ADMIN -> R.string.step_admin_title
    CleanupStepId.UNINSTALL -> R.string.step_uninstall_title
    CleanupStepId.SECURE_ACCOUNTS -> R.string.step_secure_title
}

private fun CleanupStepId.descRes(): Int = when (this) {
    CleanupStepId.ISOLATE -> R.string.step_isolate_desc
    CleanupStepId.DISABLE_ACCESSIBILITY -> R.string.step_accessibility_desc
    CleanupStepId.REMOVE_ADMIN -> R.string.step_admin_desc
    CleanupStepId.UNINSTALL -> R.string.step_uninstall_desc
    CleanupStepId.SECURE_ACCOUNTS -> R.string.step_secure_desc
}

/** The deep-link action button label, or null for steps the user does without leaving the app. */
private fun CleanupStepId.actionRes(): Int? = when (this) {
    CleanupStepId.ISOLATE -> R.string.step_isolate_action
    CleanupStepId.DISABLE_ACCESSIBILITY -> R.string.action_accessibility
    CleanupStepId.REMOVE_ADMIN -> R.string.action_device_admin
    CleanupStepId.UNINSTALL -> R.string.action_uninstall
    CleanupStepId.SECURE_ACCOUNTS -> null
}

private fun CleanupStepId.hintRes(): Int? = when (this) {
    CleanupStepId.ISOLATE -> R.string.step_isolate_hint
    else -> null
}

/** Builds the click action for a step's button (the one-tap deep link, plus the overlay coach). */
private fun actionFor(
    id: CleanupStepId,
    packageName: String,
    context: android.content.Context,
    coachAccessibility: String,
    coachDeviceAdmin: String,
): (() -> Unit)? = when (id) {
    CleanupStepId.ISOLATE -> {
        { SettingsDeepLinks.launch(context, SettingsDeepLinks.airplaneSettings()) }
    }
    CleanupStepId.DISABLE_ACCESSIBILITY -> {
        {
            OverlayCoach.show(context, coachAccessibility) // shows only if permission granted
            SettingsDeepLinks.launch(context, SettingsDeepLinks.accessibilitySettings())
        }
    }
    CleanupStepId.REMOVE_ADMIN -> {
        {
            OverlayCoach.show(context, coachDeviceAdmin)
            SettingsDeepLinks.launch(context, SettingsDeepLinks.deviceAdminSettings())
        }
    }
    CleanupStepId.UNINSTALL -> {
        { SettingsDeepLinks.launch(context, SettingsDeepLinks.uninstall(packageName)) }
    }
    CleanupStepId.SECURE_ACCOUNTS -> null
}
