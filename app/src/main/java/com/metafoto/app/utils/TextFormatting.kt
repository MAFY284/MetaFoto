package com.metafoto.app.utils

import java.util.Locale

object TextFormatting {
    fun clean(value: String?): String? {
        if (value == null) return null
        val cleaned = value
            .replace('\u0000', ' ')
            .replace("\r\n", "\n")
            .trim()
        return cleaned.ifBlank { null }
    }

    fun truncate(value: String, maxLength: Int = 4_000): String {
        if (value.length <= maxLength) return value
        return value.take(maxLength).trimEnd() + "…"
    }

    fun bytes(size: Long?): String? {
        if (size == null || size < 0) return null
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var amount = size.toDouble()
        var index = 0
        while (amount >= 1024.0 && index < units.lastIndex) {
            amount /= 1024.0
            index++
        }
        val pattern = if (index == 0) "%.0f %s" else "%.2f %s"
        return String.format(Locale.getDefault(), pattern, amount, units[index])
    }

    fun mimeTypeLabel(mimeType: String?): String {
        if (mimeType.isNullOrBlank()) return "Desconocido"
        return when (mimeType.lowercase(Locale.ROOT)) {
            "image/jpeg" -> "JPEG"
            "image/jpg" -> "JPEG"
            "image/png" -> "PNG"
            "image/webp" -> "WebP"
            "image/heic" -> "HEIC"
            "image/heif" -> "HEIF"
            "image/tiff" -> "TIFF"
            "image/avif" -> "AVIF"
            else -> mimeType.substringAfter('/', mimeType).uppercase(Locale.getDefault())
        }
    }
}
