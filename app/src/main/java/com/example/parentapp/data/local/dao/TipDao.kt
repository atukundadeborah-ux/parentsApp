package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.domain.model.TipCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: Tip)

    @Query("SELECT COUNT(*) FROM tips")
    suspend fun count(): Int

    @Query("SELECT * FROM tips ORDER BY category, title")
    fun getAllTips(): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE category = :category ORDER BY title")
    fun getTipsByCategory(category: TipCategory): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY title")
    fun searchTips(query: String): Flow<List<Tip>>

    @Query("SELECT * FROM tips WHERE id = :id")
    suspend fun getTipById(id: Long): Tip?
}
