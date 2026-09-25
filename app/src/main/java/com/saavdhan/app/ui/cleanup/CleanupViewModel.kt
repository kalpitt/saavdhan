package com.saavdhan.app.ui.cleanup

import android.app.Application
import android.os.SystemClock
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
import com.saavdhan.app.domain.cleanup.CleanupStepId
import com.saavdhan.app.domain.cleanup.StepStatus
import com.saavdhan.app.domain.cleanup.UninstallPhase
import com.saavdhan.app.domain.cleanup.UninstallWatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /** Live feedback on the Uninstall step after the user comes back from Android's dialog. */
    var uninstallPhase by mutableStateOf(UninstallPhase.IDLE)
        private set
    private var watchJob: Job? = null
    private var holdingRemoved = false

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

    // When the user last tapped Uninstall / App info (elapsedRealtime, survives process death).
    private var uninstallAttemptAt: Long?
        get() = savedState.get("uninstallAttemptAt")
        set(value) = savedState.set("uninstallAttemptAt", value)

    fun start(packageName: String) {
        this.packageName = packageName
        refresh()
    }

    /** Call right before opening the uninstall dialog (or App info), so the return is watched. */
    fun onUninstallRequested() {
        uninstallAttemptAt = SystemClock.elapsedRealtime()
    }

    fun refresh() {
        if (packageName.isEmpty()) return
        // Restore the label after process death (the app may already be uninstalled by now).
        if (appLabel.isEmpty()) appLabel = savedState.get("appLabel") ?: ""
        if (holdingRemoved || watchJob?.isActive == true) return // a watch is already checking
        if (UninstallWatch.isAttemptFresh(uninstallAttemptAt, SystemClock.elapsedRealtime())) {
            watchUninstall()
            return
        }
        uninstallAttemptAt = null
        viewModelScope.launch {
            val state = readAndLabel()
            if (!state.isInstalled && uninstallIsCurrent()) {
                celebrateRemoved(state) // gone while we were away: still the user's win
            } else {
                // Keep a "wasn't removed" message visible until the user acts again.
                if (uninstallPhase != UninstallPhase.NOT_REMOVED) uninstallPhase = UninstallPhase.IDLE
                plan = CleanupEngine.plan(state)
            }
        }
    }

    /** The system said [removedPackage] was removed: confirm it right away, don't wait for a poll. */
    fun onPackageRemoved(removedPackage: String) {
        if (removedPackage != packageName || holdingRemoved) return
        // Still behind Android's dialog: the watch that starts on return shows it where it can be seen.
        if (UninstallWatch.isAttemptFresh(uninstallAttemptAt, SystemClock.elapsedRealtime())) return
        watchJob?.cancel()
        viewModelScope.launch {
            val state = readAndLabel()
            if (!state.isInstalled) {
                celebrateRemoved(state)
            } else {
                plan = CleanupEngine.plan(state)
            }
        }
    }

    /**
     * Android often finishes removing the app a moment AFTER our screen is back, so re-check every
     * [UninstallWatch.POLL_MS] for a short while instead of trusting one read on return.
     */
    private fun watchUninstall() {
        uninstallAttemptAt = null // consumed: coming back again later is a fresh look, not an attempt
        watchJob?.cancel()
        val returnedAt = SystemClock.elapsedRealtime()
        uninstallPhase = UninstallPhase.CHECKING
        watchJob = viewModelScope.launch {
            while (true) {
                val state = readAndLabel()
                val phase = UninstallWatch.phase(SystemClock.elapsedRealtime() - returnedAt, state.isInstalled)
                if (phase == UninstallPhase.REMOVED) {
                    celebrateRemoved(state)
                    return@launch
                }
                uninstallPhase = phase
                plan = CleanupEngine.plan(state)
                if (phase == UninstallPhase.NOT_REMOVED) return@launch
                delay(UninstallWatch.POLL_MS)
            }
        }
    }

    /** Show "removed" on the Uninstall card for a moment, then let the checklist move on. */
    private suspend fun celebrateRemoved(state: CleanupState) {
        if (uninstallIsCurrent()) {
            holdingRemoved = true
            uninstallPhase = UninstallPhase.REMOVED
            delay(UninstallWatch.REMOVED_HOLD_MS)
            holdingRemoved = false
        }
        uninstallPhase = UninstallPhase.IDLE
        plan = CleanupEngine.plan(state)
    }

    private fun uninstallIsCurrent(): Boolean =
        plan?.steps?.any { it.id == CleanupStepId.UNINSTALL && it.status == StepStatus.CURRENT } == true

    private suspend fun readAndLabel(): CleanupState {
        val (state, label) = withContext(Dispatchers.Default) { readState() }
        if (label.isNotEmpty()) {
            appLabel = label
            savedState.set("appLabel", label)
        }
        return state
    }

    private suspend fun readState(): Pair<CleanupState, String> {
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
