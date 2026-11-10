package com.example.parentapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parentapp.ParentsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers pending reminder alarms after the device reboots or the app is updated —
 * [AlarmManager][android.app.AlarmManager] alarms do not survive either event.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pending = goAsync()
                val app = context.applicationContext as? ParentsApp ?: run { pending.finish(); return }
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val reminders = app.container.reminderRepository.getSchedulableReminders()
                            .map { it.reminder }
                        ReminderScheduler.rescheduleAll(context, reminders)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
