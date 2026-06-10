package com.saavdhan.app.ui.scan

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saavdhan.app.data.scanner.AppScanner
import com.saavdhan.app.data.scanner.AssessedApp
import com.saavdhan.app.data.scanner.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The scan states the home screen can be in. */
sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val result: ScanResult) : ScanState
    data object Error : ScanState
}

/**
 * Drives scanning. Uses [AndroidViewModel] so it can reach the app context to read packages.
 * The scan runs on a background thread (Dispatchers.Default) so the screen stays responsive.
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf<ScanState>(ScanState.Idle)
        private set

    fun scan() {
        state = ScanState.Scanning
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    AppScanner(getApplication()).scan()
                }
                state = ScanState.Done(result)
            } catch (e: Exception) {
                // Offline app, no crash reporting by design — log for local diagnostics only.
                android.util.Log.w("Saavdhan", "Scan failed", e)
                state = ScanState.Error
            }
        }
    }

    /** Look up one assessed app by package name (used by the detail screen). */
    fun find(packageName: String): AssessedApp? =
        (state as? ScanState.Done)?.result?.apps?.firstOrNull { it.app.packageName == packageName }
}
