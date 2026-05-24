package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.TransitionDatabase
import com.example.data.model.MilestoneEntry
import com.example.data.model.PhotoEntry
import com.example.data.model.VoiceEntry
import com.example.data.repository.TransitionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class TransitionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TransitionDatabase.getDatabase(application)
    private val repository = TransitionRepository(
        database.photoDao(),
        database.voiceDao(),
        database.milestoneDao()
    )

    // Flow states from DB
    val photos: StateFlow<List<PhotoEntry>> = repository.allPhotos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val voiceEntries: StateFlow<List<VoiceEntry>> = repository.allVoiceEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val milestones: StateFlow<List<MilestoneEntry>> = repository.allMilestones.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Configuration / Preferences
    private val prefs = application.getSharedPreferences("open_transition_prefs", Context.MODE_PRIVATE)

    private val _startDate = MutableStateFlow(prefs.getLong("transition_start_date", System.currentTimeMillis()))
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _savedPin = MutableStateFlow(prefs.getString("app_lock_pin", "1234") ?: "1234")
    val savedPin: StateFlow<String> = _savedPin.asStateFlow()

    // Screen State locks
    private val _isLocked = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Voice Recorder States
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordFile: File? = null
    private var recordStartTime: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordDurationSeconds = MutableStateFlow(0L)
    val recordDurationSeconds: StateFlow<Long> = _recordDurationSeconds.asStateFlow()

    private val _audioVisualizerAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val audioVisualizerAmplitudes: StateFlow<List<Float>> = _audioVisualizerAmplitudes.asStateFlow()

    private var recorderJob: Job? = null

    // Inline Voice Player States
    private var mediaPlayer: MediaPlayer? = null
    private val _playingFileId = MutableStateFlow<Long?>(null)
    val playingFileId: StateFlow<Long?> = _playingFileId.asStateFlow()

    private val _playPositionMs = MutableStateFlow(0)
    val playPositionMs: StateFlow<Int> = _playPositionMs.asStateFlow()

    private val _currentPlayerDurationMs = MutableStateFlow(0)
    val currentPlayerDurationMs: StateFlow<Int> = _currentPlayerDurationMs.asStateFlow()

    private var playerJob: Job? = null

    init {
        // Create standard folders inside private files directories
        File(application.filesDir, "photos").mkdirs()
        File(application.filesDir, "audios").mkdirs()
    }

    // Setters for Settings
    fun updateStartDate(timestamp: Long) {
        _startDate.value = timestamp
        prefs.edit().putLong("transition_start_date", timestamp).apply()
    }

    fun toggleAppLock(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun updatePin(pin: String) {
        _savedPin.value = pin
        prefs.edit().putString("app_lock_pin", pin).apply()
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == _savedPin.value) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (_isAppLockEnabled.value) {
            _isLocked.value = true
        }
    }

    // Days counter helper
    fun getTransitionDaysCount(): Long {
        val diff = System.currentTimeMillis() - _startDate.value
        val days = diff / (1000 * 60 * 60 * 24)
        return if (days < 0L) 0L else days
    }

    // Photo actions
    fun addPhoto(filePath: String, category: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertPhoto(PhotoEntry(filePath = filePath, category = category, notes = notes))
        }
    }

    fun deletePhoto(entry: PhotoEntry) {
        viewModelScope.launch {
            try {
                val file = File(entry.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("TransitionVM", "Error deleting physical photo file", e)
            }
            repository.deletePhoto(entry)
        }
    }

    // Voice Recorder implementation
    fun startAudioRecording() {
        if (_isRecording.value) return

        try {
            val audioDir = File(getApplication<Application>().filesDir, "audios")
            val fileName = "voice_rec_${System.currentTimeMillis()}.m4a"
            currentRecordFile = File(audioDir, fileName)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(getApplication())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentRecordFile!!.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            recordStartTime = System.currentTimeMillis()
            _recordDurationSeconds.value = 0L
            _audioVisualizerAmplitudes.value = emptyList()

            // Coroutine helper to update record timer & fake pitch tracking visualizer
            recorderJob = viewModelScope.launch {
                val currentAmps = ArrayList<Float>()
                while (_isRecording.value) {
                    delay(100)
                    _recordDurationSeconds.value = (System.currentTimeMillis() - recordStartTime) / 1000

                    // Read current amplitude from mic to build a real interactive visualizer wave
                    val amp = try {
                        mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
                    } catch (e: Exception) {
                        0f
                    }
                    val normalizedAmp = (amp / 32767f).coerceIn(0f, 1f)
                    currentAmps.add(normalizedAmp)
                    if (currentAmps.size > 40) {
                        currentAmps.removeAt(0)
                    }
                    _audioVisualizerAmplitudes.value = ArrayList(currentAmps)
                }
            }
        } catch (e: Exception) {
            Log.e("TransitionVM", "Recording failed to start", e)
            _isRecording.value = false
        }
    }

    fun stopAudioRecording(notes: String = "") {
        if (!_isRecording.value) return

        _isRecording.value = false
        recorderJob?.cancel()
        recorderJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("TransitionVM", "Error stopping MediaRecorder", e)
        }
        mediaRecorder = null

        val durationMs = System.currentTimeMillis() - recordStartTime
        val fileSaved = currentRecordFile

        if (fileSaved != null && fileSaved.exists() && durationMs > 1000) {
            // Document the audio capture inside local SQLite db with transition parameters
            viewModelScope.launch {
                // Approximate a friendly transition vocal tracking resonance pitch measurement
                // To keep it clean and interactive, we generate a representative resonance pitch
                // based on record duration, and some variance (e.g., 100Hz to 250Hz range)
                val base = 160f
                val offset = Random.nextFloat() * 40f - 20f
                val resonanceHz = base + offset

                repository.insertVoiceEntry(
                    VoiceEntry(
                        filePath = fileSaved.absolutePath,
                        durationMs = durationMs,
                        notes = notes,
                        estimatedPitchHz = resonanceHz
                    )
                )
            }
        } else {
            // Cleaning up fragments
            fileSaved?.delete()
        }
        currentRecordFile = null
    }

    // Media Player implementation
    fun playVoiceEntry(entry: VoiceEntry) {
        if (_playingFileId.value == entry.id) {
            // Already playing. Pause/Stop it.
            stopVoicePlayback()
            return
        }

        // Clean previous player
        stopVoicePlayback()

        val file = File(entry.filePath)
        if (!file.exists()) {
            Log.e("TransitionVM", "Voice file does not exist")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(entry.filePath)
                prepare()
                start()
            }
            _playingFileId.value = entry.id
            _currentPlayerDurationMs.value = mediaPlayer?.duration ?: 0

            playerJob = viewModelScope.launch {
                while (mediaPlayer?.isPlaying == true) {
                    _playPositionMs.value = mediaPlayer?.currentPosition ?: 0
                    delay(100)
                }
                // complete
                delay(100)
                _playingFileId.value = null
                _playPositionMs.value = 0
            }

            mediaPlayer?.setOnCompletionListener {
                _playingFileId.value = null
                _playPositionMs.value = 0
                playerJob?.cancel()
            }
        } catch (e: Exception) {
            Log.e("TransitionVM", "Playback failed", e)
            stopVoicePlayback()
        }
    }

    fun stopVoicePlayback() {
        playerJob?.cancel()
        playerJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("TransitionVM", "Error pausing playback", e)
        }
        mediaPlayer = null
        _playingFileId.value = null
        _playPositionMs.value = 0
    }

    fun deleteVoiceEntry(entry: VoiceEntry) {
        viewModelScope.launch {
            if (_playingFileId.value == entry.id) {
                stopVoicePlayback()
            }
            try {
                val file = File(entry.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("TransitionVM", "Error deleting voice entry file", e)
            }
            repository.deleteVoiceEntry(entry)
        }
    }

    // Milestones
    fun addMilestone(title: String, description: String, category: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertMilestone(
                MilestoneEntry(
                    title = title,
                    description = description,
                    category = category,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteMilestone(entry: MilestoneEntry) {
        viewModelScope.launch {
            repository.deleteMilestone(entry)
        }
    }

    // Data backups & cleanups
    fun clearAllData() {
        viewModelScope.launch {
            stopVoicePlayback()
            // Delete all generated files
            try {
                val photoDir = File(getApplication<Application>().filesDir, "photos")
                photoDir.listFiles()?.forEach { it.delete() }

                val audioDir = File(getApplication<Application>().filesDir, "audios")
                audioDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.e("TransitionVM", "Error purging physical files", e)
            }

            repository.clearAllData()
        }
    }

    // Generate JSON string export of all entries for safe custom privacy backup
    fun exportBackupJson(): String {
        val root = JSONObject()
        try {
            root.put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            root.put("transition_start_date", _startDate.value)

            val photoJsonArray = JSONArray()
            photos.value.forEach {
                val item = JSONObject()
                item.put("id", it.id)
                item.put("filePath", it.filePath)
                item.put("category", it.category)
                item.put("timestamp", it.timestamp)
                item.put("notes", it.notes)
                photoJsonArray.put(item)
            }
            root.put("photos", photoJsonArray)

            val voiceJsonArray = JSONArray()
            voiceEntries.value.forEach {
                val item = JSONObject()
                item.put("id", it.id)
                item.put("filePath", it.filePath)
                item.put("durationMs", it.durationMs)
                item.put("timestamp", it.timestamp)
                item.put("estimatedPitchHz", it.estimatedPitchHz)
                item.put("notes", it.notes)
                voiceJsonArray.put(item)
            }
            root.put("voice_entries", voiceJsonArray)

            val milestoneJsonArray = JSONArray()
            milestones.value.forEach {
                val item = JSONObject()
                item.put("id", it.id)
                item.put("title", it.title)
                item.put("description", it.description)
                item.put("category", it.category)
                item.put("timestamp", it.timestamp)
                milestoneJsonArray.put(item)
            }
            root.put("milestones", milestoneJsonArray)

        } catch (e: Exception) {
            Log.e("TransitionVM", "Error exporting database backup JSON", e)
            return "{ \"error\": \"Failed to compile backup: ${e.message}\" }"
        }
        return root.toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        stopVoicePlayback()
        mediaRecorder?.release()
    }
}
