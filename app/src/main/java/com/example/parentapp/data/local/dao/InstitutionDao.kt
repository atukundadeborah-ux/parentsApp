package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.parentapp.data.local.entity.Institution
import kotlinx.coroutines.flow.Flow

@Dao
interface InstitutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitution(institution: Institution): Long

    @Update
    suspend fun updateInstitution(institution: Institution)

    @Query("SELECT COUNT(*) FROM institutions")
    suspend fun count(): Int

    @Query("SELECT * FROM institutions WHERE id = :id")
    suspend fun getInstitutionById(id: Long): Institution?

    @Query("SELECT * FROM institutions ORDER BY name ASC")
    fun getAllInstitutions(): Flow<List<Institution>>
}
