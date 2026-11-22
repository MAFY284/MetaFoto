package com.metafoto.app.data.metadata

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.iptc.IptcDirectory
import com.drew.metadata.xmp.XmpDirectory
import com.metafoto.app.data.local.MediaFileInfoReader
import com.metafoto.app.data.local.MediaUriResolver
import com.metafoto.app.domain.model.CameraInfo
import com.metafoto.app.domain.model.CaptureDateInfo
import com.metafoto.app.domain.model.DescriptiveInfo
import com.metafoto.app.domain.model.GpsData
import com.metafoto.app.domain.model.MetadataException
import com.metafoto.app.domain.model.MetadataItem
import com.metafoto.app.domain.model.PhotoFileInfo
import com.metafoto.app.domain.model.PhotoMetadata
import com.metafoto.app.domain.model.TechnicalInfo
import com.metafoto.app.utils.TextFormatting
import java.util.Locale

/**
 * Lectura principal con metadata-extractor (EXIF, IPTC, XMP, ICC, GPS...).
 * ExifInterface de Android actúa como respaldo para formatos o bloques que el
 * lector principal no pueda interpretar.
 */
class MetadataExtractor(private val context: Context) {

    private val fileInfoReader = MediaFileInfoReader(context)
    private val fallbackReader = ExifFallbackReader(context)

