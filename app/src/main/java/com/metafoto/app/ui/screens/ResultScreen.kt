package com.metafoto.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metafoto.app.R
import com.metafoto.app.domain.model.CaptureDateInfo
import com.metafoto.app.domain.model.DescriptiveInfo
import com.metafoto.app.domain.model.GpsData
import com.metafoto.app.domain.model.PhotoMetadata
import com.metafoto.app.domain.model.TechnicalInfo
import com.metafoto.app.ui.components.LabeledValueGrid
import com.metafoto.app.ui.components.MetadataCard
import com.metafoto.app.ui.components.MetadataRow
import com.metafoto.app.ui.components.PhotoMap
import com.metafoto.app.ui.components.PhotoPreview
import com.metafoto.app.ui.components.RawMetadataSheet
import com.metafoto.app.utils.CoordinateFormatter
import com.metafoto.app.utils.DateDisplayFormatter
import com.metafoto.app.utils.MapLinks
import com.metafoto.app.utils.MetadataTextFormatter
import com.metafoto.app.utils.ShareAndClipboard
import com.metafoto.app.utils.TextFormatting
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    metadata: PhotoMetadata,
    onBack: () -> Unit,
    onChooseAnother: () -> Unit,
    onThemeClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRawMetadata by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun shareSummary() {
        val text = MetadataTextFormatter.summary(metadata)
        val title = context.getString(R.string.share_chooser_title)
        if (!ShareAndClipboard.share(context, title, text)) {
            showMessage(context.getString(R.string.share_error))
        }
    }

    fun copyAll() {
        val text = MetadataTextFormatter.full(metadata)
        val label = context.getString(R.string.section_all)
        if (ShareAndClipboard.copy(context, label, text)) {
            showMessage(context.getString(R.string.copied))
        } else {
            showMessage(context.getString(R.string.copy_error))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.result_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = metadata.fileInfo.fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onThemeClick) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = stringResource(R.string.theme),
                        )
                    }
                    IconButton(onClick = ::shareSummary) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share_summary),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    PhotoHero(metadata)
                }

                item {
                    ResultActions(
                        onShare = ::shareSummary,
                        onCopyAll = ::copyAll,
                    )
                }

                if (!metadata.hasEmbeddedMetadata) {
                    item { EmptyMetadataNotice() }
                }

                item {
                    val gps = metadata.gps
                    if (gps != null) {
                        LocationCard(
                            gps = gps,
                            onCopyCoordinates = {
                                val coordinates = CoordinateFormatter.copyString(gps)
                                if (ShareAndClipboard.copy(
                                        context,
                                        context.getString(R.string.latitude),
                                        coordinates,
                                    )
                                ) {
                                    showMessage(context.getString(R.string.coordinates_copied))
                                } else {
                                    showMessage(context.getString(R.string.copy_error))
                                }
                            },
                            onOpenMap = { provider ->
                                if (!MapLinks.open(context, provider, gps.latitude, gps.longitude)) {
                                    showMessage(context.getString(R.string.map_error))
                                }
                            },
                        )
                    } else {
                        MissingDataCard(
                            title = stringResource(R.string.section_location),
                            icon = Icons.Outlined.LocationOff,
                            message = stringResource(R.string.no_location),
                            detail = stringResource(R.string.no_location_detail),
                        )
                    }
                }

                item {
                    DateCard(metadata.captureDate)
                }

                item {
                    val camera = metadata.camera
                    if (camera == null || !camera.hasAnyValue) {
                        MissingDataCard(
                            title = stringResource(R.string.section_camera),
                            icon = Icons.Outlined.PhotoCamera,
                            message = stringResource(R.string.camera_no_info),
                        )
                    } else {
                        CameraCard(metadata)
                    }
                }

                item {
                    TechnicalCard(
                        technical = metadata.technical,
                        fallbackResolution = metadata.fileInfo.dimensions,
                    )
                }

                if (metadata.descriptive.hasAnyValue) {
                    item {
                        DescriptiveCard(metadata.descriptive)
                    }
                }

                item {
                    FileCard(metadata)
                }

                if (metadata.warnings.isNotEmpty()) {
                    item {
                        WarningsCard(metadata.warnings)
                    }
                }

                if (metadata.sections.isNotEmpty()) {
                    item {
                        RawMetadataCard(
                            tagCount = metadata.totalMetadataTags,
                            groupCount = metadata.sections.size,
                            onClick = { showRawMetadata = true },
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onChooseAnother,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.choose_another))
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.generated_by),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }

    if (showRawMetadata) {
        RawMetadataSheet(
            sections = metadata.sections,
            onDismiss = { showRawMetadata = false },
        )
    }
}

