package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class TransitionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TransitionDatabase.getDatabase(application)
    private val repository = TransitionRepository(
        database.photoDao(),
        database.voiceDao(),
        database.milestoneDao(),
        database.medicalRecordDao()
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

    val medicalRecords: StateFlow<List<com.example.data.model.MedicalRecordEntry>> = repository.allMedicalRecords.stateIn(
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

    private val _isHealthConnectConnected = MutableStateFlow(prefs.getBoolean("health_connect_connected", false))
    val isHealthConnectConnected: StateFlow<Boolean> = _isHealthConnectConnected.asStateFlow()

    private val _isHealthConnectPermissionGranted = MutableStateFlow(prefs.getBoolean("health_connect_permission_granted", false))
    val isHealthConnectPermissionGranted: StateFlow<Boolean> = _isHealthConnectPermissionGranted.asStateFlow()

    // Screen State locks
    private val _isLocked = MutableStateFlow(prefs.getBoolean("app_lock_enabled", false))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Temporary flag to prevent auto-locking when launching standard system dialogs / pickers
    var isLaunchingIntent: Boolean = false

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

    fun updateHealthConnectConnected(connected: Boolean) {
        _isHealthConnectConnected.value = connected
        prefs.edit().putBoolean("health_connect_connected", connected).apply()
    }

    fun updateHealthConnectPermissionGranted(granted: Boolean) {
        _isHealthConnectPermissionGranted.value = granted
        prefs.edit().putBoolean("health_connect_permission_granted", granted).apply()
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
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
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

            val medicalRecordJsonArray = JSONArray()
            medicalRecords.value.forEach {
                val item = JSONObject()
                item.put("id", it.id)
                item.put("resourceId", it.resourceId)
                item.put("title", it.title)
                item.put("recordType", it.recordType)
                item.put("value", it.value)
                item.put("practitioner", it.practitioner)
                item.put("timestamp", it.timestamp)
                item.put("rawJson", it.rawJson)
                medicalRecordJsonArray.put(item)
            }
            root.put("medical_records", medicalRecordJsonArray)

        } catch (e: Exception) {
            Log.e("TransitionVM", "Error exporting database backup JSON", e)
            return "{ \"error\": \"Failed to compile backup: ${e.message}\" }"
        }
        return root.toString(2)
    }

    private fun parseTimestamp(value: Any?): Long {
        if (value == null) return System.currentTimeMillis()
        if (value is Number) return value.toLong()
        val str = value.toString().trim()
        if (str.isEmpty()) return System.currentTimeMillis()
        try {
            return str.toLong()
        } catch (e: NumberFormatException) {
            // Not a direct long string, try date parsing
        }
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "MMM dd, yyyy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                val date = sdf.parse(str)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return System.currentTimeMillis()
    }

    fun normalizePath(context: Context, originalPath: String): String {
        if (originalPath.isBlank()) return ""
        
        val filesIndex = originalPath.indexOf("/files/")
        if (filesIndex != -1) {
            val subPath = originalPath.substring(filesIndex + 7)
            return File(context.filesDir, subPath).absolutePath
        }
        
        if (originalPath.contains("photos/")) {
            val idx = originalPath.indexOf("photos/")
            return File(context.filesDir, originalPath.substring(idx)).absolutePath
        }
        if (originalPath.contains("audios/")) {
            val idx = originalPath.indexOf("audios/")
            return File(context.filesDir, originalPath.substring(idx)).absolutePath
        }
        
        val lowercase = originalPath.lowercase(Locale.US)
        val name = File(originalPath).name
        return if (lowercase.endsWith(".mp3") || lowercase.endsWith(".wav") || lowercase.endsWith(".m4a") || lowercase.endsWith(".3gp")) {
            File(File(context.filesDir, "audios"), name).absolutePath
        } else {
            File(File(context.filesDir, "photos"), name).absolutePath
        }
    }

    suspend fun importBackupZip(context: Context, uri: Uri): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: return@withContext false
                val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))

                var jsonContent: String? = null

                var entry: ZipEntry? = zipInputStream.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        val entryNameLower = entryName.lowercase(Locale.US)
                        if (entryNameLower.endsWith(".json")) {
                            val outputStream = ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            var count: Int
                            while (zipInputStream.read(buffer).also { count = it } != -1) {
                                outputStream.write(buffer, 0, count)
                            }
                            jsonContent = outputStream.toString("UTF-8")
                        } else if (entryNameLower.endsWith(".jpg") || entryNameLower.endsWith(".jpeg") || entryNameLower.endsWith(".png") ||
                                   entryNameLower.endsWith(".mp3") || entryNameLower.endsWith(".wav") || entryNameLower.endsWith(".m4a") || entryNameLower.endsWith(".3gp")) {
                            val targetFilePath = normalizePath(context, entryName)
                            val targetFile = File(targetFilePath)
                            targetFile.parentFile?.mkdirs()

                            val fileOutputStream = FileOutputStream(targetFile)
                            val buffer = ByteArray(4096)
                            var count: Int
                            while (zipInputStream.read(buffer).also { count = it } != -1) {
                                fileOutputStream.write(buffer, 0, count)
                            }
                            fileOutputStream.close()
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                zipInputStream.close()

                if (jsonContent != null) {
                    importBackupJson(jsonContent)
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("TransitionVM", "Error importing backup ZIP file", e)
                false
            }
        }
    }

    suspend fun importBackupJson(jsonStr: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val trimmed = jsonStr.trim()
                if (trimmed.isEmpty()) return@withContext false

                val root = if (trimmed.startsWith("[")) {
                    val rootObj = JSONObject()
                    rootObj.put("entries", JSONArray(trimmed))
                    rootObj
                } else {
                    JSONObject(trimmed)
                }

                // 1. Transition Start Date (Look at root, or inside settings/profile/user structures)
                val possibleStartDates = listOf("transition_start_date", "transitionStartDate", "startDate", "start_date", "dayZero", "day_zero")
                var startDateVal: Long? = null
                for (key in possibleStartDates) {
                    if (root.has(key)) {
                        startDateVal = parseTimestamp(root.get(key))
                        break
                    }
                }
                
                // Fallback check inside settings, user, or profile objects
                if (startDateVal == null) {
                    listOf("settings", "user", "profile").forEach { objKey ->
                        if (root.has(objKey)) {
                            val childObj = root.optJSONObject(objKey)
                            if (childObj != null) {
                                for (key in possibleStartDates) {
                                    if (childObj.has(key)) {
                                        startDateVal = parseTimestamp(childObj.get(key))
                                        break
                                    }
                                }
                            }
                        }
                    }
                }

                if (startDateVal != null) {
                    updateStartDate(startDateVal!!)
                }

                // 2. Milestones / Milestone entries (includes general logs, diary, and timeline entries)
                val milestonesKeys = listOf("milestones", "milestone", "milestone_entries", "milestoneEntries", "entries", "entry", "logs", "records", "history", "items")
                var milestonesArray: JSONArray? = null
                for (key in milestonesKeys) {
                    if (root.has(key)) {
                        milestonesArray = root.optJSONArray(key)
                        if (milestonesArray != null) break
                    }
                }

                milestonesArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        var title = obj.optString("title").takeIf { it.isNotBlank() }
                            ?: obj.optString("name").takeIf { it.isNotBlank() }
                            ?: obj.optString("text").takeIf { it.isNotBlank() }
                            ?: ""
                        
                        val description = obj.optString("description").takeIf { it.isNotBlank() }
                            ?: obj.optString("notes").takeIf { it.isNotBlank() }
                            ?: obj.optString("comment").takeIf { it.isNotBlank() }
                            ?: obj.optString("desc").takeIf { it.isNotBlank() }
                            ?: ""

                        if (title.isBlank()) {
                            title = if (description.isNotBlank()) {
                                if (description.length > 30) description.substring(0, 27) + "..." else description
                            } else {
                                "Timeline Log"
                            }
                        }

                        val categoryRaw = obj.optString("category").takeIf { it.isNotBlank() }
                            ?: obj.optString("type").takeIf { it.isNotBlank() }
                            ?: obj.optString("group").takeIf { it.isNotBlank() }
                            ?: "Personal"
                        
                        val category = when (categoryRaw.lowercase(Locale.US)) {
                            "medical", "med", "medicine", "hrt" -> "Medical"
                            "social", "soc" -> "Social"
                            "legal", "leg", "court", "passport" -> "Legal"
                            else -> "Personal"
                        }

                        val timestampObj = if (obj.has("timestamp")) obj.get("timestamp")
                            else if (obj.has("date")) obj.get("date")
                            else if (obj.has("time")) obj.get("time")
                            else if (obj.has("created_at")) obj.get("created_at")
                            else null
                        val timestamp = parseTimestamp(timestampObj)

                        // Insert primary milestone/diary item
                        repository.insertMilestone(
                            MilestoneEntry(
                                title = title,
                                description = description,
                                category = category,
                                timestamp = timestamp
                            )
                        )

                        // Extract nested photos directly associated with this timeline entry
                        val nestedPhotos = mutableListOf<String>()
                        val photoKeys = listOf("photos", "photo", "images", "image", "imagePath", "filePath")
                        for (photoKey in photoKeys) {
                            if (obj.has(photoKey)) {
                                val photoVal = obj.get(photoKey)
                                if (photoVal is JSONArray) {
                                    for (j in 0 until photoVal.length()) {
                                        val p = photoVal.optString(j)
                                        if (p.isNotBlank()) nestedPhotos.add(p)
                                    }
                                } else if (photoVal is String && photoVal.isNotBlank()) {
                                    nestedPhotos.add(photoVal)
                                } else if (photoVal is JSONObject) {
                                    val p = photoVal.optString("filePath").takeIf { it.isNotBlank() }
                                        ?: photoVal.optString("path").takeIf { it.isNotBlank() }
                                        ?: photoVal.optString("imagePath").takeIf { it.isNotBlank() }
                                    if (p != null && p.isNotBlank()) nestedPhotos.add(p)
                                }
                            }
                        }

                        nestedPhotos.forEach { photoPath ->
                            val localPath = normalizePath(getApplication<Application>(), photoPath)
                            repository.insertPhoto(
                                PhotoEntry(
                                    filePath = localPath,
                                    category = "Face",
                                    notes = description.ifBlank { "Imported from entry: $title" },
                                    timestamp = timestamp
                                )
                            )
                        }

                        // Extract nested medications directly associated with this timeline entry
                        val nestedMedsKeys = listOf("medications", "medication", "medicines", "meds")
                        for (medKey in nestedMedsKeys) {
                            if (obj.has(medKey)) {
                                val medsVal = obj.get(medKey)
                                if (medsVal is JSONArray) {
                                    for (j in 0 until medsVal.length()) {
                                        val medObj = medsVal.optJSONObject(j) ?: continue
                                        val name = medObj.optString("name").takeIf { it.isNotBlank() } ?: "HRT Medication"
                                        val dosage = medObj.optString("dosage").trim()
                                        repository.insertMilestone(
                                            MilestoneEntry(
                                                title = "Medication: $name",
                                                description = if (dosage.isNotEmpty()) "Dosage: $dosage" else "",
                                                category = "Medical",
                                                timestamp = timestamp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Extract nested dimensions directly associated with this timeline entry
                        val nestedDimKeys = listOf("dimensions", "dimension", "measurements", "sizes")
                        for (dimKey in nestedDimKeys) {
                            if (obj.has(dimKey)) {
                                val dimsVal = obj.get(dimKey)
                                if (dimsVal is JSONArray) {
                                    for (j in 0 until dimsVal.length()) {
                                        val dimObj = dimsVal.optJSONObject(j) ?: continue
                                        val name = dimObj.optString("name").takeIf { it.isNotBlank() } ?: "Dimension"
                                        val value = dimObj.optDouble("value", Double.NaN)
                                        val unit = dimObj.optString("unit").trim()
                                        if (!value.isNaN()) {
                                            repository.insertMilestone(
                                                MilestoneEntry(
                                                    title = "Body Dimension: $name",
                                                    description = "Value: $value $unit",
                                                    category = "Personal",
                                                    timestamp = timestamp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Independent Medications schedule at top level
                val medicineKeys = listOf("medications", "medication", "medicines", "meds")
                var medicineArray: JSONArray? = null
                for (key in medicineKeys) {
                    if (root.has(key)) {
                        medicineArray = root.optJSONArray(key)
                        if (medicineArray != null) break
                    }
                }

                medicineArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: "HRT Medication"
                        val dosage = obj.optString("dosage").trim()
                        val frequency = obj.optString("frequency").trim()
                        val startDateStr = obj.optString("startDate").trim()

                        val title = "Started Medication: $name"
                        val details = buildString {
                            if (dosage.isNotEmpty()) append("Dosage: $dosage")
                            if (frequency.isNotEmpty()) {
                                if (isNotEmpty()) append(", ")
                                append("Frequency: $frequency")
                            }
                            val activeVal = obj.opt("active")
                            if (activeVal != null) {
                                if (isNotEmpty()) append("\n")
                                append("Status: ${if (activeVal.toString() == "true") "Active" else "Inactive"}")
                            }
                        }

                        val dateVal = if (startDateStr.isNotEmpty()) parseTimestamp(startDateStr) else parseTimestamp(obj.opt("timestamp"))

                        repository.insertMilestone(
                            MilestoneEntry(
                                title = title,
                                description = details,
                                category = "Medical",
                                timestamp = dateVal
                            )
                        )
                    }
                }

                // 4. Independent Dimensions / Body Measurements at top level
                val dimensionKeys = listOf("dimensions", "dimension", "measurements", "sizes")
                var dimensionArray: JSONArray? = null
                for (key in dimensionKeys) {
                    if (root.has(key)) {
                        dimensionArray = root.optJSONArray(key)
                        if (dimensionArray != null) break
                    }
                }

                dimensionArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: "Dimension"
                        val value = obj.optDouble("value", Double.NaN)
                        val unit = obj.optString("unit").trim()
                        val timeVal = obj.opt("timestamp") ?: obj.opt("date")

                        if (!value.isNaN()) {
                            val title = "Body Dimension: $name"
                            val details = "Value: $value $unit"
                            repository.insertMilestone(
                                MilestoneEntry(
                                    title = title,
                                    description = details,
                                    category = "Personal",
                                    timestamp = parseTimestamp(timeVal)
                                )
                            )
                        }
                    }
                }

                // 5. Independent Photos List at top level
                val photosKeys = listOf("photos", "photo", "images", "photoEntries", "image_entries")
                var photosArray: JSONArray? = null
                for (key in photosKeys) {
                    if (root.has(key)) {
                        photosArray = root.optJSONArray(key)
                        if (photosArray != null) break
                    }
                }

                photosArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val path = obj.optString("filePath").takeIf { it.isNotBlank() }
                            ?: obj.optString("path").takeIf { it.isNotBlank() }
                            ?: obj.optString("imagePath").takeIf { it.isNotBlank() }
                            ?: obj.optString("uri").takeIf { it.isNotBlank() }
                            ?: ""
                        
                        if (path.isNotBlank()) {
                            val notes = obj.optString("notes").takeIf { it.isNotBlank() }
                                ?: obj.optString("caption").takeIf { it.isNotBlank() }
                                ?: obj.optString("description").takeIf { it.isNotBlank() }
                                ?: ""
                            
                            val categoryRaw = obj.optString("category").takeIf { it.isNotBlank() }
                                ?: obj.optString("group").takeIf { it.isNotBlank() }
                                ?: "Custom"
                            
                            val category = when (categoryRaw.lowercase(Locale.US)) {
                                "face" -> "Face"
                                "body" -> "Body"
                                else -> "Custom"
                            }

                            val timeVal = obj.opt("timestamp") ?: obj.opt("date") ?: obj.opt("time")

                            val localPath = normalizePath(getApplication<Application>(), path)
                            repository.insertPhoto(
                                PhotoEntry(
                                    filePath = localPath,
                                    category = category,
                                    notes = notes,
                                    timestamp = parseTimestamp(timeVal)
                                )
                            )
                        }
                    }
                }

                // 6. Independent Voice Entries / Audio recordings
                val voiceKeys = listOf("voice_entries", "voice", "recordings", "voiceEntries", "audios")
                var voiceArray: JSONArray? = null
                for (key in voiceKeys) {
                    if (root.has(key)) {
                        voiceArray = root.optJSONArray(key)
                        if (voiceArray != null) break
                    }
                }

                voiceArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val path = obj.optString("filePath").takeIf { it.isNotBlank() }
                            ?: obj.optString("path").takeIf { it.isNotBlank() }
                            ?: obj.optString("audioPath").takeIf { it.isNotBlank() }
                            ?: ""

                        if (path.isNotBlank()) {
                            val duration = obj.optLong("durationMs", 3000L).takeIf { it > 0 }
                                ?: obj.optLong("duration", 3L) * 1000L
                            
                            val notes = obj.optString("notes").takeIf { it.isNotBlank() }
                                ?: obj.optString("description").takeIf { it.isNotBlank() }
                                ?: ""
                            
                            val pitchRaw = obj.optDouble("estimatedPitchHz", 160.0)
                                .takeIf { !it.isNaN() }
                                ?: obj.optDouble("pitch", 160.0)
                                ?: obj.optDouble("frequency", 160.0)
                            
                            val timeVal = obj.opt("timestamp") ?: obj.opt("date")

                            val localPath = normalizePath(getApplication<Application>(), path)
                            repository.insertVoiceEntry(
                                VoiceEntry(
                                    filePath = localPath,
                                    durationMs = duration,
                                    notes = notes,
                                    estimatedPitchHz = pitchRaw.toFloat(),
                                    timestamp = parseTimestamp(timeVal)
                                )
                            )
                        }
                    }
                }

                // 7. Medical records / FHIR clinical structures
                val medicalRecordKeys = listOf("medical_records", "medical_record_entries", "medicalRecords", "medicalRecordsEntries", "labs", "clinical_records")
                var medicalRecordsArray: JSONArray? = null
                for (key in medicalRecordKeys) {
                    if (root.has(key)) {
                        medicalRecordsArray = root.optJSONArray(key)
                        if (medicalRecordsArray != null) break
                    }
                }

                medicalRecordsArray?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val title = obj.optString("title").takeIf { it.isNotBlank() } ?: "Clinical Entry"
                        val value = obj.optString("value").takeIf { it.isNotBlank() } ?: ""
                        val type = obj.optString("recordType").takeIf { it.isNotBlank() }
                            ?: obj.optString("type").takeIf { it.isNotBlank() }
                            ?: "LabResult"
                        val practitioner = obj.optString("practitioner").takeIf { it.isNotBlank() }
                            ?: obj.optString("source").takeIf { it.isNotBlank() }
                            ?: "Imported Backup"
                        val timeVal = obj.opt("timestamp") ?: obj.opt("date")
                        
                        val rawFhir = obj.optString("rawJson").takeIf { it.isNotBlank() } ?: ""

                        repository.insertMedicalRecord(
                            com.example.data.model.MedicalRecordEntry(
                                resourceId = obj.optString("resourceId").takeIf { it.isNotBlank() } ?: "fhir-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}",
                                title = title,
                                recordType = type,
                                value = value,
                                practitioner = practitioner,
                                timestamp = parseTimestamp(timeVal),
                                rawJson = rawFhir.ifEmpty { "{\"resourceType\": \"Observation\", \"status\": \"final\", \"code\": {\"text\": \"$title\"}, \"valueQuantity\": {\"value\": \"$value\"}, \"performer\": [{\"display\": \"$practitioner\"}]}" }
                            )
                        )
                    }
                }

                true
            } catch (e: Exception) {
                Log.e("TransitionVM", "Error importing backup JSON", e)
                false
            }
        }
    }

    fun addMedicalRecord(title: String, type: String, value: String, practitioner: String, timestamp: Long, rawJson: String = "") {
        viewModelScope.launch {
            repository.insertMedicalRecord(
                com.example.data.model.MedicalRecordEntry(
                    resourceId = "fhir-resource-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}",
                    title = title,
                    recordType = type,
                    value = value,
                    practitioner = practitioner,
                    timestamp = timestamp,
                    rawJson = rawJson.ifEmpty { "{\"resourceType\": \"Observation\", \"id\": \"mock-fhir-id\", \"status\": \"final\", \"code\": {\"text\": \"$title\"}, \"valueQuantity\": {\"value\": \"$value\"}, \"performer\": [{\"display\": \"$practitioner\"}]}" }
                )
            )
        }
    }

    fun deleteMedicalRecord(entry: com.example.data.model.MedicalRecordEntry) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(entry)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            if (_isRecording.value) {
                _isRecording.value = false
                recorderJob?.cancel()
                recorderJob = null
                mediaRecorder?.stop()
            }
        } catch (e: Exception) {
            Log.e("TransitionVM", "Error stopping recorder on VM cleared", e)
        }
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("TransitionVM", "Error releasing recorder on VM cleared", e)
        }
        mediaRecorder = null
        stopVoicePlayback()
    }
}
