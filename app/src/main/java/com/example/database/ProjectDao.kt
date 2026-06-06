package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM workspace_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<WorkspaceProject>>

    @Query("SELECT * FROM workspace_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): WorkspaceProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WorkspaceProject): Long

    @Update
    suspend fun updateProject(project: WorkspaceProject)

    @Delete
    suspend fun deleteProject(project: WorkspaceProject)

    @Query("DELETE FROM workspace_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}
