package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val category: String, // "Face", "Body", "Custom"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "voice_entries")
data class VoiceEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val estimatedPitchHz: Float = 0f
)

@Entity(tableName = "milestones")
data class MilestoneEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // "Medical", "Social", "Legal", "Personal"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "medical_records")
data class MedicalRecordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resourceId: String, // Unique FHIR Resource ID
    val title: String, // e.g., "Lab Result: Serum Estradiol"
    val recordType: String, // "LabResult", "Prescription", "Immunization"
    val value: String, // e.g., "210 pg/mL" or "4mg Daily"
    val practitioner: String, // e.g., "Quest Diagnostics" or "Planned Parenthood"
    val timestamp: Long,
    val status: String = "Final", // "Final", "Active", "Completed"
    val rawJson: String // Full FHIR resource format JSON string
)

