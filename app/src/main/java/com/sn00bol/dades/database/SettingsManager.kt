package com.sn00bol.dades.database

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "System", "Light", "Dark"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
        val TRASH_AUTO_DELETE_DAYS = intPreferencesKey("trash_auto_delete_days")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "System"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR] ?: true
    }

    val blurEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BLUR_ENABLED] ?: true
    }

    val trashAutoDeleteDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TRASH_AUTO_DELETE_DAYS] ?: 30
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_ENABLED] = enabled
        }
    }

    suspend fun setTrashAutoDeleteDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[TRASH_AUTO_DELETE_DAYS] = days
        }
    }
}
