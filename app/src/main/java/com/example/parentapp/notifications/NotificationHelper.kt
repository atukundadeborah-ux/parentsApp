package com.example.parentapp.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.parentapp.MainActivity
import com.example.parentapp.R
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.data.local.model.ReminderWithDetails
import com.example.parentapp.domain.model.Priority

/**
 * Builds and posts reminder notifications. Scheduling of the alarms that trigger these
 * notifications lives in [ReminderScheduler]; the notification buttons are handled by
 * [ReminderActionReceiver].
 *
 * Reminder alerts are treated as alarms, not quiet notifications: they play the device
 * alarm ringtone on the alarm audio stream, vibrate, light up, wake the screen via a
 * full-screen intent, and keep sounding (FLAG_INSISTENT) until the user acts on them —
 * so a missed care reminder is hard to sleep through. "Stop alarm" re-posts the same
 * card on a silent channel so the details stay visible after the sound stops.
 */
object NotificationHelper {

    /** Bumped to _v2 when the channel gained the alarm sound; a channel's sound is immutable once created. */
    const val CHANNEL_ID = "parentsapp_reminders_v2"
    private const val LEGACY_CHANNEL_ID = "parentsapp_reminders"
    private const val CHANNEL_NAME = "Care reminders"
    private const val CHANNEL_DESCRIPTION = "Alarm-style alerts for child activities, medication and care tasks"

    /** Used for a reminder whose alarm the user has stopped but not yet acted on. */
    private const val SILENT_CHANNEL_ID = "parentsapp_reminders_silent"
    private const val SILENT_CHANNEL_NAME = "Silenced reminders"
    private const val SILENT_CHANNEL_DESCRIPTION = "Reminders whose alarm has been stopped"

    /** Long extra on the [MainActivity] launch intent: the reminder to open when the notification is tapped. */
    const val EXTRA_OPEN_REMINDER_ID = "extra_open_reminder_id"

    private const val ACCENT_COLOR = 0xFF00796B.toInt()
    private val VIBRATION_PATTERN = longArrayOf(0, 600, 300, 600, 300, 600)

    private fun alarmSoundUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            // Drop the old, quieter channel so its settings don't linger on upgrades.
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = CHANNEL_DESCRIPTION
                setSound(alarmSoundUri(), audioAttributes)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val silentChannel = NotificationChannel(
                SILENT_CHANNEL_ID,
                SILENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = SILENT_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannels(listOf(channel, silentChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun notificationIdFor(reminderId: Long): Int = reminderId.toInt()

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(reminderId))
    }

    /**
     * Posts the full alarm for [item]: who it is for, what it is, when it is due and how
     * urgent it is, with Stop alarm / Snooze / Mark done buttons. With [silenced] the same
     * card is posted quietly (after the user pressed Stop alarm).
     */
    fun showReminderAlarm(context: Context, item: ReminderWithDetails, silenced: Boolean = false) {
        createReminderChannel(context)
        if (!hasNotificationPermission(context)) return

        val reminder = item.reminder
        val notificationId = notificationIdFor(reminder.id)
        val contentIntent = openAppIntent(context, reminder.id, notificationId)

        val dueLabel = dueLabel(reminder.reminderDateTime)
        val summary = "For ${item.childName} · ${reminder.category.label} · $dueLabel"

        val details = SpannableStringBuilder().apply {
            labelled("Child", item.childName)
            labelled("Type", reminder.category.label)
            labelled("Due", "${DateTimeFormatters.formatDateTime(reminder.reminderDateTime)} ($dueLabel)")
            labelled("Priority", reminder.priority.label)
            item.activityTitle?.let { labelled("Activity", it) }
            append(
                if (silenced) "Alarm stopped. Mark it done when finished, or snooze it."
                else "Press Stop alarm to silence, or tap to open the reminder.",
            )
        }

        val builder = NotificationCompat.Builder(context, if (silenced) SILENT_CHANNEL_ID else CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ACCENT_COLOR)
            .setContentTitle(titleFor(reminder.title, reminder.priority))
            .setContentText(summary)
            .setSubText(if (silenced) "Alarm stopped" else "${reminder.priority.label} priority")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(titleFor(reminder.title, reminder.priority))
                    .bigText(details),
            )
            // Not reminder.reminderDateTime: Android 15+ silently drops notifications whose
            // `when` is weeks old (e.g. an overdue reminder); the due time is in the body instead.
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(silenced)
            .addAction(
                if (silenced) actionMarkDone(context, reminder.id)
                else action(context, reminder.id, ReminderActionReceiver.ACTION_STOP, "Stop alarm"),
            )
            .addAction(action(context, reminder.id, ReminderActionReceiver.ACTION_SNOOZE, "Snooze ${ReminderActionReceiver.SNOOZE_MINUTES} min"))

        if (!silenced) {
            builder.addAction(actionMarkDone(context, reminder.id))
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            // Wakes the screen / shows over the lock screen like a clock alarm. On Android 14+
            // without USE_FULL_SCREEN_INTENT it degrades gracefully to a heads-up notification.
            builder.setFullScreenIntent(contentIntent, true)
            // Pre-O the sound/vibration live on the notification, not on a channel.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setSound(alarmSoundUri(), AudioManager.STREAM_ALARM)
                builder.setVibrate(VIBRATION_PATTERN)
                builder.setLights(0xFF00E5FFL.toInt(), 1000, 1000)
            }
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
            builder.setSilent(true)
        }

