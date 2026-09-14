package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DailyRecallUiState(val transcript: List<String> = emptyList())

/**
 * Open reflection with no right answer, so nothing is checked or scored. The transcript
 * lives only in this ViewModel for now; analysing it belongs to the AI service later.
 */
@HiltViewModel
class DailyRecallViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DailyRecallUiState())
    val uiState: StateFlow<DailyRecallUiState> = _uiState.asStateFlow()

    fun onSpeechResult(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(transcript = it.transcript + text.trim()) }
    }
}
