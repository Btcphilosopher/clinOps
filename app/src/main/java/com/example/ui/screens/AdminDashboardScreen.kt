package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun AdminDashboardScreen(
    viewModel: ClinicViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val appointments by viewModel.loggedInUserAppointments.collectAsState() // Admin can see all
    val invoices by viewModel.loggedInUserInvoices.collectAsState() // Admin can see all

    var activeSubTab by remember { mutableStateOf("AUDITING") } // AUDITING, BILLING_ISSUER, SCHEDULE_OVERSEER

    val accentEmerald = Color(0xFF26A69A)
    val cardBg = Color(0xFF162226)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F181B))
    ) {
        // Admin Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C25)),
            shape = RectangleShape,
            border = BorderStroke(1.dp, Color(0xFF2B2335)),
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
                            text = "SECURITY ADMIN INTERFACE",
                            color = Color(0xFFAB47BC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = currentUser?.name ?: "Sarah Jenkins",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Clinic Compliance Officer • Full Access Authority",
                            color = Color(0xFF7C8D93),
                            fontSize = 11.sp
                        )
                    }

                    // Security lock badge
                    IconButton(
                        onClick = { viewModel.lockSession() },
                        modifier = Modifier
                            .background(Color(0xFF2A202E), RoundedCornerShape(100))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock administrative session",
                            tint = Color(0xFFFFB74D)
                        )
                    }
                }
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15121E))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "AUDITING" to "Access Audits",
                "BILLING_ISSUER" to "Issue Bills",
                "SCHEDULE_OVERSEER" to "Schedules"
            )

            tabs.forEach { (key, title) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeSubTab == key) Color(0xFF8E24AA) else Color(0xFF221A2B))
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

        // Subsections
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSubTab) {
                "AUDITING" -> SecurityAuditingTab(logs = auditLogs)
                "BILLING_ISSUER" -> InvoicingCenterTab(patients = patients, invoices = invoices, onCreateBill = { pId, pName, items, amount, due ->
                    viewModel.createInvoice(pId, pName, items, amount, due)
                })
                "SCHEDULE_OVERSEER" -> ScheduleOverseerTab(appointments = appointments, onCancel = { id, pId ->
                    viewModel.cancelAppointment(id, pId)
                })
            }
        }
    }
}

@Composable
fun SecurityAuditingTab(logs: List<AuditLog>) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("System Operations Audit Trails", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("GDPR/HIPAA Mandatory Ledger Activity Log", color = Color(0xFF7C8D93), fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF1B5E20), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("SECURE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No audited security entries recorded.", color = Color(0xFF7C8D93), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D11), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E1D25), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14131A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1F1D27), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (log.actionType) {
                                                "READ" -> Color(0xFF4FC3F7)
                                                "WRITE" -> Color(0xFF81C784)
                                                "AUTHENTICATION" -> Color(0xFFFFB74D)
                                                "CONSENT_UPDATE" -> Color(0xFFBA68C8)
                                                else -> Color(0xFFFF8A65)
                                            }
                                        )
                                )
                                Text(
                                    text = log.actionType,
                                    color = when (log.actionType) {
                                        "READ" -> Color(0xFF4FC3F7)
                                        "WRITE" -> Color(0xFF81C784)
                                        "AUTHENTICATION" -> Color(0xFFFFB74D)
                                        "CONSENT_UPDATE" -> Color(0xFFBA68C8)
                                        else -> Color(0xFFFF8A65)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "${dateFormatter.format(Date(log.timestamp))} ${timeFormatter.format(Date(log.timestamp))}",
                                color = Color(0xFF5A5C64),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.details,
                            color = Color(0xFFCFD8DC),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Actor: ${log.actorName} (${log.actorRole}) • Target: ${log.resourceAffected}",
                            color = Color(0xFF7C8D93),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoicingCenterTab(
    patients: List<User>,
    invoices: List<Invoice>,
    onCreateBill: (String, String, String, Double, Int) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<User?>(null) }
    var billText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDaysAheadText by remember { mutableStateOf("14") }
    var isBillFeedbackTrue by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isBillFeedbackTrue) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Invoice compiled on ledgers securely!",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Text("Billing Operations: Draft Invoices", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
            border = BorderStroke(1.dp, Color(0xFF23353B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Billing Patient", color = Color(0xFF7C8D93), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    patients.forEach { patient ->
                        val isSel = patient.id == selectedPatient?.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF8E24AA).copy(alpha = 0.2f) else Color(0xFF0F1E24))
                                .border(1.dp, if (isSel) Color(0xFF8E24AA) else Color(0xFF23353B), RoundedCornerShape(8.dp))
                                .clickable { selectedPatient = patient }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(patient.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = billText,
                    onValueChange = { billText = it },
                    label = { Text("Itemized details Breakdown (semi-colon separated)", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                    placeholder = { Text("Consultation: £80; Blood diagnostics: £45", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8E24AA),
                        unfocusedBorderColor = Color(0xFF23353B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Total Bill (GBP)", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8E24AA),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1.5f)
                    )

                    OutlinedTextField(
                        value = dueDaysAheadText,
                        onValueChange = { dueDaysAheadText = it },
                        label = { Text("Days to pay", color = Color(0xFF7C8D93), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF8E24AA),
                            unfocusedBorderColor = Color(0xFF23353B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val patient = selectedPatient
                        val amt = amountText.toDoubleOrNull()
                        val due = dueDaysAheadText.toIntOrNull() ?: 14
                        if (patient != null && amt != null && billText.isNotBlank()) {
                            onCreateBill(patient.id, patient.name, billText, amt, due)
                            billText = ""
                            amountText = ""
                            selectedPatient = null
                            isBillFeedbackTrue = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = (selectedPatient != null && amountText.isNotBlank() && billText.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Issue Signed Invoicing Ledger Bill", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Text("All Care Facility Active Invoices", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        invoices.forEach { invoice ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14131C)),
                border = BorderStroke(1.dp, Color(0xFF1F1C25)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(invoice.patientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Invoice ID: #${invoice.id} • ${invoice.itemsJson.take(30)}...", color = Color(0xFF7C8D93), fontSize = 11.sp, maxLines = 1)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("£${String.format("%.2f", invoice.totalAmount)}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(invoice.status, color = if (invoice.status == "PAID") Color(0xFF81C784) else Color(0xFFFF8A65), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleOverseerTab(
    appointments: List<Appointment>,
    onCancel: (Int, String) -> Unit
) {
    val nonCancelled = appointments.filter { it.status != "CANCELLED" }
    val formatter = SimpleDateFormat("MMM dd yyyy, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Active Schedule Allocations", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        if (nonCancelled.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No reservations registered in clinic databases.", color = Color(0xFF7C8D93), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(nonCancelled) { appt ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162226)),
                        border = BorderStroke(1.dp, Color(0xFF23353B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appt.patientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFB74D).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(appt.status, color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text("Practitioner: ${appt.clinicianName}", color = Color(0xFFCFD8DC), fontSize = 12.sp)
                            Text("Scheduled Slot: ${formatter.format(Date(appt.dateTimeMillis))}", color = Color(0xFF7C8D93), fontSize = 11.sp)
                            Text("Indicated complaints: ${appt.reason}", color = Color(0xFFCFD8DC), fontSize = 11.sp)

                            Divider(color = Color(0xFF23353B), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onCancel(appt.id, appt.patientId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Revoke Booker Slot", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
