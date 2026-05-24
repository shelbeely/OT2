package com.example.data.database

import androidx.room.*
import com.example.data.model.MilestoneEntry
import com.example.data.model.PhotoEntry
import com.example.data.model.VoiceEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntry>>

    @Query("SELECT * FROM photos WHERE category = :category ORDER BY timestamp DESC")
    fun getPhotosByCategory(category: String): Flow<List<PhotoEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntry): Long

    @Delete
    suspend fun deletePhoto(photo: PhotoEntry)

    @Query("DELETE FROM photos")
    suspend fun clearAllPhotos()
}

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voice_entries ORDER BY timestamp DESC")
    fun getAllVoiceEntries(): Flow<List<VoiceEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceEntry(entry: VoiceEntry): Long

    @Delete
    suspend fun deleteVoiceEntry(entry: VoiceEntry)

    @Query("DELETE FROM voice_entries")
    suspend fun clearAllVoiceEntries()
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones ORDER BY timestamp DESC")
    fun getAllMilestones(): Flow<List<MilestoneEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneEntry): Long

    @Delete
    suspend fun deleteMilestone(milestone: MilestoneEntry)

    @Query("DELETE FROM milestones")
    suspend fun clearAllMilestones()
}
