package com.example.parentapp.domain

import com.example.parentapp.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun `category maps to the expected role`() {
        assertEquals(UserRole.PARENT_GUARDIAN, UserRole.fromCategory("Parent / Guardian"))
        assertEquals(UserRole.CAREGIVER, UserRole.fromCategory("Professional caregiver"))
        assertEquals(UserRole.INSTITUTIONAL_USER, UserRole.fromCategory("Institution staff"))
        assertEquals(UserRole.ADMIN, UserRole.fromCategory("Administrator"))
    }

    @Test
    fun `unknown category falls back to parent guardian`() {
        assertEquals(UserRole.PARENT_GUARDIAN, UserRole.fromCategory("something else"))
    }
}