@Composable
private fun PhotoHero(metadata: PhotoMetadata) {
    val file = metadata.fileInfo
    val overlayChips = buildList {
        add(TextFormatting.mimeTypeLabel(file.mimeType))
        file.dimensions?.let { (width, height) -> add("$width × $height") }
        TextFormatting.bytes(file.sizeBytes)?.let(::add)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        PhotoPreview(
            uri = file.uri,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 24,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                overlayChips.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultActions(
    onShare: () -> Unit,
    onCopyAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.share),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
        OutlinedButton(
            onClick = onCopyAll,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.copy_all),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LocationCard(
    gps: GpsData,
    onCopyCoordinates: () -> Unit,
    onOpenMap: (MapLinks.Provider) -> Unit,
) {
    var mapMenuOpen by remember { mutableStateOf(false) }

    MetadataCard(
        title = stringResource(R.string.section_location),
        icon = Icons.Outlined.LocationOn,
        subtitle = CoordinateFormatter.decimal(gps.latitude, gps.longitude),
    ) {
        Column {
            PhotoMap(
                latitude = gps.latitude,
                longitude = gps.longitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(
                    onClick = onCopyCoordinates,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.copy_coordinates),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Box {
                    OutlinedButton(
                        onClick = { mapMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.open_in),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = mapMenuOpen,
                        onDismissRequest = { mapMenuOpen = false },
                    ) {
                        MapLinks.Provider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(providerLabel(provider)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    mapMenuOpen = false
                                    onOpenMap(provider)
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MetadataRow(
                label = stringResource(R.string.latitude),
                value = CoordinateFormatter.latitude(gps.latitude),
            )
            MetadataRow(
                label = stringResource(R.string.longitude),
                value = CoordinateFormatter.longitude(gps.longitude),
            )
            MetadataRow(
                label = stringResource(R.string.gps_date),
                value = gps.gpsTimestamp,
            )
            MetadataRow(
                label = stringResource(R.string.altitude),
                value = CoordinateFormatter.altitude(gps.altitudeMeters),
            )
            MetadataRow(
                label = stringResource(R.string.gps_direction),
                value = CoordinateFormatter.direction(gps.imageDirectionDegrees),
            )
            MetadataRow(
                label = stringResource(R.string.map_datum),
                value = gps.mapDatum,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.map_online_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun providerLabel(provider: MapLinks.Provider): String = stringResource(
    when (provider) {
        MapLinks.Provider.GOOGLE_MAPS -> R.string.map_google
        MapLinks.Provider.APPLE_MAPS -> R.string.map_apple
        MapLinks.Provider.WAZE -> R.string.map_waze
        MapLinks.Provider.OPENSTREETMAP -> R.string.map_osm
    },
)

@Composable
private fun DateCard(date: CaptureDateInfo?) {
    val info = date
    val captured = info?.capturedAt
    if (info == null || captured == null) {
        MissingDataCard(
            title = stringResource(R.string.section_date),
            icon = Icons.Outlined.Schedule,
            message = stringResource(R.string.date_unknown),
        )
        return
    }

    val explicitZone = info.hasExplicitTimeZone
    val zoneText = if (explicitZone) {
        DateDisplayFormatter.zoneLabel(captured, explicit = true)
    } else {
        stringResource(R.string.date_timezone_device)
    }

    MetadataCard(
        title = stringResource(R.string.section_date),
        icon = Icons.Outlined.Schedule,
        subtitle = DateDisplayFormatter.dateTime(captured),
    ) {
        Column {
            LabeledValueGrid(
                values = listOf(
                    stringResource(R.string.date_weekday) to DateDisplayFormatter.weekday(captured),
                    stringResource(R.string.date_time) to DateDisplayFormatter.time(captured),
                ),
            )
            Spacer(Modifier.height(10.dp))
            MetadataRow(stringResource(R.string.date_timezone), zoneText)
            MetadataRow(stringResource(R.string.date_source), info.source)
            MetadataRow(stringResource(R.string.date_original_raw), info.originalRaw)
            MetadataRow(stringResource(R.string.date_digitized), info.digitizedRaw)
            MetadataRow(stringResource(R.string.date_modified), info.modifiedRaw)
        }
    }
}

@Composable
private fun CameraCard(metadata: PhotoMetadata) {
    val camera = metadata.camera ?: return
    MetadataCard(
        title = stringResource(R.string.section_camera),
        icon = Icons.Outlined.PhotoCamera,
        subtitle = camera.displayName,
    ) {
        Column {
            MetadataRow(stringResource(R.string.camera_make), camera.make)
            MetadataRow(stringResource(R.string.camera_model), camera.model)
            MetadataRow(stringResource(R.string.camera_lens), camera.lensModel)
            MetadataRow(stringResource(R.string.camera_lens_make), camera.lensMake)
            MetadataRow(
                stringResource(R.string.camera_lens_spec),
                camera.lensSpecification,
            )
            MetadataRow(stringResource(R.string.camera_software), camera.software)
            MetadataRow(stringResource(R.string.camera_serial), camera.serialNumber)
            MetadataRow(stringResource(R.string.camera_owner), camera.ownerName)
        }
    }
}

@Composable
private fun TechnicalCard(
    technical: TechnicalInfo,
    fallbackResolution: Pair<Int, Int>?,
) {
    val resolution = technical.resolution ?: formatDimensions(fallbackResolution)
    val entries = listOfNotNull(
        technical.iso?.let { R.string.technical_iso to it },
        technical.aperture?.let { R.string.technical_aperture to it },
        technical.shutterSpeed?.let { R.string.technical_shutter to it },
        technical.focalLength?.let { R.string.technical_focal to it },
        technical.focalLength35mm?.let { R.string.technical_focal_35mm to it },
        technical.flash?.let { R.string.technical_flash to it },
        technical.whiteBalance?.let { R.string.technical_white_balance to it },
        technical.orientation?.let { R.string.technical_orientation to it },
        resolution?.let { R.string.technical_resolution to it },
        technical.compression?.let { R.string.technical_compression to it },
        technical.exposureProgram?.let { R.string.technical_exposure_program to it },
        technical.exposureBias?.let { R.string.technical_exposure_bias to it },
        technical.meteringMode?.let { R.string.technical_metering to it },
        technical.colorSpace?.let { R.string.technical_color_space to it },
        technical.digitalZoom?.let { R.string.technical_digital_zoom to it },
        technical.sceneType?.let { R.string.technical_scene to it },
        technical.contrast?.let { R.string.technical_contrast to it },
        technical.saturation?.let { R.string.technical_saturation to it },
        technical.sharpness?.let { R.string.technical_sharpness to it },
    )

    if (entries.isEmpty()) {
        MissingDataCard(
            title = stringResource(R.string.section_technical),
            icon = Icons.Outlined.Tune,
            message = stringResource(R.string.technical_no_info),
        )
        return
    }

    val values = entries.map { (labelRes, value) ->
        stringResource(labelRes) to value
    }
    MetadataCard(
        title = stringResource(R.string.section_technical),
        icon = Icons.Outlined.Tune,
        subtitle = stringResource(R.string.tag_count, entries.size),
    ) {
        LabeledValueGrid(values = values)
    }
}

@Composable
private fun DescriptiveCard(descriptive: DescriptiveInfo) {
    val entries = listOfNotNull(
        descriptive.title?.let { R.string.other_title to it },
        descriptive.description?.let { R.string.other_description to it },
        descriptive.author?.let { R.string.other_author to it },
        descriptive.copyright?.let { R.string.other_copyright to it },
        descriptive.keywords?.let { R.string.other_keywords to it },
        descriptive.city?.let { R.string.other_city to it },
        descriptive.location?.let { R.string.other_location to it },
        descriptive.province?.let { R.string.other_province to it },
        descriptive.country?.let { R.string.other_country to it },
        descriptive.credit?.let { R.string.other_credit to it },
        descriptive.headline?.let { R.string.other_headline to it },
        descriptive.instructions?.let { R.string.other_instructions to it },
    )
    val values = entries.map { (labelRes, value) ->
        stringResource(labelRes) to value
    }

    MetadataCard(
        title = stringResource(R.string.section_other),
        icon = Icons.Outlined.Info,
        subtitle = descriptive.title ?: descriptive.author,
        initiallyExpanded = false,
    ) {
        if (values.isEmpty()) {
            Text(
                text = stringResource(R.string.other_no_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LabeledValueGrid(values = values)
        }
    }
}

@Composable
private fun FileCard(metadata: PhotoMetadata) {
    val file = metadata.fileInfo
    val type = TextFormatting.mimeTypeLabel(file.mimeType)
    val size = TextFormatting.bytes(file.sizeBytes)
    val subtitle = listOfNotNull(type, size).joinToString(" · ")

    MetadataCard(
        title = stringResource(R.string.section_file),
        icon = Icons.Outlined.InsertDriveFile,
        subtitle = subtitle,
        initiallyExpanded = false,
    ) {
        Column {
            MetadataRow(stringResource(R.string.file_name), file.fileName)
            MetadataRow(stringResource(R.string.file_type), file.mimeType)
            MetadataRow(stringResource(R.string.file_size), size)
            MetadataRow(
                stringResource(R.string.file_dimensions),
                formatDimensions(file.dimensions),
            )
            MetadataRow(
                stringResource(R.string.file_modified),
                DateDisplayFormatter.timestamp(file.lastModifiedMillis),
            )
            MetadataRow(stringResource(R.string.file_path), file.path)
            MetadataRow(stringResource(R.string.file_uri), file.uri)
        }
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.28f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.warnings_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            warnings.forEach { warning ->
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RawMetadataCard(
    tagCount: Int,
    groupCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.section_all),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.all_metadata_count,
                            tagCount,
                            groupCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.all_metadata_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.all_metadata_open),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EmptyMetadataNotice() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.no_metadata_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.no_metadata_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MissingDataCard(
    title: String,
    icon: ImageVector,
    message: String,
    detail: String? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!detail.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

private fun formatDimensions(dimensions: Pair<Int, Int>?): String? =
    dimensions?.let { (width, height) -> "$width × $height px" }
