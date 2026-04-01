package com.example.cardwords

import com.example.cardwords.util.DateUtil
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilTest {

    private val UTC = TimeZone.UTC

    @Test
    fun epochMillisToDateString_epoch() {
        assertEquals("1970-01-01", DateUtil.epochMillisToDateString(0L, UTC))
    }

    @Test
    fun epochMillisToDateString_knownDate() {
        // 2023-11-14 12:00:00 UTC = 1699963200000
        assertEquals("2023-11-14", DateUtil.epochMillisToDateString(1699963200000L, UTC))
    }

    @Test
    fun epochMillisToDateString_startOfDay() {
        // 2024-01-01 00:00:00 UTC = 1704067200000
        assertEquals("2024-01-01", DateUtil.epochMillisToDateString(1704067200000L, UTC))
    }

    @Test
    fun daysAgoFromMillis_today() {
        val now = 1699963200000L // 2023-11-14
        assertEquals("2023-11-14", DateUtil.daysAgoFromMillis(now, 0, UTC))
    }

    @Test
    fun daysAgoFromMillis_yesterday() {
        val now = 1699963200000L // 2023-11-14
        assertEquals("2023-11-13", DateUtil.daysAgoFromMillis(now, 1, UTC))
    }

    @Test
    fun daysAgoFromMillis_weekAgo() {
        val now = 1699963200000L // 2023-11-14
        assertEquals("2023-11-07", DateUtil.daysAgoFromMillis(now, 7, UTC))
    }

    @Test
    fun todayString_usesClockFunction() {
        val fixedTime = 1699963200000L // 2023-11-14 12:00 UTC
        // todayString uses system default timezone, so we test via epochMillisToDateString with UTC
        assertEquals("2023-11-14", DateUtil.epochMillisToDateString(fixedTime, UTC))
    }

    @Test
    fun daysAgo_usesClockFunction() {
        val fixedTime = 1699963200000L // 2023-11-14 12:00 UTC
        assertEquals("2023-11-12", DateUtil.daysAgoFromMillis(fixedTime, 2, UTC))
    }

    @Test
    fun epochMillisToDateString_leapYear() {
        // 2024-02-29 12:00:00 UTC = 1709208000000
        assertEquals("2024-02-29", DateUtil.epochMillisToDateString(1709208000000L, UTC))
    }

    @Test
    fun epochMillisToDateString_yearBoundary() {
        // 2024-12-31 12:00:00 UTC = 1735646400000
        assertEquals("2024-12-31", DateUtil.epochMillisToDateString(1735646400000L, UTC))
    }

    @Test
    fun epochMillisToDateString_respectsTimezone() {
        // 2024-01-01 02:00:00 UTC = 1704074400000
        // In UTC+3 this should be 2024-01-01 05:00 → still Jan 1
        // In UTC-5 this should be 2023-12-31 21:00 → Dec 31
        val millis = 1704074400000L
        assertEquals("2024-01-01", DateUtil.epochMillisToDateString(millis, TimeZone.of("UTC+3")))
        assertEquals("2023-12-31", DateUtil.epochMillisToDateString(millis, TimeZone.of("UTC-5")))
    }
}
