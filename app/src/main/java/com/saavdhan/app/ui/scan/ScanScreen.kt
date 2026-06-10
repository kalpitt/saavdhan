package com.saavdhan.app.ui.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saavdhan.app.R
import com.saavdhan.app.data.scanner.AssessedApp
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.ui.color
import com.saavdhan.app.ui.components.InfoCard
import com.saavdhan.app.ui.components.PrimaryButton
import com.saavdhan.app.ui.components.RiskChip
import com.saavdhan.app.ui.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        when (val state = viewModel.state) {
            is ScanState.Idle -> IdleContent(padding, onScan = viewModel::scan)
            is ScanState.Scanning -> ScanningContent(padding)
            is ScanState.Done -> ResultsContent(
                padding = padding,
                apps = state.result.apps,
                partial = state.result.partial,
                onAppClick = onAppClick,
                onScanAgain = viewModel::scan,
            )
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
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = stringResource(R.string.scan_button), onClick = onScan)
    }
}

@Composable
private fun ScanningContent(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.scanning), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultsContent(
    padding: PaddingValues,
    apps: List<AssessedApp>,
    partial: Boolean,
    onAppClick: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    // Only surface apps that are worth attention (not the calm LOW/allowlisted ones).
    val flagged = apps.filter { it.assessment.level != RiskLevel.LOW }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (partial) {
            item { InfoCard(text = stringResource(R.string.partial_scan_note)) }
        }

        if (flagged.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.result_safe_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.result_safe_body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            item {
                val title = if (flagged.size == 1) {
                    stringResource(R.string.result_found_title, flagged.size)
                } else {
                    stringResource(R.string.result_found_title_plural, flagged.size)
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            items(flagged, key = { it.app.packageName }) { item ->
                AppRiskCard(item, onClick = { onAppClick(item.app.packageName) })
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = stringResource(R.string.scan_again), onClick = onScanAgain)
        }
    }
}

@Composable
private fun AppRiskCard(item: AssessedApp, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.padding(end = 12.dp)) {
                Text(item.app.label, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiskChip(
                text = stringResource(item.assessment.level.labelRes()),
                color = item.assessment.level.color(),
            )
        }
    }
}
