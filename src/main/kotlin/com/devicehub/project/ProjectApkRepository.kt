package com.devicehub.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectApkRepository : JpaRepository<ProjectApk, Long> {
    fun findAllByProjectIdOrderByUploadedAtDesc(projectId: Long): List<ProjectApk>
    fun findByIdAndProjectId(id: Long, projectId: Long): ProjectApk?
    fun findFirstByProjectIdOrderByUploadedAtDesc(projectId: Long): ProjectApk?
    fun findFirstByProjectIdAndEnvironmentTypeOrderByUploadedAtDesc(
        projectId: Long,
        environmentType: ProjectEnvironmentType,
    ): ProjectApk?
}
