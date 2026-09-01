package com.devicehub.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectKeystoreRepository : JpaRepository<ProjectKeystore, Long> {
    fun findAllByProjectIdOrderByCreatedAtDesc(projectId: Long): List<ProjectKeystore>
    fun findByIdAndProjectId(id: Long, projectId: Long): ProjectKeystore?
}
