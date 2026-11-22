package com.metafoto.app.domain.model

class MetadataException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
