package com.metafoto.app.data.local

import android.content.Context
import com.metafoto.app.BuildConfig

object MapSettings {

    private const val PREFERENCES_NAME = "metafoto_settings"
    private const val KEY_CARTO_API_KEY = "carto_api_key"

    fun loadKey(context: Context): String {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.contains(KEY_CARTO_API_KEY)) {
            return preferences.getString(KEY_CARTO_API_KEY, "").orEmpty()
        }
        val localDefault = BuildConfig.CARTO_API_KEY
        if (localDefault.isNotBlank()) {
            preferences.edit().putString(KEY_CARTO_API_KEY, localDefault).apply()
        }
        return localDefault
    }

    fun saveKey(context: Context, key: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CARTO_API_KEY, key.trim())
            .apply()
    }

    fun clearKey(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CARTO_API_KEY, "")
            .apply()
    }
}
