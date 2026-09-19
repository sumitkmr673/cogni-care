package com.example.cognicare.repository

import android.util.Log
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.core.time.parseIsoDateMillis
import com.example.cognicare.core.time.parseIsoDateTimeMillis
import com.example.cognicare.core.time.startOfDay
import com.example.cognicare.data.model.DailyScorePoint
import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GamePerformance
import com.example.cognicare.data.model.GameSession
import com.example.cognicare.data.model.GameType
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.data.model.ReminderKind
import com.example.cognicare.data.model.SyncStatus
import com.example.cognicare.data.remote.CogniCareApi
import com.example.cognicare.data.remote.dto.PatientProfileDto
import com.example.cognicare.data.remote.dto.PatientSummaryDto
import com.example.cognicare.data.remote.dto.RecentGameSessionDto
import com.example.cognicare.data.remote.dto.ReminderItemDto
import com.example.cognicare.data.remote.dto.StartGameSessionRequestDto
import com.example.cognicare.data.remote.dto.SubmitGameResultRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RemoteCareRepository"

@Singleton
class RemoteCareRepository @Inject constructor(
    private val api: CogniCareApi,
    private val locale: AppLocaleProvider
) : CareRepository {

    // The backend has no "mark reminder done" endpoint (see cogni-care/backend/app/models/reminder.py —
    // there is no completed flag, only is_active). Completion is tracked locally until that exists.
    private val locallyCompletedReminderIds = MutableStateFlow<Set<String>>(emptySet())

    private val gameCatalogMutex = Mutex()
    private var cachedGameIdsByCode: Map<String, String>? = null

    override fun observePatients(patientIds: List<String>): Flow<List<PatientProfile>> = flow {
        // patientIds is ignored: the backend already scopes /patients to whoever is signed in
        // (their linked caregiver or doctor patients), so there is nothing to filter by here.
        val patients = runCatching { api.listPatients().patients }
            .onFailure { Log.w(TAG, "listPatients failed", it) }
            .getOrDefault(emptyList())
        emit(patients.map { it.toPatientProfile() })
    }

    override fun observeReminders(patientId: String): Flow<List<Reminder>> = flow {
        val remote = api.getReminders(patientId)
        val reminders = remote.map { it.toReminder(patientId) }
        emit(reminders)
    }

    override fun observeDashboard(patientId: String): Flow<PatientDashboard?> = flow {
        emit(fetchDashboard(patientId))
    }

    override suspend fun setReminderCompleted(reminderId: String, completed: Boolean) {
        locallyCompletedReminderIds.update { current ->
            if (completed) current + reminderId else current - reminderId
        }
    }

    override suspend fun recordGameCompletion(gameType: GameType, outcome: GameOutcome) {
        val code = gameType.toBackendCode() ?: run {
            Log.i(TAG, "$gameType has no backend game code; result kept on-device only")
            return
        }
        try {
            val gameId = gameIdForCode(code) ?: return
            val session = api.startGameSession(
                gameId,
                StartGameSessionRequestDto(appLevelToBackendDifficulty(outcome.difficultyLevel))
            )
            api.submitGameResult(
                session.id,
                SubmitGameResultRequestDto(
                    score = outcome.scorePercent,
                    accuracy = outcome.accuracyPercent,
                    correct_answers = outcome.correctAnswers,
                    total_questions = outcome.totalQuestions,
                    response_time_ms = outcome.responseTimeMs,
                    mistakes = outcome.mistakes
                )
            )
        } catch (error: HttpException) {
            Log.w(TAG, "recordGameCompletion($gameType) failed: HTTP ${error.code()}", error)
        } catch (error: IOException) {
            Log.w(TAG, "recordGameCompletion($gameType) failed: offline", error)
        }
    }

    private suspend fun gameIdForCode(code: String): String? {
        cachedGameIdsByCode?.get(code)?.let { return it }
        return gameCatalogMutex.withLock {
            cachedGameIdsByCode?.get(code)?.let { return@withLock it }
            val byCode = api.listGames().games.associate { it.code to it.id }
            cachedGameIdsByCode = byCode
            byCode[code]
        }
    }

    private suspend fun fetchDashboard(patientId: String): PatientDashboard? {
        val response = runCatching { api.getDashboard(patientId) }
            .onFailure { Log.w(TAG, "getDashboard($patientId) failed", it) }
            .getOrNull() ?: return null

        val trendPoints = runCatching { api.getTrends(patientId).metrics }.getOrDefault(emptyList())
        val trend = trendPoints.mapNotNull { point ->
            val dayMillis = parseIsoDateMillis(point.metric_date) ?: return@mapNotNull null
            DailyScorePoint(
                dayStartMillis = dayMillis,
                memory = (point.memory_score ?: 0.0).toFloat(),
                attention = (point.attention_score ?: 0.0).toFloat()
            )
        }

        // The dashboard endpoint only returns the 10 most recent sessions; ask for more so the
        // per-game breakdown and "today" count aren't skewed by whichever games are freshest.
        val allSessions = runCatching { api.getSessions(patientId, limit = 100) }
            .getOrDefault(response.recent_sessions)
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)

        val gameSessions = allSessions.mapNotNull { it.toGameSession(patientId) }
        val gamesCompletedToday = gameSessions.count { session ->
            val completedAt = session.completedAt ?: return@count false
            completedAt >= todayStart
        }

        val latest = response.latest_performance
        val patient = response.patient.toPatientProfile()

        return PatientDashboard(
            patient = patient,
            gamesCompletedToday = gamesCompletedToday,
            averageAccuracyPercent = latest?.average_accuracy?.toInt() ?: 0,
            memoryScore = latest?.memory_score?.toInt() ?: 0,
            attentionScore = latest?.attention_score?.toInt() ?: 0,
            averageResponseSeconds = (latest?.average_response_time_ms ?: 0) / 1000.0,
            trend = trend,
            gamePerformance = buildGamePerformance(allSessions),
            recentSessions = gameSessions.take(10),
            upcomingReminders = applyLocalCompletion(response.active_reminders.map { it.toReminder(patientId) }),
            lastSyncedAt = now
        )
    }

    private fun buildGamePerformance(sessions: List<RecentGameSessionDto>): List<GamePerformance> =
        sessions
            .mapNotNull { session -> backendCodeToGameType(session.game_code)?.let { it to session } }
            .groupBy({ it.first }, { it.second })
            .map { (gameType, sessionsForGame) ->
                val withResult = sessionsForGame.filter { it.result != null }
                val accuracies = withResult.mapNotNull { it.result?.accuracy }
                GamePerformance(
                    gameType = gameType,
                    completedCount = withResult.size,
                    latestLevel = sessionsForGame.maxByOrNull { it.started_at }?.difficulty_level ?: 1,
                    accuracyPercent = if (accuracies.isEmpty()) 0 else (accuracies.sum() / accuracies.size).toInt()
                )
            }
            .sortedByDescending { it.completedCount }

    /** One-off snapshot for the caregiver dashboard; [observeReminders] applies the same rule live. */
    private fun applyLocalCompletion(reminders: List<Reminder>): List<Reminder> {
        val completed = locallyCompletedReminderIds.value
        return reminders.map { it.copy(completed = it.id in completed) }
    }
}

