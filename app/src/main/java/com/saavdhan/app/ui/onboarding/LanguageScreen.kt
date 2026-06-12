package com.saavdhan.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saavdhan.app.R
import com.saavdhan.app.i18n.LocaleManager
import com.saavdhan.app.ui.components.PrimaryButton
import com.saavdhan.app.ui.components.SecondaryButton

/**
 * First screen on first launch: pick Hindi or English. Tapping a language saves it and continues.
 */
@Composable
fun LanguageScreen(onChosen: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.choose_language),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            text = stringResource(R.string.lang_english),
            onClick = { onChosen(LocaleManager.ENGLISH) }
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = stringResource(R.string.lang_hindi),
            onClick = { onChosen(LocaleManager.HINDI) }
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.choose_language_sub),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
