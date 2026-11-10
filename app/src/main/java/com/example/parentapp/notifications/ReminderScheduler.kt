package com.example.parentapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.entity.Reminder

/**
 * Schedules / cancels the [AlarmManager] alarms that deliver reminder notifications via
 * [ReminderAlarmReceiver]. Uses exact alarms when the OS permits, otherwise an inexact
 * best-effort alarm.
 */
object ReminderScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
    }

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.notificationEnabled) return
        val triggerAt = reminder.triggerAtMillis
        if (triggerAt <= System.currentTimeMillis()) return

        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, reminder)

        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun cancel(context: Context, reminder: Reminder) =
        cancel(context, reminder.notificationRequestCode ?: reminder.id.toInt())

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val requestCode = reminder.notificationRequestCode ?: reminder.id.toInt()
        val dueAt = DateTimeFormatters.formatDateTime(reminder.reminderDateTime)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(
                ReminderAlarmReceiver.EXTRA_MESSAGE,
                "${reminder.category.label} reminder · due $dueAt (${reminder.priority.label} priority)",
            )
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
