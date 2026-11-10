package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.ChildDao
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

class ChildRepository(private val childDao: ChildDao) {
    suspend fun count(): Int = childDao.count()
    suspend fun insertChild(child: ChildProfile): Long = childDao.insertChild(child)
    suspend fun updateChild(child: ChildProfile) = childDao.updateChild(child)
    /** Removes the child and its activities, reminders and caregiver tasks (D-01). */
    suspend fun deleteChild(child: ChildProfile) = childDao.deleteChildCascade(child)
    suspend fun getChildById(childId: Long): ChildProfile? = childDao.getChildById(childId)
    fun getChildByIdFlow(childId: Long): Flow<ChildProfile?> = childDao.getChildByIdFlow(childId)
    fun getAllChildren(): Flow<List<ChildProfile>> = childDao.getAllChildren()
    fun getChildrenByGuardian(userId: Long): Flow<List<ChildProfile>> = childDao.getChildrenByGuardian(userId)
    fun getChildrenByInstitution(institutionId: Long): Flow<List<ChildProfile>> = childDao.getChildrenByInstitution(institutionId)
    fun getChildrenByCaregiver(caregiverId: Long): Flow<List<ChildProfile>> = childDao.getChildrenByCaregiver(caregiverId)
    fun searchChildren(query: String): Flow<List<ChildProfile>> = childDao.searchChildren(query)

    /** Role-scoped visibility used by dashboards and the Children list. */
    fun getChildrenVisibleToUser(role: UserRole?, userId: Long?, institutionId: Long?): Flow<List<ChildProfile>> =
        when (role) {
            UserRole.PARENT_GUARDIAN -> childDao.getChildrenByGuardian(userId ?: -1L)
            UserRole.INSTITUTIONAL_USER -> childDao.getChildrenByInstitution(institutionId ?: -1L)
            UserRole.CAREGIVER -> childDao.getChildrenByCaregiver(userId ?: -1L)
            UserRole.ADMIN, null -> childDao.getAllChildren()
        }
}
