package com.metafoto.app.data.local

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.metafoto.app.domain.model.MetadataException
import com.metafoto.app.domain.model.PhotoFileInfo
import java.io.File
import java.util.Locale

class MediaFileInfoReader(private val context: Context) {

    fun read(uri: Uri): PhotoFileInfo {
        val resolver = context.contentResolver
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val path = uri.path ?: throw MetadataException("La ruta del archivo no es válida.")
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                throw MetadataException("No se encontró el archivo seleccionado.")
            }
        }

        val displayName = queryDisplayName(uri)
        val size = querySize(uri) ?: fileLength(uri)
        val lastModified = queryLastModified(uri) ?: fileLastModified(uri)
        val extension = displayName
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
        val resolverMime = runCatching { resolver.getType(uri) }.getOrNull()
        val mimeType = resolveMimeType(resolverMime, extension)
        val fileName = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.let(Uri::decode)?.substringAfterLast('/')
            ?: "foto"

        if (!isSupportedImage(mimeType, extension)) {
            throw MetadataException(
                "El archivo seleccionado no es una imagen compatible. " +
                    "Formatos admitidos: JPG, JPEG, HEIC, PNG, TIFF y WebP.",
            )
        }

        val path = when {
            uri.scheme == ContentResolver.SCHEME_FILE -> File(uri.path.orEmpty()).absolutePath
            DocumentsContract.isDocumentUri(context, uri) -> runCatching {
                DocumentsContract.getDocumentId(uri)
            }.getOrNull() ?: uri.path.orEmpty()
            else -> uri.path.orEmpty().ifBlank { uri.toString() }
        }

        return PhotoFileInfo(
            uri = uri.toString(),
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = size,
            lastModifiedMillis = lastModified,
            path = path,
            width = null,
            height = null,
        )
    }

    fun readDecodedBounds(uri: Uri): Pair<Int, Int>? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth to options.outHeight
            } else {
                null
            }
        }
    }.getOrNull()

    private fun queryDisplayName(uri: Uri): String? = queryColumn(uri, OpenableColumns.DISPLAY_NAME)

    private fun querySize(uri: Uri): Long? {
        val raw = queryColumn(uri, OpenableColumns.SIZE) ?: return null
        return raw.toLongOrNull()?.takeIf { it >= 0L }
    }

    private fun queryLastModified(uri: Uri): Long? {
        val documentTimestamp = runCatching {
            DocumentFile.fromSingleUri(context, uri)?.lastModified()
        }.getOrNull()
        if (documentTimestamp != null && documentTimestamp > 0L) return documentTimestamp

        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val seconds = cursor.getLong(0)
                    if (seconds > 0L) seconds * 1_000L else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun queryColumn(uri: Uri, column: String): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null
        }
    }.getOrNull()

    private fun fileLength(uri: Uri): Long? {
        if (uri.scheme != ContentResolver.SCHEME_FILE) return null
        val file = uri.path?.let(::File) ?: return null
        return file.length().takeIf { it > 0L }
    }

    private fun fileLastModified(uri: Uri): Long? {
        if (uri.scheme != ContentResolver.SCHEME_FILE) return null
        val file = uri.path?.let(::File) ?: return null
        return file.lastModified().takeIf { it > 0L }
    }

    private fun resolveMimeType(mime: String?, extension: String?): String {
        if (!mime.isNullOrBlank() && mime != "application/octet-stream") return mime
        val fromExtension = extension?.let {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        }
        return fromExtension ?: mime ?: "image/*"
    }

    private fun isSupportedImage(mimeType: String, extension: String?): Boolean {
        if (mimeType.startsWith("image/", ignoreCase = true)) return true
        return extension in setOf("jpg", "jpeg", "heic", "heif", "png", "tif", "tiff", "webp")
    }
}
