package com.saavdhan.app.ui.cleanup

import android.app.Application
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.saavdhan.app.data.scanner.AppScanner
import com.saavdhan.app.domain.cleanup.CleanupEngine
import com.saavdhan.app.domain.cleanup.CleanupPlan
import com.saavdhan.app.domain.cleanup.CleanupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the guided cleanup. Each time [refresh] is called (on screen open and on resume) it
 * re-reads the live state of the target app and recomputes the reactive checklist, so steps tick
 * off automatically as the user does them.
 *
 * Process-death safe: hadAccessibility/wasDeviceAdmin are saved in SavedStateHandle so they
 * survive if Android kills the process mid-cleanup.
 */
class CleanupViewModel(application: Application, private val savedState: SavedStateHandle) :
    AndroidViewModel(application) {

    var plan by mutableStateOf<CleanupPlan?>(null)
        private set
    var appLabel by mutableStateOf("")
        private set

    private var packageName: String = ""

    // What the app held when cleanup began — persisted in SavedStateHandle so the data survives
    // process death. Steps still show (and can be ticked off) after the user turns the power off.
    private var hadAccessibility: Boolean
        get() = savedState.get("hadAccessibility") ?: false
        set(value) = savedState.set("hadAccessibility", value)
    private var wasDeviceAdmin: Boolean
        get() = savedState.get("wasDeviceAdmin") ?: false
        set(value) = savedState.set("wasDeviceAdmin", value)
    private var initialized: Boolean
        get() = savedState.get("initialized") ?: false
        set(value) = savedState.set("initialized", value)

    fun start(packageName: String) {
        this.packageName = packageName
        refresh()
    }

    fun refresh() {
        if (packageName.isEmpty()) return
        // Restore the label after process death (the app may already be uninstalled by now).
        if (appLabel.isEmpty()) appLabel = savedState.get("appLabel") ?: ""
        viewModelScope.launch {
            val (state, label) = withContext(Dispatchers.Default) { readState() }
            if (label.isNotEmpty()) {
                appLabel = label
                savedState.set("appLabel", label)
            }
            plan = CleanupEngine.plan(state)
        }
    }

    private fun readState(): Pair<CleanupState, String> {
        val context = getApplication<Application>()
        val assessed = AppScanner(context).assessSingle(packageName) // null once uninstalled
        val isInstalled = assessed != null
        val hasAccessibility = assessed?.app?.hasAccessibilityEnabled ?: false
        val isDeviceAdmin = assessed?.app?.isDeviceAdmin ?: false
        val label = assessed?.app?.label ?: appLabel

        if (!initialized) {
            hadAccessibility = hasAccessibility
            wasDeviceAdmin = isDeviceAdmin
            initialized = true
        }

        val state = CleanupState(
            isInstalled = isInstalled,
            hadAccessibility = hadAccessibility,
            hasAccessibility = hasAccessibility,
            wasDeviceAdmin = wasDeviceAdmin,
            isDeviceAdmin = isDeviceAdmin,
            isIsolated = isAirplaneModeOn()
        )
        return state to label
    }

    private fun isAirplaneModeOn(): Boolean =
        Settings.Global.getInt(
            getApplication<Application>().contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
}
