package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.ActivityDao
import com.example.parentapp.data.local.entity.Activity
import com.example.parentapp.data.local.model.ActivityWithChild
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.domain.model.ActivityCategory
import com.example.parentapp.domain.model.ActivityStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val activityDao: ActivityDao) {
    suspend fun insertActivity(activity: Activity): Long = activityDao.insertActivity(activity)
    suspend fun updateActivity(activity: Activity) = activityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: Activity) = activityDao.deleteActivity(activity)
    fun getActivitiesForChild(childId: Long): Flow<List<Activity>> = activityDao.getActivitiesForChild(childId)
    suspend fun getActivityById(id: Long): Activity? = activityDao.getActivityById(id)
    suspend fun getActivityWithChildById(id: Long): ActivityWithChild? = activityDao.getActivityWithChildById(id)
    fun getAllActivitiesWithChildren(): Flow<List<ActivityWithChild>> = activityDao.getAllActivitiesWithChildren()
    fun getActivityRecordsByChild(childId: Long): Flow<List<ActivityWithChild>> = activityDao.getActivityRecordsByChild(childId)
    fun searchActivities(query: String): Flow<List<ActivityWithChild>> = activityDao.searchActivities(query)
    fun filterActivitiesByCategory(category: ActivityCategory): Flow<List<ActivityWithChild>> =
        activityDao.filterActivitiesByCategory(category)
    fun filterActivitiesByStatus(status: ActivityStatus): Flow<List<ActivityWithChild>> =
        activityDao.filterActivitiesByStatus(status)
    fun filterActivitiesByDateRange(start: Long, end: Long): Flow<List<ActivityWithChild>> =
        activityDao.filterActivitiesByDateRange(start, end)

    fun countByCategoryInRange(start: Long, end: Long): Flow<List<LabelCount>> =
        activityDao.countByCategoryInRange(start, end)
    fun countByStatusInRange(start: Long, end: Long): Flow<List<LabelCount>> =
        activityDao.countByStatusInRange(start, end)
    fun totalInRange(start: Long, end: Long): Flow<Int> = activityDao.totalInRange(start, end)

    fun getActivitiesVisibleToUser(
        role: UserRole?,
        userId: Long?,
        institutionId: Long?,
    ): Flow<List<ActivityWithChild>> = when (role) {
        UserRole.PARENT_GUARDIAN -> activityDao.getActivitiesForGuardian(userId ?: -1L)
        UserRole.INSTITUTIONAL_USER -> activityDao.getActivitiesForInstitution(institutionId ?: -1L)
        UserRole.CAREGIVER -> activityDao.getActivitiesForCaregiver(userId ?: -1L)
        UserRole.ADMIN, null -> activityDao.getAllActivitiesWithChildren()
    }
}
