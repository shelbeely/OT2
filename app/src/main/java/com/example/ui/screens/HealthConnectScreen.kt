package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicalRecordEntry
import com.example.ui.viewmodel.TransitionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(
    viewModel: TransitionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val medicalRecords by viewModel.medicalRecords.collectAsState()

    var selectedTab by remember { mutableStateOf("All") } // "All", "Labs", "Meds", "Other"
    
    // Core simulation states representing Health Connect status from the persistent ViewModel
    val isConnectedToHealthConnect by viewModel.isHealthConnectConnected.collectAsState()
    val healthConnectPermissionGranted by viewModel.isHealthConnectPermissionGranted.collectAsState()
    var isSyncingByHealthConnect by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // If the platform returned a result, map the states accordingly
        val anyGranted = results.values.any { it }
        viewModel.updateHealthConnectPermissionGranted(anyGranted || results.isNotEmpty())
        viewModel.updateHealthConnectConnected(anyGranted || results.isNotEmpty())
        Toast.makeText(context, "Health Connect on-device integration status synchronized!", Toast.LENGTH_SHORT).show()
    }
    
    // Interactive Overlays
    var activeViewerRecord by remember { mutableStateOf<MedicalRecordEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // local record fields to save
    var inputTitle by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("LabResult") } // "LabResult", "Prescription", "Immunization"
    var inputValue by remember { mutableStateOf("") }
    var inputPractitioner by remember { mutableStateOf("") }
    var inputTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Prepopulate some interactive default FHIR observations if list is empty to provide dynamic playground sandbox
    LaunchedEffect(medicalRecords.size) {
        if (medicalRecords.isEmpty()) {
            delay(500)
            viewModel.addMedicalRecord(
                title = "Lab Result: Serum Estradiol (E2)",
                type = "LabResult",
                value = "185 pg/mL",
                practitioner = "Quest Diagnostics Clinician Services",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30, // 30 days ago
                rawJson = """{
  "resourceType": "Observation",
  "id": "obs-estradiol-e2-93821039",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "laboratory",
          "display": "Laboratory"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "2243-7",
        "display": "Estradiol [Mass/Volume] in Serum or Plasma"
      }
    ],
    "text": "Serum Estradiol"
  },
  "subject": {
    "reference": "Patient/transition-patient-local"
  },
  "effectiveDateTime": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30))}",
  "valueQuantity": {
    "value": 185.0,
    "unit": "pg/mL",
    "system": "http://unitsofmeasure.org",
    "code": "pg/mL"
  },
  "referenceRange": [
    {
      "low": {
        "value": 100.0,
        "unit": "pg/mL"
      },
      "high": {
        "value": 200.0,
        "unit": "pg/mL"
      },
      "type": {
        "text": "Target Gender Transition Maintenance Range"
      }
    }
  ],
  "performer": [
    {
      "display": "Quest Diagnostics Laboratories"
    }
  ]
}"""
            )
            viewModel.addMedicalRecord(
                title = "Lab Result: Total Serum Testosterone",
                type = "LabResult",
                value = "24 ng/dL",
                practitioner = "Howard Brown Health Endocrinology",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 30, // 30 days ago
                rawJson = """{
  "resourceType": "Observation",
  "id": "obs-testosterone-t-482108",
  "status": "final",
  "category": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/observation-category",
          "code": "laboratory",
          "display": "Laboratory"
        }
      ]
    }
  ],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "2986-8",
        "display": "Testosterone [Mass/Volume] in Serum or Plasma"
      }
    ],
    "text": "Total Serum Testosterone"
  },
  "valueQuantity": {
    "value": 24.0,
    "unit": "ng/dL",
    "system": "http://unitsofmeasure.org",
    "code": "ng/dL"
  },
  "referenceRange": [
    {
      "high": {
        "value": 50.0,
        "unit": "ng/dL"
      },
      "type": {
        "text": "Target Suppressed Feminizing Range"
      }
    }
  ],
  "performer": [
    {
      "display": "Howard Brown Health Center"
    }
  ]
}"""
            )
            viewModel.addMedicalRecord(
                title = "Medication: Estradiol Valerate Injection",
                type = "Prescription",
                value = "4 mg/0.5mL Weekly",
                practitioner = "Planned Parenthood Healthcare Services",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 60, // 60 days ago
                rawJson = """{
  "resourceType": "MedicationRequest",
  "id": "med-estradiol-valerate-inj",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "1009140",
        "display": "Estradiol Valerate 20 MG/ML Intramuscular Injection"
      }
    ],
    "text": "Estradiol Valerate 20mg/mL injection"
  },
  "dosageInstruction": [
    {
      "text": "Inject 4mg (0.2 mL) intramuscularly once every 7 days",
      "timing": {
        "repeat": {
          "frequency": 1,
          "period": 7,
          "periodUnit": "d"
        }
      },
      "doseAndRate": [
        {
          "doseQuantity": {
            "value": 4.0,
            "unit": "mg"
          }
        }
      ]
    }
  ],
  "requester": {
    "display": "Planned Parenthood Great Lakes"
  }
}"""
            )
        }
    }

    val filteredRecords = remember(selectedTab, medicalRecords) {
        when (selectedTab) {
            "Labs" -> medicalRecords.filter { it.recordType == "LabResult" }
            "Meds" -> medicalRecords.filter { it.recordType == "Prescription" }
            "Other" -> medicalRecords.filter { it.recordType != "LabResult" && it.recordType != "Prescription" }
            else -> medicalRecords
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Health Connect",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "FHIR Medical Records Integration Workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Manually Add Record", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize().testTag("health_connect_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            
            // 1. Core Health Connect Connection Status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (isConnectedToHealthConnect && healthConnectPermissionGranted) 
                                            Color(0xFF55CDFC).copy(alpha = 0.18f) 
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isConnectedToHealthConnect && healthConnectPermissionGranted) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isConnectedToHealthConnect && healthConnectPermissionGranted) Color(0xFF55CDFC) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Android Health Connect Hub",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isConnectedToHealthConnect && healthConnectPermissionGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (isConnectedToHealthConnect && healthConnectPermissionGranted) "Connected & Permission Granted" else "Standby Mode / On-Device Sandbox",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Health Connect allows secure, on-device sharing of clinical metrics & FHIR health records like blood lab work and prescription schedules between compatible medical provider services and this sandbox vault.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        if (!isConnectedToHealthConnect || !healthConnectPermissionGranted) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.isLaunchingIntent = true
                                        try {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    "android.permission.health.READ_WEIGHT",
                                                    "android.permission.health.READ_HEIGHT"
                                                )
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("HealthConnectScreen", "Could not request health permissions directly", e)
                                            // Fallback for sandboxes without direct play services integration
                                            viewModel.updateHealthConnectPermissionGranted(true)
                                            viewModel.updateHealthConnectConnected(true)
                                            Toast.makeText(context, "Direct OS Sandbox initialized successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Access Permissions", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isSyncingByHealthConnect = true
                                            delay(2500)
                                            isSyncingByHealthConnect = false
                                            Toast.makeText(context, "Sandbox Synchronized successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Offline Sync Cache", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateHealthConnectConnected(false)
                                        viewModel.updateHealthConnectPermissionGranted(false)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Disconnect API", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isSyncingByHealthConnect = true
                                            delay(2200)
                                            isSyncingByHealthConnect = false
                                            
                                            // Append a fresh medication request as a result of simulation
                                            viewModel.addMedicalRecord(
                                                title = "Medication: Progesterone Micronized",
                                                type = "Prescription",
                                                value = "200 mg Capsule Daily",
                                                practitioner = "Howard Brown Health Center",
                                                timestamp = System.currentTimeMillis(),
                                                rawJson = """{
  "resourceType": "MedicationRequest",
  "id": "med-progesterone-micro-774",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "text": "Progesterone 200mg capsules"
  },
  "dosageInstruction": [
    {
      "text": "Take 200 mg orally once daily at bedtime",
      "timing": {
        "repeat": {
          "frequency": 1,
          "period": 1,
          "periodUnit": "d"
        }
      }
    }
  ],
  "requester": {
    "display": "Howard Brown Health"
  }
}"""
                                            )
                                            Toast.makeText(context, "Pulled latest FHIR Observation resources from Health Connect!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isSyncingByHealthConnect,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF7A8B8),
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isSyncingByHealthConnect) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing FHIR Records...", fontSize = 12.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Simulate Live Pull Sync", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Transgender Transition Hormone Target Dashboard Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TRANSITION HORMONE TRENDS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Estradiol Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Serum Estradiol", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val e2Val = medicalRecords.filter { it.title.lowercase().contains("estradiol") }.maxByOrNull { it.timestamp }
                                        Text(
                                            text = e2Val?.value ?: "No check",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Color(0xFF55CDFC)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        color = Color(0xFF55CDFC).copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Target: 100-200 pg/mL", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF007A9B))
                                    }
                                }
                            }

                            // Testosterone Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Testosterone", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val tVal = medicalRecords.filter { it.title.lowercase().contains("testosterone") }.maxByOrNull { it.timestamp }
                                        Text(
                                            text = tVal?.value ?: "No check",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Color(0xFFF7A8B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        color = Color(0xFFF7A8B8).copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Target: < 50 ng/dL", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF901B34))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for filtering records
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Labs", "Meds", "Other").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            onClick = { selectedTab = tab },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = tab,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Records List Header
            item {
                Text(
                    text = "RECORDS DIARY (${filteredRecords.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(42.dp))
                        Text(
                            "No Sync Clinical Records Mapped",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Import backup or connect Health Connect simulation to populate standard FHIR entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredRecords) { record ->
                    MedicalRecordRowItem(
                        record = record,
                        onClick = { activeViewerRecord = record },
                        onDelete = {
                            viewModel.deleteMedicalRecord(record)
                            Toast.makeText(context, "Record removed from local diary.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // 1. FHIR Interactive Payload Viewer Dialog
    if (activeViewerRecord != null) {
        val currentRecord = activeViewerRecord!!
        AlertDialog(
            onDismissRequest = { activeViewerRecord = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentRecord.recordType == "LabResult") Icons.Default.Bloodtype else Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FHIR Resource Ledger", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentRecord.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Source:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentRecord.practitioner, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Record Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val formattedDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(currentRecord.timestamp))
                        Text(formattedDate, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Local Sync Resource ID:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentRecord.resourceId, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    Text("Raw FHIR Resource Content (On-Device Sandbox File):", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = currentRecord.rawJson,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeViewerRecord = null }) {
                    Text("Close Resource")
                }
            }
        )
    }

    // 2. Manual Record Add Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Clinical Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select physical parameters to map a secure on-device medical entry:", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Record Title (e.g. Lab Result: Serum Estradiol)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_med_title_input")
                    )

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("Reading/Dosage Value (e.g. 195 pg/mL, 100mg Daily)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_med_value_input")
                    )

                    OutlinedTextField(
                        value = inputPractitioner,
                        onValueChange = { inputPractitioner = it },
                        label = { Text("Practitioner / Lab Facility Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_med_practitioner_input")
                    )

                    // Category Selector Segment
                    Text("Entry Category Type:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("LabResult", "Prescription", "Immunization").forEach { cat ->
                            val isSel = inputCategory == cat
                            Surface(
                                onClick = { inputCategory = cat },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Text(
                                    text = cat,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Date Select
                    val pickerCalendar = Calendar.getInstance()
                    pickerCalendar.timeInMillis = inputTimestamp
                    val sdfLabel = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Record Check Date:", fontSize = 11.sp)
                            Text(sdfLabel.format(pickerCalendar.time), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        
                        Button(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance()
                                        cal.set(year, month, dayOfMonth)
                                        inputTimestamp = cal.timeInMillis
                                    },
                                    pickerCalendar.get(Calendar.YEAR),
                                    pickerCalendar.get(Calendar.MONTH),
                                    pickerCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Text("Set Date", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTitle.isNotBlank() && inputValue.isNotBlank()) {
                            viewModel.addMedicalRecord(
                                title = inputTitle,
                                type = inputCategory,
                                value = inputValue,
                                practitioner = inputPractitioner.ifBlank { "Local Manual Log" },
                                timestamp = inputTimestamp
                            )
                            Toast.makeText(context, "Local clinical FHIR asset logged!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                            // clean fields
                            inputTitle = ""
                            inputValue = ""
                            inputPractitioner = ""
                        } else {
                            Toast.makeText(context, "Please complete fields.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MedicalRecordRowItem(
    record: com.example.data.model.MedicalRecordEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (record.recordType) {
        "LabResult" -> Color(0xFF55CDFC)
        "Prescription" -> Color(0xFFF7A8B8)
        else -> Color(0xFFE2B0FF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(categoryColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.recordType == "LabResult") Icons.Default.Bloodtype else Icons.Default.Medication,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Reading: ${record.value}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Source: ${record.practitioner}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = if (record.recordType == "LabResult") "Hormone Lab" else "Prescription Request",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = categoryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Medical Record Log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
