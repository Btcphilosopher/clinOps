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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.ClinicViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicianDashboardScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val appointments by viewModel.loggedInUserAppointments.collectAsState()
    val prescriptions by viewModel.loggedInUserPrescriptions.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()

    val currentPatient = patients.firstOrNull { it.id == selectedPatientId }

    var activeSubTab by remember { mutableStateOf("QUEUE") } // QUEUE, EHR_WRITE, RX_WRITE, MESSAGES, RENEW_APPROVE

    val accentEmerald = Color(0xFF26A69A)
    val cardBg = Color(0xFF162226)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F181B))
    ) {
        // Clinician Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132225)),
            shape = RectangleShape,
            border = BorderStroke(1.dp, Color(0xFF22363B)),
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
                            text = "CLINICAL WORKSPACE",
                            color = accentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = currentUser?.name ?: "Dr. Alexander Thorne, MD",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Licensed Practitioner • NHS UK Profile Verified",
                            color = Color(0xFF7C8D93),
                            fontSize = 11.sp
                        )
                    }

                    // Security lock badge
                    IconButton(
                        onClick = { viewModel.lockSession() },
                        modifier = Modifier
                            .background(Color(0xFF1E282B), RoundedCornerShape(100))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock workspace session",
                            tint = Color(0xFFFFB74D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Patient Focus Selector Panel
                Text("PATIENT DOSSIER FILE UNDER REVIEW:", color = Color(0xFF7C8D93), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    patients.forEach { patient ->
                        val isSel = patient.id == selectedPatientId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF213A3E) else Color(0xFF0F181B))
                                .border(1.dp, if (isSel) accentEmerald else Color(0xFF22363B), RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectPatient(patient.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) Color(0xFF00E676) else Color(0xFF7C8D93))
                                )
                                Text(
                                    text = patient.name,
                                    color = if (isSel) Color.White else Color(0xFF7C8D93),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtabs for medical services
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121B1E))
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "QUEUE" to "Consult Queue",
                "EHR_WRITE" to "Record Note",
                "RX_WRITE" to "Issue Rx",
                "MESSAGES" to "Secure Inbox",
                "RENEW_APPROVE" to "Rx Renewals"
            )

            tabs.forEach { (key, title) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeSubTab == key) accentEmerald else Color(0xFF1C272A))
                        .clickable { activeSubTab = key }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = if (activeSubTab == key) Color.White else Color(0xFFB0BEC5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Displays
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                "QUEUE" -> ClinicianQueueTab(appointments = appointments, onComplete = { id ->
                    viewModel.cancelAppointment(id, "COMPLETED")
                })
                "EHR_WRITE" -> EHRWriteTab(
                    selectedPatient = currentPatient,
                    onWrite = { type, summary, note, treatment ->
                        val patient = currentPatient
                        if (patient != null) {
                            viewModel.addDiagnosisOrNote(patient.id, patient.name, type, summary, note, treatment)
                        }
                    },
                    viewModel = viewModel
                )
                "RX_WRITE" -> RxWriteTab(selectedPatient = currentPatient, onIssue = { med, dose, freq, start, end ->
                    val p = currentPatient
                    if (p != null) {
                        viewModel.writePrescription(p.id, p.name, med, dose, freq, start, end)
                    }
                })
                "MESSAGES" -> SecureClinicianChatTab(viewModel = viewModel, selectedPatientName = currentPatient?.name ?: "Thomas Miller")
                "RENEW_APPROVE" -> ClinicianRxRenewalsTab(prescriptions = prescriptions, onApprove = { rx ->
                    viewModel.approveRxRenewal(rx)
                })
            }
        }
    }
}

