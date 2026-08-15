package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Collections
import java.util.UUID
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.itemsIndexed

val OrchisBg = Color(0xFF1E1E1E)
val OrchisSurface = Color(0xFF2D2D2D)
val OrchisCard = Color(0xFF353535)
val OrchisText = Color(0xFFE6E6E6)
val OrchisTextMuted = Color(0xFFAAAAAA)
val OrchisPrimary = Color(0xFF3584E4)
val OrchisSplit = Color(0xFF2A4B7C) 
val OrchisMerge = Color(0xFF633232)
val OrchisCompress = Color(0xFF325463)
val OrchisMetadata = Color(0xFF5E3C73)

class MainActivity : ComponentActivity() {
    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfService.init(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

enum class Screen { Home, Split, Merge, Compress, Metadata, History, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PdfViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    if (currentScreen == Screen.Home || currentScreen == Screen.History || currentScreen == Screen.Settings) {
        BentoHomeScreen(
            currentScreen = currentScreen,
            onNavigate = { screen -> currentScreen = screen },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.Split -> "Split PDF"
                                Screen.Merge -> "Merge PDFs"
                                Screen.Compress -> "Compress PDF"
                                Screen.Metadata -> "Edit Metadata"
                                else -> ""
                            },
                            color = OrchisText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentScreen = Screen.Home }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OrchisText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OrchisBg
                    )
                )
            },
            containerColor = OrchisBg
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    Screen.Split -> SplitScreen(viewModel)
                    Screen.Merge -> MergeScreen(viewModel)
                    Screen.Compress -> CompressScreen(viewModel)
                    Screen.Metadata -> MetadataScreen(viewModel)
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun BentoHomeScreen(currentScreen: Screen, onNavigate: (Screen) -> Unit, viewModel: PdfViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrchisBg)
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ILHAM'S PDF TOOLS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OrchisText, letterSpacing = (-0.5).sp)
            }
            Box(
                modifier = Modifier.size(48.dp).background(OrchisSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("I", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OrchisPrimary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                Screen.History -> HistoryScreen(viewModel)
                Screen.Settings -> SettingsScreen(viewModel)
                else -> {
                    val history by viewModel.historyState.collectAsStateWithLifecycle()
                    BentoGrid(onNavigate, history)
                }
            }
        }

        // Bottom Nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .background(OrchisSurface, RoundedCornerShape(32.dp))
                .border(1.dp, OrchisCard, RoundedCornerShape(32.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("Tools", Icons.Default.Build, currentScreen == Screen.Home) { onNavigate(Screen.Home) }
            BottomNavItem("History", Icons.Default.History, currentScreen == Screen.History) { onNavigate(Screen.History) }
            BottomNavItem("Settings", Icons.Default.Settings, currentScreen == Screen.Settings) { onNavigate(Screen.Settings) }
        }
    }
}

