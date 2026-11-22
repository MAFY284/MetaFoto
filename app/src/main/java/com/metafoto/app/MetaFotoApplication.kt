package com.metafoto.app

import android.app.Application
import org.osmdroid.config.Configuration

class MetaFotoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configuración local de osmdroid: caché y preferencias propias, sin
        // servicios de terceros. El User-Agent identifica a la app ante el
        // servidor de teselas de OpenStreetMap.
        Configuration.getInstance().apply {
            userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
            load(this@MetaFotoApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
    }
}
