package com.example.cardwords.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object DateUtil {

    fun epochMillisToDateString(
        millis: Long,
        tz: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val localDate = instant.toLocalDateTime(tz).date
        return "${localDate.year.toString().padStart(4, '0')}-${
            localDate.monthNumber.toString().padStart(2, '0')
        }-${localDate.day.toString().padStart(2, '0')}"
    }

    fun todayString(clock: () -> Long): String = epochMillisToDateString(clock())

    fun daysAgo(clock: () -> Long, days: Int): String {
        return daysAgoFromMillis(clock(), days)
    }

    fun daysAgoFromMillis(
        millis: Long,
        days: Int,
        tz: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val localDate = instant.toLocalDateTime(tz).date
        val targetDate = localDate.minus(days, DateTimeUnit.DAY)
        return "${targetDate.year.toString().padStart(4, '0')}-${
            targetDate.monthNumber.toString().padStart(2, '0')
        }-${targetDate.day.toString().padStart(2, '0')}"
    }
}
