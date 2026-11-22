package com.metafoto.app.domain.model

import java.time.ZonedDateTime

/**
 * Resultado completo del análisis de una imagen. Los campos "curados" alimentan
 * las tarjetas principales; [sections] conserva todos los grupos y etiquetas
 * encontrados para que ningún metadato quede oculto.
 */
data class PhotoMetadata(
    val fileInfo: PhotoFileInfo,
    val gps: GpsData?,
    val captureDate: CaptureDateInfo?,
    val camera: CameraInfo?,
    val technical: TechnicalInfo,
    val descriptive: DescriptiveInfo,
    val sections: List<MetadataSection>,
    val warnings: List<String> = emptyList(),
) {
    val totalMetadataTags: Int
        get() = sections.sumOf { it.items.size }

    val hasEmbeddedMetadata: Boolean
        get() = sections.any { it.items.isNotEmpty() }
}

data class PhotoFileInfo(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val path: String,
    val width: Int?,
    val height: Int?,
) {
    val dimensions: Pair<Int, Int>?
        get() = if (width != null && height != null && width > 0 && height > 0) {
            width to height
        } else {
            null
        }
}

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val gpsTimestamp: String?,
    val mapDatum: String?,
    val imageDirectionDegrees: Double?,
)

data class CaptureDateInfo(
    val capturedAt: ZonedDateTime?,
    val originalRaw: String?,
    val digitizedRaw: String?,
    val modifiedRaw: String?,
    val hasExplicitTimeZone: Boolean,
    val source: String?,
)

data class CameraInfo(
    val make: String?,
    val model: String?,
    val lensMake: String?,
    val lensModel: String?,
    val lensSpecification: String?,
    val software: String?,
    val serialNumber: String?,
    val ownerName: String?,
) {
    val hasAnyValue: Boolean
        get() = listOf(
            make,
            model,
            lensMake,
            lensModel,
            lensSpecification,
            software,
            serialNumber,
            ownerName,
        ).any { !it.isNullOrBlank() }

    val displayName: String?
        get() = listOfNotNull(make, model)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { null }
}

data class TechnicalInfo(
    val iso: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val focalLength: String? = null,
    val focalLength35mm: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val orientation: String? = null,
    val resolution: String? = null,
    val compression: String? = null,
    val exposureProgram: String? = null,
    val exposureBias: String? = null,
    val meteringMode: String? = null,
    val colorSpace: String? = null,
    val digitalZoom: String? = null,
    val sceneType: String? = null,
    val contrast: String? = null,
    val saturation: String? = null,
    val sharpness: String? = null,
)

data class DescriptiveInfo(
    val title: String? = null,
    val description: String? = null,
    val author: String? = null,
    val copyright: String? = null,
    val keywords: String? = null,
    val city: String? = null,
    val location: String? = null,
    val province: String? = null,
    val country: String? = null,
    val credit: String? = null,
    val headline: String? = null,
    val instructions: String? = null,
) {
    val hasAnyValue: Boolean
        get() = listOf(
            title,
            description,
            author,
            copyright,
            keywords,
            city,
            location,
            province,
            country,
            credit,
            headline,
            instructions,
        ).any { !it.isNullOrBlank() }
}
