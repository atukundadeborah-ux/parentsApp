package com.example.parentapp.data.local.dao

import androidx.room.*
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity): Long

    @Update
    suspend fun updateActivity(activity: Activity)

    @Delete
    suspend fun deleteActivity(activity: Activity)

    @Query("SELECT * FROM activities WHERE childId = :childId ORDER BY activityDateTime DESC")
    fun getActivitiesForChild(childId: Long): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): Activity?

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.id = :id
    """)
    suspend fun getActivityWithChildById(id: Long): ActivityWithChild?

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        ORDER BY activities.activityDateTime DESC
    """)
    fun getAllActivitiesWithChildren(): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE child_profiles.createdByUserId = :userId
        ORDER BY activities.activityDateTime DESC
    """)
    fun getActivitiesForGuardian(userId: Long): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE child_profiles.institutionId = :institutionId
        ORDER BY activities.activityDateTime DESC
    """)
    fun getActivitiesForInstitution(institutionId: Long): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.childId IN (
            SELECT childId FROM caregiver_tasks WHERE assignedToCaregiverId = :caregiverId
        )
        ORDER BY activities.activityDateTime DESC
    """)
    fun getActivitiesForCaregiver(caregiverId: Long): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.childId = :childId
        ORDER BY activities.activityDateTime DESC
    """)
    fun getActivityRecordsByChild(childId: Long): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.title LIKE '%' || :query || '%'
           OR activities.description LIKE '%' || :query || '%'
        ORDER BY activities.activityDateTime DESC
    """)
    fun searchActivities(query: String): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.category = :category
        ORDER BY activities.activityDateTime DESC
    """)
    fun filterActivitiesByCategory(category: ActivityCategory): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.status = :status
        ORDER BY activities.activityDateTime DESC
    """)
    fun filterActivitiesByStatus(status: ActivityStatus): Flow<List<ActivityWithChild>>

    @Query("""
        SELECT activities.*, child_profiles.fullName AS childName
        FROM activities
        INNER JOIN child_profiles ON child_profiles.id = activities.childId
        WHERE activities.activityDateTime BETWEEN :startDateTime AND :endDateTime
        ORDER BY activities.activityDateTime DESC
    """)
    fun filterActivitiesByDateRange(startDateTime: Long, endDateTime: Long): Flow<List<ActivityWithChild>>

    // ---- Reports / aggregates ----

    @Query("""
        SELECT category AS label, COUNT(*) AS count
        FROM activities
        WHERE activityDateTime BETWEEN :start AND :end
        GROUP BY category ORDER BY count DESC
    """)
    fun countByCategoryInRange(start: Long, end: Long): Flow<List<LabelCount>>

    @Query("""
        SELECT status AS label, COUNT(*) AS count
        FROM activities
        WHERE activityDateTime BETWEEN :start AND :end
        GROUP BY status ORDER BY count DESC
    """)
    fun countByStatusInRange(start: Long, end: Long): Flow<List<LabelCount>>

    @Query("SELECT COUNT(*) FROM activities WHERE activityDateTime BETWEEN :start AND :end")
    fun totalInRange(start: Long, end: Long): Flow<Int>
}
