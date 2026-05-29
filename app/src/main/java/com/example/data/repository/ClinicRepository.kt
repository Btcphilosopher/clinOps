package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import com.example.data.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClinicRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val appointmentDao = db.appointmentDao()
    private val patientRecordDao = db.patientRecordDao()
    private val prescriptionDao = db.prescriptionDao()
    private val secureMessageDao = db.secureMessageDao()
    private val auditLogDao = db.auditLogDao()
    private val invoiceDao = db.invoiceDao()

    // --- Users ---
    suspend fun findUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun findUserById(id: String): User? = userDao.getUserById(id)
    fun getPatientsFlow(): Flow<List<User>> = userDao.getAllPatientsFlow()
    fun getCliniciansFlow(): Flow<List<User>> = userDao.getAllCliniciansFlow()
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        logAudit(
            actorId = "SYSTEM",
            actorName = "System Auth",
            actorRole = "SYSTEM",
            actionType = "WRITE",
            resourceAffected = "User ${user.id}",
            details = "Created user account for ${user.name} (${user.role})"
        )
    }

    // --- Audit Logs ---
    fun getAuditLogsFlow(): Flow<List<AuditLog>> = auditLogDao.getAllLogsFlow()
    
    suspend fun logAudit(
        actorId: String,
        actorName: String,
        actorRole: String,
        actionType: String,
        resourceAffected: String,
        details: String
    ) {
        val auditLog = AuditLog(
            actorId = actorId,
            actorName = actorName,
            actorRole = actorRole,
            actionType = actionType,
            resourceAffected = resourceAffected,
            details = details
        )
        auditLogDao.insertLog(auditLog)
    }

    // --- Appointments ---
    fun getAppointmentsForPatient(patientId: String): Flow<List<Appointment>> =
        appointmentDao.getAppointmentsForPatientFlow(patientId)

    fun getAppointmentsForClinician(clinicianId: String): Flow<List<Appointment>> =
        appointmentDao.getAppointmentsForClinicianFlow(clinicianId)

    fun getAllAppointments(): Flow<List<Appointment>> =
        appointmentDao.getAllAppointmentsFlow()

    suspend fun createAppointment(appointment: Appointment, actor: User) {
        appointmentDao.insertAppointment(appointment)
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "WRITE",
            resourceAffected = "Appointment for Patient ${appointment.patientId}",
            details = "Booked appointment with ${appointment.clinicianName} on ${appointment.dateTimeMillis} - Reason: ${appointment.reason}"
        )
    }

    suspend fun updateAppointmentStatus(appointmentId: Int, status: String, actor: User, patientId: String) {
        appointmentDao.updateAppointmentStatus(appointmentId, status)
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "UPDATE",
            resourceAffected = "Appointment #$appointmentId",
            details = "Changed reservation status to: $status"
        )
    }

    // --- Decrypted EHR Records (High Security) ---
    fun getRecordsForPatientSecured(patientId: String, viewer: User): Flow<List<PatientRecord>> {
        // Log access audit whenever Flow is observed
        return patientRecordDao.getRecordsForPatientFlow(patientId).map { records ->
            // Side effect-like log of clinical record reads as mandated by strict medical audits
            logAudit(
                actorId = viewer.id,
                actorName = viewer.name,
                actorRole = viewer.role,
                actionType = "READ",
                resourceAffected = "Patient Record: $patientId",
                details = "Accessed EHR list for patient. Items retrieved: ${records.size}"
            )
            // Decrypt fields on the fly
            records.map { record ->
                record.copy(
                    encryptedNotes = SecurityManager.decrypt(record.encryptedNotes),
                    encryptedTreatment = SecurityManager.decrypt(record.encryptedTreatment)
                )
            }
        }
    }

    suspend fun addMedicalRecord(
        patientId: String,
        patientName: String,
        author: User,
        type: String,
        summary: String,
        notes: String,
        treatment: String
    ) {
        val encryptedNotes = SecurityManager.encrypt(notes)
        val encryptedTreatment = SecurityManager.encrypt(treatment)
        val record = PatientRecord(
            patientId = patientId,
            patientName = patientName,
            authorId = author.id,
            authorName = author.name,
            recordType = type,
            summary = summary,
            encryptedNotes = encryptedNotes,
            encryptedTreatment = encryptedTreatment
        )
        patientRecordDao.insertRecord(record)
        logAudit(
            actorId = author.id,
            actorName = author.name,
            actorRole = author.role,
            actionType = "WRITE",
            resourceAffected = "Patient Records: $patientId",
            details = "Added secure medical record of type $type: '$summary'"
        )
    }

    // --- Message Exchanges (Encrypted) ---
    fun getMessagesSecured(userId1: String, userId2: String, viewer: User): Flow<List<SecureMessage>> {
        return secureMessageDao.getConversationFlow(userId1, userId2).map { list ->
            // Decrypt message content base on the fly
            list.map { msg ->
                msg.copy(encryptedContent = SecurityManager.decrypt(msg.encryptedContent))
            }
        }
    }

    suspend fun sendSecureMessage(sender: User, receiverId: String, receiverName: String, text: String, fileUri: String? = null) {
        val encryptedText = SecurityManager.encrypt(text)
        val msg = SecureMessage(
            senderId = sender.id,
            senderName = sender.name,
            receiverId = receiverId,
            receiverName = receiverName,
            encryptedContent = encryptedText,
            attachmentUri = fileUri
        )
        secureMessageDao.insertMessage(msg)
        logAudit(
            actorId = sender.id,
            actorName = sender.name,
            actorRole = sender.role,
            actionType = "WRITE",
            resourceAffected = "Messaging Channel",
            details = "Sent an encrypted message to $receiverName"
        )
    }

    // --- Prescriptions ---
    fun getPrescriptionsFlow(patientId: String): Flow<List<Prescription>> =
        prescriptionDao.getPrescriptionsForPatientFlow(patientId)

    fun getAllPrescriptions(): Flow<List<Prescription>> =
        prescriptionDao.getAllPrescriptionsFlow()

    suspend fun issuePrescription(
        patientId: String,
        patientName: String,
        clinician: User,
        medName: String,
        dosage: String,
        freq: String,
        startStr: String,
        endStr: String
    ) {
        val prescription = Prescription(
            patientId = patientId,
            patientName = patientName,
            clinicianId = clinician.id,
            clinicianName = clinician.name,
            medicationName = medName,
            dosage = dosage,
            frequency = freq,
            startDate = startStr,
            endDate = endStr,
            status = "ACTIVE"
        )
        prescriptionDao.insertPrescription(prescription)
        logAudit(
            actorId = clinician.id,
            actorName = clinician.name,
            actorRole = clinician.role,
            actionType = "WRITE",
            resourceAffected = "Prescriptions For Patient $patientId",
            details = "Issued active Rx for $medName ($dosage, $freq)"
        )
    }

    suspend fun requestRenewal(id: Int, renewalsCount: Int, actor: User, medName: String) {
        prescriptionDao.updatePrescriptionStatus(id, "RENEWAL_REQUESTED", renewalsCount + 1)
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "UPDATE",
            resourceAffected = "Prescription #$id",
            details = "Requested medication renewal for '$medName'"
        )
    }

    suspend fun approveRenewal(id: Int, renewalsCount: Int, actor: User, medName: String) {
        prescriptionDao.updatePrescriptionStatus(id, "ACTIVE", renewalsCount)
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "UPDATE",
            resourceAffected = "Prescription #$id",
            details = "Approved renewal request for '$medName'"
        )
    }

    // --- Invoices ---
    fun getInvoicesForPatient(patientId: String): Flow<List<Invoice>> =
        invoiceDao.getInvoicesForPatientFlow(patientId)

    fun getAllInvoices(): Flow<List<Invoice>> =
        invoiceDao.getAllInvoicesFlow()

    suspend fun issueInvoice(patientId: String, patientName: String, items: String, amount: Double, dueMillis: Long, actor: User) {
        val invoice = Invoice(
            patientId = patientId,
            patientName = patientName,
            itemsJson = items,
            totalAmount = amount,
            dueDateMillis = dueMillis,
            status = "UNPAID"
        )
        invoiceDao.insertInvoice(invoice)
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "WRITE",
            resourceAffected = "Invoices For Patient $patientId",
            details = "Created bill of £${String.format("%.2f", amount)} - Description: ($items)"
        )
    }

    suspend fun payInvoice(id: Int, actor: User, amount: Double) {
        invoiceDao.updateInvoiceStatus(id, "PAID")
        logAudit(
            actorId = actor.id,
            actorName = actor.name,
            actorRole = actor.role,
            actionType = "UPDATE",
            resourceAffected = "Invoice #$id",
            details = "Settled invoice balance of £${String.format("%.2f", amount)}"
        )
    }

    // --- Database Seeding ---
    suspend fun seedDatabase() {
        val patients = userDao.getAllPatients()
        if (patients.isNotEmpty()) return

        // 1. Admins
        val admin = User(
            id = "CLI_ADM",
            name = "Sarah Jenkins",
            email = "admin@clinic.com",
            role = "ADMIN",
            dob = "1980-04-12",
            passcode = SecurityManager.hashPasscode("123456"),
            consentTracked = true
        )
        userDao.insertUser(admin)

        // 2. Clinicians
        val doctor1 = User(
            id = "CLI_DOC1",
            name = "Dr. Alexander Thorne, MD",
            email = "thorn@clinic.com",
            role = "CLINICIAN",
            dob = "1975-08-22",
            passcode = SecurityManager.hashPasscode("123456"),
            consentTracked = true
        )
        val doctor2 = User(
            id = "CLI_DOC2",
            name = "Dr. Evelyn Ross, FRCP",
            email = "ross@clinic.com",
            role = "CLINICIAN",
            dob = "1982-11-09",
            passcode = SecurityManager.hashPasscode("123456"),
            consentTracked = true
        )
        userDao.insertUser(doctor1)
        userDao.insertUser(doctor2)

        // 3. Patients
        val patient1 = User(
            id = "PAT_01",
            name = "Thomas Miller",
            email = "tom@ahyx.org", // matches current active email for personalization
            role = "PATIENT",
            dob = "1994-06-15",
            passcode = SecurityManager.hashPasscode("123123"),
            consentTracked = true
        )
        val patient2 = User(
            id = "PAT_02",
            name = "Eleanor Vance",
            email = "eleanor@vance.com",
            role = "PATIENT",
            dob = "1988-02-18",
            passcode = SecurityManager.hashPasscode("123123"),
            consentTracked = true
        )
        userDao.insertUser(patient1)
        userDao.insertUser(patient2)

        // Seed some starter clinical operations
        // A. Appointments
        val apptTime1 = System.currentTimeMillis() + 86400000L * 2 // In 2 days
        appointmentDao.insertAppointment(Appointment(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            clinicianId = "CLI_DOC1",
            clinicianName = "Dr. Alexander Thorne, MD",
            dateTimeMillis = apptTime1,
            reason = "Hypertension Follow-Up",
            status = "CONFIRMED"
        ))

        appointmentDao.insertAppointment(Appointment(
            patientId = "PAT_02",
            patientName = "Eleanor Vance",
            clinicianId = "CLI_DOC2",
            clinicianName = "Dr. Evelyn Ross, FRCP",
            dateTimeMillis = System.currentTimeMillis() + 86400000L * 3,
            reason = "Eczema Treatment Review",
            status = "CONFIRMED"
        ))

        // B. Records (EHR)
        addMedicalRecord(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            author = doctor1,
            type = "VISIT_NOTE",
            summary = "Cardiology follow up",
            notes = "Patient reports mild fatigue. Blood pressure has improved to 128/82 mmHg after initiating Lisinopril. Lungs are clear on auscultation, regular cardiac rhythm.",
            treatment = "Continue Lisinopril 10mg PO QD. Return for checkup in 3 months with complete metabolic blood profile."
        )

        addMedicalRecord(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            author = doctor1,
            type = "DIAGNOSIS",
            summary = "Primary Hypertension",
            notes = "Initial screening showed sustained BP of 145/95. Reviewed potassium diet alternatives.",
            treatment = "Initiated ACE-inhibitor 10mg once daily."
        )

        // C. Prescriptions
        prescriptionDao.insertPrescription(Prescription(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            clinicianId = "CLI_DOC1",
            clinicianName = "Dr. Alexander Thorne, MD",
            medicationName = "Lisinopril",
            dosage = "10mg",
            frequency = "Once daily in the morning",
            startDate = "2026-05-10",
            endDate = "2026-08-10",
            status = "ACTIVE"
        ))

        // D. Messages
        val sysActor = User("SYSTEM", "System Auto", "system-main", "SYSTEM", "N/A", "N/A")
        sendSecureMessage(doctor1, "PAT_01", "Thomas Miller", "Hello Thomas, please ensure you complete your blood test panel at least 3 days prior to our next meeting so I have the lab results ready.")
        sendSecureMessage(patient1, "CLI_DOC1", "Dr. Alexander Thorne, MD", "Understood, Dr. Thorne. I've booked the lab slot for next Monday morning.")

        // E. Invoices
        invoiceDao.insertInvoice(Invoice(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            dueDateMillis = System.currentTimeMillis() + 86400000L * 10,
            itemsJson = "Cardiology review consult: £120.00; Blood chemistry panel: £55.00",
            totalAmount = 175.00,
            status = "UNPAID"
        ))

        invoiceDao.insertInvoice(Invoice(
            patientId = "PAT_01",
            patientName = "Thomas Miller",
            dueDateMillis = System.currentTimeMillis() - 86400000L,
            itemsJson = "Electrocardiography service: £95.00",
            totalAmount = 95.00,
            status = "PAID"
        ))
    }
}
