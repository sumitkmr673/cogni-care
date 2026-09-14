package com.example.cognicare.data.model

data class DailyScorePoint(
    val dayStartMillis: Long,
    val memory: Float,
    val attention: Float
)

data class GamePerformance(
    val gameType: GameType,
    val completedCount: Int,
    val latestLevel: Int,
    val accuracyPercent: Int
)

data class PatientDashboard(
    val patient: PatientProfile,
    val gamesCompletedToday: Int,
    val averageAccuracyPercent: Int,
    val memoryScore: Int,
    val attentionScore: Int,
    val averageResponseSeconds: Double,
    val trend: List<DailyScorePoint>,
    val gamePerformance: List<GamePerformance>,
    val recentSessions: List<GameSession>,
    val upcomingReminders: List<Reminder>,
    val lastSyncedAt: Long?
)
