package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.InstitutionDao
import com.example.parentapp.data.local.entity.Institution
import kotlinx.coroutines.flow.Flow

class InstitutionRepository(private val institutionDao: InstitutionDao) {
    suspend fun count(): Int = institutionDao.count()
    suspend fun insertInstitution(institution: Institution): Long = institutionDao.insertInstitution(institution)
    suspend fun updateInstitution(institution: Institution) = institutionDao.updateInstitution(institution)
    suspend fun getInstitutionById(id: Long): Institution? = institutionDao.getInstitutionById(id)
    fun getAllInstitutions(): Flow<List<Institution>> = institutionDao.getAllInstitutions()
}
