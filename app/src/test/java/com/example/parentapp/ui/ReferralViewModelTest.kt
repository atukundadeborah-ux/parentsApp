package com.example.parentapp.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.parentapp.data.local.dao.ReferralContactDao
import com.example.parentapp.data.local.entity.ReferralContact
import com.example.parentapp.data.repository.ReferralContactRepository
import com.example.parentapp.domain.model.ReferralCategory
import com.example.parentapp.testutil.MainDispatcherRule
import com.example.parentapp.ui.referral.ReferralUiState
import com.example.parentapp.ui.referral.ReferralViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sauti = contact(1, "Sauti 116", ReferralCategory.EMERGENCY, emergency = true)
    private val clinic = contact(2, "Riverside Clinic", ReferralCategory.HEALTH)
    private val legalAid = contact(3, "Legal Aid Desk", ReferralCategory.LEGAL)

    private lateinit var viewModel: ReferralViewModel

    @Before
    fun setUp() {
        val dao = FakeReferralContactDao(listOf(sauti, clinic, legalAid))
        viewModel = ReferralViewModel(ReferralContactRepository(dao))
    }

    @Test
    fun `all contacts load with emergency entries separated`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.uiState.test {
            val loaded = awaitSettled()
            assertEquals(listOf(sauti), loaded.emergency)
            // non-emergency entries come back alphabetical: "Legal Aid Desk" before "Riverside Clinic"
            assertEquals(listOf(legalAid, clinic), loaded.others)
        }
    }

    @Test
    fun `category filter narrows the list`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.uiState.test {
            awaitSettled()
            viewModel.onCategory(ReferralCategory.HEALTH)
            val filtered = awaitUntil { it.category == ReferralCategory.HEALTH }
            assertEquals(listOf(clinic), filtered.others)
            assertTrue(filtered.emergency.isEmpty())
        }
    }

    @Test
    fun `query search matches by name and wins over category`() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.uiState.test {
            awaitSettled()
            viewModel.onCategory(ReferralCategory.HEALTH)
            awaitUntil { it.category == ReferralCategory.HEALTH }
            viewModel.onQuery("legal")
            val searched = awaitUntil { it.query == "legal" }
            assertEquals(listOf(legalAid), searched.others)
        }
    }

    private suspend fun ReceiveTurbine<ReferralUiState>.awaitSettled(): ReferralUiState =
        awaitUntil { !it.loading }

    private suspend fun ReceiveTurbine<ReferralUiState>.awaitUntil(
        predicate: (ReferralUiState) -> Boolean,
    ): ReferralUiState {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    private fun contact(
        id: Long,
        name: String,
        category: ReferralCategory,
        emergency: Boolean = false,
    ) = ReferralContact(
        id = id, name = name, category = category, phone = "116",
        location = "Kampala", description = "", isEmergency = emergency,
    )
}

/** In-memory [ReferralContactDao] that applies the same filtering the real queries do. */
private class FakeReferralContactDao(initial: List<ReferralContact>) : ReferralContactDao {

    private val contacts = MutableStateFlow(initial)

    override suspend fun insertContact(contact: ReferralContact) {
        contacts.value = contacts.value.filterNot { it.id == contact.id } + contact
    }

    override suspend fun count(): Int = contacts.value.size

    override fun getAllContacts(): Flow<List<ReferralContact>> =
        contacts.map { list ->
            list.sortedWith(compareByDescending<ReferralContact> { it.isEmergency }.thenBy { it.name })
        }

    override fun getContactsByCategory(category: ReferralCategory): Flow<List<ReferralContact>> =
        contacts.map { list -> list.filter { it.category == category }.sortedBy { it.name } }

    override fun getEmergencyContacts(): Flow<List<ReferralContact>> =
        contacts.map { list -> list.filter { it.isEmergency } }

    override fun searchContacts(query: String): Flow<List<ReferralContact>> =
        contacts.map { list ->
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)
            }.sortedBy { it.name }
        }

    override suspend fun getContactById(id: Long): ReferralContact? =
        contacts.value.firstOrNull { it.id == id }
}
