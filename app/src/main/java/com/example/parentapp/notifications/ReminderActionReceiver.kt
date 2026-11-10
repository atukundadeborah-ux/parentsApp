package com.example.parentapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.parentapp.ParentsApp
import com.example.parentapp.domain.model.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles the buttons on a reminder alarm notification:
 * - **Stop alarm** silences the ringing but keeps the reminder card in the shade.
 * - **Snooze** silences it and re-arms the alarm [SNOOZE_MINUTES] from now.
 * - **Mark done** completes the reminder and clears the notification.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, reminderId.toInt())
        val action = intent.action

        // Silence immediately — FLAG_INSISTENT keeps ringing until this notification is replaced or removed.
        NotificationManagerCompat.from(context).cancel(notificationId)

        val app = context.applicationContext as? ParentsApp
        if (app == null || reminderId <= 0L) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context, app, reminderId, action)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, app: ParentsApp, reminderId: Long, action: String?) {
        val container = app.container
        val repo = container.reminderRepository
        val item = repo.getReminderWithDetailsById(reminderId) ?: return
        val reminder = item.reminder

        val verb = when (action) {
            ACTION_STOP -> {
                NotificationHelper.showReminderAlarm(context, item, silenced = true)
                return
            }
            ACTION_SNOOZE -> {
                ReminderScheduler.cancel(context, reminder)
                val now = System.currentTimeMillis()
                val updated = reminder.copy(
                    reminderDateTime = now + SNOOZE_MINUTES * 60_000L,
                    leadTimeMinutes = 0,
                    status = ReminderStatus.SNOOZED,
                    updatedAt = now,
                )
                repo.updateReminder(updated)
                ReminderScheduler.schedule(context, updated)
                "Snoozed reminder '${reminder.title}' for ${SNOOZE_MINUTES}m"
            }
            ACTION_DONE -> {
                ReminderScheduler.cancel(context, reminder)
                repo.updateStatus(reminder.id, ReminderStatus.COMPLETED)
                "Completed reminder '${reminder.title}'"
            }
            else -> return
        }

        val actorId = container.userSession.userId.first() ?: 0L
        val actorName = container.userSession.userName.first().orEmpty()
        container.auditLogger.updated(actorId, actorName, "Reminder", reminder.id, "$verb (from notification)")
    }

    companion object {
        const val ACTION_STOP = "com.example.parentapp.action.REMINDER_STOP"
        const val ACTION_SNOOZE = "com.example.parentapp.action.REMINDER_SNOOZE"
        const val ACTION_DONE = "com.example.parentapp.action.REMINDER_DONE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val SNOOZE_MINUTES = 10L
    }
}
