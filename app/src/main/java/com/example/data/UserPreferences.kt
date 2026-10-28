package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeMode
import com.example.model.WhatsAppAppType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "status_saver_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_SEEN_TUTORIAL = booleanPreferencesKey("seen_tutorial")
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_ids")
        private val KEY_ALBUM_TAGS_PREFIX = "album_tag_"
        private val KEY_SAF_URI_PREFIX = "saf_uri_"
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    val hasSeenTutorialFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEEN_TUTORIAL] ?: false
    }

    suspend fun setTutorialSeen(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEEN_TUTORIAL] = seen
        }
    }

    val favoriteIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES] ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_FAVORITES] ?: emptySet()).toMutableSet()
            if (current.contains(id)) {
                current.remove(id)
            } else {
                current.add(id)
            }
            prefs[KEY_FAVORITES] = current
        }
    }

    fun getSafUriFlow(appType: WhatsAppAppType): Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[stringPreferencesKey(KEY_SAF_URI_PREFIX + appType.name)]
    }

    suspend fun setSafUri(appType: WhatsAppAppType, uriString: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(KEY_SAF_URI_PREFIX + appType.name)] = uriString
        }
    }

    suspend fun setAlbumTag(statusId: String, tag: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(KEY_ALBUM_TAGS_PREFIX + statusId)] = tag
        }
    }

    val allAlbumTagsFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        val map = mutableMapOf<String, String>()
        for ((key, value) in prefs.asMap()) {
            if (key.name.startsWith(KEY_ALBUM_TAGS_PREFIX) && value is String) {
                val statusId = key.name.removePrefix(KEY_ALBUM_TAGS_PREFIX)
                map[statusId] = value
            }
        }
        map
    }
}
