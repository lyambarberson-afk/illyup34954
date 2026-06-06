package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.ProjectRepository
import com.example.database.WorkspaceProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

// Representation of a Plate on the GPlates map
data class GPlatesPlate(
    val name: String,
    val initialX: Float,
    val initialY: Float,
    val targetX: Float, // coordinates at 0 Ma (Modern)
    val targetY: Float,
    val sizeWidth: Float,
    val sizeHeight: Float,
    val rotationStart: Float, // rotating at Cambrian (540 Ma)
    val rotationEnd: Float,   // rotation at Modern (0 Ma)
    val color: Color,
    val plateType: String // "Continental" or "Oceanic"
)

// Representation of a Paint Stroke or Shape
data class PaintPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

data class PaintShape(
    val type: String, // "Rectangle", "Circle", "Triangle", "Star", "Heart"
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color
)

// Legend entry for Mapchart
data class MapchartLegend(
    val color: Color,
    val label: String
)

// Plate Longevity model for GPlates stats
data class PlateLongevity(
    val name: String,
    val ageBirth: Float, // in Ma
    val lifespan: Float, // in Ma
    val type: String,    // "Oceanic" or "Continental"
    val region: String
)

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    
    // Loaded Workspace List
    val allProjects: StateFlow<List<WorkspaceProject>>
    
    // Active Loaded Project State
    private val _currentProject = MutableStateFlow<WorkspaceProject?>(null)
    val currentProject: StateFlow<WorkspaceProject?> = _currentProject.asStateFlow()

    // 1. ACTIVE SELECTION / TAB STATE
    private val _activeWorkspaceTab = MutableStateFlow("GPLATES") // GPLATES, MAPCHART, FLAG, FRACTAL, MS_OFFICE, MOVIE
    val activeWorkspaceTab: StateFlow<String> = _activeWorkspaceTab.asStateFlow()

    // 2. G-PLATES STATE
    private val _gplatesAge = MutableStateFlow(0f) // from 0f (Modern) to 540f (Cambrian)
    val gplatesAge: StateFlow<Float> = _gplatesAge.asStateFlow()
    
    val gplatesPlatesList = listOf(
        GPlatesPlate("Laurentia (N. America)", -100f, 60f, -120f, 100f, 130f, 90f, -45f, 0f, Color(0xFFE11D48), "Continental"),
        GPlatesPlate("Gondwana Superplate", 10f, -80f, 60f, -140f, 210f, 130f, 90f, -10f, Color(0xFFF59E0B), "Continental"),
        GPlatesPlate("Baltica Microcontinent", -20f, 40f, 30f, 60f, 70f, 50f, 120f, 12f, Color(0xFF3B82F6), "Continental"),
        GPlatesPlate("Siberian Craton", 60f, 120f, 100f, 110f, 80f, 70f, -90f, 15f, Color(0xFF10B981), "Continental"),
        GPlatesPlate("Indian Plate Crag", -10f, -70f, 80f, 20f, 60f, 50f, 45f, 70f, Color(0xFFD946EF), "Continental"),
        GPlatesPlate("Antarctic Plate Shield", 0f, -140f, 0f, -160f, 140f, 100f, -180f, 0f, Color(0xFF06B6D4), "Continental"),
        GPlatesPlate("Farallon Plate Litho", -160f, 20f, -220f, 10f, 90f, 110f, 25f, -5f, Color(0xFF8B5CF6), "Oceanic"),
        GPlatesPlate("Pacific Plate Oceanic", -240f, -40f, -280f, -30f, 110f, 120f, 0f, 20f, Color(0xFF64748B), "Oceanic")
    )
    
    val geologicalStatsList = listOf(
        PlateLongevity("Panthalassa Oceanic", 540f, 360f, "Oceanic", "Global Pan-Ocean"),
        PlateLongevity("Gondwana Cratonic", 600f, 450f, "Continental", "Southern Hemisphere"),
        PlateLongevity("Baltica Plate Section", 850f, 550f, "Continental", "Proto-Europe"),
        PlateLongevity("Laurentia Shield", 1100f, 800f, "Continental", "Proto-North America"),
        PlateLongevity("Farallon Plate Basin", 350f, 250f, "Oceanic", "Paleo-Pacific"),
        PlateLongevity("Izanagi Fast Plate", 250f, 130f, "Oceanic", "East Asia Margin"),
        PlateLongevity("Tethys Suture Plate", 280f, 190f, "Oceanic", "Equatorial Suture"),
        PlateLongevity("Kula Ancient Craton", 120f, 80f, "Oceanic", "North Pacific Rim")
    )

    private val _selectedStatPlate = MutableStateFlow<PlateLongevity?>(null)
    val selectedStatPlate: StateFlow<PlateLongevity?> = _selectedStatPlate.asStateFlow()

    // 3. MAPCHART STATE
    // Map with custom regional values: e.g. region-id -> Hex Colors
    private val _mapchartColors = MutableStateFlow<Map<String, Color>>(
        mapOf(
            "north_america" to Color(0xFF334155),
            "south_america" to Color(0xFF475569),
            "europe" to Color(0xFF64748B),
            "asia" to Color(0xFF475569),
            "africa" to Color(0xFF334155),
            "australia" to Color(0xFF475569),
            "antarctica" to Color(0xFF94A3B8),
            "greenland" to Color(0xFF64748B)
        )
    )
    val mapchartColors: StateFlow<Map<String, Color>> = _mapchartColors.asStateFlow()
    
    private val _mapchartSubdivisionsEnabled = MutableStateFlow(false)
    val mapchartSubdivisionsEnabled: StateFlow<Boolean> = _mapchartSubdivisionsEnabled.asStateFlow()

    private val _mapchartSubdivisionColors = MutableStateFlow<Map<String, Color>>(
        mapOf(
            "sub_canada" to Color(0xFF3B82F6),
            "sub_usa_east" to Color(0xFF10B981),
            "sub_usa_west" to Color(0xFFEF4444),
            "sub_mexico" to Color(0xFFF59E0B),
            "sub_scandinavia" to Color(0xFF8B5CF6),
            "sub_western_europe" to Color(0xFFEC4899),
            "sub_siberia" to Color(0xFF06B6D4),
            "sub_china" to Color(0xFF14B8A6),
            "sub_india" to Color(0xFFF97316),
            "sub_australia_west" to Color(0xFF6366F1),
            "sub_australia_east" to Color(0xFFD946EF)
        )
    )
    val mapchartSubdivisionColors: StateFlow<Map<String, Color>> = _mapchartSubdivisionColors.asStateFlow()

    private val _activeBrushColor = MutableStateFlow(Color(0xFFE11D48))
    val activeBrushColor: StateFlow<Color> = _activeBrushColor.asStateFlow()

    private val _mapchartLegends = MutableStateFlow(
        listOf(
            MapchartLegend(Color(0xFF3B82F6), "Intense Seismic Activity Zones"),
            MapchartLegend(Color(0xFF10B981), "Orogenic Uplift Belts"),
            MapchartLegend(Color(0xFFEF4444), "Cratonic Shield Base"),
            MapchartLegend(Color(0xFFF59E0B), "Active Sedimentary Margins")
        )
    )
    val mapchartLegends: StateFlow<List<MapchartLegend>> = _mapchartLegends.asStateFlow()

    // 4. FLAG CREATOR STATE
    private val _flagRatio = MutableStateFlow(1.5f) // width height scale
    val flagRatio: StateFlow<Float> = _flagRatio.asStateFlow()

    private val _flagPattern = MutableStateFlow("Stripes Vertical") // Stripes Vertical, Stripes Horizontal, Tricolor, Nordic Cross, Canton Corner, Diamond Saltire
    val flagPattern: StateFlow<String> = _flagPattern.asStateFlow()

    private val _flagPrimaryColor = MutableStateFlow(Color(0xFF0F172A))
    val flagPrimaryColor: StateFlow<Color> = _flagPrimaryColor.asStateFlow()

    private val _flagSecondaryColor = MutableStateFlow(Color(0xFF38BDF8))
    val flagSecondaryColor: StateFlow<Color> = _flagSecondaryColor.asStateFlow()

    private val _flagTertiaryColor = MutableStateFlow(Color(0xFFF59E0B))
    val flagTertiaryColor: StateFlow<Color> = _flagTertiaryColor.asStateFlow()

    private val _flagEmblemType = MutableStateFlow("Star") // Star, Crescent, Golden Sun, Crest Shield, Laurel Wreath, None
    val flagEmblemType: StateFlow<String> = _flagEmblemType.asStateFlow()

    private val _flagEmblemScale = MutableStateFlow(0.4f)
    val flagEmblemScale: StateFlow<Float> = _flagEmblemScale.asStateFlow()

    private val _flagEmblemX = MutableStateFlow(0.5f)
    val flagEmblemX: StateFlow<Float> = _flagEmblemX.asStateFlow()

    private val _flagEmblemY = MutableStateFlow(0.5f)
    val flagEmblemY: StateFlow<Float> = _flagEmblemY.asStateFlow()

    // 5. MATH FRACTAL CREATIVE STATE
    private val _fractalCenterX = MutableStateFlow(-0.75)
    val fractalCenterX: StateFlow<Double> = _fractalCenterX.asStateFlow()

    private val _fractalCenterY = MutableStateFlow(0.0)
    val fractalCenterY: StateFlow<Double> = _fractalCenterY.asStateFlow()

    private val _fractalZoom = MutableStateFlow(1.2)
    val fractalZoom: StateFlow<Double> = _fractalZoom.asStateFlow()

    private val _fractalMaxIterations = MutableStateFlow(45)
    val fractalMaxIterations: StateFlow<Int> = _fractalMaxIterations.asStateFlow()

    private val _fractalPaletteName = MutableStateFlow("Neon Fire") // Neon Fire, Deep Deep Ocean, Emerald Forest, Cosmic Grayscale
    val fractalPaletteName: StateFlow<String> = _fractalPaletteName.asStateFlow()

    private val _fractalBitmap = MutableStateFlow<ImageBitmap?>(null)
    val fractalBitmap: StateFlow<ImageBitmap?> = _fractalBitmap.asStateFlow()

    private val _isFractalCalculating = MutableStateFlow(false)
    val isFractalCalculating: StateFlow<Boolean> = _isFractalCalculating.asStateFlow()

    // 6. MS PAINT & WORD & PPT SLIDES LAYOUTS
    // Paint details
    private val _paintPaths = MutableStateFlow<List<PaintPath>>(emptyList())
    val paintPaths: StateFlow<List<PaintPath>> = _paintPaths.asStateFlow()

    private val _paintShapes = MutableStateFlow<List<PaintShape>>(emptyList())
    val paintShapes: StateFlow<List<PaintShape>> = _paintShapes.asStateFlow()

    private val _activePaintTool = MutableStateFlow("BRUSH") // BRUSH, ERASER, SHAPE_RECT, SHAPE_CIRCLE, SHAPE_TRIANGLE, SHAPE_STAR
    val activePaintTool: StateFlow<String> = _activePaintTool.asStateFlow()

    private val _paintBrushSize = MutableStateFlow(12f)
    val paintBrushSize: StateFlow<Float> = _paintBrushSize.asStateFlow()

    private val _paintBrushColorNum = MutableStateFlow(Color(0xFF38BDF8))
    val paintBrushColorNum: StateFlow<Color> = _paintBrushColorNum.asStateFlow()

    // Word report note editable
    private val _wordReportDesc = MutableStateFlow(
        "OMNICREATE RESEARCH REPORT PROTOCOL\n\n" +
        "Section I: Plates Tectonics and Lithospheric drift simulations reconstruct the Gondwana supercontinent rift phases. Boundary margins verify oceanic Farallon subduction rate averages.\n\n" +
        "Section II: Thematic maps visualize dynamic cratonic segments. Custom layout configurations highlight educational study locations.\n\n" +
        "Section III: Creative design aspects focus on flag geometries. The infinite mathematical fractals exemplify architectural and physical symmetries in nature."
    )
    val wordReportDesc: StateFlow<String> = _wordReportDesc.asStateFlow()

    // PowerPoint
    private val _activePowerpointTheme = MutableStateFlow("Dark Executive") // Dark Executive, Clean Academic, Modern Creative
    val activePowerpointTheme: StateFlow<String> = _activePowerpointTheme.asStateFlow()

    private val _selectedPowerpointSlideIndex = MutableStateFlow(0) // 0 to 4
    val selectedPowerpointSlideIndex: StateFlow<Int> = _selectedPowerpointSlideIndex.asStateFlow()

    // 7. WINDOWS MOVIE Maker TIMELINE CONFIGS
    private val _movieTransitionType = MutableStateFlow("Crossfade") // Crossfade, Slide In, Zoom Out, Fade to Black, Wipe Left
    val movieTransitionType: StateFlow<String> = _movieTransitionType.asStateFlow()

    private val _movieSpeedMs = MutableStateFlow(1500) // 500 to 3000 ms
    val movieSpeedMs: StateFlow<Int> = _movieSpeedMs.asStateFlow()

    private val _movieCinemaFilter = MutableStateFlow("Technicolor Red") // Technicolor Red, Vintage Sepia, Teal Tint, Emerald Shadow, Cosmic Emerald
    val movieCinemaFilter: StateFlow<String> = _movieCinemaFilter.asStateFlow()

    private val _isMoviePlaying = MutableStateFlow(false)
    val isMoviePlaying: StateFlow<Boolean> = _isMoviePlaying.asStateFlow()

    private val _movieActiveSlideIdx = MutableStateFlow(0)
    val movieActiveSlideIdx: StateFlow<Int> = _movieActiveSlideIdx.asStateFlow()

    // 8. CLOUD SYNC & VECTOR FORMATS SYNCING
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncProgress = MutableStateFlow(0f)
    val cloudSyncProgress: StateFlow<Float> = _cloudSyncProgress.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Local storage up-to-date")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao())
        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Render initial fractal instantly on load
        triggerFractalRecalculation()
    }

    // TAB SELECTION CONTROL
    fun switchTab(tab: String) {
        _activeWorkspaceTab.value = tab
    }

    // GPLATES CONTROLS
    fun updateGPlatesAge(newAge: Float) {
        _gplatesAge.value = newAge
    }
    
    fun selectStatPlate(plate: PlateLongevity?) {
        _selectedStatPlate.value = plate
    }

    // MAPCHART CONTROLS
    fun updateMapRegionColor(regionId: String, color: Color) {
        val currentMap = _mapchartColors.value.toMutableMap()
        currentMap[regionId] = color
        _mapchartColors.value = currentMap
    }

    fun updateSubdivisionColor(subId: String, color: Color) {
        val currentMap = _mapchartSubdivisionColors.value.toMutableMap()
        currentMap[subId] = color
        _mapchartSubdivisionColors.value = currentMap
    }

    fun toggleSubdivisions(enabled: Boolean) {
        _mapchartSubdivisionsEnabled.value = enabled
    }

    fun addMapchartLegend(color: Color, description: String) {
        if (description.isNotBlank()) {
            val currentList = _mapchartLegends.value.toMutableList()
            currentList.add(MapchartLegend(color, description))
            _mapchartLegends.value = currentList
        }
    }

    fun removeMapchartLegend(index: Int) {
        val currentList = _mapchartLegends.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _mapchartLegends.value = currentList
        }
    }

    // FLAG CREATOR CONTROLS
    fun updateFlagRatio(ratio: Float) { _flagRatio.value = ratio }
    fun updateFlagPattern(pattern: String) { _flagPattern.value = pattern }
    fun updateFlagPrimaryColor(color: Color) { _flagPrimaryColor.value = color }
    fun updateFlagSecondaryColor(color: Color) { _flagSecondaryColor.value = color }
    fun updateFlagTertiaryColor(color: Color) { _flagTertiaryColor.value = color }
    fun updateFlagEmblemType(type: String) { _flagEmblemType.value = type }
    fun updateFlagEmblemScale(scale: Float) { _flagEmblemScale.value = scale }
    fun updateFlagEmblemX(x: Float) { _flagEmblemX.value = x }
    fun updateFlagEmblemY(y: Float) { _flagEmblemY.value = y }

    // RENDER Flag design to Vector SVG String format representation
    fun generateVectorSvgString(): String {
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 300 ${300 / _flagRatio.value}\" width=\"100%\" height=\"100%\">\n")
            append("  <!-- Background / Base Base Structure -->\n")
            val w = 300f
            val h = 300f / _flagRatio.value
            val primaryHex = toHexColor(_flagPrimaryColor.value)
            val secondaryHex = toHexColor(_flagSecondaryColor.value)
            val tertiaryHex = toHexColor(_flagTertiaryColor.value)
            
            when (_flagPattern.value) {
                "Stripes Vertical" -> {
                    append("  <rect width=\"${w/3}\" height=\"$h\" fill=\"$primaryHex\" />\n")
                    append("  <rect x=\"${w/3}\" width=\"${w/3}\" height=\"$h\" fill=\"$secondaryHex\" />\n")
                    append("  <rect x=\"${2*w/3}\" width=\"${w/3}\" height=\"$h\" fill=\"$tertiaryHex\" />\n")
                }
                "Stripes Horizontal" -> {
                    append("  <rect width=\"$w\" height=\"${h/3}\" fill=\"$primaryHex\" />\n")
                    append("  <rect y=\"${h/3}\" width=\"$w\" height=\"${h/3}\" fill=\"$secondaryHex\" />\n")
                    append("  <rect y=\"${2*h/3}\" width=\"$w\" height=\"${h/3}\" fill=\"$tertiaryHex\" />\n")
                }
                "Tricolor" -> {
                    append("  <rect width=\"$w\" height=\"$h\" fill=\"$primaryHex\" />\n")
                    append("  <polygon points=\"0,0 $w,${h/2} 0,$h\" fill=\"$secondaryHex\" />\n")
                    append("  <circle cx=\"${w/2}\" cy=\"${h/2}\" r=\"${h/4}\" fill=\"$tertiaryHex\" />\n")
                }
                "Nordic Cross" -> {
                    append("  <rect width=\"$w\" height=\"$h\" fill=\"$primaryHex\" />\n")
                    append("  <!-- Horizontal cross strip -->\n")
                    append("  <rect y=\"${h*0.35f}\" width=\"$w\" height=\"${h*0.3f}\" fill=\"$secondaryHex\" />\n")
                    append("  <!-- Vertical cross strip -->\n")
                    append("  <rect x=\"${w*0.35f}\" width=\"${w*0.15f}\" height=\"$h\" fill=\"$secondaryHex\" />\n")
                }
                "Canton Corner" -> {
                    append("  <rect width=\"$w\" height=\"$h\" fill=\"$primaryHex\" />\n")
                    append("  <rect width=\"${w*0.45f}\" height=\"${h*0.5f}\" fill=\"$secondaryHex\" />\n")
                    append("  <rect x=\"${w*0.45f}\" width=\"${w*0.55f}\" height=\"10\" fill=\"$tertiaryHex\" />\n")
                }
                else -> {
                    append("  <rect width=\"$w\" height=\"$h\" fill=\"$primaryHex\" />\n")
                    append("  <line x1=\"0\" y1=\"0\" x2=\"$w\" y2=\"$h\" stroke=\"$secondaryHex\" stroke-width=\"16\" />\n")
                    append("  <line x1=\"$w\" y1=\"0\" x2=\"0\" y2=\"$h\" stroke=\"$secondaryHex\" stroke-width=\"16\" />\n")
                }
            }
            
            // Add Emblem if present
            if (_flagEmblemType.value != "None") {
                val embX = w * _flagEmblemX.value
                val embY = h * _flagEmblemY.value
                val embRad = (h * 0.25f) * _flagEmblemScale.value
                append("  <!-- Flag emblem: ${_flagEmblemType.value} -->\n")
                append("  <circle cx=\"$embX\" cy=\"$embY\" r=\"$embRad\" fill=\"$tertiaryHex\" />\n")
            }
            append("</svg>")
        }
    }

    private fun toHexColor(color: Color): String {
        return "#" + Integer.toHexString(color.hashCode()).substring(2).uppercase()
    }

    // FRACTAL ENGINE CONTROLS WITH MULTI-THREADED RENDERING
    fun updateFractalSettings(zoom: Double, centerX: Double, centerY: Double, iterations: Int, palette: String) {
        _fractalZoom.value = zoom
        _fractalCenterX.value = centerX
        _fractalCenterY.value = centerY
        _fractalMaxIterations.value = iterations
        _fractalPaletteName.value = palette
        triggerFractalRecalculation()
    }

    fun triggerFractalRecalculation() {
        viewModelScope.launch {
            _isFractalCalculating.value = true
            val cx = _fractalCenterX.value
            val cy = _fractalCenterY.value
            val z = _fractalZoom.value
            val maxIter = _fractalMaxIterations.value
            val palette = _fractalPaletteName.value

            val renderedBmp = withContext(Dispatchers.Default) {
                renderFractalBackground(cx, cy, z, maxIter, palette)
            }
            if (renderedBmp != null) {
                _fractalBitmap.value = renderedBmp.asImageBitmap()
            }
            _isFractalCalculating.value = false
        }
    }

    // Generates high fidelity Mandelbrot Fractal dynamically
    private fun renderFractalBackground(centerX: Double, centerY: Double, zoom: Double, maxIter: Int, palette: String): Bitmap? {
        val w = 180
        val h = 180
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        
        for (py in 0 until h) {
            for (px in 0 until w) {
                // map pixels to complex coordinates
                val zReal = (px - w / 2.0) * (3.5 / (w * zoom)) + centerX
                val zImag = (py - h / 2.0) * (2.0 / (h * zoom)) + centerY
                
                var cR = zReal
                var cI = zImag
                var n = 0
                while (n < maxIter) {
                    val r2 = cR * cR
                    val i2 = cI * cI
                    if (r2 + i2 > 4.0) break
                    cI = 2.0 * cR * cI + zImag
                    cR = r2 - i2 + zReal
                    n++
                }

                val colorVal = if (n == maxIter) {
                    AndroidColor.BLACK
                } else {
                    getFractalPaletteColor(n, maxIter, palette)
                }
                bmp.setPixel(px, py, colorVal)
            }
        }
        return bmp
    }

    private fun getFractalPaletteColor(n: Int, maxIter: Int, palette: String): Int {
        val value = n.toFloat() / maxIter.toFloat()
        return when (palette) {
            "Neon Fire" -> {
                val red = (value * 255).toInt()
                val green = ((value * value) * 120).toInt()
                AndroidColor.rgb(red, green, 0)
            }
            "Deep Deep Ocean" -> {
                val blue = (value * 255).toInt()
                val green = ((1.0 - value) * 80).toInt()
                val red = (value * value * 40).toInt()
                AndroidColor.rgb(red, green, blue)
            }
            "Emerald Forest" -> {
                val green = (value * 255).toInt()
                val red = ((1.0 - value) * 60).toInt()
                val blue = (value * 120).toInt()
                AndroidColor.rgb(red, green, blue)
            }
            else -> { // Cosmic Grayscale
                val gray = (value * 255).toInt()
                AndroidColor.rgb(gray, gray, gray)
            }
        }
    }

    // PAINT REPO & CONTROLS
    fun updatePaintTool(tool: String) { _activePaintTool.value = tool }
    fun updatePaintSize(size: Float) { _paintBrushSize.value = size }
    fun updatePaintColor(color: Color) { _paintBrushColorNum.value = color }

    fun addStrokePath(stroke: PaintPath) {
        val current = _paintPaths.value.toMutableList()
        current.add(stroke)
        _paintPaths.value = current
    }

    fun addPaintShape(shape: PaintShape) {
        val current = _paintShapes.value.toMutableList()
        current.add(shape)
        _paintShapes.value = current
    }

    fun clearPaintCanvas() {
        _paintPaths.value = emptyList()
        _paintShapes.value = emptyList()
    }

    // WORD NOTE CONTROLS
    fun updateWordReport(text: String) {
        _wordReportDesc.value = text
    }

    // POWERPOINT ACTION CONTROLS
    fun updatePowerpointTheme(theme: String) { _activePowerpointTheme.value = theme }
    fun selectPowerpointSlide(index: Int) { _selectedPowerpointSlideIndex.value = index }

    // MOVIE MAKER CINEMATIC ACTIONS
    fun playMovieSlideshow() {
        if (_isMoviePlaying.value) return
        _isMoviePlaying.value = true
        _movieActiveSlideIdx.value = 0
        
        viewModelScope.launch {
            while (_isMoviePlaying.value) {
                kotlinx.coroutines.delay(_movieSpeedMs.value.toLong())
                if (!_isMoviePlaying.value) break
                val nextIdx = (_movieActiveSlideIdx.value + 1) % 5
                _movieActiveSlideIdx.value = nextIdx
            }
        }
    }

    fun pauseMovieSlideshow() {
        _isMoviePlaying.value = false
    }

    fun updateMovieTransition(transition: String) { _movieTransitionType.value = transition }
    fun updateMovieSpeed(ms: Int) { _movieSpeedMs.value = ms }
    fun updateMovieCinemaFilter(filter: String) { _movieCinemaFilter.value = filter }

    // CLOUD SYNC SIMULATOR ACTION
    fun syncWithCloud() {
        if (_isCloudSyncing.value) return
        _isCloudSyncing.value = true
        _cloudSyncStatus.value = "Establishing handshake..."
        viewModelScope.launch {
            for (i in 1..100 step 15) {
                kotlinx.coroutines.delay(180)
                _cloudSyncProgress.value = i.toFloat() / 100f
                _cloudSyncStatus.value = "Synchronizing files to OneDrive/Google Drive ($i%)..."
            }
            _cloudSyncProgress.value = 1f
            _cloudSyncStatus.value = "Successfully synchronized all creator assets!"
            kotlinx.coroutines.delay(1000)
            _isCloudSyncing.value = false
        }
    }

    // DATABASE SAVE & LOAD WORKSPACES FOR SEAMLESS DOCUMENT REPOSITORY
    fun saveWorkspaceProjectToDb(name: String) {
        viewModelScope.launch {
            // Serialize Map colors
            val mapColsStr = _mapchartColors.value.entries.joinToString(";") { "${it.key}:${toHexColor(it.value)}" }
            val mapLegsStr = _mapchartLegends.value.joinToString(";") { "${toHexColor(it.color)}:${it.label}" }

            val project = WorkspaceProject(
                name = name,
                timestamp = System.currentTimeMillis(),
                gplatesAge = _gplatesAge.value,
                mapColorsSerialized = mapColsStr,
                mapLegendsSerialized = mapLegsStr,
                
                flagRatio = _flagRatio.value,
                flagPatternName = _flagPattern.value,
                flagPrimaryColor = toHexColor(_flagPrimaryColor.value),
                flagSecondaryColor = toHexColor(_flagSecondaryColor.value),
                flagTertiaryColor = toHexColor(_flagTertiaryColor.value),
                flagEmblemType = _flagEmblemType.value,
                flagEmblemScale = _flagEmblemScale.value,
                flagEmblemX = _flagEmblemX.value,
                flagEmblemY = _flagEmblemY.value,
                
                fractalX = _fractalCenterX.value,
                fractalY = _fractalCenterY.value,
                fractalZoom = _fractalZoom.value,
                fractalIterations = _fractalMaxIterations.value,
                fractalColorPalette = _fractalPaletteName.value,
                
                paintShapesCount = _paintShapes.value.size,
                reportTitle = "Synthesized Geologic Document - $name",
                reportAuthor = "Digital Creator Pro",
                activePowerpointTheme = _activePowerpointTheme.value,
                movieTransitionType = _movieTransitionType.value,
                movieTransitionSpeedMs = _movieSpeedMs.value,
                movieCinemaFilter = _movieCinemaFilter.value
            )
            val savedId = repository.insertProject(project)
            val savedProj = project.copy(id = savedId.toInt())
            _currentProject.value = savedProj
        }
    }

    fun loadWorkspaceProjectFromDb(project: WorkspaceProject) {
        viewModelScope.launch {
            _currentProject.value = project
            _gplatesAge.value = project.gplatesAge
            _flagRatio.value = project.flagRatio
            _flagPattern.value = project.flagPatternName
            _flagPrimaryColor.value = parseColorString(project.flagPrimaryColor, Color(0xFF0F172A))
            _flagSecondaryColor.value = parseColorString(project.flagSecondaryColor, Color(0xFF38BDF8))
            _flagTertiaryColor.value = parseColorString(project.flagTertiaryColor, Color(0xFFF59E0B))
            _flagEmblemType.value = project.flagEmblemType
            _flagEmblemScale.value = project.flagEmblemScale
            _flagEmblemX.value = project.flagEmblemX
            _flagEmblemY.value = project.flagEmblemY

            _fractalCenterX.value = project.fractalX
            _fractalCenterY.value = project.fractalY
            _fractalZoom.value = project.fractalZoom
            _fractalMaxIterations.value = project.fractalIterations
            _fractalPaletteName.value = project.fractalColorPalette
            triggerFractalRecalculation()

            _activePowerpointTheme.value = project.activePowerpointTheme
            _movieTransitionType.value = project.movieTransitionType
            _movieSpeedMs.value = project.movieTransitionSpeedMs
            _movieCinemaFilter.value = project.movieCinemaFilter

            // Deserialize Map
            if (project.mapColorsSerialized.isNotEmpty()) {
                val newMap = mutableMapOf<String, Color>()
                project.mapColorsSerialized.split(";").forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        newMap[parts[0]] = parseColorString(parts[1], Color(0xFF64748B))
                    }
                }
                if (newMap.isNotEmpty()) {
                    _mapchartColors.value = newMap
                }
            }

            // Deserialize Map legends
            if (project.mapLegendsSerialized.isNotEmpty()) {
                val newLegs = mutableListOf<MapchartLegend>()
                project.mapLegendsSerialized.split(";").forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        newLegs.add(MapchartLegend(parseColorString(parts[0], Color.White), parts[1]))
                    }
                }
                _mapchartLegends.value = newLegs
            }
        }
    }

    fun deleteWorkspaceProject(project: WorkspaceProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
            }
        }
    }

    private fun parseColorString(hex: String, fallback: Color): Color {
        return try {
            if (hex.startsWith("#")) {
                Color(AndroidColor.parseColor(hex))
            } else fallback
        } catch (e: Exception) {
            fallback
        }
    }
}
