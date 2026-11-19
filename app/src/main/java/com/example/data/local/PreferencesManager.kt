package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.journalDataStore: DataStore<Preferences> by preferencesDataStore(name = "alanhar_prefs")

data class LocalAppPreferences(
    val language: String = "en",
    val themeMode: String = "system",
    val unpaywallEmail: String = "research@alanharhospital.org",
    val syncFrequencyHours: Int = 12,
    val lastSyncMillis: Long = 0L
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UNPAYWALL_EMAIL = stringPreferencesKey("unpaywall_email")
        val SYNC_FREQUENCY_HOURS = intPreferencesKey("sync_frequency_hours")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.journalDataStore.data.map { prefs ->
        LocalAppPreferences(
            language = prefs[Keys.LANGUAGE] ?: "en",
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            unpaywallEmail = prefs[Keys.UNPAYWALL_EMAIL] ?: "research@alanharhospital.org",
            syncFrequencyHours = prefs[Keys.SYNC_FREQUENCY_HOURS] ?: 12,
            lastSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L
        )
    }

    suspend fun setLanguage(language: String) {
        context.journalDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setThemeMode(mode: String) {
        context.journalDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setUnpaywallEmail(email: String) {
        context.journalDataStore.edit { it[Keys.UNPAYWALL_EMAIL] = email.trim() }
    }

    suspend fun setSyncFrequencyHours(hours: Int) {
        context.journalDataStore.edit { it[Keys.SYNC_FREQUENCY_HOURS] = hours.coerceIn(1, 168) }
    }

    suspend fun setLastSyncMillis(millis: Long) {
        context.journalDataStore.edit { it[Keys.LAST_SYNC_MILLIS] = millis }
    }
}