        val notification = builder.build().apply {
            // Repeat the alarm sound until the user presses Stop alarm or dismisses the alert.
            if (!silenced) flags = flags or Notification.FLAG_INSISTENT
        }

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Fallback when the reminder can no longer be loaded from the database (e.g. it was
     * deleted after the alarm was scheduled): still alarm, with whatever the intent carried.
     */
    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        message: String,
    ) {
        createReminderChannel(context)
        if (!hasNotificationPermission(context)) return

        val notificationId = if (reminderId > 0L) notificationIdFor(reminderId) else title.hashCode()
        val contentIntent = openAppIntent(context, reminderId, notificationId)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(contentIntent, true)
            .addAction(action(context, reminderId, ReminderActionReceiver.ACTION_STOP, "Stop alarm", notificationId))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(alarmSoundUri(), AudioManager.STREAM_ALARM)
            builder.setVibrate(VIBRATION_PATTERN)
            builder.setLights(0xFF00E5FFL.toInt(), 1000, 1000)
        }

        val notification = builder.build().apply {
            flags = flags or Notification.FLAG_INSISTENT
        }

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun titleFor(title: String, priority: Priority): String = when (priority) {
        Priority.URGENT -> "URGENT: $title"
        else -> title
    }

    /** "due now", "due in 15 min", "was due 5 min ago" — relative to the event time. */
    private fun dueLabel(eventMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diffMinutes = (eventMillis - now) / 60_000L
        return when {
            diffMinutes in -1L..1L -> "due now"
            diffMinutes > 0L -> "due ${DateTimeFormatters.relative(eventMillis, now)}"
            else -> "was due ${DateTimeFormatters.relative(eventMillis, now)}"
        }
    }

    private fun SpannableStringBuilder.labelled(label: String, value: String) {
        val start = length
        append("$label: ")
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        append(value).append('\n')
    }

    private fun openAppIntent(context: Context, reminderId: Long, requestCode: Int): PendingIntent {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            // SINGLE_TOP: deliver to the running activity via onNewIntent instead of recreating it.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (reminderId > 0L) putExtra(EXTRA_OPEN_REMINDER_ID, reminderId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionMarkDone(context: Context, reminderId: Long) =
        action(context, reminderId, ReminderActionReceiver.ACTION_DONE, "Mark done")

    private fun action(
        context: Context,
        reminderId: Long,
        action: String,
        label: String,
        notificationId: Int = notificationIdFor(reminderId),
    ): NotificationCompat.Action {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        // Distinct request code per (reminder, action) so the PendingIntents don't collide.
        val pi = PendingIntent.getBroadcast(
            context,
            31 * notificationId + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, label, pi).build()
    }
}
