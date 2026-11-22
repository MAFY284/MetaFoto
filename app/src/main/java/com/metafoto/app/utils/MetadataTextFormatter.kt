package com.metafoto.app.utils

import com.metafoto.app.domain.model.CameraInfo
import com.metafoto.app.domain.model.CaptureDateInfo
import com.metafoto.app.domain.model.DescriptiveInfo
import com.metafoto.app.domain.model.PhotoMetadata
import com.metafoto.app.domain.model.TechnicalInfo

object MetadataTextFormatter {

    fun summary(metadata: PhotoMetadata): String = build(metadata, includeRawSections = false)

    fun full(metadata: PhotoMetadata): String = build(metadata, includeRawSections = true)

    private fun build(metadata: PhotoMetadata, includeRawSections: Boolean): String = buildString {
        appendLine("METAFOTO · METADATOS DE IMAGEN")
        appendLine("=".repeat(34))
        appendLine()

        appendFile(metadata)
        appendLocation(metadata)
        appendDate(metadata.captureDate)
        appendCamera(metadata.camera)
        appendTechnical(metadata.technical)
        appendDescriptive(metadata.descriptive)

        if (metadata.warnings.isNotEmpty()) {
            appendLine("AVISOS")
            appendLine("-".repeat(34))
            metadata.warnings.forEach { appendLine("• $it") }
            appendLine()
        }

        if (includeRawSections && metadata.sections.isNotEmpty()) {
            appendLine("TODOS LOS METADATOS")
            appendLine("=".repeat(34))
            metadata.sections.forEach { section ->
                appendLine()
                appendLine("[${section.name}]")
                section.items.forEach { item ->
                    appendLine("${item.tag}: ${sanitize(item.value, multiline = true)}")
                }
            }
            appendLine()
        }

        appendLine("—")
        appendLine("Generado localmente con MetaFoto. Sin anuncios, cuentas ni telemetría.")
    }

    private fun StringBuilder.appendFile(metadata: PhotoMetadata) {
        val file = metadata.fileInfo
        appendLine("ARCHIVO")
        appendLine("-".repeat(34))
        row("Nombre", file.fileName)
        row("Tipo", TextFormatting.mimeTypeLabel(file.mimeType))
        row("Tamaño", TextFormatting.bytes(file.sizeBytes))
        file.dimensions?.let { (width, height) -> row("Dimensiones", "$width × $height px") }
        row("Última modificación", DateDisplayFormatter.timestamp(file.lastModifiedMillis))
        row("Ruta / identificador", file.path)
        row("URI", file.uri)
        appendLine()
    }

    private fun StringBuilder.appendLocation(metadata: PhotoMetadata) {
        appendLine("UBICACIÓN")
        appendLine("-".repeat(34))
        val gps = metadata.gps
        if (gps == null) {
            appendLine("Sin coordenadas GPS.")
        } else {
            row("Latitud", CoordinateFormatter.latitude(gps.latitude))
            row("Longitud", CoordinateFormatter.longitude(gps.longitude))
            row("Coordenadas decimales", CoordinateFormatter.decimal(gps.latitude, gps.longitude))
            row("Altitud", CoordinateFormatter.altitude(gps.altitudeMeters))
            row("Dirección de la imagen", CoordinateFormatter.direction(gps.imageDirectionDegrees))
            row("Fecha GPS", gps.gpsTimestamp)
            row("Datum", gps.mapDatum)
        }
        appendLine()
    }

    private fun StringBuilder.appendDate(date: CaptureDateInfo?) {
        appendLine("FECHA Y HORA")
        appendLine("-".repeat(34))
        val captured = date?.capturedAt
        if (captured == null) {
            appendLine("No se encontró una fecha de captura.")
            row("Fecha original (EXIF)", date?.originalRaw)
        } else {
            val explicitTimeZone = date?.hasExplicitTimeZone == true
            row("Día de la semana", DateDisplayFormatter.weekday(captured))
            row("Fecha", DateDisplayFormatter.fullDate(captured))
            row("Hora", DateDisplayFormatter.time(captured))
            row(
                "Zona horaria",
                DateDisplayFormatter.zoneLabel(captured, explicitTimeZone) +
                    if (explicitTimeZone) "" else " (zona del dispositivo; no indicada en EXIF)",
            )
            row("Origen de la fecha", date?.source)
            row("Fecha original (EXIF)", date?.originalRaw)
        }
        row("Fecha digitalizada", date?.digitizedRaw)
        row("Fecha de modificación EXIF", date?.modifiedRaw)
        appendLine()
    }

    private fun StringBuilder.appendCamera(camera: CameraInfo?) {
        appendLine("DISPOSITIVO Y CÁMARA")
        appendLine("-".repeat(34))
        if (camera == null || !camera.hasAnyValue) {
            appendLine("Sin información del dispositivo.")
        } else {
            row("Marca", camera.make)
            row("Modelo", camera.model)
            row("Lente", camera.lensModel)
            row("Marca de la lente", camera.lensMake)
            row("Especificación de la lente", camera.lensSpecification)
            row("Software", camera.software)
            row("Número de serie", camera.serialNumber)
            row("Propietario", camera.ownerName)
        }
        appendLine()
    }

    private fun StringBuilder.appendTechnical(technical: TechnicalInfo) {
        appendLine("PARÁMETROS TÉCNICOS")
        appendLine("-".repeat(34))
        row("ISO", technical.iso)
        row("Apertura", technical.aperture)
        row("Velocidad de obturación", technical.shutterSpeed)
        row("Distancia focal", technical.focalLength)
        row("Equivalente 35 mm", technical.focalLength35mm)
        row("Flash", technical.flash)
        row("Balance de blancos", technical.whiteBalance)
        row("Orientación", technical.orientation)
        row("Resolución original", technical.resolution)
        row("Compresión", technical.compression)
        row("Programa de exposición", technical.exposureProgram)
        row("Compensación de exposición", technical.exposureBias)
        row("Medición", technical.meteringMode)
        row("Espacio de color", technical.colorSpace)
        row("Zoom digital", technical.digitalZoom)
        row("Tipo de escena", technical.sceneType)
        row("Contraste", technical.contrast)
        row("Saturación", technical.saturation)
        row("Nitidez", technical.sharpness)
        appendLine()
    }

    private fun StringBuilder.appendDescriptive(descriptive: DescriptiveInfo) {
        appendLine("OTROS METADATOS")
        appendLine("-".repeat(34))
        if (!descriptive.hasAnyValue) {
            appendLine("Sin datos descriptivos, de autoría o derechos.")
        } else {
            row("Título", descriptive.title)
            row("Descripción", descriptive.description)
            row("Autor", descriptive.author)
            row("Copyright", descriptive.copyright)
            row("Palabras clave", descriptive.keywords)
            row("Ciudad", descriptive.city)
            row("Ubicación descrita", descriptive.location)
            row("Provincia / estado", descriptive.province)
            row("País", descriptive.country)
            row("Crédito", descriptive.credit)
            row("Titular", descriptive.headline)
            row("Instrucciones", descriptive.instructions)
        }
        appendLine()
    }

    private fun StringBuilder.row(label: String, value: String?) {
        val clean = value?.let { TextFormatting.clean(it) } ?: return
        appendLine("$label: ${sanitize(clean, multiline = true)}")
    }

    private fun sanitize(value: String, multiline: Boolean): String {
        val normalized = value.replace('\u0000', ' ').trim()
        return if (multiline) {
            normalized.lineSequence().joinToString("\n    ") { it.trim() }
        } else {
            normalized.replace('\n', ' ')
        }
    }
}
