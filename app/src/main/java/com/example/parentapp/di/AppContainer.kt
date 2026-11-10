package com.example.parentapp.di

import android.content.Context
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.AppDatabase
import com.example.parentapp.data.local.DatabaseSeeder
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.AuditLogRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.InstitutionRepository
import com.example.parentapp.data.repository.ReferralContactRepository
import com.example.parentapp.data.repository.ReminderRepository
import com.example.parentapp.data.repository.SettingsRepository
import com.example.parentapp.data.repository.TipRepository
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.utils.session.UserSession

/**
 * Manual DI graph. A single instance lives on [com.example.parentapp.ParentsApp]; ViewModels
 * reach it through [appContainer] on `CreationExtras`.
 */
interface AppContainer {
    val userRepository: UserRepository
    val childRepository: ChildRepository
    val institutionRepository: InstitutionRepository
    val activityRepository: ActivityRepository
    val reminderRepository: ReminderRepository
    val caregiverTaskRepository: CaregiverTaskRepository
    val tipRepository: TipRepository
    val referralContactRepository: ReferralContactRepository
    val auditLogRepository: AuditLogRepository
    val settingsRepository: SettingsRepository
    val auditLogger: AuditLogger
    val userSession: UserSession

    /** Wipes every table and re-seeds the demo dataset (Settings → "Reset demo data"). */
    suspend fun resetDemoData()
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(appContext) }

    override val userRepository by lazy { UserRepository(database.userDao()) }
    override val childRepository by lazy { ChildRepository(database.childDao()) }
    override val institutionRepository by lazy { InstitutionRepository(database.institutionDao()) }
    override val activityRepository by lazy { ActivityRepository(database.activityDao()) }
    override val reminderRepository by lazy { ReminderRepository(database.reminderDao()) }
    override val caregiverTaskRepository by lazy { CaregiverTaskRepository(database.caregiverTaskDao()) }
    override val tipRepository by lazy { TipRepository(database.tipDao()) }
    override val referralContactRepository by lazy { ReferralContactRepository(database.referralContactDao()) }
    override val auditLogRepository by lazy { AuditLogRepository(database.auditLogDao()) }
    override val settingsRepository by lazy { SettingsRepository(appContext) }
    override val auditLogger by lazy { AuditLogger(auditLogRepository) }
    override val userSession by lazy { UserSession(appContext) }

    /** Exposed for the one-time seeder. */
    val db: AppDatabase get() = database

    override suspend fun resetDemoData() {
        database.clearAllTables()
        settingsRepository.setOnboardingComplete(true)
        DatabaseSeeder.seedIfEmpty(database)
        userSession.clearSession()
    }
}
