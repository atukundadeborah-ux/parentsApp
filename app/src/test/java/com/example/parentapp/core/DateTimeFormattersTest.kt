package com.example.parentapp.core

import com.example.parentapp.core.util.DateTimeFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateTimeFormattersTest {

    @Test
    fun `iso round trip`() {
        val millis = DateTimeFormatters.localDateToMillis(LocalDate.of(2020, 6, 15))
        assertEquals("2020-06-15", DateTimeFormatters.formatIsoDate(millis))
        assertEquals(millis, DateTimeFormatters.parseIsoDateToMillis("2020-06-15"))
    }

    @Test
    fun `parse rejects garbage`() {
        assertNull(DateTimeFormatters.parseIsoDateToMillis("nonsense"))
    }

    @Test
    fun `age in years is computed from dob`() {
        val tenYearsAgo = LocalDate.now().minusYears(10).minusDays(2)
        assertEquals(10, DateTimeFormatters.ageInYears(tenYearsAgo.toString()))
        assertNull(DateTimeFormatters.ageInYears("not-a-date"))
    }

    @Test
    fun `relative label reflects direction`() {
        val now = 1_000_000_000_000L
        assertTrue(DateTimeFormatters.relative(now + 3 * 3_600_000L, now).contains("in"))
        assertTrue(DateTimeFormatters.relative(now - 2 * 86_400_000L, now).contains("ago"))
        assertEquals("just now", DateTimeFormatters.relative(now + 10_000L, now))
    }
}
