package com.metafoto.app.data.repository

import android.content.Context
import android.net.Uri
import com.metafoto.app.data.metadata.MetadataExtractor
import com.metafoto.app.domain.model.MetadataException
import com.metafoto.app.domain.model.PhotoMetadata
import com.metafoto.app.domain.repository.PhotoMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class PhotoMetadataRepositoryImpl(context: Context) : PhotoMetadataRepository {

    private val extractor = MetadataExtractor(context.applicationContext)

    override suspend fun extract(uri: String): Result<PhotoMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            extractor.extract(Uri.parse(uri))
        }.recoverCatching { error ->
            throw when (error) {
                is MetadataException -> error
                is SecurityException -> MetadataException(
                    "No hay permiso para leer esta foto. Vuelve a seleccionarla desde la app.",
                    error,
                )
                is FileNotFoundException -> MetadataException(
                    "No se encontró la foto. Puede que se haya movido o eliminado.",
                    error,
                )
                else -> MetadataException(
                    "No se pudo leer la foto. El archivo puede estar dañado o no ser compatible.",
                    error,
                )
            }
        }
    }
}
