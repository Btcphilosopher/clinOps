package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE role = 'PATIENT' ORDER BY name ASC")
    fun getAllPatientsFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'PATIENT' ORDER BY name ASC")
    suspend fun getAllPatients(): List<User>

    @Query("SELECT * FROM users WHERE role = 'CLINICIAN' ORDER BY name ASC")
    fun getAllCliniciansFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY dateTimeMillis ASC")
    fun getAppointmentsForPatientFlow(patientId: String): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE clinicianId = :clinicianId ORDER BY dateTimeMillis ASC")
    fun getAppointmentsForClinicianFlow(clinicianId: String): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments ORDER BY dateTimeMillis ASC")
    fun getAllAppointmentsFlow(): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Query("UPDATE appointments SET status = :status WHERE id = :appointmentId")
    suspend fun updateAppointmentStatus(appointmentId: Int, status: String)
}

@Dao
interface PatientRecordDao {
    @Query("SELECT * FROM patient_records WHERE patientId = :patientId AND isActive = 1 ORDER BY timestamp DESC")
    fun getRecordsForPatientFlow(patientId: String): Flow<List<PatientRecord>>

    @Query("SELECT * FROM patient_records WHERE id = :recordId LIMIT 1")
    suspend fun getRecordById(recordId: Int): PatientRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PatientRecord)
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions WHERE patientId = :patientId ORDER BY id DESC")
    fun getPrescriptionsForPatientFlow(patientId: String): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions ORDER BY id DESC")
    fun getAllPrescriptionsFlow(): Flow<List<Prescription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription)

    @Query("UPDATE prescriptions SET status = :status, renewalsRequested = :renewalsCount WHERE id = :id")
    suspend fun updatePrescriptionStatus(id: Int, status: String, renewalsCount: Int)
}

@Dao
interface SecureMessageDao {
    @Query("""
        SELECT * FROM secure_messages 
        WHERE (senderId = :userId1 AND receiverId = :userId2) 
           OR (senderId = :userId2 AND receiverId = :userId1) 
        ORDER BY timestamp ASC
    """)
    fun getConversationFlow(userId1: String, userId2: String): Flow<List<SecureMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SecureMessage)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE patientId = :patientId ORDER BY dueDateMillis ASC")
    fun getInvoicesForPatientFlow(patientId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY dueDateMillis ASC")
    fun getAllInvoicesFlow(): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Query("UPDATE invoices SET status = :status WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: Int, status: String)
}
