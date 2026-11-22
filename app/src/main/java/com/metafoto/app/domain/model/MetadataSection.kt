package com.metafoto.app.domain.model

/** Un grupo de metadatos, normalmente equivalente a un directorio del archivo. */
data class MetadataSection(
    val name: String,
    val items: List<MetadataItem>,
)

/** Etiqueta y valor ya formateados para mostrarse o compartirse. */
data class MetadataItem(
    val tag: String,
    val value: String,
)
