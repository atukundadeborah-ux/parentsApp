package com.example.parentapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.parentapp.data.local.dao.ActivityDao
import com.example.parentapp.data.local.dao.AuditLogDao
import com.example.parentapp.data.local.dao.CaregiverTaskDao
import com.example.parentapp.data.local.dao.ChildDao
import com.example.parentapp.data.local.dao.InstitutionDao
import com.example.parentapp.data.local.dao.ReferralContactDao
import com.example.parentapp.data.local.dao.ReminderDao
import com.example.parentapp.data.local.dao.TipDao
import com.example.parentapp.data.local.dao.UserDao
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.entity.AuditLog
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Institution
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.data.local.entity.UserAccount

@Database(
    entities = [
        UserAccount::class,
        ChildProfile::class,
        Institution::class,
        Activity::class,
        Reminder::class,
        CaregiverTask::class,
        Tip::class,
        ReferralContact::class,
        AuditLog::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun childDao(): ChildDao
    abstract fun institutionDao(): InstitutionDao
    abstract fun activityDao(): ActivityDao
    abstract fun reminderDao(): ReminderDao
    abstract fun caregiverTaskDao(): CaregiverTaskDao
    abstract fun tipDao(): TipDao
    abstract fun referralContactDao(): ReferralContactDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        private const val DB_NAME = "parents_app.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    // Pre-release: no production data to preserve.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
