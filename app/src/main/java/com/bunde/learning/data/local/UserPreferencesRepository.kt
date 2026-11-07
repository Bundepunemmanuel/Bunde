package com.bunde.learning.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bunde_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val STUDENT_CLASS_LEVEL = stringPreferencesKey("student_class_level")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LOW_DATA_MODE = booleanPreferencesKey("low_data_mode")
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.GEMINI_API_KEY] ?: "" }

    val studentClassLevelFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.STUDENT_CLASS_LEVEL] ?: "SS2" }

    val studentNameFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.STUDENT_NAME] ?: "Learner" }

    val isLowDataModeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.LOW_DATA_MODE] ?: true }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun updateStudentProfile(name: String, classLevel: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STUDENT_NAME] = name.trim()
            preferences[PreferencesKeys.STUDENT_CLASS_LEVEL] = classLevel.trim()
        }
    }

    suspend fun setLowDataMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOW_DATA_MODE] = enabled
        }
    }
}
