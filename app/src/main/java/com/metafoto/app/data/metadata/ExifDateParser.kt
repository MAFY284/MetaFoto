package com.metafoto.app.data.metadata

import com.metafoto.app.utils.TextFormatting
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

internal data class ParsedCaptureDate(
    val value: ZonedDateTime,
    val hasExplicitTimeZone: Boolean,
)

internal object ExifDateParser {
    private val exifFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)

    private val exifWithOffsetFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX", Locale.US),
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss Z", Locale.US),
        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss.SSSXXX", Locale.US),
    )

    fun parseExif(
        rawValue: String?,
        subSeconds: String?,
        offsetValue: String?,
    ): ParsedCaptureDate? {
        val raw = TextFormatting.clean(rawValue) ?: return null
        val normalized = raw.replace('T', ' ').trim()

        exifWithOffsetFormatters.forEach { formatter ->
            runCatching {
                val parsed = OffsetDateTime.parse(normalized, formatter)
                return ParsedCaptureDate(parsed.toZonedDateTime(), true)
            }
        }

        val base = normalized.take(19)
        val localDateTime = runCatching {
            LocalDateTime.parse(base, exifFormatter)
        }.getOrNull() ?: return null

        val withFraction = applySubSeconds(localDateTime, subSeconds)
        val offset = parseOffset(offsetValue)
        return if (offset != null) {
            ParsedCaptureDate(withFraction.atZone(offset), true)
        } else {
            ParsedCaptureDate(withFraction.atZone(ZoneId.systemDefault()), false)
        }
    }

    fun parseIso(rawValue: String?): ParsedCaptureDate? {
        val raw = TextFormatting.clean(rawValue) ?: return null
        val value = raw.removeSuffix("\u0000").trim()

        try {
            return ParsedCaptureDate(OffsetDateTime.parse(value).toZonedDateTime(), true)
        } catch (_: DateTimeParseException) {
            // Sigue con los demás formatos.
        }

        try {
            return ParsedCaptureDate(ZonedDateTime.parse(value), true)
        } catch (_: DateTimeParseException) {
            // Sigue con los demás formatos.
        }

        try {
            val local = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return ParsedCaptureDate(local.atZone(ZoneId.systemDefault()), false)
        } catch (_: DateTimeParseException) {
            // Sigue con los demás formatos.
        }

        try {
            val date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            return ParsedCaptureDate(date.atStartOfDay(ZoneId.systemDefault()), false)
        } catch (_: DateTimeParseException) {
            return null
        }
    }

    private fun applySubSeconds(dateTime: LocalDateTime, rawSubSeconds: String?): LocalDateTime {
        val digits = rawSubSeconds
            ?.let(TextFormatting::clean)
            ?.filter(Char::isDigit)
            ?.take(9)
            ?.takeIf { it.isNotEmpty() }
            ?: return dateTime
        val nanos = digits.padEnd(9, '0').toIntOrNull() ?: return dateTime
        return dateTime.withNano(nanos)
    }

    private fun parseOffset(rawValue: String?): ZoneOffset? {
        val raw = TextFormatting.clean(rawValue) ?: return null
        val withoutPrefix = raw
            .removePrefix("UTC")
            .removePrefix("GMT")
            .replace(" ", "")
            .replace(":", "")
        val normalized = when {
            withoutPrefix == "Z" -> "+0000"
            withoutPrefix.matches(Regex("[+-]\\d{2}")) -> "${withoutPrefix}00"
            withoutPrefix.matches(Regex("[+-]\\d{4}")) -> withoutPrefix
            else -> return null
        }
        val formatted = normalized.take(3) + ":" + normalized.drop(3)
        return runCatching { ZoneOffset.of(formatted) }.getOrNull()
    }
}
