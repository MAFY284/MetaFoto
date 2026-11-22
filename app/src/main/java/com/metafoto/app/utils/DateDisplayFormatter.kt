package com.metafoto.app.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateDisplayFormatter {
    private val spanish: Locale = Locale("es", "ES")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", spanish)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", spanish)
    private val fileDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", spanish)

    fun weekday(value: ZonedDateTime): String =
        value.format(DateTimeFormatter.ofPattern("EEEE", spanish)).replaceFirstChar { it.titlecase(spanish) }

    fun fullDate(value: ZonedDateTime): String =
        value.format(fullDateFormatter).replaceFirstChar { it.titlecase(spanish) }

    fun shortDate(value: ZonedDateTime): String = value.format(shortDateFormatter)

    fun time(value: ZonedDateTime): String = value.format(timeFormatter)

    fun dateTime(value: ZonedDateTime): String = "${fullDate(value)} · ${time(value)}"

    fun zoneLabel(value: ZonedDateTime, explicit: Boolean): String {
        if (!explicit) return value.zone.id
        val offset = value.offset.id
        return if (offset == "Z") "UTC" else "UTC$offset"
    }

    fun timestamp(epochMillis: Long?): String? {
        if (epochMillis == null || epochMillis <= 0L) return null
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(fileDateFormatter)
    }
}
