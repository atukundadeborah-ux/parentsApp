package com.example.parentapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.parentapp.core.designsystem.theme.ParentsAppTheme
import com.example.parentapp.data.repository.AppSettings
import com.example.parentapp.data.repository.ThemeMode
import com.example.parentapp.notifications.NotificationHelper
import com.example.parentapp.ui.navigation.ParentAppNavHost

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    /** Reminder to open from a tapped notification; cleared by the nav host once shown. */
    private val pendingReminderId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh launch — after a config change the intent would re-open the reminder.
        if (savedInstanceState == null) consumeReminderIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val settingsFlow = (application as ParentsApp).container.settingsRepository.settings

        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            ParentsAppTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                val reminderToOpen by pendingReminderId.collectAsStateWithLifecycle()
                ParentAppNavHost(
                    pendingReminderId = reminderToOpen,
                    onPendingReminderOpened = { pendingReminderId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeReminderIntent(intent)
    }

    private fun consumeReminderIntent(intent: Intent?) {
        val id = intent?.getLongExtra(NotificationHelper.EXTRA_OPEN_REMINDER_ID, -1L) ?: -1L
        if (id > 0L) {
            pendingReminderId.value = id
            intent?.removeExtra(NotificationHelper.EXTRA_OPEN_REMINDER_ID)
        }
    }
}
