package com.example.parentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Theme mode independent of the OS setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val dynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultLeadTimeMinutes: Int = 30,
    val quietHoursEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_LEAD_TIME = intPreferencesKey("default_lead_time_minutes")
        val QUIET_HOURS = booleanPreferencesKey("quiet_hours_enabled")
        val ANALYTICS = booleanPreferencesKey("analytics_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            onboardingComplete = p[Keys.ONBOARDING_COMPLETE] ?: false,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: false,
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: true,
            defaultLeadTimeMinutes = p[Keys.DEFAULT_LEAD_TIME] ?: 30,
            quietHoursEnabled = p[Keys.QUIET_HOURS] ?: false,
            analyticsEnabled = p[Keys.ANALYTICS] ?: false,
        )
    }

    suspend fun setOnboardingComplete(complete: Boolean) = edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setDefaultLeadTime(minutes: Int) = edit { it[Keys.DEFAULT_LEAD_TIME] = minutes }
    suspend fun setQuietHoursEnabled(enabled: Boolean) = edit { it[Keys.QUIET_HOURS] = enabled }
    suspend fun setAnalyticsEnabled(enabled: Boolean) = edit { it[Keys.ANALYTICS] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}
