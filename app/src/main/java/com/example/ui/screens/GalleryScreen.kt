package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas as CompCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.PhotoEntry
import com.example.ui.viewmodel.TransitionViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GalleryScreen(
    viewModel: TransitionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCameraActive by remember { mutableStateOf(false) }

    // Core gallery tabs
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Face", "Body", "Custom")

    val photos by viewModel.photos.collectAsState()

    // Compare mode states
    var isCompareModeActive by remember { mutableStateOf(false) }
    val selectedComparePhotos = remember { mutableStateListOf<PhotoEntry>() }

    // Selected photo details drawer/dialog code
    var selectedDetailPhoto by remember { mutableStateOf<PhotoEntry?>(null) }

    val filteredPhotos = remember(photos, selectedTab) {
        val categoryStr = tabs[selectedTab]
        photos.filter { it.category.lowercase() == categoryStr.lowercase() }
    }

    Scaffold(
        modifier = modifier.testTag("gallery_root_screen"),
        floatingActionButton = {
            if (!isCameraActive && !isCompareModeActive) {
                FloatingActionButton(
                    onClick = { isCameraActive = true },
                    modifier = Modifier.testTag("open_camera_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Open Guided Camera")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isCameraActive) {
                CameraViewLayout(
                    viewModel = viewModel,
                    onCloseCamera = { isCameraActive = false }
                )
            } else if (isCompareModeActive) {
                CompareModeLayout(
                    selectedPhotos = selectedComparePhotos,
                    onExitCompare = {
                        isCompareModeActive = false
                        selectedComparePhotos.clear()
                    }
                )
            } else {
                // Main Tabbed Gallery view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your Progress Photo Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Keep your snaps aligned over time to evaluate subtle physical progress matches.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledTonalButton(
                            onClick = {
                                isCompareModeActive = true
                                selectedComparePhotos.clear()
                            },
                            modifier = Modifier.testTag("compare_mode_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isCompareModeActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compare", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredPhotos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No photos captured in ${tabs[selectedTab]}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Tap the camera button to take your first guided timeline photo.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredPhotos) { photo ->
                                val isSelectedToCompare = selectedComparePhotos.contains(photo)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSelectedToCompare) 3.dp else 1.dp,
                                            color = if (isSelectedToCompare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedDetailPhoto = photo
                                        }
                                ) {
                                    AsyncImage(
                                        model = File(photo.filePath),
                                        contentDescription = "Transition Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Display Date banner at bottom
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .align(Alignment.BottomCenter)
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(photo.timestamp)),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Simple details screen dialog
            selectedDetailPhoto?.let { photo ->
                AlertDialog(
                    onDismissRequest = { selectedDetailPhoto = null },
                    title = {
                        Text(
                            text = "Photo Detail - ${photo.category}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = File(photo.filePath),
                                    contentDescription = "Large photo view",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recorded: " + SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(photo.timestamp)),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            if (photo.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Notes: ${photo.notes}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (selectedComparePhotos.contains(photo)) {
                                    selectedComparePhotos.remove(photo)
                                } else if (selectedComparePhotos.size < 2) {
                                    selectedComparePhotos.add(photo)
                                }
                                selectedDetailPhoto = null
                            }
                        ) {
                            val label = if (selectedComparePhotos.contains(photo)) "Remove from compare" else "Add to compare"
                            Text(label)
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.deletePhoto(photo)
                                    selectedDetailPhoto = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { selectedDetailPhoto = null }) {
                                Text("Close")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CompareModeLayout(
    selectedPhotos: List<PhotoEntry>,
    onExitCompare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Side-by-Side Comparison",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Observe visual changes over time timeline.",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            IconButton(onClick = onExitCompare) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit compare mode")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPhotos.size < 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select 2 photos to compare",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Go back to the gallery, click a photo, and tap 'Add to compare' for exactly two photos to see them here side-by-side.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExitCompare) {
                    Text("Select Photos")
                }
            }
        } else {
            // Display exactly two photos side-by-side
            val p1 = selectedPhotos[0]
            val p2 = selectedPhotos[1]

            // Ensure chronological order
            val (before, after) = if (p1.timestamp <= p2.timestamp) Pair(p1, p2) else Pair(p2, p1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Before Photo
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Earlier",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(before.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = File(before.filePath),
                            contentDescription = "Before profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // After Photo
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Later",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(after.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = File(after.filePath),
                            contentDescription = "After profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onExitCompare,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Different Photos")
            }
        }
    }
}

@Composable
fun CameraViewLayout(
    viewModel: TransitionViewModel,
    onCloseCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedCategory by remember { mutableStateOf("Face") }
    val categories = listOf("Face", "Body", "Custom")

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Capture Trigger
    val takePhoto = {
        val photosDir = File(context.filesDir, "photos")
        val photoFile = File(photosDir, "log_${selectedCategory.lowercase()}_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.addPhoto(photoFile.absolutePath, selectedCategory, "Guided capture")
                    onCloseCamera()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraLayout", "Error capturing guide photo", exception)
                    // If hardware CameraX capture fails on emulator, trigger Mock captured photo
                    generateMockPhotoAndSave(context, viewModel, selectedCategory)
                    onCloseCamera()
                }
            }
        ) ?: run {
            // Mock capture fallback when camera is not supported
            generateMockPhotoAndSave(context, viewModel, selectedCategory)
            onCloseCamera()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_guided_view")
    ) {
        // Upper Controls of Camera Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Guides System Overlay",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            IconButton(
                onClick = onCloseCamera,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
            }
        }

        if (hasCameraPermission) {
            // Centered guided frame (Takes remaining space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray)
            ) {
                // Camera Preview Frame
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraLayout", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Silhouette Guide Overlay Drawn using Canvas depending on selection
                CompCanvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

                    if (selectedCategory == "Face") {
                        // Oval silhouette for centering head
                        drawOval(
                            color = Color(0xFF55CDFC),
                            topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.25f, canvasHeight * 0.15f),
                            size = androidx.compose.ui.geometry.Size(canvasWidth * 0.5f, canvasHeight * 0.45f),
                            style = Stroke(width = 3.dp.toPx(), pathEffect = dashPathEffect)
                        )
                        // Drawn eyes guide line
                        drawLine(
                            color = Color(0xFFF7A8B8),
                            start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.2f, canvasHeight * 0.35f),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.8f, canvasHeight * 0.35f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = dashPathEffect
                        )
                    } else if (selectedCategory == "Body") {
                        // Frame/Shoulders outline placeholder guide
                        drawLine(
                            color = Color(0xFF55CDFC),
                            start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.5f, 0f),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.5f, canvasHeight),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = dashPathEffect
                        )
                        drawLine(
                            color = Color(0xFF55CDFC),
                            start = androidx.compose.ui.geometry.Offset(0f, canvasHeight * 0.5f),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth, canvasHeight * 0.5f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = dashPathEffect
                        )
                        // Shoulder Guides arcs
                        drawArc(
                            color = Color(0xFFF7A8B8),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.15f, canvasHeight * 0.35f),
                            size = androidx.compose.ui.geometry.Size(canvasWidth * 0.7f, canvasHeight * 0.4f),
                            style = Stroke(width = 3.dp.toPx(), pathEffect = dashPathEffect)
                        )
                    } else {
                        // Standard grid overlays for Custom Alignment
                        // 3x3 Grid lines
                        drawLine(color = Color.White.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.33f, 0f), end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.33f, canvasHeight), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color.White.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.66f, 0f), end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.66f, canvasHeight), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color.White.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, canvasHeight * 0.33f), end = androidx.compose.ui.geometry.Offset(canvasWidth, canvasHeight * 0.33f), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = Color.White.copy(alpha = 0.5f), start = androidx.compose.ui.geometry.Offset(0f, canvasHeight * 0.66f), end = androidx.compose.ui.geometry.Offset(canvasWidth, canvasHeight * 0.66f), strokeWidth = 1.5.dp.toPx())
                    }
                }

                // Help guideline indicator text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    val helpText = when (selectedCategory) {
                        "Face" -> "Align your face oval with the azure oval. Dotted pink line shows eye-level."
                        "Body" -> "Stand centered along the vertical axis. Line up shoulders with the guide curves."
                        else -> "Maximize grid boxes to align specific features consistently."
                    }
                    Text(
                        text = helpText,
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Camera permission required.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Or tap Capture below to generate a smart mock photo logs fallback.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Lower Toolbar controls of Camera View
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category selector tabs for guides
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isCatSelected) Color.White else Color.Transparent)
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) Color.Black else Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Snap photo action button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { takePhoto() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                ) {
                    // Empty contents represents camera shutter trigger
                }
            }
        }
    }
}