    fun extract(uri: Uri): PhotoMetadata {
        val warnings = mutableListOf<String>()
        val sourceUri = MediaUriResolver.preferOriginal(context, uri)
        val baseFileInfo = fileInfoReader.read(sourceUri)
        val fallback = fallbackReader.read(sourceUri)

        var metadata: Metadata? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                metadata = ImageMetadataReader.readMetadata(stream)
            }
        } catch (error: Exception) {
            warnings += "El lector principal no pudo interpretar todo el archivo: " +
                safeErrorMessage(error)
        } catch (error: OutOfMemoryError) {
            warnings += "El archivo contiene metadatos demasiado grandes para procesarlos por completo."
        }

        var rawSections = metadata
            ?.let { runCatching { RawMetadataMapper.toSections(it) }.getOrDefault(emptyList()) }
            .orEmpty()

        if (rawSections.isEmpty()) {
            rawSections = RawMetadataMapper.toSections(metadata ?: emptyMetadata())
        }

        val sections = RawMetadataMapper.addFallbackSection(rawSections, fallback)
        val lookup = MetadataLookup(metadata, fallback)
        val dimensions = resolveDimensions(metadata, fallback, sourceUri)
        val fileInfo = baseFileInfo.copy(
            width = dimensions?.first,
            height = dimensions?.second,
        )

        val hasReadableData = sections.isNotEmpty() ||
            fallback?.attributes?.isNotEmpty() == true ||
            dimensions != null

        if (!hasReadableData) {
            throw MetadataException(
                "La imagen está dañada o su formato no es compatible. " +
                    "Prueba con un archivo JPG, PNG, HEIC, TIFF o WebP válido.",
            )
        }

        val technical = extractTechnical(lookup, metadata, fallback, fileInfo)
        val gps = extractGps(metadata, fallback)
        val metadataResult = PhotoMetadata(
            fileInfo = fileInfo,
            gps = gps,
            captureDate = extractDate(lookup, fallback),
            camera = extractCamera(lookup),
            technical = technical,
            descriptive = extractDescriptive(lookup),
            sections = sections,
            warnings = warnings.distinct(),
        )

        return metadataResult
    }

    private fun resolveDimensions(
        metadata: Metadata?,
        fallback: ExifFallbackData?,
        uri: Uri,
    ): Pair<Int, Int>? {
        val subIfd = metadata?.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val fromSub = runCatching {
            val width = subIfd?.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH)
            val height = subIfd?.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT)
            validDimensions(width, height)
        }.getOrNull()

        val fromFallback = validDimensions(fallback?.imageWidth, fallback?.imageHeight)

        val fromIfd0 = runCatching {
            val ifd0 = metadata?.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            validDimensions(
                ifd0?.getInteger(ExifDirectoryBase.TAG_IMAGE_WIDTH),
                ifd0?.getInteger(ExifDirectoryBase.TAG_IMAGE_HEIGHT),
            )
        }.getOrNull()

        val fromDecoded = fileInfoReader.readDecodedBounds(uri)

        return fromSub ?: fromFallback ?: fromIfd0 ?: fromDecoded
    }

    private fun validDimensions(width: Int?, height: Int?): Pair<Int, Int>? =
        if (width != null && height != null && width > 0 && height > 0) width to height else null

    private fun extractGps(metadata: Metadata?, fallback: ExifFallbackData?): GpsData? {
        val gpsDirectory = metadata?.getFirstDirectoryOfType(GpsDirectory::class.java)
        val geoLocation = runCatching { gpsDirectory?.geoLocation }.getOrNull()

        var latitude = geoLocation?.latitude
        var longitude = geoLocation?.longitude

        if (!isValidCoordinate(latitude, longitude)) {
            latitude = fallback?.latitude
            longitude = fallback?.longitude
        }
        if (!isValidCoordinate(latitude, longitude)) return null

        val altitude = gpsDirectory?.let {
            runCatching {
                val raw = it.getDoubleObject(GpsDirectory.TAG_ALTITUDE) ?: return@runCatching null
                val reference = it.getString(GpsDirectory.TAG_ALTITUDE_REF)
                    ?: it.getDescription(GpsDirectory.TAG_ALTITUDE_REF)
                if (reference?.contains("below", ignoreCase = true) == true || reference?.trim() == "1") {
                    -raw
                } else {
                    raw
                }
            }.getOrNull()
        } ?: fallback?.altitudeMeters

        val dateStamp = cleanDescription(gpsDirectory, GpsDirectory.TAG_DATE_STAMP)
        val timeStamp = cleanDescription(gpsDirectory, GpsDirectory.TAG_TIME_STAMP)
        val gpsTimestamp = listOfNotNull(dateStamp, timeStamp)
            .joinToString(" ")
            .ifBlank { fallback?.gpsTimestamp }

        val direction = gpsDirectory?.let {
            runCatching { it.getDoubleObject(GpsDirectory.TAG_IMG_DIRECTION) }.getOrNull()
        } ?: fallback?.value(ExifInterface.TAG_GPS_IMG_DIRECTION)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        return GpsData(
            latitude = latitude ?: return null,
            longitude = longitude ?: return null,
            altitudeMeters = altitude,
            gpsTimestamp = gpsTimestamp,
            mapDatum = cleanDescription(gpsDirectory, GpsDirectory.TAG_MAP_DATUM)
                ?: fallback?.value(ExifInterface.TAG_GPS_MAP_DATUM),
            imageDirectionDegrees = direction,
        )
    }

    private fun isValidCoordinate(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun extractDate(lookup: MetadataLookup, fallback: ExifFallbackData?): CaptureDateInfo? {
        val originalRaw = lookup.description(ExifDirectoryBase.TAG_DATETIME_ORIGINAL)
            ?: fallback?.value(ExifInterface.TAG_DATETIME_ORIGINAL)
        val digitizedRaw = lookup.description(ExifDirectoryBase.TAG_DATETIME_DIGITIZED)
            ?: fallback?.value(ExifInterface.TAG_DATETIME_DIGITIZED)
        val modifiedRaw = lookup.description(ExifDirectoryBase.TAG_DATETIME)
            ?: fallback?.value(ExifInterface.TAG_DATETIME)
        val subSeconds = lookup.string(ExifDirectoryBase.TAG_SUBSECOND_TIME_ORIGINAL)
            ?: fallback?.value(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL)
        val offset = lookup.string(ExifDirectoryBase.TAG_TIME_ZONE_ORIGINAL)
            ?: lookup.description(ExifDirectoryBase.TAG_TIME_ZONE_ORIGINAL)
            ?: fallback?.value(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
            ?: fallback?.value(ExifInterface.TAG_OFFSET_TIME)

        var parsed = ExifDateParser.parseExif(originalRaw, subSeconds, offset)
        var source = if (parsed != null) "EXIF · Date/Time Original" else null

        if (parsed == null) {
            val xmpDate = lookup.firstXmpValue(
                "exif:DateTimeOriginal",
                "DateTimeOriginal",
                "xmp:CreateDate",
                "photoshop:DateCreated",
            )
            parsed = ExifDateParser.parseIso(xmpDate)
            if (parsed != null) source = "XMP · fecha de creación"
        }

        if (parsed == null) {
            parsed = ExifDateParser.parseExif(digitizedRaw, null, offset)
            if (parsed != null) source = "EXIF · Date/Time Digitized"
        }

        if (parsed == null) {
            parsed = ExifDateParser.parseExif(modifiedRaw, null, offset)
            if (parsed != null) source = "EXIF · Date/Time"
        }

        if (parsed == null && originalRaw == null && digitizedRaw == null && modifiedRaw == null) {
            return null
        }

        if (source == null) {
            source = when {
                originalRaw != null -> "EXIF · Date/Time Original (sin formato reconocido)"
                digitizedRaw != null -> "EXIF · Date/Time Digitized (sin formato reconocido)"
                else -> "EXIF · Date/Time (sin formato reconocido)"
            }
        }

        return CaptureDateInfo(
            capturedAt = parsed?.value,
            originalRaw = TextFormatting.clean(originalRaw),
            digitizedRaw = TextFormatting.clean(digitizedRaw),
            modifiedRaw = TextFormatting.clean(modifiedRaw),
            hasExplicitTimeZone = parsed?.hasExplicitTimeZone ?: hasZoneOffset(offset),
            source = source,
        )
    }

    private fun hasZoneOffset(raw: String?): Boolean =
        raw?.trim()?.matches(Regex("(UTC|GMT)?[+-]\\d{2}:?\\d{2}")) == true

    private fun extractCamera(lookup: MetadataLookup): CameraInfo? {
        val fallback = lookup.fallback
        val camera = CameraInfo(
            make = lookup.description(ExifDirectoryBase.TAG_MAKE)
                ?: fallback?.value(ExifInterface.TAG_MAKE),
            model = lookup.description(ExifDirectoryBase.TAG_MODEL)
                ?: fallback?.value(ExifInterface.TAG_MODEL),
            lensMake = lookup.description(ExifDirectoryBase.TAG_LENS_MAKE)
                ?: fallback?.value(ExifInterface.TAG_LENS_MAKE),
            lensModel = lookup.description(ExifDirectoryBase.TAG_LENS_MODEL)
                ?: fallback?.value(ExifInterface.TAG_LENS_MODEL),
            lensSpecification = lookup.description(ExifDirectoryBase.TAG_LENS_SPECIFICATION)
                ?: fallback?.value(ExifInterface.TAG_LENS_SPECIFICATION),
            software = lookup.description(ExifDirectoryBase.TAG_SOFTWARE)
                ?: fallback?.value(ExifInterface.TAG_SOFTWARE),
            serialNumber = lookup.description(ExifDirectoryBase.TAG_BODY_SERIAL_NUMBER)
                ?: fallback?.value(ExifInterface.TAG_BODY_SERIAL_NUMBER),
            ownerName = lookup.description(ExifDirectoryBase.TAG_CAMERA_OWNER_NAME)
                ?: fallback?.value(ExifInterface.TAG_CAMERA_OWNER_NAME),
        )
        return camera.takeIf { it.hasAnyValue }
            ?.copy(
                make = meaningful(camera.make),
                model = meaningful(camera.model),
                lensMake = meaningful(camera.lensMake),
                lensModel = meaningful(camera.lensModel),
                lensSpecification = meaningful(camera.lensSpecification),
                software = meaningful(camera.software),
                serialNumber = meaningful(camera.serialNumber),
                ownerName = meaningful(camera.ownerName),
            )
            ?.takeIf { it.hasAnyValue }
    }

    private fun extractTechnical(
        lookup: MetadataLookup,
        metadata: Metadata?,
        fallback: ExifFallbackData?,
        fileInfo: PhotoFileInfo,
    ): TechnicalInfo {
        val iso = lookup.description(ExifDirectoryBase.TAG_ISO_EQUIVALENT)
            ?: lookup.description(ExifDirectoryBase.TAG_ISO_SPEED)
            ?: fallback?.value(ExifInterface.TAG_ISO_SPEED)
            ?: fallback?.value(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)

        val aperture = formatAperture(
            lookup.description(ExifDirectoryBase.TAG_FNUMBER)
                ?: fallback?.value(ExifInterface.TAG_F_NUMBER)
                ?: lookup.description(ExifDirectoryBase.TAG_APERTURE),
        )

        val shutter = formatShutter(
            lookup.description(ExifDirectoryBase.TAG_EXPOSURE_TIME)
                ?: fallback?.value(ExifInterface.TAG_EXPOSURE_TIME),
        )

        val focalLength = formatMillimeters(
            lookup.description(ExifDirectoryBase.TAG_FOCAL_LENGTH)
                ?: fallback?.value(ExifInterface.TAG_FOCAL_LENGTH),
        )
        val focal35 = formatMillimeters(
            lookup.description(ExifDirectoryBase.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH)
                ?: fallback?.value(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM),
        )

        val flashValue = lookup.int(ExifDirectoryBase.TAG_FLASH)
            ?: fallback?.value(ExifInterface.TAG_FLASH)?.toIntOrNull()
        val flash = translateFlash(flashValue)
            ?: lookup.description(ExifDirectoryBase.TAG_FLASH)
                ?.let(::meaningful)

        val whiteBalanceValue = lookup.int(ExifDirectoryBase.TAG_WHITE_BALANCE)
            ?: lookup.int(ExifDirectoryBase.TAG_WHITE_BALANCE_MODE)
            ?: fallback?.value(ExifInterface.TAG_WHITE_BALANCE)?.toIntOrNull()
        val whiteBalance = when (whiteBalanceValue) {
            0 -> "Automático"
            1 -> "Manual"
            else -> lookup.description(ExifDirectoryBase.TAG_WHITE_BALANCE)
                ?.let(::meaningful)
        }

        val orientationValue = lookup.int(ExifDirectoryBase.TAG_ORIENTATION)
            ?: fallback?.orientation
        val orientation = translateOrientation(orientationValue)
            ?: lookup.description(ExifDirectoryBase.TAG_ORIENTATION)?.let(::meaningful)

        val exposureProgram = translateExposureProgram(
            lookup.int(ExifDirectoryBase.TAG_EXPOSURE_PROGRAM),
        ) ?: lookup.description(ExifDirectoryBase.TAG_EXPOSURE_PROGRAM)?.let(::meaningful)

        val exposureBias = lookup.description(ExifDirectoryBase.TAG_EXPOSURE_BIAS)
            ?: fallback?.value(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)

        val metering = translateMetering(
            lookup.int(ExifDirectoryBase.TAG_METERING_MODE),
        ) ?: lookup.description(ExifDirectoryBase.TAG_METERING_MODE)?.let(::meaningful)

        val colorSpace = translateColorSpace(
            lookup.int(ExifDirectoryBase.TAG_COLOR_SPACE),
            lookup.description(ExifDirectoryBase.TAG_COLOR_SPACE),
        )

        val digitalZoom = lookup.description(ExifDirectoryBase.TAG_DIGITAL_ZOOM_RATIO)
            ?: fallback?.value(ExifInterface.TAG_DIGITAL_ZOOM_RATIO)

        val sceneType = translateSceneType(
            lookup.int(ExifDirectoryBase.TAG_SCENE_CAPTURE_TYPE),
        ) ?: lookup.description(ExifDirectoryBase.TAG_SCENE_CAPTURE_TYPE)?.let(::meaningful)

        val contrast = translateNormalLowHigh(
            lookup.int(ExifDirectoryBase.TAG_CONTRAST),
            low = "Suave",
            high = "Alto",
        ) ?: lookup.description(ExifDirectoryBase.TAG_CONTRAST)?.let(::meaningful)

        val saturation = translateNormalLowHigh(
            lookup.int(ExifDirectoryBase.TAG_SATURATION),
            low = "Baja",
            high = "Alta",
        ) ?: lookup.description(ExifDirectoryBase.TAG_SATURATION)?.let(::meaningful)

        val sharpness = translateNormalLowHigh(
            lookup.int(ExifDirectoryBase.TAG_SHARPNESS),
            low = "Suave",
            high = "Alta",
        ) ?: lookup.description(ExifDirectoryBase.TAG_SHARPNESS)?.let(::meaningful)

        val resolution = fileInfo.dimensions?.let { (width, height) -> "$width × $height px" }
            ?: metadata?.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)?.let {
                val width = runCatching { it.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH) }.getOrNull()
                val height = runCatching { it.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT) }.getOrNull()
                if (width != null && height != null) "$width × $height px" else null
            }

        val compression = lookup.description(ExifDirectoryBase.TAG_COMPRESSION)
            ?.let(::meaningful)
            ?: formatCompression(fileInfo.mimeType)

        return TechnicalInfo(
            iso = meaningful(iso),
            aperture = meaningful(aperture),
            shutterSpeed = meaningful(shutter),
            focalLength = meaningful(focalLength),
            focalLength35mm = meaningful(focal35),
            flash = flash,
            whiteBalance = whiteBalance,
            orientation = orientation,
            resolution = resolution,
            compression = compression,
            exposureProgram = exposureProgram,
            exposureBias = meaningful(exposureBias),
            meteringMode = metering,
            colorSpace = colorSpace,
            digitalZoom = meaningful(digitalZoom),
            sceneType = sceneType,
            contrast = contrast,
            saturation = saturation,
            sharpness = sharpness,
        )
    }

    private fun extractDescriptive(lookup: MetadataLookup): DescriptiveInfo {
        val keywords = buildList {
            lookup.xmpValues("dc:subject").forEach(::add)
            lookup.namedTag("Keywords")?.let {
                addAll(it.split(',', ';').map(String::trim).filter(String::isNotBlank))
            }
            lookup.iptcKeywords().forEach(::add)
        }.distinctBy { it.lowercase(Locale.ROOT) }.joinToString(", ")

        return DescriptiveInfo(
            title = meaningful(
                lookup.firstXmpValue("dc:title", "photoshop:Headline")
                    ?: lookup.namedTag("Document Name", "Object Name", "Headline"),
            ),
            description = meaningful(
                lookup.firstXmpValue("dc:description")
                    ?: lookup.namedTag("Image Description", "Caption/Abstract", "Caption"),
            ),
            author = meaningful(
                lookup.firstXmpValue("dc:creator")
                    ?: lookup.namedTag("Artist", "By-line", "Creator"),
            ),
            copyright = meaningful(
                lookup.firstXmpValue("dc:rights")
                    ?: lookup.namedTag("Copyright", "Copyright Notice"),
            ),
            keywords = keywords.ifBlank { null },
            city = meaningful(
                lookup.firstXmpValue("photoshop:City")
                    ?: lookup.namedTag("City"),
            ),
            location = meaningful(
                lookup.firstXmpValue("Iptc4xmpCore:Location", "photoshop:Location")
                    ?: lookup.namedTag("Sub-location", "Content Location Name"),
            ),
            province = meaningful(
                lookup.firstXmpValue("photoshop:State", "Iptc4xmpCore:ProvinceState")
                    ?: lookup.namedTag("Province/State"),
            ),
            country = meaningful(
                lookup.firstXmpValue("photoshop:Country", "Iptc4xmpCore:CountryName")
                    ?: lookup.namedTag("Country/Primary Location Name"),
            ),
            credit = meaningful(
                lookup.firstXmpValue("photoshop:Credit")
                    ?: lookup.namedTag("Credit"),
            ),
            headline = meaningful(
                lookup.firstXmpValue("photoshop:Headline")
                    ?: lookup.namedTag("Headline"),
            ),
            instructions = meaningful(
                lookup.firstXmpValue("photoshop:Instructions")
                    ?: lookup.namedTag("Special Instructions"),
            ),
        )
    }

    private fun formatAperture(value: String?): String? {
        val clean = meaningful(value) ?: return null
        if (clean.startsWith("f/") || clean.startsWith("ƒ/")) return clean
        return clean.toDoubleOrNull()?.let { String.format(Locale.getDefault(), "ƒ/%.1f", it) } ?: clean
    }

    private fun formatShutter(value: String?): String? {
        val clean = meaningful(value) ?: return null
        if (clean.any { it.isLetter() } || clean.contains('/')) return clean
        return "$clean s"
    }

    private fun formatMillimeters(value: String?): String? {
        val clean = meaningful(value) ?: return null
        return if (clean.contains("mm", ignoreCase = true)) clean else "$clean mm"
    }

    private fun formatCompression(mimeType: String): String = when (mimeType.lowercase(Locale.ROOT)) {
        "image/jpeg", "image/jpg" -> "JPEG (compresión con pérdida)"
        "image/png" -> "PNG (sin pérdida)"
        "image/webp" -> "WebP"
        "image/heic", "image/heif" -> "HEIC/HEIF"
        "image/tiff" -> "TIFF"
        else -> "No indicada"
    }

    private fun translateOrientation(value: Int?): String? = when (value) {
        1 -> "Normal (0°)"
        2 -> "Volteada horizontalmente"
        3 -> "Girada 180°"
        4 -> "Volteada verticalmente"
        5 -> "Girada 90° y volteada"
        6 -> "Girada 90° en sentido horario"
        7 -> "Girada 270° y volteada"
        8 -> "Girada 270° en sentido horario"
        else -> null
    }

    private fun translateFlash(value: Int?): String? {
        if (value == null) return null
        if (value and 0x20 != 0) return "Sin función de flash"

        val fired = value and 0x01 != 0
        val returnLight = (value shr 1) and 0x03
        val mode = (value shr 3) and 0x03
        val redEye = value and 0x40 != 0

        val parts = buildList {
            add(if (fired) "Disparó" else "No disparó")
            when (returnLight) {
                1 -> add("retorno de luz detectado")
                2 -> add("sin retorno de luz")
                3 -> add("retorno automático")
            }
            when (mode) {
                1 -> add("obligatorio")
                2 -> add("suprimido")
                3 -> add("automático")
            }
            if (redEye) add("reducción de ojos rojos")
        }
        return parts.joinToString(", ").replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun translateExposureProgram(value: Int?): String? = when (value) {
        0 -> "No definido"
        1 -> "Manual"
        2 -> "Programa normal"
        3 -> "Prioridad a la apertura"
        4 -> "Prioridad a la obturación"
        5 -> "Modo creativo"
        6 -> "Deportes"
        7 -> "Retrato"
        8 -> "Paisaje"
        9 -> "Bulb"
        else -> null
    }

    private fun translateMetering(value: Int?): String? = when (value) {
        0 -> "Desconocido"
        1 -> "Promedio"
        2 -> "Promedio ponderado al centro"
        3 -> "Puntual"
        4 -> "Multipunto"
        5 -> "Evaluativa"
        6 -> "Parcial"
        255 -> "Otro"
        else -> null
    }

    private fun translateColorSpace(value: Int?, fallback: String?): String? = when (value) {
        1 -> "sRGB"
        2 -> "Adobe RGB"
        0xFFFF -> "Sin calibrar"
        else -> meaningful(fallback)
    }

    private fun translateSceneType(value: Int?): String? = when (value) {
        0 -> "Estándar"
        1 -> "Paisaje"
        2 -> "Retrato"
        3 -> "Nocturna"
        else -> null
    }

    private fun translateNormalLowHigh(value: Int?, low: String, high: String): String? = when (value) {
        0 -> "Normal"
        1 -> low
        2 -> high
        else -> null
    }

    private fun meaningful(value: String?): String? {
        val clean = TextFormatting.clean(value) ?: return null
        val ignored = setOf(
            "unknown",
            "undefined",
            "not available",
            "n/a",
            "no disponible",
            "desconocido",
        )
        return clean.takeUnless { it.lowercase(Locale.ROOT) in ignored }
    }

    private fun cleanDescription(directory: Directory?, tag: Int): String? {
        if (directory == null || !directory.containsTag(tag)) return null
        return runCatching { directory.getDescription(tag) }
            .getOrNull()
            ?.let(TextFormatting::clean)
            ?.let(::meaningful)
    }

    private fun safeErrorMessage(error: Throwable): String {
        val message = error.message?.replace('\n', ' ')?.trim()
        return if (message.isNullOrBlank()) {
            error.javaClass.simpleName
        } else {
            message.take(180)
        }
    }
}

