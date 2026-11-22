package com.metafoto.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object MapLinks {
    enum class Provider(val label: String) {
        GOOGLE_MAPS("Google Maps"),
        APPLE_MAPS("Apple Maps"),
        WAZE("Waze"),
        OPENSTREETMAP("OpenStreetMap"),
    }

    fun open(context: Context, provider: Provider, latitude: Double, longitude: Double): Boolean {
        val url = when (provider) {
            Provider.GOOGLE_MAPS ->
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

            Provider.APPLE_MAPS ->
                "https://maps.apple.com/?ll=$latitude,$longitude&q=${encodedLabel()}"

            Provider.WAZE ->
                "https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"

            Provider.OPENSTREETMAP ->
                "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=16/$latitude/$longitude"
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun encodedLabel(): String =
        URLEncoder.encode("Ubicación de la foto", Charsets.UTF_8.name())
}
