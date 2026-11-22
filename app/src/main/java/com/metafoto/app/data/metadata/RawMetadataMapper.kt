package com.metafoto.app.data.metadata

import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.Tag
import com.drew.metadata.xmp.XmpDirectory
import com.metafoto.app.domain.model.MetadataItem
import com.metafoto.app.domain.model.MetadataSection
import com.metafoto.app.utils.TextFormatting

/** Convierte los directorios de metadata-extractor en grupos legibles sin perder etiquetas. */
internal object RawMetadataMapper {

    fun toSections(metadata: Metadata): List<MetadataSection> {
        val result = mutableListOf<MetadataSection>()
        var anonymousIndex = 1

        metadata.directories.forEach { directory ->
            if (directory is XmpDirectory) {
                val xmpItems = toXmpItems(directory)
                if (xmpItems.isNotEmpty()) {
                    result += MetadataSection("XMP", xmpItems)
                    return@forEach
                }
            }

            val items = mutableListOf<MetadataItem>()
            directory.tags.forEach { tag ->
                toItem(tag)?.let(items::add)
            }
            directory.errors
                .mapNotNull(TextFormatting::clean)
                .takeIf { it.isNotEmpty() }
                ?.let { errors ->
                    items += MetadataItem("Avisos de extracción", errors.joinToString(" · "))
                }

            if (items.isNotEmpty()) {
                val rawName = directory.name.takeUnless { it.isNullOrBlank() }
                    ?: "Grupo ${anonymousIndex++}"
                result += MetadataSection(translateDirectory(rawName), items)
            }
        }

        return deduplicateNames(result)
    }

    fun addFallbackSection(
        sections: List<MetadataSection>,
        fallback: ExifFallbackData?,
    ): List<MetadataSection> {
        if (fallback == null) return sections
        val hasExifData = sections.any {
            val name = it.name.uppercase()
            name.contains("EXIF") || name.contains("GPS") || name.contains("XMP") || name.contains("IPTC")
        }
        if (hasExifData) return sections

        val fallbackSection = fallback.section() ?: return sections
        return sections + fallbackSection
    }

    private fun toXmpItems(directory: XmpDirectory): List<MetadataItem> = runCatching {
        directory.xmpProperties
            .filterValues { it.isNotBlank() }
            .map { (path, value) ->
                val tag = path.trimStart('/').replace("/", " › ")
                MetadataItem(
                    tag = tag.ifBlank { "XMP" },
                    value = TextFormatting.truncate(value.replace('\u0000', ' ').trim()),
                )
            }
            .sortedBy { it.tag.lowercase() }
    }.getOrDefault(emptyList())

    private fun toItem(tag: Tag): MetadataItem? {
        val name = runCatching { tag.tagName }.getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Etiqueta ${tag.tagTypeHex}"
        val description = runCatching { tag.description }.getOrNull()
            ?.let(TextFormatting::clean)
            ?: return null

        return MetadataItem(
            tag = name,
            value = TextFormatting.truncate(description),
        )
    }

    private fun deduplicateNames(sections: List<MetadataSection>): List<MetadataSection> {
        val occurrences = mutableMapOf<String, Int>()
        return sections.map { section ->
            val count = (occurrences[section.name] ?: 0) + 1
            occurrences[section.name] = count
            if (count == 1) section else section.copy(name = "${section.name} ($count)")
        }
    }

    private fun translateDirectory(name: String): String = when (name) {
        "Exif IFD0" -> "EXIF · Cámara"
        "Exif SubIFD" -> "EXIF · Captura"
        "Exif Thumbnail" -> "EXIF · Miniatura"
        "Exif Interop" -> "EXIF · Interoperabilidad"
        "GPS" -> "GPS"
        "IPTC" -> "IPTC"
        "XMP" -> "XMP"
        "ICC Profile" -> "Perfil de color ICC"
        "Photoshop" -> "Adobe Photoshop"
        "File Type" -> "Tipo de archivo"
        "JPEG" -> "JPEG"
        "PNG" -> "PNG"
        "WebP" -> "WebP"
        "HEIF" -> "HEIF"
        "QuickTime" -> "QuickTime"
        "MP4" -> "MP4"
        "GeoTIFF" -> "GeoTIFF"
        "GIF" -> "GIF"
        "BMP" -> "BMP"
        "PCX" -> "PCX"
        "ICO" -> "ICO"
        "EPS" -> "EPS"
        "MP3" -> "MP3"
        "WAV" -> "WAV"
        else -> name
    }
}