@Composable
fun ClinicianQueueTab(
    appointments: List<Appointment>,
    onComplete: (Int) -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val currentActiveList = appointments.filter { it.status == "CONFIRMED" || it.status == "PENDING" }

    if (currentActiveList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF334B52), modifier = Modifier.size(48.dp))
                Text("No pending consultation queue sessions", color = Color(0xFF7C8D93), fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Today's Patient Schedule workload", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            items(currentActiveList) { appt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
                    border = BorderStroke(1.dp, Color(0xFF23353B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(appt.patientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Patient ID: ${appt.patientId}", color = Color(0xFF7C8D93), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF26A69A).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(appt.status, color = Color(0xFF26A69A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Text("Scheduled time: ${formatter.format(Date(appt.dateTimeMillis))}", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        Text("Reason / Chief Complaint: ${appt.reason}", color = Color(0xFFCFD8DC), fontSize = 12.sp)

                        Divider(color = Color(0xFF23353B), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onComplete(appt.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Encounter Completed", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EHRWriteTab(
    selectedPatient: User?,
    onWrite: (String, String, String, String) -> Unit,
    viewModel: ClinicViewModel
) {
    var recordType by remember { mutableStateOf("VISIT_NOTE") }
    var summary by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var feedbackSaved by remember { mutableStateOf(false) }

    val recentRecords by viewModel.currentPatientEHRRecords.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selectedPatient == null) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Please select a patient file from the top list", color = Color(0xFF7C8D93), fontSize = 13.sp)
            }
            return
        }

        if (feedbackSaved) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Record written, encrypted, & audit log generated successfully!",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Text("Write Sensitive Health Record for: ${selectedPatient.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
            border = BorderStroke(1.dp, Color(0xFF23353B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Record Type selection
                Text("Select Record Category", color = Color(0xFF7C8D93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val types = listOf("VISIT_NOTE", "DIAGNOSIS", "LAB_RESULT", "VITAL_SIGNS")
                    types.forEach { type ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (recordType == type) Color(0xFF26A69A) else Color(0xFF0F1E24))
                                .clickable { recordType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.replace("_", " "),
                                color = if (recordType == type) Color.White else Color(0xFFB0BEC5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Clinical Summary Encounter Header (unencrypted)", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                    placeholder = { Text("e.g. Cardiorespiratory checkup", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF26A69A),
                        unfocusedBorderColor = Color(0xFF23353B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Confidential Practitioner Notes (Symmetric AES Rest Encrypted)", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                    placeholder = { Text("Enter private diagnostics details here...", fontSize = 12.sp) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF26A69A),
                        unfocusedBorderColor = Color(0xFF23353B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = treatment,
                    onValueChange = { treatment = it },
                    label = { Text("Prescribed Treatment Plan (AES Encrypted)", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                    placeholder = { Text("e.g. Continue medicine Lisinopril 10mgQD...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF26A69A),
                        unfocusedBorderColor = Color(0xFF23353B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (summary.isNotBlank() && notes.isNotBlank()) {
                            onWrite(recordType, summary, notes, treatment)
                            summary = ""
                            notes = ""
                            treatment = ""
                            feedbackSaved = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = (summary.isNotBlank() && notes.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seal & Encrypt EHR Record File", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Display recent clinical logs for context
        Text("Recent Encrypted Encounters File Records for Patient", color = Color(0xFF7C8D93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        recentRecords.forEach { record ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121B1E)),
                border = BorderStroke(1.dp, Color(0xFF23353B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(record.recordType, color = Color(0xFF26A69A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.timestamp)), color = Color(0xFF5A6C72), fontSize = 10.sp)
                    }
                    Text(record.summary, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Decrypted Diagnostics Note: ${record.encryptedNotes}", color = Color(0xFFCFD8DC), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun RxWriteTab(
    selectedPatient: User?,
    onIssue: (String, String, String, String, String) -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("2026-05-29") }
    var end by remember { mutableStateOf("2026-08-29") }
    var feedbackSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selectedPatient == null) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("Please select a patient file from the top list", color = Color(0xFF7C8D93), fontSize = 13.sp)
            }
            return
        }

        if (feedbackSaved) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Prescription issued securely!",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Text("Issue Digital Prescription to: ${selectedPatient.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
            border = BorderStroke(1.dp, Color(0xFF23353B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Medication Generic Name", color = Color(0xFF7C8D93)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF26A69A),
                        unfocusedBorderColor = Color(0xFF23353B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage (e.g. 10mg)", color = Color(0xFF7C8D93)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26A69A),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency", color = Color(0xFF7C8D93)) },
                        placeholder = { Text("Once daily") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26A69A),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Date", color = Color(0xFF7C8D93)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26A69A),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Date", color = Color(0xFF7C8D93)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF26A69A),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        if (medName.isNotBlank() && dosage.isNotBlank()) {
                            onIssue(medName, dosage, frequency, start, end)
                            medName = ""
                            dosage = ""
                            frequency = ""
                            feedbackSaved = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = (medName.isNotBlank() && dosage.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seal & Sign Digital Rx", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SecureClinicianChatTab(
    viewModel: ClinicViewModel,
    selectedPatientName: String
) {
    var chatText by remember { mutableStateOf("") }
    val messages by viewModel.secureConversationMessages.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121B1E))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Secure Cryptographic Channel: Thomas Miller (PATIENT)",
                color = Color(0xFF26A69A),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F181B))
                .padding(horizontal = 16.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversation history found.", color = Color(0xFF5A6C72), fontSize = 13.sp)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        val isMe = message.senderId != "PAT_01" // Anyone that's not Thomas Miller in this view is the doctor
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
                                    .background(if (isMe) Color(0xFF26A69A) else Color(0xFF1E2F34))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    if (!isMe) {
                                        Text(
                                            text = message.senderName,
                                            color = Color(0xFF81C784),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = message.encryptedContent,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat text entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121B1E))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Write encrypted clinical message...", color = Color(0xFF5A6C72), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF26A69A),
                    unfocusedBorderColor = Color(0xFF23353B)
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("clinician_chat_input")
            )

            IconButton(
                onClick = {
                    if (chatText.isNotBlank()) {
                        viewModel.sendSecureMsg(chatText)
                        chatText = ""
                    }
                },
                enabled = chatText.isNotBlank(),
                modifier = Modifier
                    .background(Color(0xFF26A69A), CircleShape)
                    .testTag("clinician_send_btn")
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

@Composable
fun ClinicianRxRenewalsTab(
    prescriptions: List<Prescription>,
    onApprove: (Prescription) -> Unit
) {
    val pendingRx = prescriptions.filter { it.status == "RENEWAL_REQUESTED" }

    if (pendingRx.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = Color(0xFF334B52), modifier = Modifier.size(48.dp))
                Text("No pending prescription renewal requests", color = Color(0xFF7C8D93), fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Review Prescription renewal requests", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            items(pendingRx) { rx ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
                    border = BorderStroke(1.dp, Color(0xFF23353B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Patient: ${rx.patientName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Medication: ${rx.medicationName} (${rx.dosage})", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFB74D).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("RENEWAL PENDING", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }

                        Text("Usage Details: ${rx.frequency}. Previous Period: ${rx.startDate} to ${rx.endDate}", color = Color(0xFFB0BEC5), fontSize = 12.sp)

                        Divider(color = Color(0xFF23353B), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onApprove(rx) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Approve & Digitally Sign Rx", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