@Composable
fun BentoGrid(onNavigate: (Screen) -> Unit, history: List<PdfHistory>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.weight(2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = OrchisSplit,
                onClick = { onNavigate(Screen.Split) }
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                        Icon(Icons.Default.CallSplit, contentDescription = null, tint = OrchisText)
                    }
                    Column {
                        Text("Split", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OrchisText)
                        Text("By pages or ranges", fontSize = 12.sp, color = OrchisTextMuted, maxLines = 1)
                    }
                }
            }

            BentoCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = OrchisMetadata,
                onClick = { onNavigate(Screen.Metadata) }
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = OrchisText)
                    }
                    Column {
                        Text("Metadata", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OrchisText)
                        Text("Edit properties", fontSize = 12.sp, color = OrchisTextMuted)
                    }
                }
            }
        }

        Row(modifier = Modifier.weight(2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = OrchisMerge,
                onClick = { onNavigate(Screen.Merge) }
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                     Box(modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                          Icon(Icons.Default.MergeType, contentDescription = null, tint = OrchisText)
                     }
                     Column {
                         Text("Merge", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OrchisText)
                         Text("Combine files", fontSize = 12.sp, color = OrchisTextMuted)
                     }
                }
            }

            BentoCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = OrchisCompress,
                onClick = { onNavigate(Screen.Compress) }
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                     Box(modifier = Modifier.background(Color.Black.copy(alpha=0.3f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                          Icon(Icons.Default.Compress, contentDescription = null, tint = OrchisText)
                     }
                     Column {
                         Text("Compress", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OrchisText)
                         Text("High Quality", fontSize = 12.sp, color = OrchisTextMuted)
                     }
                }
            }
        }

        Column(modifier = Modifier.weight(2f).fillMaxWidth()) {
            Text("Recent Files", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OrchisText, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            if (history.isEmpty()) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OrchisSurface, RoundedCornerShape(28.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = OrchisCard,
                                style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect),
                                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                            )
                        }
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).background(OrchisCard, CircleShape), contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 24.sp, color = OrchisTextMuted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("No recent files yet", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OrchisText, textAlign = TextAlign.Center)
                        Text("Processed files will appear here", fontSize = 10.sp, fontStyle = FontStyle.Italic, color = OrchisTextMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                val context = LocalContext.current
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history.take(5)) { item ->
                        Card(
                            onClick = {
                                item.filePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        shareFile(context, file)
                                    } else {
                                        android.widget.Toast.makeText(context, "File no longer exists", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxHeight().width(160.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = OrchisSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                val icon = when (item.operationType) {
                                    "Split" -> Icons.Default.CallSplit
                                    "Merged" -> Icons.Default.MergeType
                                    else -> Icons.Default.Compress
                                }
                                Box(modifier = Modifier.background(OrchisCard, RoundedCornerShape(12.dp)).padding(8.dp)) {
                                    Icon(icon, contentDescription = null, tint = OrchisPrimary, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Text(item.fileName, color = OrchisText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(item.operationType, color = OrchisTextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: PdfViewModel) {
    val history by viewModel.historyState.collectAsStateWithLifecycle()
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Version History", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OrchisText, modifier = Modifier.padding(bottom = 16.dp))
        if (history.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No changes tracked yet.", color = OrchisTextMuted)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OrchisSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.fileName, fontWeight = FontWeight.Bold, color = OrchisText)
                                Text(format.format(Date(item.timestamp)), fontSize = 12.sp, color = OrchisTextMuted)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(item.operationType, color = OrchisPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(item.details, color = OrchisTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        if (document.numberOfPages > 0) {
                            val renderer = PDFRenderer(document)
                            bitmap = renderer.renderImageWithDPI(0, 72f) // Render first page
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "PDF Preview",
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(OrchisCard, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OrchisTextMuted, modifier = Modifier.size(32.dp))
        }
    }
}

fun getOriginalFileName(context: Context, uri: Uri): String {
    var name = "document"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index != -1) {
                name = cursor.getString(index)
            }
        }
    }
    return name.removeSuffix(".pdf")
}

@Composable
fun SettingsScreen(viewModel: PdfViewModel) {
    val context = LocalContext.current
    var isClearing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OrchisText, modifier = Modifier.padding(bottom = 16.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = OrchisSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable {
                viewModel.clearHistory()
                android.widget.Toast.makeText(context, "History cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = OrchisPrimary)
                Spacer(Modifier.width(16.dp))
                Text("Clear History", color = OrchisText, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = OrchisSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable {
                isClearing = true
                Thread {
                    val cacheDirs = listOf("split", "merge", "compress")
                    cacheDirs.forEach { 
                        File(context.cacheDir, it).deleteRecursively()
                    }
                    (context as android.app.Activity).runOnUiThread {
                        isClearing = false
                        android.widget.Toast.makeText(context, "Cached files cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isClearing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrchisPrimary)
                } else {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = OrchisPrimary)
                }
                Spacer(Modifier.width(16.dp))
                Text("Clear Cached Files", color = OrchisText, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SplitScreen(viewModel: PdfViewModel) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var ranges by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { selectedUri = it } }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = OrchisPrimary)
                Spacer(Modifier.height(8.dp))
                Text(if (selectedUri == null) "Tap to Select PDF" else "PDF Selected", color = OrchisText)
            }
        }
        
        selectedUri?.let { uri ->
            PdfPreview(uri = uri, modifier = Modifier.size(100.dp, 140.dp))
        }

        OutlinedTextField(
            value = ranges,
            onValueChange = { ranges = it },
            label = { Text("Chapter Ranges (e.g., 1-3, 4-5, 6)", color = OrchisTextMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OrchisText,
                unfocusedTextColor = OrchisText,
                focusedBorderColor = OrchisPrimary,
                unfocusedBorderColor = OrchisCard
            )
        )

        if (isProcessing) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OrchisPrimary,
                trackColor = OrchisCard
            )
        }

        Button(
            onClick = {
                selectedUri?.let { uri ->
                    isProcessing = true
                    progress = 0f
                    coroutineScope.launch {
                        try {
                            val originalName = getOriginalFileName(context, uri)
                            val outputDir = File(context.cacheDir, "split").apply { mkdirs() }
                            val files = PdfService.splitPdf(context, uri, originalName, ranges, outputDir) { p ->
                                progress = p
                            }
                            val name = "${originalName}_split"
                            viewModel.addHistory(name, "Split", "Split into ${files.size} parts for ranges: $ranges", files.firstOrNull()?.absolutePath)
                            shareFiles(context, files)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                            progress = 0f
                        }
                    }
                }
            },
            enabled = selectedUri != null && ranges.isNotBlank() && !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisPrimary)
        ) {
            if (isProcessing) Text("Processing...", color = OrchisText)
            else Text("Split PDF", color = OrchisText)
        }
    }
}

data class MergeItem(val id: String = UUID.randomUUID().toString(), val uri: Uri)

@Composable
fun MergeScreen(viewModel: PdfViewModel) {
    var selectedItems by remember { mutableStateOf<List<MergeItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedItems = selectedItems + uris.map { MergeItem(uri = it) }
            }
        }
    )

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = OrchisPrimary)
                Spacer(Modifier.height(8.dp))
                Text(if (selectedItems.isEmpty()) "Tap to Select Multiple PDFs" else "${selectedItems.size} PDFs Selected", color = OrchisText)
            }
        }
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(selectedItems, key = { _, item -> item.id }) { index, item ->
                val isDragging = draggingIndex == index
                val zIndex = if (isDragging) 1f else 0f
                val translationY = if (isDragging) draggingOffset else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(zIndex)
                        .graphicsLayer { this.translationY = translationY }
                        .onGloballyPositioned { coordinates ->
                            if (itemHeightPx == 0f) {
                                val spacing = with(density) { 8.dp.toPx() }
                                itemHeightPx = coordinates.size.height.toFloat() + spacing
                            }
                        }
                        .pointerInput(index, itemHeightPx, selectedItems.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    draggingOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggingOffset += dragAmount.y

                                    val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    if (itemHeightPx > 0) {
                                        if (draggingOffset > itemHeightPx && currentIndex < selectedItems.size - 1) {
                                            val newList = selectedItems.toMutableList()
                                            Collections.swap(newList, currentIndex, currentIndex + 1)
                                            selectedItems = newList
                                            draggingIndex = currentIndex + 1
                                            draggingOffset -= itemHeightPx
                                        } else if (draggingOffset < -itemHeightPx && currentIndex > 0) {
                                            val newList = selectedItems.toMutableList()
                                            Collections.swap(newList, currentIndex, currentIndex - 1)
                                            selectedItems = newList
                                            draggingIndex = currentIndex - 1
                                            draggingOffset += itemHeightPx
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    draggingOffset = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    draggingOffset = 0f
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = if (isDragging) OrchisCard.copy(alpha = 0.8f) else OrchisCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DragIndicator, contentDescription = "Drag to reorder", tint = OrchisTextMuted)
                        Spacer(Modifier.width(8.dp))
                        PdfPreview(uri = item.uri, modifier = Modifier.size(40.dp, 56.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(getOriginalFileName(context, item.uri) + ".pdf", color = OrchisText, modifier = Modifier.weight(1f))
                        
                        Column {
                            IconButton(onClick = {
                                if (index > 0) {
                                    val newList = selectedItems.toMutableList()
                                    Collections.swap(newList, index, index - 1)
                                    selectedItems = newList
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = OrchisTextMuted)
                            }
                            IconButton(onClick = {
                                if (index < selectedItems.size - 1) {
                                    val newList = selectedItems.toMutableList()
                                    Collections.swap(newList, index, index + 1)
                                    selectedItems = newList
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = OrchisTextMuted)
                            }
                        }
                        
                        IconButton(onClick = {
                            val newList = selectedItems.toMutableList()
                            newList.removeAt(index)
                            selectedItems = newList
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = OrchisPrimary)
                        }
                    }
                }
            }
        }

        if (isProcessing) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OrchisPrimary,
                trackColor = OrchisCard
            )
        }

        Button(
            onClick = {
                if (selectedItems.size > 1) {
                    isProcessing = true
                    progress = 0f
                    coroutineScope.launch {
                        try {
                            val urisToMerge = selectedItems.map { it.uri }
                            val firstOriginal = getOriginalFileName(context, urisToMerge.first())
                            val generatedName = if (urisToMerge.size == 2) {
                                "${firstOriginal}_and_${getOriginalFileName(context, urisToMerge.last())}_merged.pdf"
                            } else {
                                "${firstOriginal}_and_${urisToMerge.size - 1}_others_merged.pdf"
                            }
                            
                            val outputDir = File(context.cacheDir, "merge").apply { mkdirs() }
                            val outputFile = File(outputDir, generatedName)
                            val file = PdfService.mergePdfs(context, urisToMerge, outputFile) { p ->
                                progress = p
                            }
                            viewModel.addHistory(generatedName, "Merged", "Combined ${urisToMerge.size} PDF documents", file.absolutePath)
                            shareFile(context, file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                            progress = 0f
                        }
                    }
                }
            },
            enabled = selectedItems.size > 1 && !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisPrimary)
        ) {
            if (isProcessing) Text("Processing...", color = OrchisText)
            else Text("Merge PDFs", color = OrchisText)
        }
    }
}

@Composable
fun CompressScreen(viewModel: PdfViewModel) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var compressionLevel by remember { mutableStateOf("Standard") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { selectedUri = it } }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = OrchisPrimary)
                Spacer(Modifier.height(8.dp))
                Text(if (selectedUri == null) "Tap to Select PDF" else "PDF Selected", color = OrchisText)
            }
        }

        selectedUri?.let { uri ->
            PdfPreview(uri = uri, modifier = Modifier.size(140.dp, 196.dp))
            
            Spacer(Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    compressionLevel = if (compressionLevel == "Standard") "Aggressive" else "Standard"
                },
                colors = CardDefaults.cardColors(containerColor = OrchisSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Compression Rate", color = OrchisText, fontWeight = FontWeight.SemiBold)
                        Text(if (compressionLevel == "Standard") "Lossless structure copy" else "Removes more metadata", color = OrchisTextMuted, fontSize = 12.sp)
                    }
                    Text(compressionLevel, color = OrchisPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (isProcessing) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OrchisPrimary,
                trackColor = OrchisCard
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                selectedUri?.let { uri ->
                    isProcessing = true
                    progress = 0f
                    coroutineScope.launch {
                        try {
                            val originalName = getOriginalFileName(context, uri)
                            val outputDir = File(context.cacheDir, "compress").apply { mkdirs() }
                            val outputFile = File(outputDir, "${originalName}_compressed.pdf")
                            val file = PdfService.compressPdf(context, uri, outputFile, compressionLevel) { p ->
                                progress = p
                            }
                            val name = "${originalName}_compressed"
                            viewModel.addHistory(name, "Compressed", "$compressionLevel compression applied.", file.absolutePath)
                            shareFile(context, file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isProcessing = false
                            progress = 0f
                        }
                    }
                }
            },
            enabled = selectedUri != null && !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisPrimary)
        ) {
            if (isProcessing) Text("Processing...", color = OrchisText)
            else Text("Compress PDF", color = OrchisText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), content = content)
    }
}

