package com.example.cognicare.repository

import android.util.Log
import com.example.cognicare.core.reminder.ReminderType
import com.example.cognicare.core.reminder.canManageReminder
import com.example.cognicare.core.reminder.toBackendDateTime
import com.example.cognicare.core.time.parseIsoDateTimeMillis
import com.example.cognicare.data.remote.CogniCareApi
import com.example.cognicare.data.remote.dto.ReminderCreateRequestDto
import com.example.cognicare.data.remote.dto.ReminderItemDto
import com.example.cognicare.data.remote.dto.ReminderStatusRequestDto
import com.example.cognicare.data.remote.dto.ReminderUpdateRequestDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A reminder as the caregiver's reminder manager shows it, with who made it and whether it's paused. */
data class ManagedReminder(
    val id: String,
    val title: String,
    val description: String?,
    val type: ReminderType,
    val scheduledAt: Long,
    val isRecurring: Boolean,
    val isActive: Boolean,
    val creatorName: String?,
    val creatorPublicId: String?,
    /** Created by the signed-in caregiver: shown as "(You)". */
    val isMine: Boolean,
    /** Primary caregiver or creator: may pause, edit and delete it. */
    val canManage: Boolean
)

/** What the caregiver is saving: a one-off reminder, as on the web portal. */
data class ReminderDraft(val title: String, val type: ReminderType, val scheduledAt: Long)

enum class ReminderActionResult { SUCCESS, NOT_ALLOWED, INVALID, NOT_FOUND, NETWORK_ERROR }

interface ReminderRepository {
    /** Every reminder for the patient, oldest first as the backend orders them; null if unreachable. */
    suspend fun reminders(patientId: String): List<ManagedReminder>?
    suspend fun create(patientId: String, draft: ReminderDraft): ReminderActionResult
    suspend fun update(patientId: String, reminderId: String, draft: ReminderDraft): ReminderActionResult
    suspend fun setActive(patientId: String, reminderId: String, active: Boolean): ReminderActionResult
    suspend fun delete(patientId: String, reminderId: String): ReminderActionResult
}

@Singleton
class RemoteReminderRepository @Inject constructor(
    private val api: CogniCareApi
) : ReminderRepository {

    override suspend fun reminders(patientId: String): List<ManagedReminder>? = try {
        coroutineScope {
            // The dashboard says whether this caregiver is primary and gives their own CG- id.
            val relationship = async { api.getDashboard(patientId).caregiver_relationship }
            val items = async { api.getReminders(patientId) }
            val me = relationship.await()
            items.await().map { it.toManaged(isPrimary = me?.is_primary == true, myPublicId = me?.public_id) }
        }
    } catch (error: HttpException) {
        Log.w(TAG, "reminders failed: HTTP ${error.code()}", error)
        null
    } catch (error: IOException) {
        null
    }

    override suspend fun create(patientId: String, draft: ReminderDraft) = attempt("create") {
        api.createReminder(
            patientId,
            ReminderCreateRequestDto(draft.title.trim(), draft.type.wireValue, toBackendDateTime(draft.scheduledAt))
        )
    }

    override suspend fun update(patientId: String, reminderId: String, draft: ReminderDraft) = attempt("update") {
        api.updateReminder(
            patientId,
            reminderId,
            ReminderUpdateRequestDto(draft.title.trim(), draft.type.wireValue, toBackendDateTime(draft.scheduledAt))
        )
    }

    override suspend fun setActive(patientId: String, reminderId: String, active: Boolean) = attempt("setActive") {
        api.setReminderStatus(patientId, reminderId, ReminderStatusRequestDto(active))
    }

    override suspend fun delete(patientId: String, reminderId: String) = attempt("delete") {
        val response = api.deleteReminder(patientId, reminderId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private suspend fun attempt(action: String, call: suspend () -> Unit): ReminderActionResult = try {
        call()
        ReminderActionResult.SUCCESS
    } catch (error: HttpException) {
        when (error.code()) {
            403 -> ReminderActionResult.NOT_ALLOWED
            404 -> ReminderActionResult.NOT_FOUND
            422 -> ReminderActionResult.INVALID
            else -> {
                Log.w(TAG, "$action failed: HTTP ${error.code()}", error)
                ReminderActionResult.NETWORK_ERROR
            }
        }
    } catch (error: IOException) {
        ReminderActionResult.NETWORK_ERROR
    }

    private companion object {
        const val TAG = "ReminderRepository"
    }
}

private fun ReminderItemDto.toManaged(isPrimary: Boolean, myPublicId: String?) = ManagedReminder(
    id = id,
    title = title,
    description = description?.takeIf { it.isNotBlank() },
    type = ReminderType.fromWire(reminder_type),
    scheduledAt = parseIsoDateTimeMillis(scheduled_at) ?: 0L,
    isRecurring = is_recurring,
    isActive = is_active,
    creatorName = created_by_display_name,
    creatorPublicId = created_by_caregiver_public_id,
    isMine = myPublicId != null && myPublicId == created_by_caregiver_public_id,
    canManage = canManageReminder(isPrimary, myPublicId, created_by_caregiver_public_id)
)