private fun PatientSummaryDto.toPatientProfile() = PatientProfile(
    id = id,
    name = display_name,
    languageTag = languageNameToTag(preferred_language),
    timeZoneId = timezone ?: "Asia/Kolkata",
    publicId = public_id
)

private fun PatientProfileDto.toPatientProfile() = PatientProfile(
    id = id,
    name = display_name,
    languageTag = languageNameToTag(preferred_language),
    timeZoneId = timezone ?: "Asia/Kolkata",
    publicId = public_id
)

// The backend stores a full language name (see PROFILE_DATA in seed_demo.py), not an IETF tag.
private fun languageNameToTag(name: String?): String = when (name?.trim()?.lowercase()) {
    "hindi" -> "hi"
    "assamese" -> "as"
    "bengali" -> "bn"
    "manipuri" -> "mni"
    "nepali" -> "ne"
    "khasi" -> "kha"
    "mizo" -> "lus"
    else -> "en"
}

private fun ReminderItemDto.toReminder(patientId: String) = Reminder(
    id = id,
    patientId = patientId,
    title = title,
    kind = backendReminderTypeToKind(reminder_type),
    scheduledTime = parseIsoDateTimeMillis(scheduled_at) ?: System.currentTimeMillis(),
    isRecurring = is_recurring,
    completed = false
)

private fun backendReminderTypeToKind(type: String): ReminderKind = when (type) {
    "MEDICATION" -> ReminderKind.MEDICATION
    "APPOINTMENT" -> ReminderKind.APPOINTMENT
    "GAME" -> ReminderKind.GAME
    "ACTIVITY" -> ReminderKind.ACTIVITY
    else -> ReminderKind.OTHER
}

private fun RecentGameSessionDto.toGameSession(patientId: String): GameSession? {
    val gameType = backendCodeToGameType(game_code) ?: return null
    val startedMillis = parseIsoDateTimeMillis(started_at) ?: return null
    return GameSession(
        id = id,
        patientId = patientId,
        gameType = gameType,
        startedAt = startedMillis,
        completedAt = completed_at?.let(::parseIsoDateTimeMillis),
        rawResultPayload = result?.let {
            "{\"score\":${it.score},\"accuracy\":${it.accuracy},\"correctAnswers\":${it.correct_answers}," +
                "\"totalQuestions\":${it.total_questions},\"mistakes\":${it.mistakes}}"
        }.orEmpty(),
        syncStatus = SyncStatus.SYNCED
    )
}
