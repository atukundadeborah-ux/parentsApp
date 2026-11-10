package com.example.parentapp.core

import com.example.parentapp.core.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash is not the plaintext and has the encoded shape`() {
        val encoded = PasswordHasher.hash("Password123!")
        assertNotEquals("Password123!", encoded)
        assertEquals(4, encoded.split(":").size)
    }

    @Test
    fun `verify accepts the correct password`() {
        val encoded = PasswordHasher.hash("Sup3rSecret")
        assertTrue(PasswordHasher.verify("Sup3rSecret", encoded))
    }

    @Test
    fun `verify rejects a wrong password`() {
        val encoded = PasswordHasher.hash("Sup3rSecret")
        assertFalse(PasswordHasher.verify("sup3rsecret", encoded))
        assertFalse(PasswordHasher.verify("", encoded))
    }

    @Test
    fun `two hashes of the same password differ because of the salt`() {
        val a = PasswordHasher.hash("same-input")
        val b = PasswordHasher.hash("same-input")
        assertNotEquals(a, b)
        assertTrue(PasswordHasher.verify("same-input", a))
        assertTrue(PasswordHasher.verify("same-input", b))
    }

    @Test
    fun `verify returns false for a malformed hash`() {
        assertFalse(PasswordHasher.verify("x", "not-a-valid-hash"))
    }
}
