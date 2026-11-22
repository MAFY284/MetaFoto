package com.metafoto.app.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat

object MediaUriResolver {

    fun preferOriginal(context: Context, uri: Uri): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return uri
        val locationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted) return uri

        val mediaUri = runCatching { MediaStore.getMediaUri(context, uri) }.getOrNull() ?: uri
        Log.d("MediaUriResolver", "input=$uri media=$mediaUri")
        if (mediaUri.authority != MediaStore.AUTHORITY) return uri
        if (mediaUri.pathSegments.firstOrNull() == "picker") return uri

        return runCatching { MediaStore.setRequireOriginal(mediaUri) }.getOrDefault(uri)
    }
}
