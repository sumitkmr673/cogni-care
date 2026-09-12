package com.example.cognicare.core.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.UserRole

enum class AppArea { PATIENT, CAREGIVER }

fun UserRole.canAccess(area: AppArea): Boolean = when (area) {
    AppArea.PATIENT -> this == UserRole.PATIENT
    AppArea.CAREGIVER -> isCaregiver
}

/**
 * In-app RBAC gate. Each role already gets its own NavHost, so a patient's nav
 * controller has no caregiver destinations; this is the second line of defence.
 */
@Composable
fun RequireArea(
    session: AuthSession,
    area: AppArea,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    if (session.role.canAccess(area)) {
        content()
    } else {
        LaunchedEffect(session.userId, area) { onDenied() }
    }
}
