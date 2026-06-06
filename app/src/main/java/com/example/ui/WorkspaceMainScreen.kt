package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.WorkspaceProject
import com.example.ui.theme.*
import com.example.viewmodel.GPlatesPlate
import com.example.viewmodel.MapchartLegend
import com.example.viewmodel.PaintPath
import com.example.viewmodel.PaintShape
import com.example.viewmodel.PlateLongevity
import com.example.viewmodel.WorkspaceViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceMainScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeWorkspaceTab.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    
    // Dialog state for saving workspace
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveProjectName by remember { mutableStateOf("") }
    
    // Toast status for actions
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Hexagon,
                            contentDescription = "OmniCreate Logo",
                            tint = SleekPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Professional Suite".uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = SleekPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "OMNICREATE STUDIO",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                fontSize = 17.sp,
                                color = SleekOnBackground
                            )
                        }
                    }
                },
                actions = {
                    // Save Workspace Action
                    IconButton(
                        onClick = {
                            saveProjectName = currentProject?.name ?: "New Pro Project"
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("save_workspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save project",
                            tint = SleekPrimary
                        )
                    }
                    
                    // Cloud Sync Trigger Action
                    val isSyncing by viewModel.isCloudSyncing.collectAsState()
                    IconButton(
                        onClick = {
                            viewModel.syncWithCloud()
                            scope.launch {
                                snackbarHostState.showSnackbar("Handshake initiated. Syncing documents to cloud repository...")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Cloud upload",
                            tint = if (isSyncing) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    // Sleek Interface JD User Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SleekPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekOnPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBackground,
                    titleContentColor = SleekOnBackground
                )
            )
        },
        bottomBar = {
            // Adaptive Tab Selector for mobile or tablet
            NavigationBar(
                containerColor = SleekSurface,
                tonalElevation = 8.dp
            ) {
                val tabItems = listOf(
                    Triple("GPLATES", Icons.Filled.Public, "GPlates"),
                    Triple("MAPCHART", Icons.Filled.Map, "Mapchart"),
                    Triple("FLAG", Icons.Filled.Flag, "Flag Creator"),
                    Triple("FRACTAL", Icons.Filled.Sailing, "FractStudio"),
                    Triple("MS_OFFICE", Icons.Filled.Description, "Office Suite"),
                    Triple("MOVIE", Icons.Filled.VideoCameraBack, "Movie Maker")
                )
                
                tabItems.forEach { (key, icon, label) ->
                    NavigationBarItem(
                        selected = activeTab == key,
                        onClick = { viewModel.switchTab(key) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(icon, contentDescription = label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SleekPrimary,
                            selectedTextColor = SleekPrimary,
                            unselectedIconColor = SleekOnSurfaceVariant,
                            unselectedTextColor = SleekOnSurfaceVariant,
                            indicatorColor = SleekPrimaryContainer
                        ),
                        modifier = Modifier.testTag("tab_item_$key")
                    )
                }
            }
        },
        containerColor = SleekBackground
    ) { innerPadding ->
        
        // Modal Save Workspace Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Active Workspace", fontWeight = FontWeight.Bold, color = SleekOnBackground) },
                text = {
                    Column {
                        Text(
                            "Enter a unique name to store this design parameters in the local database.",
                            color = SleekOnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = saveProjectName,
                            onValueChange = { saveProjectName = it },
                            label = { Text("Workspace Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SleekOnBackground,
                                unfocusedTextColor = SleekOnBackground,
                                focusedLabelColor = SleekPrimary,
                                unfocusedLabelColor = SleekOnSurfaceVariant,
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekOutline
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("save_dialog_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                        onClick = {
                            if (saveProjectName.isNotBlank()) {
                                viewModel.saveWorkspaceProjectToDb(saveProjectName)
                                showSaveDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Workspace '$saveProjectName' saved successfully.")
                                }
                            }
                        },
                        modifier = Modifier.testTag("confirm_save_button")
                    ) {
                        Text("Confirm Preserve")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = SleekPrimary)
                    }
                },
                containerColor = SleekSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left Panel (Projects / Backups) - visible on adaptive screens, scrollable drawer layout
            WorkspaceSidebar(
                allProjects = allProjects,
                currentProject = currentProject,
                onLoadProject = { viewModel.loadWorkspaceProjectFromDb(it) },
                onDeleteProject = { viewModel.deleteWorkspaceProject(it) },
                viewModel = viewModel,
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(SleekSurface)
                    .border(1.dp, SleekOutline)
            )

            // Right Viewport of core chosen tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }
                ) { targetTab ->
                    when (targetTab) {
                        "GPLATES" -> GPlatesWorkspace(viewModel)
                        "MAPCHART" -> MapchartWorkspace(viewModel)
                        "FLAG" -> FlagCreatorWorkspace(viewModel)
                        "FRACTAL" -> FractalStudioWorkspace(viewModel)
                        "MS_OFFICE" -> MsOfficeSuiteWorkspace(viewModel)
                        "MOVIE" -> MovieMakerWorkspace(viewModel)
                        else -> GPlatesWorkspace(viewModel)
                    }
                }
            }
        }
    }
}

