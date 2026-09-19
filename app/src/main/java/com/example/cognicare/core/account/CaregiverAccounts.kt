package com.example.cognicare.core.account

/**
 * Account rules shared by caregiver sign-up and patient linking. They mirror the backend
 * (backend/app/api/auth.py, backend/app/identifiers.py) so a form never sends something the
 * server is certain to reject.
 */

/** The backend's caregiver types, in the order the sign-up form offers them. [wireValue] is what it accepts. */
enum class CaregiverType(val wireValue: String) {
    FAMILY("FAMILY"),
    DOCTOR("DOCTOR"),
    PROFESSIONAL_CAREGIVER("PROFESSIONAL_CAREGIVER"),
    OTHER("OTHER")
}

/** backend/app/schemas/auth.py: CaregiverRegisterRequest.password has min_length=8. */
const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

enum class RegistrationProblem { NAME_REQUIRED, INVALID_EMAIL, PASSWORD_TOO_SHORT }

/** The first problem with the form, in the order the fields appear, or null when it can be sent. */
fun registrationProblem(name: String, email: String, password: String): RegistrationProblem? = when {
    name.isBlank() -> RegistrationProblem.NAME_REQUIRED
    !EMAIL_PATTERN.matches(email.trim()) -> RegistrationProblem.INVALID_EMAIL
    password.length < MIN_PASSWORD_LENGTH -> RegistrationProblem.PASSWORD_TOO_SHORT
    else -> null
}

// backend/app/identifiers.py: "PT-" followed by 8 upper-case letters or digits.
private val PATIENT_ID_BODY = Regex("^[A-Z0-9]{8}$")

/**
 * Turns what a caregiver typed into the backend's patient ID form, forgiving the usual slips —
 * lower case, spaces, a missing "PT-" or a missing hyphen — or returns null when it cannot be a
 * patient ID at all. "pt ab12 cd34", "AB12CD34" and "PT-ab12cd34" all become "PT-AB12CD34".
 */
fun normalizePatientPublicId(input: String): String? {
    val compact = input.uppercase().filter { !it.isWhitespace() }
    val body = when {
        compact.startsWith("PT-") -> compact.removePrefix("PT-")
        compact.startsWith("PT") && compact.length == 10 -> compact.removePrefix("PT")
        else -> compact
    }
    return if (PATIENT_ID_BODY.matches(body)) "PT-$body" else null
}
