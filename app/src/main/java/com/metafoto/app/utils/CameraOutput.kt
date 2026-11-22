package com.metafoto.app.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

object CameraOutput {
    fun create(context: Context): Pair<File, Uri> {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        cleanup(directory)
        val output = File.createTempFile("metafoto_", ".jpg", directory)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
        return output to uri
    }

    private fun cleanup(directory: File) {
        val threshold = Instant.now().minusSeconds(TimeUnit.DAYS.toSeconds(1)).toEpochMilli()
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < threshold) {
                file.delete()
            }
        }
    }
}
