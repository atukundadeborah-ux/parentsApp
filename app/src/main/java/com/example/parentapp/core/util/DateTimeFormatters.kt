package com.example.parentapp.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Central date/time helpers. All persistence uses epoch-millis `Long`s; the UI converts
 * through here. Backed by `java.time` (available on minSdk 24 via core-library desugaring).
 */
object DateTimeFormatters {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dateTimeFmt: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun formatDate(epochMillis: Long): String = localDateTime(epochMillis).format(dateFmt)

    fun formatTime(epochMillis: Long): String = localDateTime(epochMillis).format(timeFmt)

    fun formatDateTime(epochMillis: Long): String = localDateTime(epochMillis).format(dateTimeFmt)

    /** ISO `yyyy-MM-dd`, used for [com.example.parentapp.data.local.entity.ChildProfile.dateOfBirth]. */
    fun formatIsoDate(epochMillis: Long): String = localDate(epochMillis).format(isoDate)

    fun parseIsoDateToMillis(iso: String): Long? = runCatching {
        LocalDate.parse(iso, isoDate).atStartOfDay(zone).toInstant().toEpochMilli()
    }.getOrNull()

    fun localDateToMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDateTimeToMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone).toInstant().toEpochMilli()

    fun localDateTime(epochMillis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)

    fun localDate(epochMillis: Long): LocalDate = localDateTime(epochMillis).toLocalDate()

    /** Whole years between [dobIso] (`yyyy-MM-dd`) and today, or null if unparseable. */
    fun ageInYears(dobIso: String): Int? = runCatching {
        ChronoUnit.YEARS.between(LocalDate.parse(dobIso, isoDate), LocalDate.now(zone)).toInt()
    }.getOrNull()

    /** "in 3h", "2d ago", "just now" — coarse relative label for lists. */
    fun relative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = epochMillis - now
        val past = diff < 0
        val mins = abs(diff) / 60_000
        val label = when {
            mins < 1L -> return "just now"
            mins < 60L -> "${mins}m"
            mins < 1_440L -> "${mins / 60}h"
            mins < 43_200L -> "${mins / 1_440}d"
            else -> "${mins / 43_200}mo"
        }
        return if (past) "$label ago" else "in $label"
    }
}