// SIDEBAR COMPOSABLE FOR RECENT PROJECTS AND BACKUPS
@Composable
fun WorkspaceSidebar(
    allProjects: List<WorkspaceProject>,
    currentProject: WorkspaceProject?,
    onLoadProject: (WorkspaceProject) -> Unit,
    onDeleteProject: (WorkspaceProject) -> Unit,
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val cloudProgress by viewModel.cloudSyncProgress.collectAsState()
    val cloudStatus by viewModel.cloudSyncStatus.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()

    Column(
        modifier = modifier.padding(12.dp)
    ) {
        Text(
            text = "PROJECT FILES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = SleekPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (allProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SleekBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, SleekOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved projects.\nPreserve project with top save icon.",
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(allProjects) { _, project ->
                    val isSelected = currentProject?.id == project.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onLoadProject(project) }
                            .testTag("project_item_${project.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SleekPrimaryContainer else SleekBackground
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) SleekPrimary else SleekOutline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SleekOnPrimaryContainer else SleekOnBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Age: ${project.gplatesAge} Ma | Flag: ${project.flagEmblemType}",
                                    fontSize = 10.sp,
                                    color = if (isSelected) SleekOnPrimaryContainer.copy(alpha = 0.75f) else SleekOnSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteProject(project) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete workspace",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document backups & Sync status - Restyled like the gorgeous "Office 365 Sync" in sleek HTML
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekPrimaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SleekOnPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudQueue,
                            contentDescription = "Cloud Status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CLOUD PROJECT SYNC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SleekOnPrimaryContainer,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = cloudStatus,
                    fontSize = 10.sp,
                    color = SleekOnPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                )
                if (isSyncing) {
                    LinearProgressIndicator(
                        progress = { cloudProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        color = SleekPrimary,
                        trackColor = Color(0xFFE2E7F0)
                    )
                }
            }
        }
    }
}

