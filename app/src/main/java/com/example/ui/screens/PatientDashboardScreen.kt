package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.ClinicViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val appointments by viewModel.loggedInUserAppointments.collectAsState()
    val prescriptions by viewModel.loggedInUserPrescriptions.collectAsState()
    val medicalRecords by viewModel.currentPatientEHRRecords.collectAsState()
    val messages by viewModel.secureConversationMessages.collectAsState()
    val invoices by viewModel.loggedInUserInvoices.collectAsState()
    val clinicians by viewModel.clinicians.collectAsState()

    // Dashboard navigation tab status
    var activeSubTab by remember { mutableStateOf("SUMMARY") } // SUMMARY, RECORDS, BOOKING, MESSAGES, PRESCRIPTIONS, BILLS

    val cardBg = Color.White
    val accentEmerald = Color(0xFF2563EB) // Professional blue clinical accent
    val borderCol = Color(0xFFE2E8F0) // Clean slate-200 border

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {
        // Patient Header with active name & quick consent check
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RectangleShape,
            border = BorderStroke(1.dp, borderCol),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .safeDrawingPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Patient Portal",
                            color = accentEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentUser?.name ?: "Thomas Miller",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "DOB: ${currentUser?.dob ?: "1994-06-15"}",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    // Security lock system trigger
                    IconButton(
                        onClick = { viewModel.lockSession() },
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(100))
                            .testTag("lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Clinical Session",
                            tint = Color(0xFFD97706)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GDPR Consent status banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentUser?.consentTracked == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                        .border(1.dp, if (currentUser?.consentTracked == true) Color(0xFFBBF7D0) else Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (currentUser?.consentTracked == true) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                            contentDescription = "Consent icon",
                            tint = if (currentUser?.consentTracked == true) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = if (currentUser?.consentTracked == true) "GDPR Clinical Consent: GRANTED" else "GDPR Private Consent: REVOKED",
                                color = if (currentUser?.consentTracked == true) Color(0xFF15803D) else Color(0xFF991B1B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (currentUser?.consentTracked == true) "Private research tracking enabled" else "Medical records restricted from shared statistics",
                                color = Color(0xFF475569),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentUser?.consentTracked == true) viewModel.revokeConsent() else viewModel.grantConsent()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentUser?.consentTracked == true) Color(0xFFDC2626) else Color(0xFF16A34A)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (currentUser?.consentTracked == true) "Revoke" else "Grant",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sub Navigation Horizontal Scroll Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, borderCol))
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "SUMMARY" to "Home",
                "RECORDS" to "My Records",
                "BOOKING" to "Book Consult",
                "MESSAGES" to "Messages",
                "PRESCRIPTIONS" to "Prescriptions",
                "BILLS" to "Billing"
            )

            tabs.forEach { (key, title) ->
                val active = activeSubTab == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                        .border(
                            1.dp,
                            if (active) Color(0xFFDBEAFE) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { activeSubTab = key }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = if (active) Color(0xFF2563EB) else Color(0xFF64748B),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Dynamic Screen Sub-views
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                "SUMMARY" -> PatientHomeTab(
                    appointments = appointments,
                    prescriptions = prescriptions,
                    medicalRecords = medicalRecords,
                    invoices = invoices,
                    onNavigate = { activeSubTab = it },
                    viewModel = viewModel
                )
                "RECORDS" -> PatientRecordsTab(records = medicalRecords)
                "BOOKING" -> BookingTab(clinicians = clinicians, onBook = { clinId, clinName, time, reason ->
                    viewModel.bookAppointment(clinId, clinName, time, reason)
                }, appointments = appointments, onCancel = { id -> viewModel.cancelAppointment(id, currentUser?.id ?: "") })
                "MESSAGES" -> MessagesTab(messages = messages, clinicians = clinicians, viewModel = viewModel)
                "PRESCRIPTIONS" -> PrescriptionsTab(prescriptions = prescriptions, onRenewal = { rx ->
                    viewModel.requestRxRenewal(rx)
                })
                "BILLS" -> BillingTab(invoices = invoices, onPay = { id, amount ->
                    viewModel.payInvoiceBill(id, amount)
                })
            }
        }
    }
}

