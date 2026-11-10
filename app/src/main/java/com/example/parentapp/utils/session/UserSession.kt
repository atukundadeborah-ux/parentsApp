package com.example.parentapp.utils.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class UserSession(private val context: Context) {

    companion object {
        val USER_ID = longPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val INSTITUTION_ID = longPreferencesKey("institution_id")
    }

    val userId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    val userRole: Flow<UserRole?> = context.dataStore.data.map { preferences ->
        preferences[USER_ROLE]?.let { UserRole.valueOf(it) }
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME]
    }

    val institutionId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[INSTITUTION_ID]
    }

    suspend fun saveSession(userId: Long, userName: String, role: UserRole, institutionId: Long? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_NAME] = userName
            preferences[USER_ROLE] = role.name
            if (institutionId != null) {
                preferences[INSTITUTION_ID] = institutionId
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
