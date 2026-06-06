package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_projects")
data class WorkspaceProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // GPlates parameters
    val gplatesAge: Float = 0f, // 0.0 Ma to 542.0 Ma
    
    // Mapchart parameters
    val mapColorsSerialized: String = "", // e.g. "continent_north_america:#FF5722;continent_europe:#4CAF50"
    val mapLegendTitle: String = "Map Legends",
    val mapLegendsSerialized: String = "", // e.g. "#FF5722:Active Tectonic; #4CAF50:Stable Craton"
    
    // Flag structures
    val flagRatio: Float = 1.5f, // e.g. 2:3 ratio
    val flagPatternName: String = "Stripes Vertical",
    val flagPrimaryColor: String = "#0F172A",
    val flagSecondaryColor: String = "#38BDF8",
    val flagTertiaryColor: String = "#F59E0B",
    val flagEmblemType: String = "Star",
    val flagEmblemScale: Float = 0.4f,
    val flagEmblemX: Float = 0.5f,
    val flagEmblemY: Float = 0.5f,
    
    // Fractal parameters
    val fractalX: Double = -0.7,
    val fractalY: Double = 0.0,
    val fractalZoom: Double = 1.2,
    val fractalIterations: Int = 50,
    val fractalColorPalette: String = "Neon Fire",
    
    // Paint canvas data (strokes, shapes representation)
    val paintBrushColor: String = "#E2E8F0",
    val paintBrushSize: Float = 10f,
    val paintShapesCount: Int = 0,
    
    // MS Word / MS PowerPoint formatting
    val reportTitle: String = "Creative Tectonic Report",
    val reportAuthor: String = "Scientific Creator",
    val activePowerpointTheme: String = "Dark Executive",
    
    // Windows Movie Maker configuration
    val movieTransitionType: String = "Crossfade",
    val movieTransitionSpeedMs: Int = 1500,
    val movieCinemaFilter: String = "Technicolor"
)
