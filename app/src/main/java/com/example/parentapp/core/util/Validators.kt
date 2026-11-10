package com.example.parentapp.core.util

/** Lightweight form validation shared by auth and profile screens. */
object Validators {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(value: String): String? = when {
        value.isBlank() -> "Email is required"
        !EMAIL.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun requiredError(value: String, field: String): String? =
        if (value.isBlank()) "$field is required" else null

    fun nameError(value: String): String? = when {
        value.isBlank() -> "Full name is required"
        value.trim().length < 2 -> "Enter your full name"
        else -> null
    }

    fun phoneError(value: String): String? {
        val digits = value.filter { it.isDigit() || it == '+' }
        return when {
            value.isBlank() -> "Phone number is required"
            digits.count { it.isDigit() } < 7 -> "Enter a valid phone number"
            else -> null
        }
    }

    /** Requires 8+ chars with at least one letter and one digit. */
    fun passwordError(value: String): String? = when {
        value.isEmpty() -> "Password is required"
        value.length < 8 -> "Use at least 8 characters"
        value.none { it.isLetter() } || value.none { it.isDigit() } ->
            "Include at least one letter and one number"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Re-enter your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }
}
