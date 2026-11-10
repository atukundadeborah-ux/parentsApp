package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.local.model.LabelCount
import com.example.parentapp.domain.model.AccountStatus
import com.example.parentapp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :userId")
    fun getUserByIdFlow(userId: Long): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE role = :role ORDER BY fullName ASC")
    fun getUsersByRole(role: UserRole): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE role = 'CAREGIVER' AND status = 'ACTIVE' ORDER BY fullName ASC")
    fun getActiveCaregivers(): Flow<List<UserAccount>>

    @Query("UPDATE user_accounts SET status = :status WHERE id = :userId")
    suspend fun updateStatus(userId: Long, status: AccountStatus)

    @Query("UPDATE user_accounts SET role = :role WHERE id = :userId")
    suspend fun updateRole(userId: Long, role: UserRole)

    @Query("UPDATE user_accounts SET passwordHash = :hash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, hash: String)

    @Query("SELECT role AS label, COUNT(*) AS count FROM user_accounts GROUP BY role ORDER BY count DESC")
    fun countByRole(): Flow<List<LabelCount>>
}
