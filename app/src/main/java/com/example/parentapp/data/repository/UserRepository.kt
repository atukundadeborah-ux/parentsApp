package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.UserDao
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    suspend fun count(): Int = userDao.count()
    suspend fun insertUser(user: UserAccount): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserAccount) = userDao.updateUser(user)
    suspend fun getUserByEmail(email: String): UserAccount? = userDao.getUserByEmail(email)
    suspend fun getUserById(userId: Long): UserAccount? = userDao.getUserById(userId)
    fun getUserByIdFlow(userId: Long): Flow<UserAccount?> = userDao.getUserByIdFlow(userId)
    fun getAllUsers(): Flow<List<UserAccount>> = userDao.getAllUsers()
    fun getUsersByRole(role: UserRole): Flow<List<UserAccount>> = userDao.getUsersByRole(role)
    fun getActiveCaregivers(): Flow<List<UserAccount>> = userDao.getActiveCaregivers()
    fun countByRole(): Flow<List<LabelCount>> = userDao.countByRole()

    suspend fun setStatus(userId: Long, status: AccountStatus) = userDao.updateStatus(userId, status)
    suspend fun setRole(userId: Long, role: UserRole) = userDao.updateRole(userId, role)
    suspend fun setPassword(userId: Long, hash: String) = userDao.updatePassword(userId, hash)
}
