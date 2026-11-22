package com.metafoto.app

import android.app.Application
import com.metafoto.app.data.local.MapSettings
import org.osmdroid.config.Configuration
import java.io.File

class MetaFotoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configuración local de osmdroid: caché y preferencias propias, sin
        // servicios de terceros. El User-Agent identifica a la app ante el
        // servidor de teselas de OpenStreetMap.
        val basePath = File(filesDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(cacheDir, "osmdroid").apply { mkdirs() }

        Configuration.getInstance().apply {
            userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
            load(this@MetaFotoApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            osmdroidBasePath = basePath
            osmdroidTileCache = tileCache
            tileFileSystemCacheMaxBytes = 200L * 1024L * 1024L
            tileFileSystemCacheTrimBytes = 160L * 1024L * 1024L
        }

        // Variante local: si carto.properties aportó una clave, se guarda en
        // preferencias privadas. El APK de GitHub se compila sin clave.
        MapSettings.loadKey(this)
    }
}
