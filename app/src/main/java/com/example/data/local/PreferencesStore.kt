package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.txlDataStore: DataStore<Preferences> by preferencesDataStore(name = "txlsocial_prefs")

class PreferencesStore(private val context: Context) {

    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val NEON_GLOW = booleanPreferencesKey("neon_glow_enabled")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val SHOW_STATUS_IN_CHAT = booleanPreferencesKey("show_status_in_chat")
        val COMPACT_VOICE_GRID = booleanPreferencesKey("compact_voice_grid")
        val PRIVATE_PROFILE = booleanPreferencesKey("private_profile_mode")
        val LANGUAGE_LABEL = stringPreferencesKey("language_label")
        val APP_BG_URI = stringPreferencesKey("app_bg_uri")
        val APP_BG_DIM = floatPreferencesKey("app_bg_dim")
        val INVITE_WEB_URL = stringPreferencesKey("invite_web_url")
    }

    val currentUserId: Flow<String?> = context.txlDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]
    }

    val settings: Flow<AppSettings> = context.txlDataStore.data.map { prefs ->
        AppSettings(
            neonGlowEnabled = prefs[Keys.NEON_GLOW] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            showStatusInChat = prefs[Keys.SHOW_STATUS_IN_CHAT] ?: true,
            compactVoiceGrid = prefs[Keys.COMPACT_VOICE_GRID] ?: false,
            privateProfileMode = prefs[Keys.PRIVATE_PROFILE] ?: false,
            languageLabel = prefs[Keys.LANGUAGE_LABEL] ?: "Español (ES)",
            appBackgroundUri = prefs[Keys.APP_BG_URI],
            appBackgroundDim = prefs[Keys.APP_BG_DIM] ?: 0.62f,
            inviteWebUrl = prefs[Keys.INVITE_WEB_URL] ?: ""
        )
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.txlDataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.CURRENT_USER_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.txlDataStore.edit { prefs ->
            val current = AppSettings(
                neonGlowEnabled = prefs[Keys.NEON_GLOW] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                showStatusInChat = prefs[Keys.SHOW_STATUS_IN_CHAT] ?: true,
                compactVoiceGrid = prefs[Keys.COMPACT_VOICE_GRID] ?: false,
                privateProfileMode = prefs[Keys.PRIVATE_PROFILE] ?: false,
                languageLabel = prefs[Keys.LANGUAGE_LABEL] ?: "Español (ES)",
                appBackgroundUri = prefs[Keys.APP_BG_URI],
                appBackgroundDim = prefs[Keys.APP_BG_DIM] ?: 0.62f,
                inviteWebUrl = prefs[Keys.INVITE_WEB_URL] ?: ""
            )
            val next = transform(current)
            prefs[Keys.NEON_GLOW] = next.neonGlowEnabled
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
            prefs[Keys.SHOW_STATUS_IN_CHAT] = next.showStatusInChat
            prefs[Keys.COMPACT_VOICE_GRID] = next.compactVoiceGrid
            prefs[Keys.PRIVATE_PROFILE] = next.privateProfileMode
            prefs[Keys.LANGUAGE_LABEL] = next.languageLabel
            if (next.appBackgroundUri.isNullOrBlank()) {
                prefs.remove(Keys.APP_BG_URI)
            } else {
                prefs[Keys.APP_BG_URI] = next.appBackgroundUri
            }
            prefs[Keys.APP_BG_DIM] = next.appBackgroundDim.coerceIn(0.15f, 0.9f)
            if (next.inviteWebUrl.isBlank()) {
                prefs.remove(Keys.INVITE_WEB_URL)
            } else {
                prefs[Keys.INVITE_WEB_URL] = next.inviteWebUrl.trim()
            }
        }
    }
}