@Composable
fun PatientHomeTab(
    appointments: List<Appointment>,
    prescriptions: List<Prescription>,
    medicalRecords: List<PatientRecord>,
    invoices: List<Invoice>,
    onNavigate: (String) -> Unit,
    viewModel: ClinicViewModel
) {
    val borderCol = Color(0xFFE2E8F0)
    val cardBg = Color.White

    val scrollState = rememberScrollState()
    val activePrescriptions = prescriptions.filter { it.status == "ACTIVE" || it.status == "RENEWAL_REQUESTED" }
    val unpaidInvoices = invoices.filter { it.status == "UNPAID" }
    val formatter = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card & metrics
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, borderCol),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Welcome Back to ClinOps Private Healthcare",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Your electronic medical records are fully encrypted under AES-256 standards locally. Only authorized clinicians can view clinical documentation and updates.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Active Rx", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("${activePrescriptions.size}", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Records Filed", color = Color(0xFF475569), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("${medicalRecords.size}", color = Color(0xFF0F172A), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (unpaidInvoices.isNotEmpty()) Color(0xFFFEF2F2) else Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .border(1.dp, if (unpaidInvoices.isNotEmpty()) Color(0xFFFEE2E2) else Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Unpaid Bills", color = if (unpaidInvoices.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(
                                text = "£${unpaidInvoices.sumOf { it.totalAmount }.toInt()}",
                                color = if (unpaidInvoices.isNotEmpty()) Color(0xFF991B1B) else Color(0xFF15803D),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Upcoming consultations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Upcoming Consultations", color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(
                text = "Book New",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigate("BOOKING") }
            )
        }

        val primaryAppointments = appointments.filter { it.status == "CONFIRMED" || it.status == "PENDING" }
        if (primaryAppointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.EventNote, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                    Text("No upcoming consultations booked", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }
        } else {
            primaryAppointments.take(2).forEach { appt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderCol),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(appt.clinicianName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(formatter.format(Date(appt.dateTimeMillis)), color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("Reason: ${appt.reason}", color = Color(0xFF475569), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(appt.status, color = Color(0xFF2563EB), fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Active Prescriptions Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Medication", color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(
                text = "View All",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigate("PRESCRIPTIONS") }
            )
        }

        if (activePrescriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active digital prescriptions", color = Color(0xFF64748B), fontSize = 13.sp)
            }
        } else {
            activePrescriptions.take(2).forEach { rx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderCol),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Bloodtype, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Text(rx.medicationName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("Dosage: ${rx.dosage} (${rx.frequency})", color = Color(0xFF475569), fontSize = 12.sp)
                            Text("Approved by Dr. ${rx.clinicianName.substringAfter("Dr. ")}", color = Color(0xFF64748B), fontSize = 10.sp)
                        }

                        if (rx.status == "RENEWAL_REQUESTED") {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Renewal Requested", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.requestRxRenewal(rx) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Quick Request Renewal", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Medical records timeline view
@Composable
fun PatientRecordsTab(records: List<PatientRecord>) {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(64.dp))
                Text("Your Electronic Health Records File is empty", color = Color(0xFF64748B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Visits, diagnoses logs, and doctor prescriptions will appear securely here.", color = Color(0xFF475569), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Medical Timeline (EHR Secured)",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(records) { record ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Timeline side element
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (record.recordType == "DIAGNOSIS") Color(0xFFFEE2E2) else Color(0xFFEFF6FF), CircleShape)
                                .border(1.dp, if (record.recordType == "DIAGNOSIS") Color(0xFFFCA5A5) else Color(0xFFDBEAFE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (record.recordType == "DIAGNOSIS") Icons.Default.ReportProblem else Icons.Default.Notes,
                                contentDescription = null,
                                tint = if (record.recordType == "DIAGNOSIS") Color(0xFFDC2626) else Color(0xFF2563EB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        // Simple connecting bar line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(120.dp)
                                .background(Color(0xFFE2E8F0))
                        )
                    }

                    // Record Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ehr_record_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.recordType,
                                    fontSize = 10.sp,
                                    color = if (record.recordType == "DIAGNOSIS") Color(0xFFDC2626) else Color(0xFF2563EB),
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = formatter.format(Date(record.timestamp)),
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Text(
                                text = record.summary,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // AES note details
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("CLINICAL NOTE (DECRYPTED):", color = Color(0xFF2563EB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(record.encryptedNotes, color = Color(0xFF334155), fontSize = 12.sp)
                                    
                                    if (record.encryptedTreatment.isNotEmpty()) {
                                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                        Text("SECURE TREATMENT PLAN:", color = Color(0xFFD97706), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(record.encryptedTreatment, color = Color(0xFF334155), fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Logged by ${record.authorName} • Integrity Hash Checked",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// Appointment reservation booking tab
@Composable
fun BookingTab(
    clinicians: List<User>,
    appointments: List<Appointment>,
    onBook: (String, String, Long, String) -> Unit,
    onCancel: (Int) -> Unit
) {
    var selectedClinician by remember { mutableStateOf<User?>(null) }
    var reason by remember { mutableStateOf("") }
    var bookedSuccessMessage by remember { mutableStateOf(false) }

    val activeAppts = appointments.filter { it.status != "CANCELLED" }
    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (bookedSuccessMessage) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                    Column {
                        Text("Appointment Registered!", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("We have generated a secure token. Review status details below.", color = Color(0xFF14532D), fontSize = 12.sp)
                    }
                }
            }
        }

        Text("Book Consultation Schedule", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), fontSize = 16.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Practitioner Specialist", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                clinicians.forEach { clinician ->
                    val isSelected = selectedClinician?.id == clinician.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { selectedClinician = clinician }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFE2E8F0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B))
                            }
                            Column {
                                Text(clinician.name, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Private Health Provider • Registered NHS FRCP", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedClinician = clinician },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB), unselectedColor = Color(0xFF64748B))
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for consultation", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val clin = selectedClinician
                        if (clin != null && reason.isNotBlank()) {
                            // Set consultation for tomorrow millisecond level
                            onBook(clin.id, clin.name, System.currentTimeMillis() + 86400000L, reason)
                            reason = ""
                            selectedClinician = null
                            bookedSuccessMessage = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), disabledContainerColor = Color(0xFF94A3B8)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (selectedClinician != null && reason.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Secure Schedule Slot", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Active appointments booked
        Text("Your Pending Schedule Workload", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), fontSize = 15.sp)

        if (activeAppts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No reservations registered", color = Color(0xFF64748B), fontSize = 13.sp)
            }
        } else {
            activeAppts.forEach { appt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(appt.clinicianName, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(appt.status, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        Text("Scheduled: ${formatter.format(Date(appt.dateTimeMillis))}", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text("Symptom details: ${appt.reason}", color = Color(0xFF475569), fontSize = 12.sp)

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onCancel(appt.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Slot Request", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
// Secure Messenging Hub
@Composable
fun MessagesTab(
    messages: List<SecureMessage>,
    clinicians: List<User>,
    viewModel: ClinicViewModel
) {
    var txtMessage by remember { mutableStateOf("") }
    val clinicianSelected by viewModel.selectedClinicianId.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // auto scroll logic
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F9FC))) {
        // Chat top clinician selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recipient:", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            clinicians.forEach { clinician ->
                val isSelected = clinicianSelected == clinician.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                        .clickable { viewModel.selectClinician(clinician.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = clinician.name.substringBefore(","),
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Messages List Scroll
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF7F9FC))
                .padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No history. Begin a secure communication chat session below.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        val isMe = message.senderId == "PAT_01" // Thomas Miller context
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .wrapContentWidth(align = if (isMe) Alignment.End else Alignment.Start)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 12.dp
                                        )
                                    )
                                    .background(if (isMe) Color(0xFF2563EB) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isMe) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 12.dp
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    if (!isMe) {
                                        Text(
                                            text = message.senderName,
                                            color = Color(0xFF2563EB),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = message.encryptedContent, // Fully decrypted already by the ViewModel Flow mapping
                                        color = if (isMe) Color.White else Color(0xFF0F172A),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Text input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = txtMessage,
                onValueChange = { txtMessage = it },
                placeholder = { Text("Write encrypted message...", color = Color(0xFF64748B), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input")
            )

            // Submit msg action button with test tag
            IconButton(
                onClick = {
                    if (txtMessage.isNotBlank()) {
                        viewModel.sendSecureMsg(txtMessage)
                        txtMessage = ""
                    }
                },
                enabled = txtMessage.isNotBlank(),
                modifier = Modifier
                    .background(Color(0xFF2563EB), CircleShape)
                    .testTag("send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Send secure encrypted packet",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Digital Prescriptions list tab
@Composable
fun PrescriptionsTab(
    prescriptions: List<Prescription>,
    onRenewal: (Prescription) -> Unit
) {
    if (prescriptions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No medical prescriptions recorded", color = Color(0xFF64748B), fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Digital Prescriptions (Rx Dossiers)", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), fontSize = 16.sp)
            }

            items(prescriptions) { rx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFEE2E2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Vaccines, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(rx.medicationName, color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text("Dosage: ${rx.dosage} • ${rx.frequency}", color = Color(0xFF475569), fontSize = 12.sp)
                                }
                            }

                            // Dynamic status pills
                            val isAct = rx.status == "ACTIVE"
                            val statusColor = if (isAct) Color(0xFF16A34A) else Color(0xFFD97706)
                            Box(
                                modifier = Modifier
                                    .background(if (isAct) Color(0xFFDCFCE7) else Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isAct) Color(0xFFBBF7D0) else Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(rx.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("VALID: ${rx.startDate} to ${rx.endDate}", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("Authorizing Doctor: Dr. ${rx.clinicianName.substringAfter("Dr. ")}", color = Color(0xFF334155), fontSize = 11.sp)
                            }

                            // Request renewal action
                            if (rx.status == "ACTIVE") {
                                Button(
                                    onClick = { onRenewal(rx) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Request Renewal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Text(
                                    text = "Requested (${rx.renewalsRequested}x)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Invoicing and Billing tab
@Composable
fun BillingTab(
    invoices: List<Invoice>,
    onPay: (Int, Double) -> Unit
) {
    if (invoices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No statement invoices recorded", color = Color(0xFF64748B), fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Billing Statements & Invoices", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), fontSize = 16.sp)
            }

            items(invoices) { invoice ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Invoice #${invoice.id}", color = Color(0xFF64748B), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("£${String.format("%.2f", invoice.totalAmount)}", color = Color(0xFF0F172A), fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }

                            // Paid/Unpaid pill tags
                            val isPaid = invoice.status == "PAID"
                            Box(
                                modifier = Modifier
                                    .background(if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), RoundedCornerShape(100))
                                    .border(1.dp, if (isPaid) Color(0xFFBBF7D0) else Color(0xFFFCA5A5), RoundedCornerShape(100))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = invoice.status,
                                    color = if (isPaid) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        // Breakdown of care services
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ITEMIZED CLINICAL CHARGES:", color = Color(0xFF2563EB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            invoice.itemsJson.split(";").forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.substringBefore(":").trim(), color = Color(0xFF475569), fontSize = 12.sp)
                                    Text(item.substringAfter(":").trim(), color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                        // Due details & Payment triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Due On: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(invoice.dueDateMillis))}",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )

                            if (invoice.status == "UNPAID") {
                                Button(
                                    onClick = { onPay(invoice.id, invoice.totalAmount) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("Pay Balance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
