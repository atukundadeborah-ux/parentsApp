package com.example.parentapp.data.repository

import com.example.parentapp.data.local.dao.ReferralContactDao
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.domain.model.ReferralCategory
import kotlinx.coroutines.flow.Flow

class ReferralContactRepository(private val dao: ReferralContactDao) {
    suspend fun count(): Int = dao.count()
    suspend fun insertContact(contact: ReferralContact) = dao.insertContact(contact)
    fun getAllContacts(): Flow<List<ReferralContact>> = dao.getAllContacts()
    fun getContactsByCategory(category: ReferralCategory): Flow<List<ReferralContact>> = dao.getContactsByCategory(category)
    fun getEmergencyContacts(): Flow<List<ReferralContact>> = dao.getEmergencyContacts()
    fun searchContacts(query: String): Flow<List<ReferralContact>> = dao.searchContacts(query)
    suspend fun getContactById(id: Long): ReferralContact? = dao.getContactById(id)
}
