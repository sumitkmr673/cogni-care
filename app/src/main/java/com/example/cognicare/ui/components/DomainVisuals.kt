package com.example.cognicare.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cognicare.R
import com.example.cognicare.data.model.GameType
import com.example.cognicare.data.model.ReminderKind
import com.example.cognicare.data.model.SyncStatus
import com.example.cognicare.data.model.UserRole
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.Tone

@get:StringRes
val GameType.titleRes: Int
    get() = when (this) {
        GameType.MEMORY_MATCH -> R.string.game_memory_match
        GameType.PATTERN_RECALL -> R.string.game_pattern_recall
        GameType.OBJECT_NAMING -> R.string.game_object_naming
        GameType.DAILY_RECALL -> R.string.game_daily_recall
        GameType.ORIENTATION -> R.string.game_orientation
        GameType.FAMILY_IDENTIFICATION -> R.string.game_family_identification
    }

@get:StringRes
val GameType.descriptionRes: Int
    get() = when (this) {
        GameType.MEMORY_MATCH -> R.string.game_memory_match_desc
        GameType.PATTERN_RECALL -> R.string.game_pattern_recall_desc
        GameType.OBJECT_NAMING -> R.string.game_object_naming_desc
        GameType.DAILY_RECALL -> R.string.game_daily_recall_desc
        GameType.ORIENTATION -> R.string.game_orientation_desc
        GameType.FAMILY_IDENTIFICATION -> R.string.game_family_identification_desc
    }

val GameType.icon: ImageVector
    get() = when (this) {
        GameType.MEMORY_MATCH -> Icons.Rounded.Style
        GameType.PATTERN_RECALL -> Icons.Rounded.Extension
        GameType.OBJECT_NAMING -> Icons.Rounded.Category
        GameType.DAILY_RECALL -> Icons.Rounded.Today
        GameType.ORIENTATION -> Icons.Rounded.Explore
        GameType.FAMILY_IDENTIFICATION -> Icons.Rounded.FamilyRestroom
    }

val GameType.tone: Tone
    @Composable @ReadOnlyComposable
    get() {
        val colors = CaregiverTheme.colors
        return when (this) {
            GameType.MEMORY_MATCH -> colors.lavender
            GameType.PATTERN_RECALL -> colors.slate
            GameType.OBJECT_NAMING -> colors.slate
            GameType.DAILY_RECALL -> colors.clay
            GameType.ORIENTATION -> colors.clay
            GameType.FAMILY_IDENTIFICATION -> colors.mint
        }
    }

val ReminderKind.icon: ImageVector
    get() = when (this) {
        ReminderKind.GAME -> Icons.Rounded.SportsEsports
        ReminderKind.MEDICATION -> Icons.Rounded.Medication
        ReminderKind.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
        ReminderKind.MEAL -> Icons.Rounded.Restaurant
        ReminderKind.APPOINTMENT -> Icons.Rounded.Event
    }

val ReminderKind.tone: Tone
    @Composable @ReadOnlyComposable
    get() {
        val colors = CaregiverTheme.colors
        return when (this) {
            ReminderKind.GAME -> colors.clay
            ReminderKind.MEDICATION -> colors.lavender
            ReminderKind.WALK -> colors.mint
            ReminderKind.MEAL -> colors.sand
            ReminderKind.APPOINTMENT -> colors.slate
        }
    }

@get:StringRes
val SyncStatus.labelRes: Int
    get() = when (this) {
        SyncStatus.PENDING -> R.string.sync_status_pending
        SyncStatus.SYNCED -> R.string.sync_status_synced
        SyncStatus.FAILED -> R.string.sync_status_failed
    }

val SyncStatus.tone: Tone
    @Composable @ReadOnlyComposable
    get() {
        val colors = CaregiverTheme.colors
        return when (this) {
            SyncStatus.PENDING -> colors.demo
            SyncStatus.SYNCED -> colors.success
            SyncStatus.FAILED -> colors.danger
        }
    }

@get:StringRes
val UserRole.labelRes: Int
    get() = when (this) {
        UserRole.PATIENT -> R.string.role_patient
        UserRole.DOCTOR -> R.string.role_doctor_caregiver
    }
