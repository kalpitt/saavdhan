package com.saavdhan.app.ui.scan

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.saavdhan.app.R
import com.saavdhan.app.data.scanner.AssessedApp
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.ui.color
import com.saavdhan.app.ui.components.InfoCard
import com.saavdhan.app.ui.components.PrimaryButton
import com.saavdhan.app.ui.components.RiskChip
import com.saavdhan.app.ui.components.SecondaryButton
import com.saavdhan.app.ui.labelRes
import com.saavdhan.app.ui.onColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        }
    ) { padding ->
        when (val state = viewModel.state) {
            is ScanState.Idle -> IdleContent(padding, onScan = viewModel::scan)
            is ScanState.Scanning -> ScanningContent(padding)
            is ScanState.Done -> ResultsContent(
                padding = padding,
                apps = state.result.apps,
                partial = state.result.partial,
                onAppClick = onAppClick,
                onScanAgain = viewModel::scan
            )
            is ScanState.Error -> ErrorContent(padding, onRetry = viewModel::scan)
        }
    }
}

@Composable
private fun IdleContent(padding: PaddingValues, onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = stringResource(R.string.scan_button), onClick = onScan)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.home_offline_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScanningContent(padding: PaddingValues) {
    // After ~8s, add a calm "this can take a minute" line so a slow scan never feels broken.
    var showSlowNote by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(8_000)
        showSlowNote = true
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.scanning), style = MaterialTheme.typography.bodyLarge)
        if (showSlowNote) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.scan_slow_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(padding: PaddingValues, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.scan_error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scan_error_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = stringResource(R.string.scan_again), onClick = onRetry)
    }
}

@Composable
private fun ResultsContent(
    padding: PaddingValues,
    apps: List<AssessedApp>,
    partial: Boolean,
    onAppClick: (String) -> Unit,
    onScanAgain: () -> Unit
) {
    val allFlagged = apps.filter { it.assessment.level != RiskLevel.LOW }
    val serious = allFlagged.filter { it.assessment.level == RiskLevel.CRITICAL || it.assessment.level == RiskLevel.HIGH }
    val mild = allFlagged.filter { it.assessment.level == RiskLevel.SUSPICIOUS }
    val worstLevel = allFlagged.maxByOrNull { it.assessment.level.ordinal }?.assessment?.level
        ?: RiskLevel.SUSPICIOUS

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (partial) {
            item { InfoCard(text = stringResource(R.string.partial_scan_note)) }
        }

        if (serious.isEmpty() && mild.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null, // decorative — the title below says it
                        tint = RiskLevel.LOW.color(),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.result_safe_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.result_safe_body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item { PhoneHealthBar(worst = worstLevel) }
            if (serious.isNotEmpty()) {
                item {
                    val title = if (serious.size == 1) {
                        stringResource(R.string.result_found_title, serious.size)
                    } else {
                        stringResource(R.string.result_found_title_plural, serious.size)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = RiskLevel.CRITICAL.color())
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                }
                items(serious, key = { it.app.packageName }) { item ->
                    AppRiskCard(item, onClick = { onAppClick(item.app.packageName) })
                }
            }

            if (mild.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.result_mild_section), style = MaterialTheme.typography.titleLarge)
                }
                items(mild, key = { it.app.packageName }) { item ->
                    AppRiskCard(item, onClick = { onAppClick(item.app.packageName) })
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = stringResource(R.string.scan_again), onClick = onScanAgain)

            // Offline "send to family": hand the worried adult child who set Saavdhan up a plain
            // summary they can read, via the phone's own share sheet (WhatsApp/SMS). No network.
            val context = LocalContext.current
            val shareLabel = stringResource(R.string.action_share_result)
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = shareLabel,
                onClick = {
                    val appLines = allFlagged.map { flagged ->
                        context.getString(
                            R.string.share_app_line,
                            flagged.app.label,
                            context.getString(flagged.assessment.level.labelRes())
                        )
                    }
                    val report = buildFamilyReport(
                        intro = context.getString(R.string.share_intro),
                        appLines = appLines,
                        safeLine = context.getString(R.string.share_safe),
                        foundHeader = context.getString(R.string.share_found_header),
                        advice = context.getString(R.string.share_advice),
                        footer = context.getString(R.string.share_footer)
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    try {
                        context.startActivity(Intent.createChooser(send, shareLabel))
                    } catch (e: ActivityNotFoundException) {
                        // No app can receive shared text (rare — e.g. a locked-down profile). Nothing
                        // to share to, so fail quietly rather than crash.
                    }
                }
            )
        }
    }
}

/**
 * A glanceable phone-health meter: four segments, green→red, filled up to the worst risk found, with
 * the verdict word beneath it in that colour. Answers "how bad is my phone, overall?" in one look,
 * above the card list — for a non-technical user who shouldn't have to count cards to feel the stakes.
 * Reuses the existing risk colours and labels; the bar is decorative (the word carries the meaning to
 * a screen reader).
 */
@Composable
private fun PhoneHealthBar(worst: RiskLevel) {
    val segments = listOf(RiskLevel.LOW, RiskLevel.SUSPICIOUS, RiskLevel.HIGH, RiskLevel.CRITICAL)
    Column(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.forEach { level ->
                val filled = level.ordinal <= worst.ordinal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (filled) {
                                level.color()
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(worst.labelRes()),
            color = worst.color(),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun AppRiskCard(item: AssessedApp, onClick: () -> Unit) {
    // A screen-reader user should hear the verdict FIRST, then the app name, then what tapping does —
    // not the package id spelled out. We replace the card's merged semantics with one ordered
    // announcement and a labelled tap action. The sighted layout below is unchanged.
    val riskLabel = stringResource(item.assessment.level.labelRes())
    val cardDescription = stringResource(R.string.cd_risk_summary, riskLabel, item.app.label)
    val seeDetailsLabel = stringResource(R.string.action_see_details)
    // Tint the whole card with the risk colour so a dangerous row never looks like a mild one.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = cardDescription
                role = Role.Button
                onClick(label = seeDetailsLabel) {
                    onClick()
                    true
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = item.assessment.level.color().copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppAvatar(item)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(end = 12.dp).weight(1f)) {
                Text(item.app.label, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RiskChip(
                text = stringResource(item.assessment.level.labelRes()),
                color = item.assessment.level.color(),
                textColor = item.assessment.level.onColor()
            )
        }
    }
}

/**
 * The app's own launcher icon, so a non-technical user recognises which app this is at a glance (the
 * way Play Protect shows it). When no icon is available (e.g. an app that hides its launcher icon, or
 * a package we can't read), we fall back to a tinted monogram circle — the app's first letter in its
 * risk colour — so the slot always looks deliberate. Decorative: the card's merged semantics already
 * announce the verdict and name to a screen reader.
 */
@Composable
private fun AppAvatar(item: AssessedApp) {
    val context = LocalContext.current
    // Load the icon OFF the main thread (PackageManager IPC + bitmap decode would otherwise jank the
    // list on a budget phone). Until it resolves — and permanently for an icon-less or unreadable
    // package — the monogram fallback below is shown.
    val icon by produceState<ImageBitmap?>(initialValue = null, item.app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(item.app.packageName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    val loaded = icon
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(item.assessment.level.color()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.app.label.firstOrNull()?.uppercase() ?: "?",
                color = item.assessment.level.onColor(),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
