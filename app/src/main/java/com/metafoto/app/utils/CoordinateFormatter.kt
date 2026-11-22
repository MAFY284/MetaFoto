package com.metafoto.app.utils

import com.metafoto.app.domain.model.GpsData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object CoordinateFormatter {
    fun decimal(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    fun copyString(gps: GpsData): String = decimal(gps.latitude, gps.longitude)

    fun latitude(latitude: Double): String =
        dms(latitude, positive = "N", negative = "S")

    fun longitude(longitude: Double): String =
        dms(longitude, positive = "E", negative = "O")

    fun altitude(meters: Double?): String? {
        if (meters == null) return null
        return String.format(Locale.getDefault(), "%.1f m", meters)
    }

    fun direction(degrees: Double?): String? {
        if (degrees == null) return null
        val normalized = ((degrees % 360) + 360) % 360
        return String.format(Locale.getDefault(), "%.0f° · %s", normalized, cardinal(normalized))
    }

    private fun dms(value: Double, positive: String, negative: String): String {
        val hemisphere = if (value >= 0) positive else negative
        val absolute = abs(value)
        val degrees = floor(absolute).toInt()
        val minutesFloat = (absolute - degrees) * 60.0
        val minutes = floor(minutesFloat).toInt()
        val seconds = (minutesFloat - minutes) * 60.0
        return String.format(
            Locale.getDefault(),
            "%d° %d′ %.1f″ %s",
            degrees,
            minutes,
            seconds,
            hemisphere,
        )
    }

    private fun cardinal(degrees: Double): String = when {
        degrees >= 337.5 || degrees < 22.5 -> "N"
        degrees < 67.5 -> "NE"
        degrees < 112.5 -> "E"
        degrees < 157.5 -> "SE"
        degrees < 202.5 -> "S"
        degrees < 247.5 -> "SO"
        degrees < 292.5 -> "O"
        else -> "NO"
    }
}
