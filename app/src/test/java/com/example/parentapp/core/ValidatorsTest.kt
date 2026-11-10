package com.example.parentapp.core

import com.example.parentapp.core.util.Validators
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `email validation`() {
        assertNull(Validators.emailError("sarah@example.com"))
        assertNotNull(Validators.emailError(""))
        assertNotNull(Validators.emailError("sarah@"))
        assertNotNull(Validators.emailError("sarah.example.com"))
    }

    @Test
    fun `password requires length letter and digit`() {
        assertNull(Validators.passwordError("Password1"))
        assertNotNull(Validators.passwordError("short1"))
        assertNotNull(Validators.passwordError("allletters"))
        assertNotNull(Validators.passwordError("12345678"))
    }

    @Test
    fun `confirm password must match`() {
        assertNull(Validators.confirmPasswordError("Password1", "Password1"))
        assertNotNull(Validators.confirmPasswordError("Password1", "Password2"))
    }

    @Test
    fun `phone needs at least seven digits`() {
        assertNull(Validators.phoneError("+256772000001"))
        assertNotNull(Validators.phoneError("12345"))
        assertNotNull(Validators.phoneError(""))
    }

    @Test
    fun `name needs two characters`() {
        assertNull(Validators.nameError("Jo"))
        assertNotNull(Validators.nameError("J"))
        assertNotNull(Validators.nameError(" "))
    }
}
