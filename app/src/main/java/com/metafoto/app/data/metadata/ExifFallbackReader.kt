package com.metafoto.app.data.metadata

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.metafoto.app.domain.model.MetadataItem
import com.metafoto.app.domain.model.MetadataSection
import com.metafoto.app.utils.TextFormatting
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Segunda pasada sobre ExifInterface de Android. Sirve como respaldo para
 * formatos en los que metadata-extractor no encuentra un bloque, y también
 * completa campos GPS o técnicos que el lector principal no haya expuesto.
 */
internal class ExifFallbackReader(private val context: Context) {

    fun read(uri: Uri): ExifFallbackData? {
        val exif = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        }.getOrNull() ?: return null

        val attributes = linkedMapOf<String, String>()
        SECTION_TAGS.forEach { (tag, _) ->
            val value = runCatching { exif.getAttribute(tag) }.getOrNull()
                ?.let(TextFormatting::clean)
            if (value != null) attributes[tag] = value
        }

        val latitudeLongitude = runCatching { exif.getLatLong() }.getOrNull()
            ?.takeIf { it.size >= 2 }
        val latitude = latitudeLongitude?.getOrNull(0)
        val longitude = latitudeLongitude?.getOrNull(1)
        val altitude = runCatching { exif.getAltitude(Double.NaN) }.getOrNull()
            ?.takeUnless { it.isNaN() }
        val gpsTimestamp = runCatching { exif.getGpsDateTime() }.getOrNull()?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'"))
        }
        val orientation = runCatching {
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        }.getOrNull()
        val width = firstPositive(
            runCatching { exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) }.getOrNull(),
            runCatching { exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0) }.getOrNull(),
        )
        val height = firstPositive(
            runCatching { exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) }.getOrNull(),
            runCatching { exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0) }.getOrNull(),
        )

        return ExifFallbackData(
            attributes = attributes,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = altitude,
            gpsTimestamp = gpsTimestamp,
            orientation = orientation,
            imageWidth = width,
            imageHeight = height,
        )
    }

    private fun firstPositive(vararg values: Int?): Int? =
        values.firstOrNull { it != null && it > 0 }

    companion object {
        internal val SECTION_TAGS = linkedMapOf(
            ExifInterface.TAG_MAKE to "Marca",
            ExifInterface.TAG_MODEL to "Modelo",
            ExifInterface.TAG_SOFTWARE to "Software",
            ExifInterface.TAG_IMAGE_WIDTH to "Ancho",
            ExifInterface.TAG_IMAGE_LENGTH to "Alto",
            ExifInterface.TAG_PIXEL_X_DIMENSION to "Ancho (píxeles)",
            ExifInterface.TAG_PIXEL_Y_DIMENSION to "Alto (píxeles)",
            ExifInterface.TAG_ORIENTATION to "Orientación",
            ExifInterface.TAG_DATETIME to "Fecha y hora",
            ExifInterface.TAG_DATETIME_ORIGINAL to "Fecha y hora original",
            ExifInterface.TAG_DATETIME_DIGITIZED to "Fecha y hora digitalizada",
            ExifInterface.TAG_OFFSET_TIME to "Zona horaria",
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL to "Zona horaria original",
            ExifInterface.TAG_OFFSET_TIME_DIGITIZED to "Zona horaria digitalizada",
            ExifInterface.TAG_SUBSEC_TIME to "Fracción de segundo",
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL to "Fracción de segundo original",
            ExifInterface.TAG_EXIF_VERSION to "Versión EXIF",
            ExifInterface.TAG_ISO_SPEED to "ISO",
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY to "Sensibilidad fotográfica",
            ExifInterface.TAG_F_NUMBER to "Número f",
            ExifInterface.TAG_APERTURE_VALUE to "Valor de apertura",
            ExifInterface.TAG_EXPOSURE_TIME to "Tiempo de exposición",
            ExifInterface.TAG_SHUTTER_SPEED_VALUE to "Velocidad de obturación",
            ExifInterface.TAG_FOCAL_LENGTH to "Distancia focal",
            ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM to "Focal equivalente 35 mm",
            ExifInterface.TAG_FLASH to "Flash",
            ExifInterface.TAG_WHITE_BALANCE to "Balance de blancos",
            ExifInterface.TAG_LIGHT_SOURCE to "Fuente de luz",
            ExifInterface.TAG_EXPOSURE_PROGRAM to "Programa de exposición",
            ExifInterface.TAG_EXPOSURE_BIAS_VALUE to "Compensación de exposición",
            ExifInterface.TAG_METERING_MODE to "Modo de medición",
            ExifInterface.TAG_SCENE_CAPTURE_TYPE to "Tipo de escena",
            ExifInterface.TAG_COLOR_SPACE to "Espacio de color",
            ExifInterface.TAG_COMPRESSION to "Compresión",
            ExifInterface.TAG_DIGITAL_ZOOM_RATIO to "Zoom digital",
            ExifInterface.TAG_CONTRAST to "Contraste",
            ExifInterface.TAG_SATURATION to "Saturación",
            ExifInterface.TAG_SHARPNESS to "Nitidez",
            ExifInterface.TAG_MAKER_NOTE to "Nota del fabricante",
            ExifInterface.TAG_ARTIST to "Autor",
            ExifInterface.TAG_COPYRIGHT to "Copyright",
            ExifInterface.TAG_IMAGE_DESCRIPTION to "Descripción",
            ExifInterface.TAG_USER_COMMENT to "Comentario de usuario",
            ExifInterface.TAG_LENS_MAKE to "Marca de la lente",
            ExifInterface.TAG_LENS_MODEL to "Modelo de la lente",
            ExifInterface.TAG_LENS_SPECIFICATION to "Especificación de la lente",
            ExifInterface.TAG_LENS_SERIAL_NUMBER to "Número de serie de la lente",
            ExifInterface.TAG_BODY_SERIAL_NUMBER to "Número de serie del cuerpo",
            ExifInterface.TAG_CAMERA_OWNER_NAME to "Propietario de la cámara",
            ExifInterface.TAG_IMAGE_UNIQUE_ID to "ID único de imagen",
            ExifInterface.TAG_GPS_LATITUDE to "Latitud GPS",
            ExifInterface.TAG_GPS_LATITUDE_REF to "Referencia de latitud",
            ExifInterface.TAG_GPS_LONGITUDE to "Longitud GPS",
            ExifInterface.TAG_GPS_LONGITUDE_REF to "Referencia de longitud",
            ExifInterface.TAG_GPS_ALTITUDE to "Altitud GPS",
            ExifInterface.TAG_GPS_ALTITUDE_REF to "Referencia de altitud",
            ExifInterface.TAG_GPS_TIMESTAMP to "Hora GPS",
            ExifInterface.TAG_GPS_DATESTAMP to "Fecha GPS",
            ExifInterface.TAG_GPS_MAP_DATUM to "Datum GPS",
            ExifInterface.TAG_GPS_PROCESSING_METHOD to "Método de posicionamiento",
            ExifInterface.TAG_GPS_IMG_DIRECTION to "Dirección de la imagen GPS",
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF to "Referencia de dirección",
        )

        fun labelFor(tag: String): String? = SECTION_TAGS[tag]
    }
}

internal data class ExifFallbackData(
    val attributes: Map<String, String>,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val gpsTimestamp: String?,
    val orientation: Int?,
    val imageWidth: Int?,
    val imageHeight: Int?,
) {
    fun value(tag: String): String? = attributes[tag]?.takeIf { it.isNotBlank() }

    fun section(): MetadataSection? {
        if (attributes.isEmpty()) return null
        return MetadataSection(
            name = "ExifInterface (Android)",
            items = attributes.mapNotNull { (tag, value) ->
                val label = ExifFallbackReader.labelFor(tag) ?: tag
                MetadataItem(label, value)
            },
        )
    }
}
