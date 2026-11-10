package com.example.parentapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.parentapp.data.local.AppDatabase
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.entity.CaregiverTask
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.ActivityRepository
import com.example.parentapp.data.repository.CaregiverTaskRepository
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.TaskStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the report aggregates through the repository layer on a real (in-memory) Room DB. */
@RunWith(AndroidJUnit4::class)
class RepositoryReportTest {

    private lateinit var db: AppDatabase
    private lateinit var activityRepository: ActivityRepository
    private lateinit var taskRepository: CaregiverTaskRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        activityRepository = ActivityRepository(db.activityDao())
        taskRepository = CaregiverTaskRepository(db.caregiverTaskDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun user(name: String, email: String, role: UserRole): Long =
        db.userDao().insertUser(
            UserAccount(
                fullName = name, email = email, phone = "+256772000000",
                passwordHash = "hash", role = role, userCategory = role.label,
            ),
        )

    private suspend fun child(guardianId: Long): Long = db.childDao().insertChild(
        ChildProfile(
            fullName = "Amani", gender = "Female", dateOfBirth = "2018-01-01",
            guardianName = "Guardian", guardianContact = "+256772000000",
            createdByUserId = guardianId, careNotes = "",
        ),
    )

    @Test
    fun activityAggregatesRespectTheDateWindow() = runBlocking {
        val guardianId = user("Guardian", "g@example.com", UserRole.PARENT_GUARDIAN)
        val childId = child(guardianId)
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        suspend fun add(at: Long, category: ActivityCategory, status: ActivityStatus) =
            activityRepository.insertActivity(
                Activity(
                    childId = childId, category = category, title = "t", description = "",
                    activityDateTime = at, status = status, createdByUserId = guardianId,
                ),
            )

        add(now - 2 * day, ActivityCategory.SCHOOL, ActivityStatus.COMPLETED)
        add(now - 3 * day, ActivityCategory.SCHOOL, ActivityStatus.PENDING)
        add(now - 4 * day, ActivityCategory.HEALTH, ActivityStatus.COMPLETED)
        add(now - 40 * day, ActivityCategory.PLAY, ActivityStatus.COMPLETED) // outside a 30-day window

        val start = now - 30 * day
        assertEquals(3, activityRepository.totalInRange(start, now).first())

        val byCategory = activityRepository.countByCategoryInRange(start, now).first().associate { it.label to it.count }
        assertEquals(2, byCategory[ActivityCategory.SCHOOL.name])
        assertEquals(1, byCategory[ActivityCategory.HEALTH.name])
        assertEquals(null, byCategory[ActivityCategory.PLAY.name])

        val byStatus = activityRepository.countByStatusInRange(start, now).first().associate { it.label to it.count }
        assertEquals(2, byStatus[ActivityStatus.COMPLETED.name])
        assertEquals(1, byStatus[ActivityStatus.PENDING.name])
    }

    @Test
    fun caregiverCompletionReportCountsPerCaregiver() = runBlocking {
        val guardianId = user("Guardian", "g@example.com", UserRole.PARENT_GUARDIAN)
        val busy = user("Bea Caregiver", "bea@example.com", UserRole.CAREGIVER)
        val idle = user("Ida Caregiver", "ida@example.com", UserRole.CAREGIVER)
        val childId = child(guardianId)
        val now = System.currentTimeMillis()

        suspend fun task(caregiverId: Long, status: TaskStatus) = taskRepository.insertTask(
            CaregiverTask(
                childId = childId, assignedToCaregiverId = caregiverId, assignedByUserId = guardianId,
                title = "t", description = "", dueDateTime = now, status = status,
            ),
        )

        task(busy, TaskStatus.COMPLETED)
        task(busy, TaskStatus.COMPLETED)
        task(busy, TaskStatus.MISSED)
        task(busy, TaskStatus.PENDING)

        val report = taskRepository.caregiverCompletionReport().first().associateBy { it.caregiverName }

        val bea = report.getValue("Bea Caregiver")
        assertEquals(4, bea.total)
        assertEquals(2, bea.completed)
        assertEquals(1, bea.missed)
        assertEquals(0.5f, bea.completionRate, 0.0001f)

        val ida = report.getValue("Ida Caregiver")
        assertEquals(0, ida.total)
        assertEquals(0f, ida.completionRate, 0.0001f)

        // guardian is not a caregiver, so the report never lists them
        assertTrue(report.keys.none { it == "Guardian" })
    }
}
