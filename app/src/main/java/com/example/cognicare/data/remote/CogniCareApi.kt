package com.example.cognicare.data.remote

import com.example.cognicare.data.remote.dto.AuthenticatedUserDto
import com.example.cognicare.data.remote.dto.DashboardResponseDto
import com.example.cognicare.data.remote.dto.DeviceLoginRequestDto
import com.example.cognicare.data.remote.dto.DeviceProvisionRequestDto
import com.example.cognicare.data.remote.dto.DeviceProvisionResponseDto
import com.example.cognicare.data.remote.dto.DeviceRevokeResponseDto
import com.example.cognicare.data.remote.dto.GameResultDto
import com.example.cognicare.data.remote.dto.GameSessionDto
import com.example.cognicare.data.remote.dto.GamesResponseDto
import com.example.cognicare.data.remote.dto.LoginRequestDto
import com.example.cognicare.data.remote.dto.PatientRegisterRequestDto
import com.example.cognicare.data.remote.dto.PatientRegisterResponseDto
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

// Matches cogni-care/backend's app/api Python routes one-for-one; see that repo's README for the full reference.
interface CogniCareApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("auth/device-login")
    suspend fun deviceLogin(@Body body: DeviceLoginRequestDto): TokenResponseDto

    @POST("auth/patient/register")
    suspend fun registerPatient(@Body body: PatientRegisterRequestDto): PatientRegisterResponseDto

    // The token is passed explicitly: this runs right after login, before the token is saved anywhere.
    @GET("auth/me")
    suspend fun currentUser(@Header("Authorization") authorization: String): AuthenticatedUserDto

    @POST("patients/{patientId}/devices")
    suspend fun provisionDevice(
        @Path("patientId") patientId: String,
        @Body body: DeviceProvisionRequestDto
    ): DeviceProvisionResponseDto

    @POST("patients/{patientId}/devices/revoke")
    suspend fun revokeDevice(
        @Path("patientId") patientId: String
    ): DeviceRevokeResponseDto

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
}