// 1. GPLATES TECTIONICS TIMELINE WORKSPACE
@Composable
fun GPlatesWorkspace(viewModel: WorkspaceViewModel) {
    val plateAge by viewModel.gplatesAge.collectAsState()
    val selectedStatPlate by viewModel.selectedStatPlate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TECTONIC PLATE DRIFT SIMULATION (GPlates)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary
                )
                Text(
                    "Drag the Geological Age Slider to drift, rift, subduct, and collide major continental plates.",
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // The Tectonic Map Canvas (Modern Sleek Dark Viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0061A4).copy(alpha = 0.25f), // Blue accent glow
                                    Color(0xFF3B714B).copy(alpha = 0.12f), // Green accent glow
                                    SleekDarkViewport
                                ),
                                radius = 700f
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp))
                ) {
                    // Draw grid meridians and tectonic boundaries
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridLinesCount = 8
                        // Draw horizontal meridians
                        for (i in 0..gridLinesCount) {
                            val y = size.height * (i.toFloat() / gridLinesCount)
                            drawLine(
                                color = Color(0xFF32353A),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                        // Draw vertical longitudes
                        for (i in 0..gridLinesCount) {
                            val x = size.width * (i.toFloat() / gridLinesCount)
                            drawLine(
                                color = Color(0xFF32353A),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // Render dynamic plates
                    viewModel.gplatesPlatesList.forEach { plate ->
                        // Interpolate coordinates dynamically
                        // Interpolant factor t: 0.0 is Modern, 1.0 is Cambrian 540 Ma.
                        val t = plateAge / 540f
                        
                        // Collision offset (Pangea convergence around 300Ma)
                        // At ~300 Ma (Permian), they squeeze closely together (convergent rate).
                        val distanceToPangeaFactor = (plateAge - 300f).absoluteValue / 300f
                        val squeezeIntensity = if (plateAge in 200f..400f) 0.35f else 1f
                        
                        val interpX = (plate.initialX * t) + (plate.targetX * (1f - t))
                        val interpY = (plate.initialY * t) + (plate.targetY * (1f - t))
                        val finalRotation = (plate.rotationStart * t) + (plate.rotationEnd * (1f - t))

                        // Render on Box centered
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = (interpX * squeezeIntensity).dp,
                                    y = (interpY * squeezeIntensity).dp
                                )
                                .width(plate.sizeWidth.dp)
                                .height(plate.sizeHeight.dp)
                                .background(
                                    color = plate.color.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (plate.plateType == "Continental") Color.White.copy(alpha = 0.9f) else SleekPrimaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = plate.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = plate.plateType,
                                        fontSize = 8.sp,
                                        color = if (plate.plateType == "Continental") Color(0xFFF59E0B) else Color(0xFF38BDF8)
                                    )
                                    Icon(
                                        imageVector = if (plate.plateType == "Continental") Icons.Filled.Landscape else Icons.Filled.Waves,
                                        contentDescription = "type",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Equator line indicator
                    HorizontalDivider(
                        color = Color(0xFFEF4444).copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        "EQUATOR",
                        color = Color(0xFFEF4444).copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeline Controller
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Geological Era: ${getGeologicEraName(plateAge)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SleekOnBackground
                    )
                    Text(
                        text = "${plateAge.toInt()} Ma",
                        color = SleekPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = plateAge,
                    onValueChange = { viewModel.updateGPlatesAge(it) },
                    valueRange = 0f..540f,
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPrimary,
                        activeTrackColor = SleekPrimary,
                        inactiveTrackColor = SleekOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("gplates_age_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 Ma (Modern)", fontSize = 10.sp, color = SleekOnSurfaceVariant)
                    Text("250 Ma (Triassic)", fontSize = 10.sp, color = SleekOnSurfaceVariant)
                    Text("540 Ma (Cambrian)", fontSize = 10.sp, color = SleekOnSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Figure 2 Research Statistics Graph: Plate Longevity graph!
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PLATE PLOTTER: LONGEVITY vs AGE OF ORIGIN",
                    color = SleekPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                     text = "Research stats mapping lithospheric survival profiles (Table 1). Tap bubbles to inspect.",
                    color = SleekOnSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Graph Draw area (Sleek Dark Viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6B5778).copy(alpha = 0.2f), // Secondary theme violet glow
                                    SleekDarkViewport
                                ),
                                radius = 500f
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
                        // Graph parameters
                        // originAge: max 1200 Ma, lifespan: max 900 Ma.
                        val margin = 35f
                        val w = size.width - margin * 2
                        val h = size.height - margin * 2
                        
                        // Draw Axes
                        drawLine(Color(0xFF535F70), Offset(margin, margin), Offset(margin, size.height - margin), 1.5f)
                        drawLine(Color(0xFF535F70), Offset(margin, size.height - margin), Offset(size.width - margin, size.height - margin), 1.5f)
                    }

                    // Render interactive custom elements
                    viewModel.geologicalStatsList.forEach { stat ->
                        // Normalize positions
                        // origin: 0 Ma to 1200 Ma -> x percentage
                        val pctX = stat.ageBirth / 1200f
                        val pctY = stat.lifespan / 900f
                        
                        val isSelected = selectedStatPlate?.name == stat.name

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(
                                    x = ((pctX * 180f) + 30f).dp,
                                    y = (-((pctY * 120f) + 20f)).dp
                                )
                                .size(if (isSelected) 22.dp else 14.dp)
                                .background(
                                    color = if (stat.type == "Oceanic") SleekPrimary else SleekSecondary,
                                    shape = CircleShape
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.selectStatPlate(if (isSelected) null else stat)
                                }
                        )
                    }

                    Text("Longevity (Ma) ⇧", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(start = 12.dp, top = 12.dp))
                    Text("Birth Age (Ma) ⇨", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp))
                }

                // Selected Details
                selectedStatPlate?.let { stat ->
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(SleekPrimaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, SleekOutline, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stat.name, fontWeight = FontWeight.Bold, color = SleekOnPrimaryContainer, fontSize = 13.sp)
                                Text("Region Base: ${stat.region}", fontSize = 11.sp, color = SleekOnPrimaryContainer.copy(alpha = 0.8f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Born: ${stat.ageBirth} Ma", fontSize = 11.sp, color = SleekPrimary, fontWeight = FontWeight.ExtraBold)
                                Text("Lifespan: ${stat.lifespan} Ma", fontSize = 10.sp, color = SleekSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. MAPCHART WORKSPACE
@Composable
fun MapchartWorkspace(viewModel: WorkspaceViewModel) {
    val mapColors by viewModel.mapchartColors.collectAsState()
    val subColors by viewModel.mapchartSubdivisionColors.collectAsState()
    val activeBrushCol by viewModel.activeBrushColor.collectAsState()
    val subsEnabled by viewModel.mapchartSubdivisionsEnabled.collectAsState()
    val legends by viewModel.mapchartLegends.collectAsState()
    
    var descText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "THEMATIC INTERACTIVE MAPBUILDER (Mapchart)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekSecondary
                )
                Text(
                    "Pick colors from the dynamic index below and tap on mapped sections to define research zones.",
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subdivisions toggle - polished look
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, SleekOutline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enable World Subdivisions Map",
                        fontSize = 11.sp,
                        color = SleekOnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = subsEnabled,
                        onCheckedChange = { viewModel.toggleSubdivisions(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SleekSecondary,
                            checkedTrackColor = SleekPrimaryContainer,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFFE2E7F0)
                        ),
                        modifier = Modifier.testTag("subdivisions_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Map Viewport (Beautiful vector geometric representations - Sleek Dark Viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6B5778).copy(alpha = 0.25f), // Violet glow
                                    SleekDarkViewport
                                ),
                                radius = 700f
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!subsEnabled) {
                        // Draw Standard World Continents
                        Spacer(modifier = Modifier.fillMaxSize().drawBehind {
                            // North America
                            drawRoundRect(
                                color = mapColors["north_america"] ?: Color.Gray,
                                topLeft = Offset(40f, 40f),
                                size = Size(140f, 100f),
                                cornerRadius = CornerRadius(16f, 16f)
                            )
                            // Greenland
                            drawRoundRect(
                                color = mapColors["greenland"] ?: Color.Gray,
                                topLeft = Offset(210f, 20f),
                                size = Size(60f, 40f),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            // South America
                            drawRoundRect(
                                color = mapColors["south_america"] ?: Color.Gray,
                                topLeft = Offset(110f, 160f),
                                size = Size(90f, 140f),
                                cornerRadius = CornerRadius(24f, 24f)
                            )
                            // Europe
                            drawRoundRect(
                                color = mapColors["europe"] ?: Color.Gray,
                                topLeft = Offset(310f, 50f),
                                size = Size(100f, 90f),
                                cornerRadius = CornerRadius(16f, 16f)
                            )
                            // Africa
                            drawRoundRect(
                                color = mapColors["africa"] ?: Color.Gray,
                                topLeft = Offset(320f, 160f),
                                size = Size(120f, 140f),
                                cornerRadius = CornerRadius(20f, 20f)
                            )
                            // Asia
                            drawRoundRect(
                                color = mapColors["asia"] ?: Color.Gray,
                                topLeft = Offset(430f, 40f),
                                size = Size(200f, 150f),
                                cornerRadius = CornerRadius(24f, 24f)
                            )
                            // Australia
                            drawRoundRect(
                                color = mapColors["australia"] ?: Color.Gray,
                                topLeft = Offset(520f, 210f),
                                size = Size(90f, 80f),
                                cornerRadius = CornerRadius(16f, 16f)
                            )
                        })

                        // Hotspots to click to change color
                        Box(modifier = Modifier.fillMaxSize()) {
                            val mapLabels = listOf(
                                "north_america" to Pair(70, 70),
                                "greenland" to Pair(220, 30),
                                "south_america" to Pair(130, 200),
                                "europe" to Pair(330, 80),
                                "africa" to Pair(350, 210),
                                "asia" to Pair(480, 100),
                                "australia" to Pair(540, 240)
                            )
                            mapLabels.forEach { (id, coords) ->
                                Button(
                                    onClick = { viewModel.updateMapRegionColor(id, activeBrushCol) },
                                    modifier = Modifier
                                        .offset(x = coords.first.dp, y = coords.second.dp)
                                        .height(28.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(id.uppercase().replace("_"," "), fontSize = 7.sp, color = Color.White)
                                }
                            }
                        }
                    } else {
                        // World Subdivisions (Divided segments)
                        Spacer(modifier = Modifier.fillMaxSize().drawBehind {
                            // Draw subdivided cells
                            drawRect(color = subColors["sub_canada"] ?: Color.Gray, topLeft = Offset(30f, 30f), size = Size(70f, 60f))
                            drawRect(color = subColors["sub_usa_west"] ?: Color.Gray, topLeft = Offset(30f, 95f), size = Size(60f, 45f))
                            drawRect(color = subColors["sub_usa_east"] ?: Color.Gray, topLeft = Offset(95f, 95f), size = Size(65f, 45f))
                            drawRect(color = subColors["sub_mexico"] ?: Color.Gray, topLeft = Offset(50f, 145f), size = Size(70f, 40f))
                            // Europe sub
                            drawRect(color = subColors["sub_scandinavia"] ?: Color.Gray, topLeft = Offset(310f, 25f), size = Size(90f, 40f))
                            drawRect(color = subColors["sub_western_europe"] ?: Color.Gray, topLeft = Offset(310f, 70f), size = Size(90f, 60f))
                            // Asia
                            drawRect(color = subColors["sub_siberia"] ?: Color.Gray, topLeft = Offset(430f, 30f), size = Size(160f, 50f))
                            drawRect(color = subColors["sub_china"] ?: Color.Gray, topLeft = Offset(430f, 85f), size = Size(100f, 60f))
                            drawRect(color = subColors["sub_india"] ?: Color.Gray, topLeft = Offset(535f, 85f), size = Size(60f, 60f))
                        })

                        Box(modifier = Modifier.fillMaxSize()) {
                            val subLabels = listOf(
                                "sub_canada" to Pair(40, 45),
                                "sub_usa_west" to Pair(35, 100),
                                "sub_usa_east" to Pair(100, 100),
                                "sub_mexico" to Pair(60, 150),
                                "sub_scandinavia" to Pair(315, 30),
                                "sub_western_europe" to Pair(315, 80),
                                "sub_siberia" to Pair(450, 40),
                                "sub_china" to Pair(440, 95),
                                "sub_india" to Pair(540, 95)
                            )
                            subLabels.forEach { (id, coords) ->
                                Button(
                                    onClick = { viewModel.updateSubdivisionColor(id, activeBrushCol) },
                                    modifier = Modifier
                                        .offset(x = coords.first.dp, y = coords.second.dp)
                                        .height(24.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.62f)),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(id.replace("sub_","").uppercase(), fontSize = 6.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brush color palette selectors
                Text("Select Tool Brush Color:", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val brushColorsList = listOf(
                        Color(0xFFEF4444), // Coral Red
                        Color(0xFFF59E0B), // Amber Orange
                        Color(0xFF10B981), // Emerald Green
                        Color(0xFF3B82F6), // Indigo Blue
                        Color(0xFF8B5CF6), // Custom Violet
                        Color(0xFFD946EF), // Neon Fuchsia
                        Color(0xFFEC4899), // Hot Pink
                        Color(0xFF64748B)  // Carbon Slate
                    )
                    brushColorsList.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (activeBrushCol == color) 3.dp else 1.dp,
                                    color = if (activeBrushCol == color) SleekOnBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updatePaintColor(color) }
                                .testTag("brush_color_picker_${brushColorsList.indexOf(color)}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Thematic Legends Creator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MAP LEGEND INDEX CREATOR", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Enter Legend Indicator description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekOnBackground,
                            unfocusedTextColor = SleekOnBackground,
                            focusedLabelColor = SleekPrimary,
                            unfocusedLabelColor = SleekOnSurfaceVariant,
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekOutline
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("legend_desc_input")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary),
                        onClick = {
                            if (descText.isNotBlank()) {
                                viewModel.addMapchartLegend(activeBrushCol, descText)
                                descText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_legend_button")
                    ) {
                        Text("Add Index")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Render current legends
                legends.forEachIndexed { idx, leg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(SleekBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, SleekOutline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp).background(leg.color, RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(leg.label, fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { viewModel.removeMapchartLegend(idx) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// 3. FLAG CREATOR WORKSPACE
@Composable
fun FlagCreatorWorkspace(viewModel: WorkspaceViewModel) {
    val flagRatio by viewModel.flagRatio.collectAsState()
    val flagPattern by viewModel.flagPattern.collectAsState()
    val baseCol by viewModel.flagPrimaryColor.collectAsState()
    val secondCol by viewModel.flagSecondaryColor.collectAsState()
    val tertCol by viewModel.flagTertiaryColor.collectAsState()
    val emblemType by viewModel.flagEmblemType.collectAsState()
    val emblemScale by viewModel.flagEmblemScale.collectAsState()
    val emblemX by viewModel.flagEmblemX.collectAsState()
    val emblemY by viewModel.flagEmblemY.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "VECTOR FLAG STUDIO (Flag Creator)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekTertiary
                )
                Text(
                    "Design scalable vector flags with multi-band configurations, stripe grids, and customizable central emblems.",
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // RENDER FLAGS CANVAS ON SCREEN (Premium Dark Viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B714B).copy(alpha = 0.25f), // Green glow
                                    SleekDarkViewport
                                ),
                                radius = 600f
                            ),
                            RoundedCornerShape(32.dp)
                        )
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Responsive Flag shape depending on user customizable Ratio
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height((220.dp / flagRatio))
                            .background(baseCol)
                            .border(1.dp, Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing base Patterns according to choice
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            when (flagPattern) {
                                "Stripes Vertical" -> {
                                    drawRect(color = baseCol, topLeft = Offset(0f, 0f), size = Size(w / 3f, h))
                                    drawRect(color = secondCol, topLeft = Offset(w / 3f, 0f), size = Size(w / 3f, h))
                                    drawRect(color = tertCol, topLeft = Offset((w * 2f) / 3f, 0f), size = Size(w / 3f, h))
                                }
                                "Stripes Horizontal" -> {
                                    drawRect(color = baseCol, topLeft = Offset(0f, 0f), size = Size(w, h / 3f))
                                    drawRect(color = secondCol, topLeft = Offset(0f, h / 3f), size = Size(w, h / 3f))
                                    drawRect(color = tertCol, topLeft = Offset(0f, (h * 2f) / 3f), size = Size(w, h / 3f))
                                }
                                "Nordic Cross" -> {
                                    drawRect(color = baseCol, topLeft = Offset(0f, 0f), size = Size(w, h))
                                    // Horizontal
                                    drawRect(color = secondCol, topLeft = Offset(0f, h * 0.35f), size = Size(w, h * 0.3f))
                                    // Vertical
                                    drawRect(color = secondCol, topLeft = Offset(w * 0.35f, 0f), size = Size(w * 0.15f, h))
                                }
                                "Canton Corner" -> {
                                    drawRect(color = baseCol, topLeft = Offset(0f, 0f), size = Size(w, h))
                                    drawRect(color = secondCol, topLeft = Offset(0f, 0f), size = Size(w * 0.45f, h * 0.5f))
                                    drawRect(color = tertCol, topLeft = Offset(w * 0.45f, 0f), size = Size(w * 0.55f, 15f))
                                }
                                "Tricolor" -> {
                                    drawRect(color = baseCol, topLeft = Offset(0f, 0f), size = Size(w, h))
                                    val path = Path().apply {
                                        moveTo(0f, 0f)
                                        lineTo(w * 0.5f, h * 0.5f)
                                        lineTo(0f, h)
                                        close()
                                    }
                                    drawPath(path, color = secondCol)
                                }
                            }
                        }

                        // Overlay Emblem position
                        if (emblemType != "None") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = (220f * emblemX - 16f).dp,
                                        y = (((220f / flagRatio) * emblemY) - 16f).dp
                                    )
                                    .size((48.dp * emblemScale))
                                    .background(tertCol, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (emblemType) {
                                        "Star" -> Icons.Filled.Star
                                        "Golden Sun" -> Icons.Filled.Brightness5
                                        "Crescent" -> Icons.Filled.Bedtime
                                        "Crest Shield" -> Icons.Filled.Shield
                                        else -> Icons.Filled.MilitaryTech
                                    },
                                    contentDescription = "Emblem Symbol",
                                    tint = Color.White,
                                    modifier = Modifier.fillMaxSize(0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ratio & Pattern configurations
                Text("FLAG CONFIGURATIONS", fontWeight = FontWeight.Bold, color = SleekOnBackground, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Flag Proportion Ratio: ${String.format("%.1f", flagRatio)}", fontSize = 11.sp, color = SleekOnSurfaceVariant)
                        Slider(
                            value = flagRatio,
                            onValueChange = { viewModel.updateFlagRatio(it) },
                            valueRange = 1.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekTertiary,
                                activeTrackColor = SleekTertiary,
                                inactiveTrackColor = SleekOutline
                            ),
                            modifier = Modifier.testTag("flag_ratio_slider")
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emblem Insignia Scale: ${String.format("%.1f", emblemScale)}", fontSize = 11.sp, color = SleekOnSurfaceVariant)
                        Slider(
                            value = emblemScale,
                            onValueChange = { viewModel.updateFlagEmblemScale(it) },
                            valueRange = 0.2f..0.8f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekTertiary,
                                activeTrackColor = SleekTertiary,
                                inactiveTrackColor = SleekOutline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Configuration Preset Pattern dropdown selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val patternPresets = listOf("Stripes Vertical", "Stripes Horizontal", "Nordic Cross", "Canton Corner", "Tricolor")
                    patternPresets.forEach { preset ->
                        FilterChip(
                            selected = flagPattern == preset,
                            onClick = { viewModel.updateFlagPattern(preset) },
                            label = { Text(preset, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekTertiary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekBackground,
                                labelColor = SleekOnSurfaceVariant
                            )
                        )
                    }
                }

                HorizontalDivider(color = SleekOutline, modifier = Modifier.padding(vertical = 12.dp))

                // Emblem Type selectors
                Text("Emblem Overlay Insignia Symbol:", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emblems = listOf("Star", "Golden Sun", "Crescent", "Crest Shield", "None")
                    emblems.forEach { emb ->
                        FilterChip(
                            selected = emblemType == emb,
                            onClick = { viewModel.updateFlagEmblemType(emb) },
                            label = { Text(emb, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekTertiary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekBackground,
                                labelColor = SleekOnSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vector Export Options buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = SleekTertiary, contentColor = SleekOnPrimary),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(viewModel.generateVectorSvgString()))
                            showExportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("export_flag_vector_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy text")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy High-Res SVG Vector")
                    }
                }
            }
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Vector Elements", color = SleekOnBackground, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "The flag details have been correctly serialized into clean, scalable SVG markup code.",
                            color = SleekOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(SleekDarkViewport, RoundedCornerShape(12.dp))
                                .border(1.dp, SleekOutline, RoundedCornerShape(12.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = viewModel.generateVectorSvgString(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = SleekOnPrimary)
                    ) {
                        Text("Done")
                    }
                },
                containerColor = SleekSurface
            )
        }
    }
}

// 4. MATHEMATICAL FRACTAL CANVASES STUDIO (Mandelbrot Explorer)
@Composable
fun FractalStudioWorkspace(viewModel: WorkspaceViewModel) {
    val fractalBmp by viewModel.fractalBitmap.collectAsState()
    val isCalculating by viewModel.isFractalCalculating.collectAsState()
    val zoom by viewModel.fractalZoom.collectAsState()
    val maxIter by viewModel.fractalMaxIterations.collectAsState()
    val palette by viewModel.fractalPaletteName.collectAsState()
    val cx by viewModel.fractalCenterX.collectAsState()
    val cy by viewModel.fractalCenterY.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "INFINITE MATHEMATICAL FRACTALS (Mandelbrot)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary
                )
                Text(
                    "Explore complex mathematical symmetries. Move zoom or coordinates to recalculate pixel values (Zn+1 = Zn² + C).",
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // RENDER BITMAP CANVAS (Modern Sleek Rounded Frame)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black)
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (fractalBmp != null) {
                        Image(
                            bitmap = fractalBmp!!,
                            contentDescription = "Mandelbrot Fractal",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isCalculating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SleekPrimary)
                        }
                    }

                    // On-screen Coordinates Text HUD (Positioned elegantly inline with 32dp card frame)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topEnd = 16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("X: ${String.format("%.4f", cx)}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f), fontFamily = FontFamily.Monospace)
                        Text("Y: ${String.format("%.4f", cy)}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f), fontFamily = FontFamily.Monospace)
                        Text("Zoom: ${String.format("%.1f", zoom)}x", fontSize = 9.sp, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tactile Directional Panning CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // D-PAD Controller Panel
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(SleekBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, SleekOutline, RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.updateFractalSettings(zoom, cx, cy - 0.15 / zoom, maxIter, palette) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Pan Up", tint = SleekPrimary)
                        }
                        Row {
                            IconButton(
                                onClick = { viewModel.updateFractalSettings(zoom, cx - 0.15 / zoom, cy, maxIter, palette) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Pan Left", tint = SleekPrimary)
                            }
                            Spacer(modifier = Modifier.width(28.dp))
                            IconButton(
                                onClick = { viewModel.updateFractalSettings(zoom, cx + 0.15 / zoom, cy, maxIter, palette) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Pan Right", tint = SleekPrimary)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.updateFractalSettings(zoom, cx, cy + 0.15 / zoom, maxIter, palette) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Pan Down", tint = SleekPrimary)
                        }
                    }

                    // Iterations and Zoom side bars
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Zoom Level Control: ${String.format("%.2f", zoom)}x", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.updateFractalSettings((zoom / 1.5).coerceAtLeast(0.5), cx, cy, maxIter, palette) },
                                modifier = Modifier.testTag("fractal_zoom_out_button")
                            ) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Zoom Out", tint = SleekPrimary)
                            }
                            Slider(
                                value = zoom.toFloat(),
                                onValueChange = { viewModel.updateFractalSettings(it.toDouble(), cx, cy, maxIter, palette) },
                                valueRange = 0.5f..8.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SleekPrimary,
                                    activeTrackColor = SleekPrimary,
                                    inactiveTrackColor = SleekOutline
                                ),
                                modifier = Modifier.weight(1f).testTag("fractal_zoom_slider")
                            )
                            IconButton(
                                onClick = { viewModel.updateFractalSettings(zoom * 1.5, cx, cy, maxIter, palette) },
                                modifier = Modifier.testTag("fractal_zoom_in_button")
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Zoom In", tint = SleekPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Resolution Complexity (Max Iter): $maxIter", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                        Slider(
                            value = maxIter.toFloat(),
                            onValueChange = { viewModel.updateFractalSettings(zoom, cx, cy, it.toInt(), palette) },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekPrimary,
                                activeTrackColor = SleekPrimary,
                                inactiveTrackColor = SleekOutline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Palette selection chips
                Text("Creative Color Profile:", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorProfiles = listOf("Neon Fire", "Deep Deep Ocean", "Emerald Forest", "Cosmic Grayscale")
                    colorProfiles.forEach { prof ->
                        FilterChip(
                            selected = palette == prof,
                            onClick = { viewModel.updateFractalSettings(zoom, cx, cy, maxIter, prof) },
                            label = { Text(prof, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekBackground,
                                labelColor = SleekOnSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

// 5. MS OFFICE WORKSPACE (Paint Canvas + Project Word doc + Slide presentations)
@Composable
fun MsOfficeSuiteWorkspace(viewModel: WorkspaceViewModel) {
    val paintPaths by viewModel.paintPaths.collectAsState()
    val paintShapes by viewModel.paintShapes.collectAsState()
    val activeTool by viewModel.activePaintTool.collectAsState()
    val brushSize by viewModel.paintBrushSize.collectAsState()
    val brushColor by viewModel.paintBrushColorNum.collectAsState()
    
    val currentReportDesc by viewModel.wordReportDesc.collectAsState()
    val pptTheme by viewModel.activePowerpointTheme.collectAsState()
    val selectedSlideIndex by viewModel.selectedPowerpointSlideIndex.collectAsState()

    var activeSubTab by remember { mutableStateOf("MS_PAINT") } // MS_PAINT, MS_WORD, MS_POWERPOINT

    Column(modifier = Modifier.fillMaxSize()) {
        // Office 365 Sub-Tab Navigator - Styled cleanly in harmony with Sleek Navigation
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "MS_PAINT" -> 0
                "MS_WORD" -> 1
                else -> 2
            },
            containerColor = SleekSurface,
            contentColor = SleekPrimary
        ) {
            Tab(
                selected = activeSubTab == "MS_PAINT",
                onClick = { activeSubTab = "MS_PAINT" },
                text = { Text("Paint Ink Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Palette, contentDescription = "paint") },
                modifier = Modifier.testTag("subtab_ms_paint")
            )
            Tab(
                selected = activeSubTab == "MS_WORD",
                onClick = { activeSubTab = "MS_WORD" },
                text = { Text("Word Doc Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Article, contentDescription = "word") },
                modifier = Modifier.testTag("subtab_ms_word")
            )
            Tab(
                selected = activeSubTab == "MS_POWERPOINT",
                onClick = { activeSubTab = "MS_POWERPOINT" },
                text = { Text("PowerPoint Presentation", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Filled.Slideshow, contentDescription = "ppt") },
                modifier = Modifier.testTag("subtab_ms_powerpoint")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeSubTab) {
            "MS_PAINT" -> {
                // MS Paint interactive view
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekOutline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("O365 INTERACTIVE PAINT STUDIO", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = SleekPrimary)
                                Text("Pick brush properties and draw on the white canvas directly.", fontSize = 10.sp, color = SleekOnSurfaceVariant)
                            }
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A), contentColor = Color.White),
                                onClick = { viewModel.clearPaintCanvas() },
                                modifier = Modifier.testTag("clear_paint_button")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Paint Canvas Section (Sleek Clean White Draw Canvas Frame with 32dp corners)
                        var currentPoints = remember { mutableStateListOf<Offset>() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White)
                                .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp))
                                .pointerInput(activeTool) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints.clear()
                                            currentPoints.add(offset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentPoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (activeTool == "BRUSH" || activeTool == "ERASER") {
                                                viewModel.addStrokePath(
                                                    PaintPath(
                                                        points = currentPoints.toList(),
                                                        color = if (activeTool == "ERASER") Color.White else brushColor,
                                                        strokeWidth = brushSize
                                                    )
                                                )
                                            } else {
                                                // Vector Shape insertion
                                                if (currentPoints.isNotEmpty()) {
                                                    viewModel.addPaintShape(
                                                        PaintShape(
                                                            type = activeTool.replace("SHAPE_",""),
                                                            x = currentPoints.first().x,
                                                            y = currentPoints.first().y,
                                                            size = 50f,
                                                            color = brushColor
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw saved strokes
                                paintPaths.forEach { stroke ->
                                    if (stroke.points.size > 1) {
                                        val path = Path().apply {
                                            moveTo(stroke.points.first().x, stroke.points.first().y)
                                            for (i in 1 until stroke.points.size) {
                                                lineTo(stroke.points[i].x, stroke.points[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = stroke.color,
                                            style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }

                                // Draw live points
                                if (currentPoints.size > 1) {
                                    val livePath = Path().apply {
                                        moveTo(currentPoints.first().x, currentPoints.first().y)
                                        for (i in 1 until currentPoints.size) {
                                            lineTo(currentPoints[i].x, currentPoints[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = livePath,
                                        color = if (activeTool == "ERASER") Color.White else brushColor,
                                        style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                // Draw vector Shapes
                                paintShapes.forEach { shape ->
                                    val sizeVal = shape.size
                                    when (shape.type) {
                                        "RECT" -> drawRect(color = shape.color, topLeft = Offset(shape.x, shape.y), size = Size(sizeVal * 1.5f, sizeVal))
                                        "CIRCLE" -> drawCircle(color = shape.color, radius = sizeVal / 2f, center = Offset(shape.x, shape.y))
                                        "TRIANGLE" -> {
                                            val trianglePath = Path().apply {
                                                moveTo(shape.x, shape.y - sizeVal / 2f)
                                                lineTo(shape.x - sizeVal / 2f, shape.y + sizeVal / 2f)
                                                lineTo(shape.x + sizeVal / 2f, shape.y + sizeVal / 2f)
                                                close()
                                            }
                                            drawPath(trianglePath, color = shape.color)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Text("Select Tool Preset Ink / Shape:", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tools = listOf("BRUSH", "ERASER", "SHAPE_RECT", "SHAPE_CIRCLE", "SHAPE_TRIANGLE")
                            tools.forEach { t ->
                                FilterChip(
                                    selected = activeTool == t,
                                    onClick = { viewModel.updatePaintTool(t) },
                                    label = { Text(t.replace("SHAPE_","").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = SleekBackground,
                                        labelColor = SleekOnSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            "MS_WORD" -> {
                // MS Word Document text editor
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekOutline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("MS WORD DOCUMENT REPORT BUILDER", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SleekPrimary)
                        Text("Analyze study targets, tectonic drifts, and flag symbolism in an official O365 report.", fontSize = 11.sp, color = SleekOnSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Rich Paper style layout representation
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, SleekOutline, RoundedCornerShape(16.dp)),
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp)
                            ) {
                                // Formal Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("OMNICREATE CORP RESEARCH", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF475569))
                                    Text("DOCUMENT PROTOCOL 305", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 2.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // Document content input
                                OutlinedTextField(
                                    value = currentReportDesc,
                                    onValueChange = { viewModel.updateWordReport(it) },
                                    label = { Text("Protocol Records Description (O365 Editable File)") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFF1E293B),
                                        unfocusedTextColor = Color(0xFF1E293B),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = SleekPrimary,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 20.sp),
                                    modifier = Modifier.fillMaxWidth().height(260.dp).testTag("word_report_text_editor")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Automated Summary log table representation
                                Text("SYSTEM GENERATED EXPORT AUDIT LOGS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                TableAuditLogs()
                            }
                        }
                    }
                }
            }

            "MS_POWERPOINT" -> {
                // MS PowerPoint presentation views
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekOutline),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("MS POWERPOINT CREATIVE SLIDE DECK", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SleekPrimary)
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Presentation themes options
                            Text("Active Master Deck Theme:", fontSize = 11.sp, color = SleekOnSurfaceVariant, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val pptThemes = listOf("Dark Executive", "Clean Academic", "Modern Creative")
                                pptThemes.forEach { th ->
                                    FilterChip(
                                        selected = pptTheme == th,
                                        onClick = { viewModel.updatePowerpointTheme(th) },
                                        label = { Text(th, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SleekPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = SleekBackground,
                                            labelColor = SleekOnSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PowerPoint layout with left slide-nav and main slide presentation canvas
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            // Slide Deck navigation thumbnails
                            Column(
                                modifier = Modifier
                                    .width(76.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val slideNames = listOf("Intro", "Drift Plan", "Mapping", "Flags", "FractMath")
                                slideNames.forEachIndexed { sIdx, name ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(45.dp)
                                            .background(
                                                if (selectedSlideIndex == sIdx) SleekPrimary else SleekBackground,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedSlideIndex == sIdx) SleekPrimary else SleekOutline,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectPowerpointSlide(sIdx) }
                                            .testTag("ppt_slide_nav_$sIdx"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSlideIndex == sIdx) Color.White else SleekOnSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Main Slide Deck Canvas (Beautiful representation in 32dp vector viewport style)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (pptTheme == "Dark Executive") Color(0xFF0F172A) else if (pptTheme == "Clean Academic") Color(0xFFF1F5F9) else Color(0xFFFFE8EC),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .border(4.dp, SleekOutlineVariant, RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor = if (pptTheme == "Clean Academic") Color(0xFF0F172A) else if (pptTheme == "Dark Executive") Color.White else Color(0xFFBA1A1A)
                                val subTextColor = if (textColor == Color.White) Color.White.copy(alpha = 0.7f) else Color(0xFF535F70)
                                
                                when (selectedSlideIndex) {
                                    0 -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "OMNICREATE DESIGNS\n& GEOLOGIC SYNTHESIS",
                                                color = textColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 20.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("- Research and Creative Presentation -", color = SleekPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    1 -> {
                                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                            Text("I. GPlates Global Drift Phase", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("• Gondwana convergence timeline is verified.", color = subTextColor, fontSize = 10.sp)
                                            Text("• Continental crust longevity plots average lifespans.", color = subTextColor, fontSize = 10.sp)
                                        }
                                    }
                                    2 -> {
                                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                            Text("II. Mapchart Legend & Zones", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("• Seismicity overlays represent crustal sutures.", color = subTextColor, fontSize = 10.sp)
                                            Text("• Mapped boundaries designate educational zones.", color = subTextColor, fontSize = 10.sp)
                                        }
                                    }
                                    3 -> {
                                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                            Text("III. Scalable Vector Symmetry", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("• Multi-band patterns establish state geometries.", color = subTextColor, fontSize = 10.sp)
                                            Text("• SVG markup copyable for vector printout.", color = subTextColor, fontSize = 10.sp)
                                        }
                                    }
                                    else -> {
                                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                            Text("IV. Fractal Geometries & Mathematics", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("• Complex plane pixel mapping formula: Zn+1 = Zn² + C", color = subTextColor, fontSize = 10.sp)
                                            Text("• Infinite recurrence demonstrates physics fractal structures.", color = subTextColor, fontSize = 10.sp)
                                        }
                                    }
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
fun TableAuditLogs() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(8.dp)
        ) {
            Text("Simulation Variable", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 9.sp)
            Text("Symmetry Value", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 9.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        ) {
            Text("Cambrian Rift Ma", modifier = Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp)
            Text("540 Ma", modifier = Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp)
        }
        HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        ) {
            Text("Farallon Subduction", modifier = Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp)
            Text("Active Rate averages", modifier = Modifier.weight(1f), color = Color(0xFF1E293B), fontSize = 9.sp)
        }
    }
}

// 6. WINDOWS MOVIE Maker TIMELINE WORKSPACE
@Composable
fun MovieMakerWorkspace(viewModel: WorkspaceViewModel) {
    val transition by viewModel.movieTransitionType.collectAsState()
    val speed by viewModel.movieSpeedMs.collectAsState()
    val filter by viewModel.movieCinemaFilter.collectAsState()
    val isPlaying by viewModel.isMoviePlaying.collectAsState()
    val activeSlideIdx by viewModel.movieActiveSlideIdx.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "WINDOWS MOVIE MAKER (Cinematic Slideshow)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary
                )
                Text(
                    "Create a cinematic process presentation of your custom layouts with premium transitions and color overlays.",
                    fontSize = 11.sp,
                    color = SleekOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cinematic Presentation Box Overlay depending on chosen Cinema filter (Beautiful 32dp card viewport)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black)
                        .border(4.dp, SleekOutlineVariant, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Frame renderer
                    when (activeSlideIdx) {
                        0 -> Text("🎬 GPLATES PHASE RECONSTRUCTION [FRAME 1]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        1 -> Text("🗺️ CUSTOMIZED WORLD MAPCHART LEGEND [FRAME 2]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        2 -> Text("🛡️ SCALABLE CREATED EMBLEM SYMMETRY [FRAME 3]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        3 -> Text("🌀 MANDELBROT DEEP COMPLEX FRACTALS [FRAME 4]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        else -> Text("✍️ MS PAINT COMPLETED DRAWINGS [FRAME 5]", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Dynamic cinematic color overlay filter
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = when (filter) {
                                    "Technicolor Red" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    "Vintage Sepia" -> Color(0xFFB45309).copy(alpha = 0.18f)
                                    "Teal Tint" -> Color(0xFF06B6D4).copy(alpha = 0.15f)
                                    "Emerald Shadow" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    else -> Color(0xFF8B5CF6).copy(alpha = 0.15f) // Cosmic Emerald
                                }
                            )
                    )

                    // On-screen HUD showing Transition effect state
                    Text(
                        text = "Transition: $transition ($speed ms)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cinema Player controllers Play / Pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.pauseMovieSlideshow() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (!isPlaying) SleekPrimaryContainer else SleekBackground, CircleShape)
                            .border(1.dp, SleekOutline, CircleShape)
                            .testTag("movie_pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause movie",
                            tint = if (!isPlaying) SleekPrimary else SleekOnSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    IconButton(
                        onClick = { viewModel.playMovieSlideshow() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isPlaying) SleekPrimaryContainer else SleekBackground, CircleShape)
                            .border(1.dp, SleekOutline, CircleShape)
                            .testTag("movie_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play movie",
                            tint = if (isPlaying) SleekPrimary else SleekOnSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transition selector options
                Text("Slideshow transition speed limits: $speed ms", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { viewModel.updateMovieSpeed(it.toInt()) },
                    valueRange = 500f..3000f,
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPrimary,
                        activeTrackColor = SleekPrimary,
                        inactiveTrackColor = SleekOutline
                    ),
                    modifier = Modifier.testTag("movie_speed_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val transitionsList = listOf("Crossfade", "Slide In", "Zoom Out", "Fade to Black")
                    transitionsList.forEach { trans ->
                        FilterChip(
                            selected = transition == trans,
                            onClick = { viewModel.updateMovieTransition(trans) },
                            label = { Text(trans, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekBackground,
                                labelColor = SleekOnSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Cinematic Filter Overlay:", fontSize = 11.sp, color = SleekOnBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filtersList = listOf("Technicolor Red", "Vintage Sepia", "Teal Tint", "Emerald Shadow")
                    filtersList.forEach { filt ->
                        FilterChip(
                            selected = filter == filt,
                            onClick = { viewModel.updateMovieCinemaFilter(filt) },
                            label = { Text(filt, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SleekBackground,
                                labelColor = SleekOnSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

// Helpers
fun getGeologicEraName(age: Float): String {
    return when {
         age <= 66f -> "Cenozoic Era / Cretaceous"
         age <= 251f -> "Mesozoic Era / Jurassic-Triassic"
         age <= 541f -> "Paleozoic Era / Permian-Cambrian"
         else -> "Neoproterozoic Era"
    }
}
