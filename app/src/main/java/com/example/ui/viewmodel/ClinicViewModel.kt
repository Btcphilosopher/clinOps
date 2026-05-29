package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.data.security.SecurityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClinicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClinicRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ClinicRepository(database)
        
        // Seed database asynchronously
        viewModelScope.launch {
            repository.seedDatabase()
        }
    }

    // --- Session & Security States (Zero-Trust) ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isSessionLocked = MutableStateFlow(false)
    val isSessionLocked: StateFlow<Boolean> = _isSessionLocked.asStateFlow()

    private val _securityError = MutableStateFlow<String?>(null)
    val securityError: StateFlow<String?> = _securityError.asStateFlow()

    // Active screen selection (Custom Router state values)
    private val _currentScreen = MutableStateFlow("LOGIN")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Role-Based Access Control State tracker (for dev preview context switching)
    private val _activeViewRole = MutableStateFlow("PATIENT") // Default role we view items in
    val activeViewRole: StateFlow<String> = _activeViewRole.asStateFlow()

    // --- Target selections ---
    private val _selectedPatientId = MutableStateFlow<String?>("PAT_01")
    val selectedPatientId: StateFlow<String?> = _selectedPatientId.asStateFlow()

    private val _selectedClinicianId = MutableStateFlow<String?>("CLI_DOC1")
    val selectedClinicianId: StateFlow<String?> = _selectedClinicianId.asStateFlow()

    // --- Data lists derived reactively based on currently active role / context ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<User>> = repository.getPatientsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clinicians: StateFlow<List<User>> = repository.getCliniciansFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val loggedInUserAppointments: StateFlow<List<Appointment>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else when (user.role) {
                "PATIENT" -> repository.getAppointmentsForPatient(user.id)
                "CLINICIAN" -> repository.getAppointmentsForClinician(user.id)
                "ADMIN" -> repository.getAllAppointments()
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val loggedInUserPrescriptions: StateFlow<List<Prescription>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else when (user.role) {
                "PATIENT" -> repository.getPrescriptionsFlow(user.id)
                "CLINICIAN", "ADMIN" -> repository.getAllPrescriptions()
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogs: StateFlow<List<AuditLog>> = _currentUser
        .flatMapLatest { user ->
            // ONLY Admin has privilege to view raw audit logs
            if (user != null && user.role == "ADMIN") {
                repository.getAuditLogsFlow()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val loggedInUserInvoices: StateFlow<List<Invoice>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else when (user.role) {
                "PATIENT" -> repository.getInvoicesForPatient(user.id)
                "CLINICIAN", "ADMIN" -> repository.getAllInvoices()
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPatientEHRRecords: StateFlow<List<PatientRecord>> = combine(_currentUser, _selectedPatientId) { user, patientId ->
        Pair(user, patientId)
    }.flatMapLatest { (user, patientId) ->
        if (user == null || patientId == null) {
            flowOf(emptyList())
        } else {
            // Decrypts securely on the fly, logs an Access audit in background repository thread
            repository.getRecordsForPatientSecured(patientId, user)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val secureConversationMessages: StateFlow<List<SecureMessage>> = combine(_currentUser, _selectedPatientId, _selectedClinicianId) { user, patId, cliId ->
        Triple(user, patId, cliId)
    }.flatMapLatest { (user, patId, cliId) ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            val user1 = user.id
            val user2 = if (user.role == "PATIENT") {
                cliId ?: "CLI_DOC1"
            } else {
                patId ?: "PAT_01"
            }
            repository.getMessagesSecured(user1, user2, user)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication Actions ---
    fun login(email: String, passcode: String) {
        _securityError.value = null
        viewModelScope.launch {
            val user = repository.findUserByEmail(email)
            if (user == null) {
                _securityError.value = "Invalid email or credentials"
                repository.logAudit("SYSTEM", "Auth Manager", "SECURITY", "AUTHENTICATION", "Login Fail", "Failed login prompt for email: $email (User not found)")
                return@launch
            }

            val hashedInput = SecurityManager.hashPasscode(passcode)
            if (user.passcode == hashedInput) {
                _currentUser.value = user
                _isSessionLocked.value = false
                _activeViewRole.value = user.role
                
                // Set default targets
                if (user.role == "PATIENT") {
                    _selectedPatientId.value = user.id
                } else if (user.role == "CLINICIAN") {
                    _selectedClinicianId.value = user.id
                    _selectedPatientId.value = "PAT_01" // default patient file
                }

                _currentScreen.value = "PORTAL"
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Login", "Successfully signed into platform session.")
            } else {
                _securityError.value = "Incorrect passcode credentials"
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Login Fail", "Passcode entry failed.")
            }
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Logout", "Session closed by user request.")
            }
        }
        _currentUser.value = null
        _isSessionLocked.value = false
        _currentScreen.value = "LOGIN"
    }

    // --- Zero-Trust Re-Authentication (Session Locking) ---
    fun lockSession() {
        _isSessionLocked.value = true
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Session Lock", "Session locked automatically or by request.")
            }
        }
    }

    fun unlockSession(passcode: String): Boolean {
        val user = _currentUser.value ?: return false
        val hashedInput = SecurityManager.hashPasscode(passcode)
        return if (user.passcode == hashedInput) {
            _isSessionLocked.value = false
            _securityError.value = null
            viewModelScope.launch {
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Session Unlock", "Session unlocked successfully via rechecking passcode.")
            }
            true
        } else {
            _securityError.value = "Unlock PIN passcode invalid"
            viewModelScope.launch {
                repository.logAudit(user.id, user.name, user.role, "AUTHENTICATION", "Session Unlock Fail", "In-session unlock passcode rejected.")
            }
            false
        }
    }

    // --- Custom Navigation Router ---
    fun navigateTo(screen: String) {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAudit(user.id, user.name, user.role, "ACCESS", "Navigation", "Navigated view panel to $screen")
            }
        }
        _currentScreen.value = screen
    }

    fun selectPatient(patientId: String) {
        _selectedPatientId.value = patientId
    }

    fun selectClinician(clinicianId: String) {
        _selectedClinicianId.value = clinicianId
    }

    // --- Clinical EHR Writes (Requires CLINICIAN/ADMIN role) ---
    fun addDiagnosisOrNote(patientId: String, patientName: String, recordType: String, summary: String, notes: String, treatment: String) {
        val author = _currentUser.value ?: return
        if (author.role != "CLINICIAN" && author.role != "ADMIN") return

        viewModelScope.launch {
            repository.addMedicalRecord(patientId, patientName, author, recordType, summary, notes, treatment)
        }
    }

    // --- Communication Writes ---
    fun sendSecureMsg(text: String, fileUri: String? = null) {
        val sender = _currentUser.value ?: return
        val patientId = _selectedPatientId.value ?: "PAT_01"
        val clinicianId = _selectedClinicianId.value ?: "CLI_DOC1"

        viewModelScope.launch {
            if (sender.role == "PATIENT") {
                val clinician = clinicians.value.firstOrNull { it.id == clinicianId }
                val clinicianName = clinician?.name ?: "Clinician Staff"
                repository.sendSecureMessage(sender, clinicianId, clinicianName, text, fileUri)
            } else {
                val patient = patients.value.firstOrNull { it.id == patientId }
                val patientName = patient?.name ?: "Patient Clinical Record"
                repository.sendSecureMessage(sender, patientId, patientName, text, fileUri)
            }
        }
    }

    // --- Booking appointments ---
    fun bookAppointment(clinicianId: String, clinicianName: String, dateTimeMillis: Long, reason: String) {
        val patient = _currentUser.value ?: return
        if (patient.role != "PATIENT") return

        viewModelScope.launch {
            val appointment = Appointment(
                patientId = patient.id,
                patientName = patient.name,
                clinicianId = clinicianId,
                clinicianName = clinicianName,
                dateTimeMillis = dateTimeMillis,
                reason = reason,
                status = "CONFIRMED"
            )
            repository.createAppointment(appointment, patient)
        }
    }

    fun cancelAppointment(appointmentId: Int, patientId: String) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, "CANCELLED", actor, patientId)
        }
    }

    // --- Prescriptions ---
    fun writePrescription(patientId: String, patientName: String, medName: String, dosage: String, frequency: String, start: String, end: String) {
        val clinician = _currentUser.value ?: return
        if (clinician.role != "CLINICIAN" && clinician.role != "ADMIN") return

        viewModelScope.launch {
            repository.issuePrescription(patientId, patientName, clinician, medName, dosage, frequency, start, end)
        }
    }

    fun requestRxRenewal(prescription: Prescription) {
        val patient = _currentUser.value ?: return
        viewModelScope.launch {
            repository.requestRenewal(prescription.id, prescription.renewalsRequested, patient, prescription.medicationName)
        }
    }

    fun approveRxRenewal(prescription: Prescription) {
        val clinician = _currentUser.value ?: return
        viewModelScope.launch {
            repository.approveRenewal(prescription.id, prescription.renewalsRequested, clinician, prescription.medicationName)
        }
    }

    // --- Invoicing / Billing ---
    fun createInvoice(patientId: String, patientName: String, description: String, amount: Double, dueDaysAhead: Int) {
        val actor = _currentUser.value ?: return
        if (actor.role != "ADMIN") return

        viewModelScope.launch {
            val dueMillis = System.currentTimeMillis() + (dueDaysAhead * 86400000L)
            repository.issueInvoice(patientId, patientName, description, amount, dueMillis, actor)
        }
    }

    fun payInvoiceBill(invoiceId: Int, amount: Double) {
        val actor = _currentUser.value ?: return
        viewModelScope.launch {
            repository.payInvoice(invoiceId, actor, amount)
        }
    }

    // --- GDPR consent updates ---
    fun revokeConsent() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(consentTracked = false, consentDate = System.currentTimeMillis())
            _currentUser.value = updatedUser
            repository.insertUser(updatedUser)
            repository.logAudit(user.id, user.name, user.role, "CONSENT_UPDATE", "GDPR Owner", "Withdrew/revoked patient data sharing & research tracking consent.")
        }
    }

    fun grantConsent() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(consentTracked = true, consentDate = System.currentTimeMillis())
            _currentUser.value = updatedUser
            repository.insertUser(updatedUser)
            repository.logAudit(user.id, user.name, user.role, "CONSENT_UPDATE", "GDPR Owner", "Granted patient data operations consent.")
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClinicViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ClinicViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