// Generate smart artistic mock photos inside private directories if camera isn't active/supported
private fun generateMockPhotoAndSave(context: Context, viewModel: TransitionViewModel, category: String) {
    try {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val fileName = "mock_${category.lowercase()}_${System.currentTimeMillis()}.jpg"
        val photoFile = File(photosDir, fileName)

        // Draw a gorgeous placeholder dynamic avatar representation of user transformation
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Background color
        paint.color = GColor.parseColor("#1C1F2E")
        canvas.drawRect(0f, 0f, 500f, 500f, paint)

        // Soft gradient circle representing physical progression
        paint.color = if (category == "Face") GColor.parseColor("#55CDFC") else GColor.parseColor("#F7A8B8")
        canvas.drawCircle(250f, 250f, 160f, paint)

        paint.color = GColor.parseColor("#FFFFFF")
        canvas.drawCircle(250f, 250f, 130f, paint)

        paint.color = GColor.parseColor("#1C1F2E")
        paint.textSize = 34f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("OpenTransition", 250f, 220f, paint)

        paint.textSize = 28f
        paint.color = GColor.parseColor("#006689")
        canvas.drawText("$category Progress", 250f, 270f, paint)

        paint.textSize = 20f
        paint.color = GColor.parseColor("#777777")
        val curDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        canvas.drawText("Logged $curDate", 250f, 320f, paint)

        val out = FileOutputStream(photoFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()

        viewModel.addPhoto(photoFile.absolutePath, category, "Simulated Timeline guided capture")
    } catch (e: Exception) {
        Log.e("GalleryScreen", "Failed generating mock thumbnail", e)
    }
}
