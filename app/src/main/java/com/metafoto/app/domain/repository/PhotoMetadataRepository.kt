package com.metafoto.app.domain.repository

import com.metafoto.app.domain.model.PhotoMetadata

interface PhotoMetadataRepository {
    suspend fun extract(uri: String): Result<PhotoMetadata>
}
