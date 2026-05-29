package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // e.g. "PAT_123", "CLI_456"
    val name: String,
    val email: String,
    val role: String, // "PATIENT", "CLINICIAN", "ADMIN"
    val dob: String,
    val passcode: String, // 6-digit numeric secure passcode/PIN
    val consentTracked: Boolean = true,
    val consentDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val clinicianId: String,
    val clinicianName: String,
    val dateTimeMillis: Long,
    val reason: String,
    val status: String, // "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"
    val notes: String = ""
)

@Entity(tableName = "patient_records")
data class PatientRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val authorId: String,
    val authorName: String,
    val recordType: String, // "DIAGNOSIS", "LAB_RESULT", "VITAL_SIGNS", "VISIT_NOTE"
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String, // High-level unencrypted summary (e.g. "Annual checkup")
    val encryptedNotes: String, // AES-encrypted clinical visit text
    val encryptedTreatment: String, // AES-encrypted treatment details
    val version: Int = 1,
    val isActive: Boolean = true
)

@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val clinicianId: String,
    val clinicianName: String,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val startDate: String,
    val endDate: String,
    val status: String, // "ACTIVE", "COMPLETED", "RENEWAL_REQUESTED"
    val renewalsRequested: Int = 0
)

@Entity(tableName = "secure_messages")
data class SecureMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val receiverName: String,
    val encryptedContent: String, // AES-encrypted message text
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentUri: String? = null,
    val isDelivered: Boolean = true
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val actionType: String, // "READ", "WRITE", "DELETE", "AUTHENTICATION", "ACCESS"
    val resourceAffected: String, // e.g. "Patient Record PAT_123"
    val details: String // Description of what happened
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val issueDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long,
    val itemsJson: String, // Semicolon-separated of items and costs: e.g. "General consult: £75; Blood analysis: £50"
    val totalAmount: Double,
    val status: String, // "PAID", "UNPAID", "OVERDUE"
    val insuranceCode: String? = null
)
