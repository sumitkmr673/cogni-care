package com.example.cognicare.data.remote

import com.example.cognicare.data.remote.dto.AuthenticatedUserDto
import com.example.cognicare.data.remote.dto.CaregiverRegisterRequestDto
import com.example.cognicare.data.remote.dto.CaregiverRegisterResponseDto
import com.example.cognicare.data.remote.dto.PatientLinkRequestDto
import com.example.cognicare.data.remote.dto.PatientProfileDto
import com.example.cognicare.data.remote.dto.DashboardResponseDto
import com.example.cognicare.data.remote.dto.GameResultDto
import com.example.cognicare.data.remote.dto.GameSessionDto
import com.example.cognicare.data.remote.dto.GamesResponseDto
import com.example.cognicare.data.remote.dto.LoginRequestDto
import com.example.cognicare.data.remote.dto.PatientsResponseDto
import com.example.cognicare.data.remote.dto.PerformanceHistoryResponseDto
import com.example.cognicare.data.remote.dto.RecentGameSessionDto
import com.example.cognicare.data.remote.dto.ReminderItemDto
import com.example.cognicare.data.remote.dto.StartGameSessionRequestDto
import com.example.cognicare.data.remote.dto.SubmitGameResultRequestDto
import com.example.cognicare.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.cognicare.data.remote.dto.ReminderCreateRequestDto
import com.example.cognicare.data.remote.dto.ReminderStatusRequestDto
import com.example.cognicare.data.remote.dto.ReminderUpdateRequestDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.PATCH

// Matches cogni-care/backend's app/api Python routes one-for-one; see that repo's README for the full reference.
interface CogniCareApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/register")
    suspend fun registerCaregiver(@Body body: CaregiverRegisterRequestDto): CaregiverRegisterResponseDto

    // The token is passed explicitly: this runs right after login, before the token is saved anywhere.
    @GET("auth/me")
    suspend fun currentUser(@Header("Authorization") authorization: String): AuthenticatedUserDto

    @GET("games")
    suspend fun listGames(): GamesResponseDto

    @POST("games/{gameId}/sessions")
    suspend fun startGameSession(
        @Path("gameId") gameId: String,
        @Body body: StartGameSessionRequestDto
    ): GameSessionDto

    @POST("games/sessions/{sessionId}/result")
    suspend fun submitGameResult(
        @Path("sessionId") sessionId: String,
        @Body body: SubmitGameResultRequestDto
    ): GameResultDto

    @GET("patients")
    suspend fun listPatients(): PatientsResponseDto

    /** Caregivers only. 404 when no patient has that ID, 409 when already linked. */
    @POST("patients/link")
    suspend fun linkPatient(@Body body: PatientLinkRequestDto): PatientProfileDto

    @GET("patients/{patientId}/dashboard")
    suspend fun getDashboard(@Path("patientId") patientId: String): DashboardResponseDto

    @GET("patients/{patientId}/trends")
    suspend fun getTrends(@Path("patientId") patientId: String): PerformanceHistoryResponseDto

    @GET("patients/{patientId}/sessions")
    suspend fun getSessions(
        @Path("patientId") patientId: String,
        @Query("limit") limit: Int = 100
    ): List<RecentGameSessionDto>

    @GET("patients/{patientId}/reminders")
    suspend fun getReminders(@Path("patientId") patientId: String): List<ReminderItemDto>

    // Managing reminders: any linked caregiver may add one; only the primary caregiver or the
    // reminder's creator may edit, pause or delete it (the backend answers 403 otherwise).
    @POST("patients/{patientId}/reminders")
    suspend fun createReminder(
        @Path("patientId") patientId: String,
        @Body body: ReminderCreateRequestDto
    ): ReminderItemDto

    @PATCH("patients/{patientId}/reminders/{reminderId}")
    suspend fun updateReminder(
        @Path("patientId") patientId: String,
        @Path("reminderId") reminderId: String,
        @Body body: ReminderUpdateRequestDto
    ): ReminderItemDto

    @PATCH("patients/{patientId}/reminders/{reminderId}/status")
    suspend fun setReminderStatus(
        @Path("patientId") patientId: String,
        @Path("reminderId") reminderId: String,
        @Body body: ReminderStatusRequestDto
    ): ReminderItemDto

    @DELETE("patients/{patientId}/reminders/{reminderId}")
    suspend fun deleteReminder(
        @Path("patientId") patientId: String,
        @Path("reminderId") reminderId: String
    ): Response<Unit>
}
