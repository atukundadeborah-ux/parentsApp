package com.example.parentapp.ui.child

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parentapp.core.security.AccessPolicy
import com.example.parentapp.core.util.DateTimeFormatters
import com.example.parentapp.core.util.Validators
import com.example.parentapp.data.audit.AuditLogger
import com.example.parentapp.data.local.entity.ChildProfile
import com.example.parentapp.data.local.entity.UserAccount
import com.example.parentapp.data.repository.ChildRepository
import com.example.parentapp.data.repository.UserRepository
import com.example.parentapp.di.appContainer
import com.example.parentapp.domain.model.ProfileStatus
import com.example.parentapp.ui.navigation.Destinations
import com.example.parentapp.utils.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChildEditorUiState(
    val editing: Boolean = false,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val fullName: String = "",
    val gender: String = "",
    val dateOfBirthMillis: Long? = null,
    val guardianName: String = "",
    val guardianContact: String = "",
    val schoolName: String = "",
    val medicalNotes: String = "",
    val careNotes: String = "",
    val assignedCaregiverId: Long? = null,
    val status: ProfileStatus = ProfileStatus.ACTIVE,
    val caregivers: List<UserAccount> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val savedId: Long? = null,
    val deleted: Boolean = false,
    /** Set when the signed-in role is not allowed to change child profiles (D-02). */
    val formError: String? = null,
)

class ChildEditorViewModel(
    private val childRepository: ChildRepository,
    private val userRepository: UserRepository,
    private val userSession: UserSession,
    private val auditLogger: AuditLogger,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val childId: Long? = savedStateHandle.get<Long>("childId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ChildEditorUiState(editing = childId != null))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val caregivers = runCatching { userRepository.getActiveCaregivers().first() }.getOrDefault(emptyList())
            val existing = childId?.let { childRepository.getChildById(it) }
            val defaultGuardianName = userSession.userName.first().orEmpty()
            _uiState.update { s ->
                if (existing != null) {
                    s.copy(
                        loading = false,
                        caregivers = caregivers,
                        fullName = existing.fullName,
                        gender = existing.gender,
                        dateOfBirthMillis = DateTimeFormatters.parseIsoDateToMillis(existing.dateOfBirth),
                        guardianName = existing.guardianName,
                        guardianContact = existing.guardianContact,
                        schoolName = existing.schoolName,
                        medicalNotes = existing.medicalNotes,
                        careNotes = existing.careNotes,
                        assignedCaregiverId = existing.assignedCaregiverId,
                        status = existing.profileStatus,
                    )
                } else {
                    s.copy(loading = false, caregivers = caregivers, guardianName = defaultGuardianName)
                }
            }
        }
    }

    fun onFullName(v: String) = _uiState.update { it.copy(fullName = v, errors = it.errors - "fullName") }
    fun onGender(v: String) = _uiState.update { it.copy(gender = v, errors = it.errors - "gender") }
    fun onDob(v: Long) = _uiState.update { it.copy(dateOfBirthMillis = v, errors = it.errors - "dob") }
    fun onGuardianName(v: String) = _uiState.update { it.copy(guardianName = v, errors = it.errors - "guardianName") }
    fun onGuardianContact(v: String) = _uiState.update { it.copy(guardianContact = v, errors = it.errors - "guardianContact") }
    fun onSchool(v: String) = _uiState.update { it.copy(schoolName = v) }
    fun onMedicalNotes(v: String) = _uiState.update { it.copy(medicalNotes = v) }
    fun onCareNotes(v: String) = _uiState.update { it.copy(careNotes = v) }
    fun onCaregiver(id: Long?) = _uiState.update { it.copy(assignedCaregiverId = id) }
    fun onStatus(status: ProfileStatus) = _uiState.update { it.copy(status = status) }

    fun save() {
        val s = _uiState.value
        val errors = buildMap {
            Validators.nameError(s.fullName)?.let { put("fullName", it) }
            Validators.requiredError(s.gender, "Gender")?.let { put("gender", it) }
            if (s.dateOfBirthMillis == null) put("dob", "Date of birth is required")
            else if (s.dateOfBirthMillis > System.currentTimeMillis()) put("dob", "Date of birth cannot be in the future")
            Validators.requiredError(s.guardianName, "Guardian name")?.let { put("guardianName", it) }
            Validators.phoneError(s.guardianContact)?.let { put("guardianContact", it) }
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (!AccessPolicy.canEditChildProfiles(userSession.userRole.first())) {
                _uiState.update { it.copy(saving = false, formError = AccessPolicy.DENIED_MESSAGE) }
                return@launch
            }
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            val institutionId = userSession.institutionId.first()
            val caregiverName = s.caregivers.firstOrNull { it.id == s.assignedCaregiverId }?.fullName.orEmpty()
            val existing = childId?.let { childRepository.getChildById(it) }
            val dobIso = DateTimeFormatters.formatIsoDate(s.dateOfBirthMillis!!)

            val profile = (existing ?: ChildProfile(
                fullName = "", gender = "", dateOfBirth = "", guardianName = "",
                guardianContact = "", createdByUserId = actorId, careNotes = "",
                institutionId = institutionId,
            )).copy(
                fullName = s.fullName.trim(),
                gender = s.gender.trim(),
                dateOfBirth = dobIso,
                guardianName = s.guardianName.trim(),
                guardianContact = s.guardianContact.trim(),
                schoolName = s.schoolName.trim(),
                medicalNotes = s.medicalNotes.trim(),
                careNotes = s.careNotes.trim(),
                assignedCaregiver = caregiverName,
                assignedCaregiverId = s.assignedCaregiverId,
                profileStatus = s.status,
                updatedAt = System.currentTimeMillis(),
            )

            val id = if (existing != null) {
                childRepository.updateChild(profile)
                auditLogger.updated(actorId, actorName, "ChildProfile", profile.id, "Updated profile for ${profile.fullName}")
                profile.id
            } else {
                val newId = childRepository.insertChild(profile)
                auditLogger.created(actorId, actorName, "ChildProfile", newId, "Added child ${profile.fullName}")
                newId
            }
            _uiState.update { it.copy(saving = false, savedId = id) }
        }
    }

    fun delete() {
        val id = childId ?: return
        viewModelScope.launch {
            if (!AccessPolicy.canEditChildProfiles(userSession.userRole.first())) {
                _uiState.update { it.copy(formError = AccessPolicy.DENIED_MESSAGE) }
                return@launch
            }
            val child = childRepository.getChildById(id) ?: return@launch
            val actorId = userSession.userId.first() ?: 0L
            val actorName = userSession.userName.first().orEmpty()
            childRepository.deleteChild(child)
            auditLogger.deleted(actorId, actorName, "ChildProfile", id, "Deleted child ${child.fullName}")
            _uiState.update { it.copy(deleted = true) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.appContainer
                ChildEditorViewModel(
                    c.childRepository, c.userRepository, c.userSession, c.auditLogger,
                    createSavedStateHandle(),
                )
            }
        }

        fun defaultLocalDate(): LocalDate = LocalDate.now().minusYears(4)
    }
}
