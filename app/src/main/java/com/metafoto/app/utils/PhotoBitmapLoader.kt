package com.metafoto.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

object PhotoBitmapLoader {
    suspend fun load(
        context: Context,
        uriString: String,
        maxDimensionPx: Int = 1_600,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri, maxDimensionPx)
                ?: decodeWithBitmapFactory(context, uri, maxDimensionPx)
        } else {
            decodeWithBitmapFactory(context, uri, maxDimensionPx)
        }
    }

    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxDimensionPx: Int): Bitmap? =
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSize = maxOf(info.size.width, info.size.height)
                if (maxSize > maxDimensionPx) {
                    val sample = ceil(maxSize / maxDimensionPx.toDouble()).toInt()
                    decoder.setTargetSampleSize(sample.coerceAtLeast(1))
                }
            }
        }.getOrNull()

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxDimensionPx: Int): Bitmap? =
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val orientation = runCatching {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

            applyOrientation(decoded, orientation)
        }.getOrNull()

    private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest / 2 >= maxDimension) {
            largest /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { rotated ->
                    if (rotated !== bitmap) bitmap.recycle()
                }
        }.getOrDefault(bitmap)
    }
}
