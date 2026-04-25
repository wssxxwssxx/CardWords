package com.example.cardwords.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object DateUtil {

    fun epochMillisToDateString(
        millis: Long,
        tz: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
        return formatDate(instant.toLocalDateTime(tz).date)
    }

    fun todayString(clock: () -> Long): String = epochMillisToDateString(clock())

    fun daysAgo(clock: () -> Long, days: Int): String = daysAgoFromMillis(clock(), days)

    fun daysAgoFromMillis(
        millis: Long,
        days: Int,
        tz: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
        val targetDate = instant.toLocalDateTime(tz).date.minus(days, DateTimeUnit.DAY)
        return formatDate(targetDate)
    }

    private fun formatDate(date: LocalDate): String =
        "${date.year.toString().padStart(4, '0')}-${
            (date.month.ordinal + 1).toString().padStart(2, '0')
        }-${date.day.toString().padStart(2, '0')}"
}