private class MetadataLookup(
    private val metadata: Metadata?,
    val fallback: ExifFallbackData?,
) {
    private val directories: List<Directory> = metadata?.directories?.toList().orEmpty()

    private val xmpProperties: Map<String, String> = runCatching {
        buildMap {
            directories.filterIsInstance<XmpDirectory>().forEach { directory ->
                putAll(directory.xmpProperties)
            }
        }
    }.getOrDefault(emptyMap())

    fun description(tagType: Int): String? {
        directories.forEach { directory ->
            if (runCatching { directory.containsTag(tagType) }.getOrDefault(false)) {
                val value = runCatching { directory.getDescription(tagType) }
                    .getOrNull()
                    ?.let(TextFormatting::clean)
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }

    fun string(tagType: Int): String? {
        directories.forEach { directory ->
            if (runCatching { directory.containsTag(tagType) }.getOrDefault(false)) {
                val value = runCatching { directory.getString(tagType) }
                    .getOrNull()
                    ?.let(TextFormatting::clean)
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }

    fun int(tagType: Int): Int? {
        directories.forEach { directory ->
            if (runCatching { directory.containsTag(tagType) }.getOrDefault(false)) {
                val value = runCatching { directory.getInt(tagType) }.getOrNull()
                if (value != null) return value
            }
        }
        return null
    }

    fun namedTag(vararg tagNames: String): String? {
        val wanted = tagNames.map { it.lowercase(Locale.ROOT) }.toSet()
        directories.forEach { directory ->
            directory.tags.forEach { tag ->
                val name = runCatching { tag.tagName }.getOrNull()?.lowercase(Locale.ROOT)
                if (name in wanted) {
                    val value = runCatching { tag.description }
                        .getOrNull()
                        ?.let(TextFormatting::clean)
                    if (!value.isNullOrBlank()) return value
                }
            }
        }
        return null
    }

    fun firstXmpValue(vararg tokens: String): String? =
        xmpValues(tokens.toList()).firstOrNull()

    fun xmpValues(vararg tokens: String): List<String> = xmpValues(tokens.toList())

    private fun xmpValues(tokens: List<String>): List<String> {
        if (xmpProperties.isEmpty()) return emptyList()
        val normalizedTokens = tokens.map(::normalizeXmpKey).filter { it.isNotBlank() }
        return xmpProperties.entries
            .filter { entry ->
                val key = normalizeXmpKey(entry.key)
                normalizedTokens.any { token -> key.contains(token) }
            }
            .map { it.value }
            .mapNotNull(TextFormatting::clean)
            .distinct()
    }

    fun iptcKeywords(): List<String> {
        return runCatching {
            metadata?.getDirectoriesOfType(IptcDirectory::class.java)
                ?.flatMap { directory ->
                    directory.keywords.mapNotNull(TextFormatting::clean)
                }
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun normalizeXmpKey(value: String): String =
        value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)
}

private fun emptyMetadata(): Metadata = Metadata()
