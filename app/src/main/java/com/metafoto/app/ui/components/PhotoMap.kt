package com.metafoto.app.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metafoto.app.R
import com.metafoto.app.data.local.MapSettings
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun PhotoMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var cartoKey by remember { mutableStateOf(MapSettings.loadKey(context)) }
    var showKeyDialog by remember { mutableStateOf(false) }
    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .fillMaxSize(),
    ) {
        AndroidView(
            factory = { androidContext ->
                MapView(androidContext).apply {
                    setTileSource(tileSourceFor(cartoKey))
                    setTilesScaledToDpi(false)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    controller.setCenter(point)
                    val marker = Marker(this).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Ubicación de la foto"
                        setDefaultIcon()
                    }
                    overlays.add(marker)
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .width(46.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            shadowElevation = 3.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(
                    onClick = { mapView?.controller?.zoomIn() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.map_zoom_in),
                        modifier = Modifier.size(20.dp),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                IconButton(
                    onClick = { mapView?.controller?.zoomOut() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.map_zoom_out),
                        modifier = Modifier.size(20.dp),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                IconButton(
                    onClick = {
                        mapView?.controller?.animateTo(point, 16.0, 400L)
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(R.string.map_center),
                        modifier = Modifier.size(20.dp),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                IconButton(
                    onClick = { showKeyDialog = true },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.map_key_configure),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.58f),
        ) {
            Text(
                text = stringResource(
                    if (cartoKey.isNotBlank()) R.string.map_attribution_carto else R.string.map_attribution
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }

    LaunchedEffect(cartoKey, mapView) {
        mapView?.let { map ->
            map.setTileSource(tileSourceFor(cartoKey))
            map.invalidate()
        }
    }

    if (showKeyDialog) {
        var keyInput by remember(cartoKey, showKeyDialog) { mutableStateOf(cartoKey) }
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text(stringResource(R.string.map_key_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.map_key_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.map_key_hint)) },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        MapSettings.saveKey(context, keyInput)
                        cartoKey = keyInput.trim()
                        showKeyDialog = false
                    },
                ) {
                    Text(stringResource(R.string.map_key_save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            MapSettings.clearKey(context)
                            cartoKey = ""
                            showKeyDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.map_key_clear))
                    }
                    TextButton(onClick = { showKeyDialog = false }) {
                        Text(stringResource(R.string.map_key_cancel))
                    }
                }
            },
        )
    }

    val latestMap by rememberUpdatedState(mapView)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> latestMap?.onResume()
                Lifecycle.Event.ON_PAUSE -> latestMap?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            latestMap?.onDetach()
        }
    }
}


private fun tileSourceFor(apiKey: String): ITileSource {
    return if (apiKey.isBlank()) TileSourceFactory.MAPNIK else CartoTileSource(apiKey)
}

private class CartoTileSource(
    private val apiKey: String,
) : OnlineTileSourceBase(
    "CARTO Voyager v2 ${Integer.toHexString(apiKey.hashCode())}",
    1,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "© OpenStreetMap contributors © CARTO",
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val query = "?key=${Uri.encode(apiKey)}"
        return "${baseUrl}$zoom/$x/$y.png$query"
    }
}
