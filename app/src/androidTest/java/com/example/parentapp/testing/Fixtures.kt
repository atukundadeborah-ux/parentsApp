package com.example.parentapp.testing

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.AppDatabase
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.AuditLogRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.ReferralContactRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

val appContext: Context get() = ApplicationProvider.getApplicationContext()

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

/**
 * An in-memory Room database with the real repositories wired on top, plus the real
 * DataStore-backed [UserSession]. Every test gets a fresh, empty database.
 */
class TestEnv {
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val users = UserRepository(db.userDao())
    val children = ChildRepository(db.childDao())
    val activities = ActivityRepository(db.activityDao())
    val reminders = ReminderRepository(db.reminderDao())
    val tasks = CaregiverTaskRepository(db.caregiverTaskDao())
    val referrals = ReferralContactRepository(db.referralContactDao())
    val auditRepo = AuditLogRepository(db.auditLogDao())
    val audit = AuditLogger(auditRepo)
    val session = UserSession(appContext)
    val settings = SettingsRepository(appContext)

    suspend fun user(
        name: String,
        email: String,
        role: UserRole,
        institutionId: Long? = null,
    ): Long = users.insertUser(
        UserAccount(
            fullName = name, email = email, phone = "+256772000000",
            passwordHash = "x", role = role, userCategory = role.label, institutionId = institutionId,
        ),
    )

    suspend fun child(
        guardianId: Long,
        name: String = "Amani Mukasa",
        caregiverId: Long? = null,
        institutionId: Long? = null,
    ): Long = children.insertChild(
        ChildProfile(
            fullName = name, gender = "Female", dateOfBirth = "2019-03-01",
            guardianName = "Guardian", guardianContact = "+256772000000",
            assignedCaregiverId = caregiverId, institutionId = institutionId,
            createdByUserId = guardianId,
        ),
    )

    suspend fun activity(
        childId: Long,
        by: Long,
        at: Long = System.currentTimeMillis(),
        category: ActivityCategory = ActivityCategory.SCHOOL,
        status: ActivityStatus = ActivityStatus.COMPLETED,
        title: String = "Activity",
    ): Long = activities.insertActivity(
        Activity(
            childId = childId, category = category, title = title, description = "",
            activityDateTime = at, status = status, createdByUserId = by,
        ),
    )

    suspend fun reminder(
        childId: Long,
        by: Long,
        at: Long,
        status: ReminderStatus = ReminderStatus.PENDING,
        activityId: Long? = null,
        lead: Int = 0,
        title: String = "Reminder",
        notify: Boolean = true,
    ): Long = reminders.insertReminder(
        Reminder(
            childId = childId, activityId = activityId, title = title,
            category = ReminderCategory.HEALTH, reminderDateTime = at, leadTimeMinutes = lead,
            priority = Priority.HIGH, status = status, notificationEnabled = notify,
            createdByUserId = by,
        ),
    )

    suspend fun task(
        childId: Long,
        caregiverId: Long,
        by: Long,
        due: Long = System.currentTimeMillis() + DAY,
        status: TaskStatus = TaskStatus.PENDING,
        title: String = "Task",
    ): Long = tasks.insertTask(
        CaregiverTask(
            childId = childId, assignedToCaregiverId = caregiverId, assignedByUserId = by,
            title = title, description = "", dueDateTime = due, status = status,
        ),
    )

    suspend fun signIn(userId: Long, name: String, role: UserRole, institutionId: Long? = null) =
        session.saveSession(userId, name, role, institutionId)

    suspend fun close() {
        session.clearSession()
        db.close()
    }
}

/** Waits (max 5 s) for a ViewModel state that satisfies [predicate]. */
suspend fun <T> StateFlow<T>.awaitState(predicate: (T) -> Boolean): T =
    withTimeout(5_000) { first(predicate) }

/** Gives ViewModel coroutines a moment to finish when no state change is expected. */
suspend fun settle() = delay(750)

fun grantNotificationPermission() {
    if (Build.VERSION.SDK_INT >= 33) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(appContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }
}

fun activeNotifications(): List<StatusBarNotification> =
    appContext.getSystemService(NotificationManager::class.java).activeNotifications.toList()

fun clearNotifications() = appContext.getSystemService(NotificationManager::class.java).cancelAll()

/** Polls the status bar (max 5 s) until a notification matching [predicate] is posted. */
suspend fun awaitNotification(predicate: (StatusBarNotification) -> Boolean): StatusBarNotification? {
    repeat(25) {
        activeNotifications().firstOrNull(predicate)?.let { return it }
        delay(200)
    }
    return null
}

fun StatusBarNotification.title(): String =
    notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
