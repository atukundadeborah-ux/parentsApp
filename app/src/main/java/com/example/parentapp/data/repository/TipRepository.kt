package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.TipDao
import com.example.parentapp.data.local.entity.Tip
import com.example.parentapp.domain.model.TipCategory
import kotlinx.coroutines.flow.Flow

class TipRepository(private val tipDao: TipDao) {
    suspend fun count(): Int = tipDao.count()
    suspend fun insertTip(tip: Tip) = tipDao.insertTip(tip)
    fun getAllTips(): Flow<List<Tip>> = tipDao.getAllTips()
    fun getTipsByCategory(category: TipCategory): Flow<List<Tip>> = tipDao.getTipsByCategory(category)
    fun searchTips(query: String): Flow<List<Tip>> = tipDao.searchTips(query)
    suspend fun getTipById(id: Long): Tip? = tipDao.getTipById(id)
}
