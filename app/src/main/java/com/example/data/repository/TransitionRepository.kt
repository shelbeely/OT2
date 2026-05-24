package com.example.data.repository

import com.example.data.database.MilestoneDao
import com.example.data.database.PhotoDao
import com.example.data.database.VoiceDao
import com.example.data.model.MilestoneEntry
import com.example.data.model.PhotoEntry
import com.example.data.model.VoiceEntry
import kotlinx.coroutines.flow.Flow

class TransitionRepository(
    private val photoDao: PhotoDao,
    private val voiceDao: VoiceDao,
    private val milestoneDao: MilestoneDao
) {
    val allPhotos: Flow<List<PhotoEntry>> = photoDao.getAllPhotos()

    fun getPhotosByCategory(category: String): Flow<List<PhotoEntry>> =
        photoDao.getPhotosByCategory(category)

    suspend fun insertPhoto(photo: PhotoEntry): Long =
        photoDao.insertPhoto(photo)

    suspend fun deletePhoto(photo: PhotoEntry) =
        photoDao.deletePhoto(photo)

    val allVoiceEntries: Flow<List<VoiceEntry>> = voiceDao.getAllVoiceEntries()

    suspend fun insertVoiceEntry(entry: VoiceEntry): Long =
        voiceDao.insertVoiceEntry(entry)

    suspend fun deleteVoiceEntry(entry: VoiceEntry) =
        voiceDao.deleteVoiceEntry(entry)

    val allMilestones: Flow<List<MilestoneEntry>> = milestoneDao.getAllMilestones()

    suspend fun insertMilestone(milestone: MilestoneEntry): Long =
        milestoneDao.insertMilestone(milestone)

    suspend fun deleteMilestone(milestone: MilestoneEntry) =
        milestoneDao.deleteMilestone(milestone)

    suspend fun clearAllData() {
        photoDao.clearAllPhotos()
        voiceDao.clearAllVoiceEntries()
        milestoneDao.clearAllMilestones()
    }
}
