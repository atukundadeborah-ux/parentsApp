package com.example.parentapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parentapp.ParentsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a reminder's alarm goes off. Loads the reminder with its child/activity so the
 * notification can say exactly who and what it is for; falls back to the intent extras if
 * the reminder is gone.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have a ParentsApp reminder."

        val app = context.applicationContext as? ParentsApp
        if (app == null || reminderId <= 0L) {
            NotificationHelper.showReminderNotification(context, reminderId, title, message)
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = runCatching {
                    app.container.reminderRepository.getReminderWithDetailsById(reminderId)
                }.getOrNull()
                if (item != null) {
                    NotificationHelper.showReminderAlarm(context, item)
                } else {
                    NotificationHelper.showReminderNotification(context, reminderId, title, message)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
    }
}