@Composable
fun BottomNavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) OrchisText else OrchisTextMuted
    val alpha = if (selected) 1f else 0.6f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .background(if (selected) OrchisCard else Color.Transparent, CircleShape)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

fun shareFiles(context: Context, files: List<File>) {
    val uris = files.map {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
    }
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "application/pdf"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDFs"))
}

fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}

@Composable
fun MetadataScreen(viewModel: PdfViewModel) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var isMetadataLoaded by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                selectedUri = it
                coroutineScope.launch {
                    val meta = PdfService.getMetadata(context, it)
                    title = meta.title
                    author = meta.author
                    subject = meta.subject
                    isMetadataLoaded = true
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { launcher.launch(arrayOf("application/pdf")) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = OrchisPrimary)
                Spacer(Modifier.height(8.dp))
                Text(if (selectedUri == null) "Tap to Select PDF" else "Select Another PDF", color = OrchisText)
            }
        }

        selectedUri?.let { uri ->
            PdfPreview(uri = uri, modifier = Modifier.size(100.dp, 140.dp))
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrchisPrimary,
                    unfocusedBorderColor = OrchisCard,
                    focusedTextColor = OrchisText,
                    unfocusedTextColor = OrchisText
                )
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrchisPrimary,
                    unfocusedBorderColor = OrchisCard,
                    focusedTextColor = OrchisText,
                    unfocusedTextColor = OrchisText
                )
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrchisPrimary,
                    unfocusedBorderColor = OrchisCard,
                    focusedTextColor = OrchisText,
                    unfocusedTextColor = OrchisText
                )
            )
        }

        Spacer(Modifier.weight(1f))

        if (isProcessing) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OrchisPrimary,
                trackColor = OrchisCard
            )
        }

        Button(
            onClick = {
                selectedUri?.let { uri ->
                    isProcessing = true
                    progress = 0f
                    coroutineScope.launch {
                        try {
                            val originalName = getOriginalFileName(context, uri)
                            val outputDir = File(context.cacheDir, "metadata").apply { mkdirs() }
                            val outputFile = File(outputDir, "${originalName}_edited.pdf")
                            
                            val meta = PdfMetadata(title.trim(), author.trim(), subject.trim())
                            val file = PdfService.editMetadata(context, uri, meta, outputFile) { p ->
                                progress = p
                            }
                            
                            val name = "${originalName}_edited"
                            viewModel.addHistory(name, "Metadata", "Updated document properties.", file.absolutePath)
                            shareFile(context, file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Error updating metadata", android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            isProcessing = false
                            progress = 0f
                        }
                    }
                }
            },
            enabled = selectedUri != null && isMetadataLoaded && !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OrchisPrimary)
        ) {
            Text(if (isProcessing) "Saving..." else "Save Changes", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
