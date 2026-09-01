package com.devicehub.project

import org.springframework.data.jpa.repository.JpaRepository

interface ProjectNetworkRepository : JpaRepository<ProjectNetwork, Long> {
    fun findAllByProjectIdOrderByEnvironmentTypeAsc(projectId: Long): List<ProjectNetwork>
    fun findByIdAndProjectId(id: Long, projectId: Long): ProjectNetwork?
}
