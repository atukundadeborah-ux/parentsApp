package com.example.parentapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.domain.model.ReferralCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferralContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ReferralContact)

    @Query("SELECT COUNT(*) FROM referral_contacts")
    suspend fun count(): Int

    /** Sauti 116 (child helpline) pinned first, then emergency + hotline entries, then alphabetical. */
    @Query("SELECT * FROM referral_contacts ORDER BY (phone = '116') DESC, isEmergency DESC, isHotline DESC, name ASC")
    fun getAllContacts(): Flow<List<ReferralContact>>

    @Query("SELECT * FROM referral_contacts WHERE category = :category ORDER BY (phone = '116') DESC, isHotline DESC, name ASC")
    fun getContactsByCategory(category: ReferralCategory): Flow<List<ReferralContact>>

    @Query("SELECT * FROM referral_contacts WHERE isEmergency = 1 ORDER BY (phone = '116') DESC, isHotline DESC, name ASC")
    fun getEmergencyContacts(): Flow<List<ReferralContact>>

    @Query("SELECT * FROM referral_contacts WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContacts(query: String): Flow<List<ReferralContact>>

    @Query("SELECT * FROM referral_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ReferralContact?
}
