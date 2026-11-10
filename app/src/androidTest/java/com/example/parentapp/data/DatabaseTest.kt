package com.example.parentapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.data.local.AppDatabase
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.Reminder
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.domain.model.Priority
import com.example.parentapp.domain.model.ReminderCategory
import com.example.parentapp.domain.model.ReminderStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedGuardian(): Long = db.userDao().insertUser(
        UserAccount(
            fullName = "Guardian", email = "g@example.com", phone = "+256772000000",
            passwordHash = "hash", role = UserRole.PARENT_GUARDIAN, userCategory = "Parent",
        ),
    )

    @Test
    fun childCrudAndGuardianScope() = runBlocking {
        val guardianId = seedGuardian()
        val otherId = db.userDao().insertUser(
            UserAccount(
                fullName = "Other", email = "o@example.com", phone = "+256772000009",
                passwordHash = "hash", role = UserRole.PARENT_GUARDIAN, userCategory = "Parent",
            ),
        )

        val childId = db.childDao().insertChild(
            ChildProfile(
                fullName = "Amani", gender = "Female", dateOfBirth = "2018-01-01",
                guardianName = "Guardian", guardianContact = "+256772000000",
                createdByUserId = guardianId, careNotes = "",
            ),
        )
        db.childDao().insertChild(
            ChildProfile(
                fullName = "NotMine", gender = "Male", dateOfBirth = "2019-01-01",
                guardianName = "Other", guardianContact = "+256772000009",
                createdByUserId = otherId, careNotes = "",
            ),
        )

        val mine = db.childDao().getChildrenByGuardian(guardianId).first()
        assertEquals(1, mine.size)
        assertEquals("Amani", mine.first().fullName)

        val loaded = db.childDao().getChildById(childId)
        assertNotNull(loaded)
        db.childDao().updateChild(loaded!!.copy(schoolName = "Riverside"))
        assertEquals("Riverside", db.childDao().getChildById(childId)?.schoolName)

        db.childDao().deleteChild(loaded)
        assertNull(db.childDao().getChildById(childId))
    }

    @Test
    fun pastPendingRemindersAreMarkedMissed() = runBlocking {
        val guardianId = seedGuardian()
        val childId = db.childDao().insertChild(
            ChildProfile(
                fullName = "Amani", gender = "Female", dateOfBirth = "2018-01-01",
                guardianName = "Guardian", guardianContact = "+256772000000",
                createdByUserId = guardianId, careNotes = "",
            ),
        )
        val now = System.currentTimeMillis()
        fun r(offset: Long) = Reminder(
            childId = childId, title = "r", category = ReminderCategory.MEDICATION,
            reminderDateTime = now + offset, priority = Priority.MEDIUM,
            status = ReminderStatus.PENDING, notificationEnabled = true, createdByUserId = guardianId,
        )
        val pastId = db.reminderDao().insertReminder(r(-3_600_000L))
        val futureId = db.reminderDao().insertReminder(r(3_600_000L))

        db.reminderDao().markPastPendingRemindersMissed(now)

        assertEquals(ReminderStatus.MISSED, db.reminderDao().getReminderById(pastId)?.status)
        assertEquals(ReminderStatus.PENDING, db.reminderDao().getReminderById(futureId)?.status)
    }
}
