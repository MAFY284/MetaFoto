package com.metafoto.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metafoto.app.R
import com.metafoto.app.utils.PhotoBitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PreviewState {
    data object Loading : PreviewState
    data object Failed : PreviewState
    data class Ready(val bitmap: android.graphics.Bitmap) : PreviewState
}

@Composable
fun PhotoPreview(
    uri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Int = 22,
) {
    val context = LocalContext.current
    val state by produceState<PreviewState>(PreviewState.Loading, uri) {
        value = withContext(Dispatchers.IO) {
            PhotoBitmapLoader.load(context, uri)?.let { PreviewState.Ready(it) }
                ?: PreviewState.Failed
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            PreviewState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 3.dp,
            )

            PreviewState.Failed -> Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = stringResource(R.string.no_preview),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(24.dp)
                    .size(42.dp),
            )

            is PreviewState.Ready -> Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.photo_preview),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
