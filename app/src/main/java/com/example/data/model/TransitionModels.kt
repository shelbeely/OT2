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
