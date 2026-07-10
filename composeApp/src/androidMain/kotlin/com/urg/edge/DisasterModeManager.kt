package com.urg.edge

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("disaster_prefs")

class DisasterModeManager(private val context: Context) {

    companion object {
        private val KEY = booleanPreferencesKey("is_disaster_mode")
    }

    val isDisasterMode: Flow<Boolean> = context.dataStore.data
        .map { it[KEY] ?: false }

    suspend fun setDisasterMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY] = enabled }
    }
}