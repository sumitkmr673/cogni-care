package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyRecallUiState(val transcript: List<String> = emptyList())

/**
 * Open reflection with no right answer, so nothing is checked or scored. The transcript
 * lives only in this ViewModel for now; analysing it belongs to the AI service later.
 */
@HiltViewModel
class DailyRecallViewModel @Inject constructor(
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyRecallUiState())
    val uiState: StateFlow<DailyRecallUiState> = _uiState.asStateFlow()

    private val startedAtMillis = System.currentTimeMillis()
    private var hasReportedCompletion = false

    fun onSpeechResult(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(transcript = it.transcript + text.trim()) }
    }

    /** Called when the patient finishes or skips. There is nothing to score, so the backend
     * only records that the session happened — see cogni-care/backend's SubmitGameResultRequest,
     * which requires a score but treats everything else as optional. */
    fun recordCompletion() {
        if (hasReportedCompletion) return
        hasReportedCompletion = true
        viewModelScope.launch {
            if (authRepository.session.first() == null) return@launch
            val elapsedMs = System.currentTimeMillis() - startedAtMillis
            careRepository.recordGameCompletion(
                GameType.DAILY_RECALL,
                GameOutcome(scorePercent = 100.0, responseTimeMs = elapsedMs.toInt())
            )
        }
    }
}
