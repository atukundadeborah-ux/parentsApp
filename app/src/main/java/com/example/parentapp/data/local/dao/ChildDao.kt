package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.parentapp.data.local.entity.ChildProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: ChildProfile): Long

    @Update
    suspend fun updateChild(child: ChildProfile)

    @Delete
    suspend fun deleteChild(child: ChildProfile)

    @Query("SELECT COUNT(*) FROM child_profiles")
    suspend fun count(): Int

    @Query("SELECT * FROM child_profiles WHERE id = :childId")
    suspend fun getChildById(childId: Long): ChildProfile?

    @Query("SELECT * FROM child_profiles WHERE id = :childId")
    fun getChildByIdFlow(childId: Long): Flow<ChildProfile?>

    @Query("SELECT * FROM child_profiles ORDER BY fullName ASC")
    fun getAllChildren(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE createdByUserId = :userId ORDER BY fullName ASC")
    fun getChildrenByGuardian(userId: Long): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE institutionId = :institutionId ORDER BY fullName ASC")
    fun getChildrenByInstitution(institutionId: Long): Flow<List<ChildProfile>>

    @Query(
        "SELECT * FROM child_profiles WHERE assignedCaregiverId = :caregiverId " +
            "OR id IN (SELECT childId FROM caregiver_tasks WHERE assignedToCaregiverId = :caregiverId) " +
            "ORDER BY fullName ASC",
    )
    fun getChildrenByCaregiver(caregiverId: Long): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE fullName LIKE '%' || :query || '%' OR guardianName LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchChildren(query: String): Flow<List<ChildProfile>>
}
