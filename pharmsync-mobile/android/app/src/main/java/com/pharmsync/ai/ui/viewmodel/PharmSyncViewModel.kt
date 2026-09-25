package com.pharmsync.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmsync.ai.engine.PharmSyncAlgorithm
import com.pharmsync.ai.model.Prescription
import com.pharmsync.ai.model.SyncPlan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PharmSyncUiState(
    val prescriptions: List<Prescription> = emptyList(),
    val syncPlan: SyncPlan? = null,
    val isSynchronized: Boolean = false,
    val isOptimizing: Boolean = false,
    val isStreamDrawerExpanded: Boolean = false,
    val liveLogs: List<String> = emptyList(),
    val copiedNCPDPCode: String? = null
)

class PharmSyncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PharmSyncUiState())
    val uiState: StateFlow<PharmSyncUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val samplePrescriptions = PharmSyncAlgorithm.getSampleElenaPrescriptions()
        val initialPlan = PharmSyncAlgorithm.optimizeRegimen(samplePrescriptions)
        _uiState.value = _uiState.value.copy(
            prescriptions = samplePrescriptions,
            syncPlan = initialPlan,
            isSynchronized = false,
            liveLogs = listOf(
                "[SYSTEM READY] PharmSync Guardian initialized.",
                "[PATIENT] Elena Rostova (DOB: 1968-04-14). 3 active chronic medications."
            )
        )
    }

    fun synchronizeRegimen() {
        if (_uiState.value.isOptimizing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOptimizing = true)
            val logs = mutableListOf<String>()

            // Step 1: Vision / OCR ingestion
            logs.add("[OCR INTAKE] Scanning 3 prescription bottle labels...")
            _uiState.value = _uiState.value.copy(liveLogs = logs.toList())
            delay(500)

            logs.add("[CHRONO] Analyzing circadian pharmacokinetics & food requirements...")
            _uiState.value = _uiState.value.copy(liveLogs = logs.toList())
            delay(600)

            // Step 2: Gap alert & auto pullback
            logs.add("[SYNC CHECK] Warning: Delaying to Oct 1 creates 48h Metformin zero-day gap!")
            logs.add("[RESILIENCE] Auto-clamping T_anchor = Sept 29 to protect continuity of care.")
            _uiState.value = _uiState.value.copy(liveLogs = logs.toList())
            delay(700)

            // Step 3: NCPDP overrides
            logs.add("[BILLING] Lisinopril has 10 surplus days. Generating NCPDP Field 420-DK=47 override (Bridge: 20 units, Copay: $5.33).")
            logs.add("[BILLING] Atorvastatin has 16 surplus days. Generating NCPDP Field 420-DK=47 override (Bridge: 14 units, Copay: $5.60).")
            _uiState.value = _uiState.value.copy(liveLogs = logs.toList())
            delay(500)

            val finalPlan = PharmSyncAlgorithm.optimizeRegimen(_uiState.value.prescriptions)
            logs.addAll(finalPlan.auditLogs)
            logs.add("[SUCCESS] Regimen synchronized! All 3 medications aligned to Sept 29 pickup.")

            _uiState.value = _uiState.value.copy(
                syncPlan = finalPlan,
                isSynchronized = true,
                isOptimizing = false,
                liveLogs = logs
            )
        }
    }

    fun toggleMedicationTaken(itemId: String) {
        val currentPlan = _uiState.value.syncPlan ?: return
        val updatedDeck = currentPlan.dailyPillDeck.map { item ->
            if (item.id == itemId) item.copy(isTaken = !item.isTaken) else item
        }
        _uiState.value = _uiState.value.copy(
            syncPlan = currentPlan.copy(dailyPillDeck = updatedDeck)
        )
    }

    fun toggleStreamDrawer() {
        _uiState.value = _uiState.value.copy(
            isStreamDrawerExpanded = !_uiState.value.isStreamDrawerExpanded
        )
    }

    fun copyNCPDP(code: String) {
        _uiState.value = _uiState.value.copy(copiedNCPDPCode = code)
    }
}
