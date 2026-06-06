package com.example.database

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<WorkspaceProject>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): WorkspaceProject? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: WorkspaceProject): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: WorkspaceProject) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: WorkspaceProject) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteProjectById(id: Int) {
        projectDao.deleteProjectById(id)
    }
}
