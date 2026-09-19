package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.reminder.ReminderProblem
import com.example.cognicare.core.reminder.ReminderType
import com.example.cognicare.core.reminder.reminderProblem
import com.example.cognicare.repository.ManagedReminder
import com.example.cognicare.repository.ReminderActionResult
import com.example.cognicare.repository.ReminderDraft
import com.example.cognicare.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderForm(
    val title: String = "",
    val type: ReminderType = ReminderType.GAME,
    val scheduledAt: Long? = null
)

/** One line of feedback under the reminders, like the web portal's success/error text. */
enum class ReminderMessage(val isError: Boolean) {
    CREATED(false), UPDATED(false), DELETED(false), PAUSED(false), RESUMED(false),
    NOT_ALLOWED(true), INVALID(true), NOT_FOUND(true), FAILED(true),
    NEED_TITLE(true), TITLE_TOO_LONG(true), NEED_TIME(true)
}

data class CaregiverRemindersUiState(
    val patientId: String? = null,
    val reminders: List<ManagedReminder> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** Set while the form is editing an existing reminder rather than adding one. */
    val editingId: String? = null,
    val form: ReminderForm = ReminderForm(),
    /** A save, pause or delete is in flight; buttons wait so nothing is sent twice. */
    val isBusy: Boolean = false,
    val message: ReminderMessage? = null,
    /** The reminder whose delete is waiting for "are you sure?". */
    val pendingDelete: ManagedReminder? = null
)

/**
 * The caregiver's reminder manager for the selected patient: the web portal's "Upcoming
 * reminders" panel — list, pause/resume, edit, delete and add — backed by the same endpoints.
 */
@HiltViewModel
class CaregiverRemindersViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaregiverRemindersUiState())
    val uiState: StateFlow<CaregiverRemindersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** Called whenever the selected patient is shown; switching patient starts afresh. */
    fun show(patientId: String) {
        if (_uiState.value.patientId == patientId) return
        _uiState.value = CaregiverRemindersUiState(patientId = patientId)
        reload()
    }

    fun reload() {
        val patientId = _uiState.value.patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val reminders = repository.reminders(patientId)
            _uiState.update {
                if (reminders == null) it.copy(isLoading = false, loadFailed = true)
                else it.copy(isLoading = false, loadFailed = false, reminders = reminders)
            }
        }
    }

    fun onTitleChange(title: String) = _uiState.update { it.copy(form = it.form.copy(title = title), message = null) }

    fun onTypeChange(type: ReminderType) = _uiState.update { it.copy(form = it.form.copy(type = type)) }

    fun onScheduleChange(millis: Long) = _uiState.update { it.copy(form = it.form.copy(scheduledAt = millis), message = null) }

    fun startEdit(reminder: ManagedReminder) = _uiState.update {
        it.copy(
            editingId = reminder.id,
            form = ReminderForm(reminder.title, reminder.type, reminder.scheduledAt),
            message = null
        )
    }

    fun cancelEdit() = _uiState.update { it.copy(editingId = null, form = ReminderForm(), message = null) }

    fun submit() {
        val state = _uiState.value
        val patientId = state.patientId ?: return
        if (state.isBusy) return
        when (reminderProblem(state.form.title, state.form.scheduledAt)) {
            ReminderProblem.MISSING_TITLE -> return showMessage(ReminderMessage.NEED_TITLE)
            ReminderProblem.TITLE_TOO_LONG -> return showMessage(ReminderMessage.TITLE_TOO_LONG)
            ReminderProblem.MISSING_TIME -> return showMessage(ReminderMessage.NEED_TIME)
            null -> Unit
        }
        val draft = ReminderDraft(state.form.title, state.form.type, state.form.scheduledAt!!)
        val editingId = state.editingId
        perform(success = if (editingId == null) ReminderMessage.CREATED else ReminderMessage.UPDATED, clearForm = true) {
            if (editingId == null) repository.create(patientId, draft) else repository.update(patientId, editingId, draft)
        }
    }

    fun toggleActive(reminder: ManagedReminder) {
        val patientId = _uiState.value.patientId ?: return
        perform(success = if (reminder.isActive) ReminderMessage.PAUSED else ReminderMessage.RESUMED) {
            repository.setActive(patientId, reminder.id, !reminder.isActive)
        }
    }

    fun requestDelete(reminder: ManagedReminder) = _uiState.update { it.copy(pendingDelete = reminder) }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val state = _uiState.value
        val patientId = state.patientId ?: return
        val reminder = state.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        perform(success = ReminderMessage.DELETED, clearForm = state.editingId == reminder.id) {
            repository.delete(patientId, reminder.id)
        }
    }

    private fun perform(success: ReminderMessage, clearForm: Boolean = false, action: suspend () -> ReminderActionResult) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val result = action()
            _uiState.update {
                val succeeded = result == ReminderActionResult.SUCCESS
                it.copy(
                    isBusy = false,
                    message = if (succeeded) success else result.toMessage(),
                    editingId = if (succeeded && clearForm) null else it.editingId,
                    form = if (succeeded && clearForm) ReminderForm() else it.form
                )
            }
            // Refresh even after a 403 or 404: someone else may have changed or removed it.
            reload()
        }
    }

    private fun showMessage(message: ReminderMessage) = _uiState.update { it.copy(message = message) }
}

private fun ReminderActionResult.toMessage(): ReminderMessage = when (this) {
    ReminderActionResult.SUCCESS -> ReminderMessage.UPDATED
    ReminderActionResult.NOT_ALLOWED -> ReminderMessage.NOT_ALLOWED
    ReminderActionResult.INVALID -> ReminderMessage.INVALID
    ReminderActionResult.NOT_FOUND -> ReminderMessage.NOT_FOUND
    ReminderActionResult.NETWORK_ERROR -> ReminderMessage.FAILED
}
